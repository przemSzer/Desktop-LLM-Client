package dev.local.ai.ui.chat.viewmodel;

import dev.langchain4j.memory.ChatMemory;
import dev.local.ai.core.chat.IChatListener;
import dev.local.ai.core.chat.ILLMChat;
import dev.local.ai.core.chat.messages.Message;
import dev.local.ai.core.chat.messages.MessageType;
import dev.local.ai.core.chat.streaming.IPartialMessageAware;
import dev.local.ai.core.chat.streaming.IPartialMessagesListener;
import dev.local.ai.core.chat.streaming.MessageToChatMessageConverter;
import dev.local.ai.core.chat.streaming.StopRequestEvent;
import dev.local.ai.core.documents.DocumentDescription;
import dev.local.ai.core.events.CoreEventBus;
import dev.local.ai.core.models.LLMInfoAndConnection;
import dev.local.ai.core.storage.conversations.ConversationStore;
import dev.local.ai.core.storage.conversations.ConversationSummariesListener;
import dev.local.ai.core.storage.conversations.ConversationSummary;
import dev.local.ai.ui.chat.command.ClearChatCommand;
import dev.local.ai.ui.chat.command.SendUserMessageToLLMCommand;
import dev.local.ai.ui.chat.converters.MessageConverter;
import dev.local.ai.ui.chat.session.ChatSession;
import dev.local.ai.ui.chat.session.ConversationSessionFactory;
import dev.local.ai.ui.commands.CommandManager;
import dev.local.ai.ui.files.viewmodel.AttachedFileViewModel;
import dev.local.ai.ui.files.viewmodel.FileStatus;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * ViewModel for the Chat UI following MVVM pattern.
 * Manages the observable data and commands for the chat interface.
 */
public class ChatViewModel implements IChatListener, IPartialMessagesListener {

    private static final Logger logger = LoggerFactory.getLogger(ChatViewModel.class);

    private final ListProperty<ChatMessageViewModel> chatMessages;
    private final SimpleStringProperty systemMessage;
    private final StringProperty inputMessage;
    private final BooleanProperty canUndo;
    private final BooleanProperty canRedo;
    private final ListProperty<AttachedFileViewModel> attachedFiles;
    private final ListProperty<AttachedFileViewModel> systemMessageAttachedFiles;
    private final BooleanProperty sendingMessageInProgress;
    private final ObjectProperty<LLMInfoAndConnection> selectedModelProperty;
    private final StringProperty currentConversationId;
    private final StringProperty currentConversationTitle;

    private ChatSession session;
    private final ConversationSessionFactory sessionFactory;
    private final ConversationStore conversationStore;
    private final CommandManager commandManager;
    private final CoreEventBus eventBus;

    private final MessageConverter messageConverter;
    private final PauseTransition textChangedDebouncer = new PauseTransition(Duration.millis(500));

    private final ConversationSummariesListener conversationSummariesListener =
            summaries -> Platform.runLater(this::refreshConversationTitleFromStore);

    public ChatViewModel(ChatSession session,
                         ConversationSessionFactory sessionFactory,
                         ConversationStore conversationStore,
                         CommandManager commandManager,
                         CoreEventBus eventBus) {
        this.session = session;
        this.sessionFactory = sessionFactory;
        this.conversationStore = conversationStore;
        this.commandManager = commandManager;
        this.eventBus = eventBus;
        this.messageConverter = new MessageConverter();
        this.systemMessage = new SimpleStringProperty(session.chat().getSystemMessage());

        this.chatMessages = new SimpleListProperty<>(FXCollections.observableArrayList());
        this.inputMessage = new SimpleStringProperty("");
        this.attachedFiles = new SimpleListProperty<>(FXCollections.observableArrayList());
        this.systemMessageAttachedFiles = new SimpleListProperty<>(FXCollections.observableArrayList());
        this.canUndo = new SimpleBooleanProperty(false);
        this.canRedo = new SimpleBooleanProperty(false);
        this.sendingMessageInProgress = new SimpleBooleanProperty(false);
        this.selectedModelProperty = new SimpleObjectProperty<>(null);
        this.currentConversationId = new SimpleStringProperty(session.conversationId());
        this.currentConversationTitle = new SimpleStringProperty("New conversation");
        attachToSession(session);
        rehydrateFromChatMemory(session.chatMemory());

        setupCommandBindings();
        setupPropertyBindings();

        refreshConversationTitleFromStore();

        conversationStore.addConversationSummariesListener(conversationSummariesListener);

        logger.info("ChatViewModel initialized for conversation {}", session.conversationId());
    }

    private void attachToSession(ChatSession session) {
        var chat = session.chat();
        chat.setCallback(this);
        if (chat instanceof IPartialMessageAware partialMessageAware) {
            partialMessageAware.setPartialMessageListener(this);
        }
    }

    private ILLMChat currentChat() {
        return session.chat();
    }

    private void rehydrateFromChatMemory(ChatMemory chatMemory) {
        var converter = new MessageConverter();
        //TODO: conversion can be done outside ui thread
        for (var chatMessage : chatMemory.messages()) {
            var coreMessage = MessageToChatMessageConverter.toCoreMessage(chatMessage);
            if (coreMessage == null) {
                continue;
            }
            converter.convert(coreMessage).ifPresent(chatMessages::add);
        }
        if (!chatMemory.messages().isEmpty()) {
            logger.info("Restored {} messages from conversation {}",
                    chatMemory.messages().size(), session.conversationId());
        }
    }
    
    public void loadConversation(String conversationId) {
        if (sendingMessageInProgress.get()) {
            logger.warn("Refusing to switch conversation while a message is in progress");
            return;
        }
        if (session != null && Objects.equals(session.conversationId(), conversationId)) {
            logger.debug("Conversation {} is already active; nothing to load", conversationId);
            return;
        }
        ChatSession newSession = sessionFactory.openConversation(conversationId);
        ChatSession previous = session;
        session = newSession;
        attachToSession(newSession);
        Platform.runLater(() -> {
            chatMessages.clear();
            rehydrateFromChatMemory(newSession.chatMemory());
            systemMessage.set(newSession.chat().getSystemMessage());
            systemMessageAttachedFiles.clear();
            attachedFiles.clear();
            inputMessage.set("");
            currentConversationId.set(newSession.conversationId());
            refreshConversationTitleFromStore();
        });
        if (previous != null) {
            previous.close();
        }
        logger.info("Switched to conversation {}", conversationId);
    }
    

    private void setupPropertyBindings() {                
        systemMessage.addListener((obs, oldVal, newVal) -> {
            textChangedDebouncer.setOnFinished(event -> updateSystemMessage());
            textChangedDebouncer.playFromStart();
        });
        systemMessageAttachedFiles.addListener((obs, oldVal, newVal) -> {
            for (var file : newVal) {
                file.descriptionProperty().addListener((obs1, oldVal1, newVal1) -> updateSystemMessage());
            }
            updateSystemMessage();
        });
    }

    private void updateSystemMessage() {
        var files = systemMessageAttachedFiles
            .get()
            .stream()
            .filter(f -> f.getStatus() == FileStatus.VALID)
            .map(AttachedFileViewModel::getDescription)
            .filter(Objects::nonNull)
            .toList();
        var newSystemMessage = new Message(
                systemMessage.get(), 
                files,
                dev.local.ai.core.chat.messages.MessageType.SYSTEM
            );
        currentChat().setSystemMessage(newSystemMessage);
    }

    private void setupCommandBindings() {
        // Update undo/redo state when commands are executed
        canUndo.bind(Bindings.createBooleanBinding(
                commandManager::canUndo,
                chatMessages));

        canRedo.bind(Bindings.createBooleanBinding(
                commandManager::canRedo,
                chatMessages));
    }

    public ObjectProperty<LLMInfoAndConnection> selectedModelProperty() {
        return selectedModelProperty;
    }

    public StringProperty currentConversationIdProperty() {
        return currentConversationId;
    }

    public String getCurrentConversationId() {
        return currentConversationId.get();
    }

    public StringProperty currentConversationTitleProperty() {
        return currentConversationTitle;
    }

    public String getCurrentConversationTitle() {
        return currentConversationTitle.get();
    }

    public void refreshConversationTitle() {
        refreshConversationTitleFromStore();
    }

    private void refreshConversationTitleFromStore() {
        String id = session != null ? session.conversationId() : getCurrentConversationId();
        Optional<ConversationSummary> summary = conversationStore.findSummary(id);
        String title = summary.map(this::titleForSummary).orElse("New conversation");
        currentConversationTitle.set(title);
    }

    private String titleForSummary(ConversationSummary summary) {
        String title = summary.title();
        if (title == null || title.isBlank()) {
            return "New conversation";
        }
        return title;
    }

    // Properties for data binding
    public ListProperty<ChatMessageViewModel> chatMessagesProperty() {
        return chatMessages;
    }

    public ObservableList<ChatMessageViewModel> getChatMessages() {
        return chatMessages;
    }

    public StringProperty inputMessageProperty() {
        return inputMessage;
    }

    public String getInputMessage() {
        return inputMessage.get();
    }

    public void setInputMessage(String message) {
        inputMessage.set(message);
    }

    public StringProperty systemMessageProperty() {
        return systemMessage;
    }

    public String getSystemMessage() {
        return systemMessage.get();
    }

    public void setSystemMessage(String message) {
        systemMessage.set(message);
    }

    public BooleanProperty canUndoProperty() {
        return canUndo;
    }

    public BooleanProperty canRedoProperty() {
        return canRedo;
    }

    public ListProperty<AttachedFileViewModel> attachedFilesProperty() {
        return attachedFiles;
    }

    public ListProperty<AttachedFileViewModel> systemMessageAttachedFilesProperty() {
        return systemMessageAttachedFiles;
    }

    public BooleanProperty sendingMessageInProgressProperty() {
        return sendingMessageInProgress;
    }

    // Commands
    public void sendMessage() {
        String message = getInputMessage().trim();
        List<DocumentDescription> files = attachedFiles.get().stream()
            .filter(f -> f.getStatus() == FileStatus.VALID)
            .map(AttachedFileViewModel::getDescription)            
            .toList();
        attachedFiles.clear();
        if (message.isEmpty()) {
            logger.debug("Empty message ignored");
            return;
        }

        try {
            setInputMessage("");

            var command = new SendUserMessageToLLMCommand(currentChat(), message, files);
            sendingMessageInProgress.set(true);
            var execution = commandManager.executeCommandAsync(command);
            execution.thenAccept(result -> {                
                if (result) {
                    logger.info("SendMessageCommand executed successfully: {}", message);
                } else {
                    logger.error("SendMessageCommand failed: {}", message);
                    Platform.runLater(() -> sendingMessageInProgress.set(false));
                }                
            });

        } catch (Exception e) {
            logger.error("Failed to send message: {}", message, e);
        }
    }

    public void undo() {
        if (commandManager.canUndo()) {
            boolean success = commandManager.undo();
            if (success) {
                logger.info("Command undone successfully");
            } else {
                logger.warn("Failed to undo command");
            }
        }
    }

    public void redo() {
        if (commandManager.canRedo()) {
            boolean success = commandManager.redo();
            if (success) {
                logger.info("Command redone successfully");
            } else {
                logger.warn("Failed to redo command");
            }
        }
    }

    public void clearChat() {
        try {
            ClearChatCommand command = new ClearChatCommand(currentChat());
            boolean success = commandManager.executeCommand(command);

            if (success) {
                logger.info("ClearChatCommand executed successfully");
            } else {
                logger.error("ClearChatCommand failed");
            }

        } catch (Exception e) {
            logger.error("Failed to clear chat", e);
        }
    }

    private void addMessage(ChatMessageViewModel message) {
        chatMessages.add(message);
        logger.debug("Message added to view model: {}", message);
    }

    public int getMessageCount() {
        return currentChat().getMessageCount();
    }

    // ChatCallback implementation
    @Override
    public void onMessageAdded(Message message, String requestId) {
        logger.debug("Message added to view model: {}", message);
        final var newChatMessageMaybe = messageConverter.convert(message);
        if (newChatMessageMaybe.isEmpty()) {
            logger.warn("Message converter returned empty optional for message: {}", message);
            return;
        }
        final var newChatMessage = newChatMessageMaybe.get();
        Platform.runLater(() -> {
            if (chatMessages.isEmpty()) {
                chatMessages.add(newChatMessage);
            }else {
                var lastMessage = chatMessages.getLast();
                boolean shouldReplaceLastMessage = newChatMessage.getType() == MessageTypeView.AI
                        && lastMessage != null
                        && lastMessage.getType() == MessageTypeView.PARTIAL_AI;
                if (shouldReplaceLastMessage) {
                    logger.debug("Replacing last message with new message: {}", newChatMessage.getContent());
                    chatMessages.set(chatMessages.size() - 1, newChatMessage);
                } else {
                    chatMessages.add(newChatMessage);
                }
            }

            if (newChatMessage.getType() == MessageTypeView.USER) {
                refreshConversationTitleFromStore();
            } else if (newChatMessage.getType() == MessageTypeView.AI) {
                sendingMessageInProgress.set(false);
            }

            var thinkingMessageFromThisRequest = findMessageExistingMessageByTypeAndId(
                    MessageTypeView.PARTIAL_THINKING, requestId
            ) ;
            if (thinkingMessageFromThisRequest != null) {
                logger.debug("Marking think message from request {} as complete", requestId);
                thinkingMessageFromThisRequest.isCompleteProperty().set(true);
            }
        });
    }
   

    @Override
    public void onError(String errorMessage, Exception exception) {
        Platform.runLater(() -> {
            ChatMessageViewModel errorMsg = new ChatMessageViewModel(errorMessage, MessageTypeView.ERROR, List.of(), null, null);
            addMessage(errorMsg);
            sendingMessageInProgress.set(false);
        });
    }

    @Override
    public void onMemoryCleared() {
        Platform.runLater(chatMessages::clear);
    }

    @Override
    public void onCancel(){
        Platform.runLater(() -> sendingMessageInProgress.set(false));
    }

    @Override
    public void onPartialMessage(String message, MessageType coreMessageType, String requestId) {
        Platform.runLater(() -> {
            logger.debug("Partial message received: {}, type: {}, reqId: {}", message,  coreMessageType, requestId);
            var viewType = coreMessageTypeToViewMessageType(coreMessageType);
            var currentPatrialMessage = findMessageExistingMessageByTypeAndId(viewType, requestId);
            var updateCurrent = currentPatrialMessage != null;
            if (updateCurrent) {
                currentPatrialMessage.setContent(currentPatrialMessage.getContent() + message);
            } else {
                var newMessage = new ChatMessageViewModel(message, viewType, List.of(), null, requestId);
                logger.debug("Adding new partial message: {}", newMessage);
                chatMessages.add(newMessage);
            }
        });
    }

    private ChatMessageViewModel findMessageExistingMessageByTypeAndId(MessageTypeView messageType, String requestId) {
        ChatMessageViewModel foundMessage = null;
        for (int i =  chatMessages.size() - 1; i >= 0; i--) {
            var currentMessage =  chatMessages.get(i);
            if (currentMessage.getType() ==  messageType
                && requestId.equals(currentMessage.getId())
            ) {
                foundMessage = currentMessage;
                break;
            }
        }
        return foundMessage;
    }

    private MessageTypeView coreMessageTypeToViewMessageType(dev.local.ai.core.chat.messages.MessageType type) {
        return switch (type) {
            case PARTIAL_THINKING -> MessageTypeView.PARTIAL_THINKING;
            case PARTIAL -> MessageTypeView.PARTIAL_AI;
            case AI -> MessageTypeView.AI;
            case USER -> MessageTypeView.USER;
            default -> null;
        };
    }

    /**
     * Shuts down the ViewModel and command manager
     */
    public void shutdown() {
        if (session != null) {
            session.close();
        }
        commandManager.shutdown();
        logger.info("ChatViewModel shutdown");
    }

    public void copyMessage(ChatMessageViewModel message) {
        try {
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();
            content.putString(message.getContent());
            clipboard.setContent(content);
        } catch (Exception e) {
            logger.error("Failed to copy message to clipboard", e);
        }
    }

    public void stopMessage() {
        eventBus.publish(new StopRequestEvent(this.getClass().getSimpleName()));
    }
}
