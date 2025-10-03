package dev.local.ai.ui.chat.viewmodel;

import dev.local.ai.core.chat.IChatListener;
import dev.local.ai.core.chat.ILLMChat;
import dev.local.ai.core.chat.messages.Message;
import dev.local.ai.core.chat.streaming.IPartialMessageAware;
import dev.local.ai.core.chat.streaming.IPartialMessagesListener;
import dev.local.ai.core.documents.DocumentDescription;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;

import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.local.ai.ui.chat.model.ChatMessageViewModel;
import dev.local.ai.ui.chat.model.ChatMessageViewModel.MessageType;
import dev.local.ai.ui.commands.CommandManager;
import dev.local.ai.ui.files.viewmodel.AttachedFileViewModel;
import dev.local.ai.ui.files.viewmodel.FileStatus;
import dev.local.ai.ui.chat.command.SendUserMessageToLLMCommand;
import dev.local.ai.ui.chat.command.ClearChatCommand;

/**
 * ViewModel for the Chat UI following MVVM pattern.
 * Manages the observable data and commands for the chat interface.
 */
public class ChatViewModel implements IChatListener, IPartialMessagesListener {

    private static final Logger logger = LoggerFactory.getLogger(ChatViewModel.class);

    // Observable properties for data binding
    private final ListProperty<ChatMessageViewModel> chatMessages;
    private SimpleStringProperty systemMessage;
    private final StringProperty inputMessage;
    private final StringProperty statusMessage;
    private final BooleanProperty canUndo;
    private final BooleanProperty canRedo;
    private final ListProperty<AttachedFileViewModel> attachedFiles;
    private final ListProperty<AttachedFileViewModel> systemMessageAttachedFiles;

    // Model and command management
    private final ILLMChat chat;
    private final CommandManager commandManager;

    public ChatViewModel(ILLMChat chat, CommandManager commandManager) {
        this.chat = chat;
        this.commandManager = commandManager;

        // Initialize observable properties
        this.systemMessage = new SimpleStringProperty(chat.getSystemMessage());

        this.chatMessages = new SimpleListProperty<>(FXCollections.observableArrayList());
        this.inputMessage = new SimpleStringProperty("");
        this.attachedFiles = new SimpleListProperty<>(FXCollections.observableArrayList());
        this.systemMessageAttachedFiles = new SimpleListProperty<>(FXCollections.observableArrayList());
        this.statusMessage = new SimpleStringProperty("Ready");
        this.canUndo = new SimpleBooleanProperty(false);
        this.canRedo = new SimpleBooleanProperty(false);

        // Set this ViewModel as the callback for the Chat model
        chat.setCallback(this);
        if (chat instanceof IPartialMessageAware partialMessageAware) {
            partialMessageAware.setPartialMessageListener(this);
        }

        // Bind undo/redo properties to command manager state
        setupCommandBindings();
        setupPropertyBindings();

        logger.info("ChatViewModel initialized");
    }

    private void setupPropertyBindings() {
        systemMessage.addListener((obs, oldVal, newVal) -> {
            updateSystemMessage();
        });
        systemMessageAttachedFiles.addListener((obs, oldVal, newVal) -> {
            for (var file : newVal) {
                file.descriptionProperty().addListener((obs1, oldVal1, newVal1) -> {
                    updateSystemMessage();
                });
            }
            updateSystemMessage();
        });
    }

    private void updateSystemMessage() {
        var newSystemMessage = new Message(
                systemMessage.get(), 
                systemMessageAttachedFiles
                    .get()
                    .stream()
                    .filter(f -> f.getStatus() == FileStatus.VALID)
                    .map(AttachedFileViewModel::getDescription)
                    .filter(Objects::nonNull)
                    .toList()
            );
        chat.setSystemMessage(newSystemMessage);
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

    public StringProperty statusMessageProperty() {
        return statusMessage;
    }

    public String getStatusMessage() {
        return statusMessage.get();
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

            // Update status
            statusMessage.set("Sending message...");

            var command = new SendUserMessageToLLMCommand(chat, message, files);
            var execution = commandManager.executeCommandAsync(command);
            execution.thenAccept(result -> {
                if (result) {
                    logger.info("SendMessageCommand executed successfully: {}", message);
                } else {
                    Platform.runLater(() -> statusMessage.set("Failed to send message"));
                    logger.error("SendMessageCommand failed: {}", message);
                }
            });

        } catch (Exception e) {
            logger.error("Failed to send message: {}", message, e);
            statusMessage.set("Error sending message");
        }
    }

    public void undo() {
        if (commandManager.canUndo()) {
            boolean success = commandManager.undo();
            if (success) {
                statusMessage.set("Command undone");
                logger.info("Command undone successfully");
            } else {
                statusMessage.set("Failed to undo command");
                logger.warn("Failed to undo command");
            }
        }
    }

    public void redo() {
        if (commandManager.canRedo()) {
            boolean success = commandManager.redo();
            if (success) {
                statusMessage.set("Command redone");
                logger.info("Command redone successfully");
            } else {
                statusMessage.set("Failed to redo command");
                logger.warn("Failed to redo command");
            }
        }
    }

    public void clearChat() {
        try {
            statusMessage.set("Clearing chat...");

            // Create and execute the clear chat command
            ClearChatCommand command = new ClearChatCommand(chat);
            boolean success = commandManager.executeCommand(command);

            if (success) {
                statusMessage.set("Chat cleared");
                logger.info("ClearChatCommand executed successfully");
            } else {
                statusMessage.set("Failed to clear chat");
                logger.error("ClearChatCommand failed");
            }

        } catch (Exception e) {
            logger.error("Failed to clear chat", e);
            statusMessage.set("Error clearing chat");
        }
    }

    private void addMessage(ChatMessageViewModel message) {
        chatMessages.add(message);
        logger.debug("Message added to view model: {}", message);
    }

    public int getMessageCount() {
        return chat.getMessageCount();
    }

    // ChatCallback implementation
    @Override
    public void onMessageAdded(Message message, boolean isUserMessage) {
        Platform.runLater(() -> {
            var attachedFiles = message.files()
                .stream()
                .map(fileDesc -> new AttachedFileViewModel(fileDesc, FileStatus.VALID))
                .toList();
            if (chatMessages.isEmpty()) {
                var lastMessage = new ChatMessageViewModel(message.text(), isUserMessage ? MessageType.USER : MessageType.AI, attachedFiles);
                chatMessages.add(lastMessage);
            } else {
                var lastMessage = chatMessages.get(chatMessages.size() - 1);
                var replaceLastOneSincePartial = lastMessage.getType() == MessageType.PARTIAL;
                if (replaceLastOneSincePartial) {
                    lastMessage.setContent(message.text());
                    lastMessage.setType(MessageType.AI);
                    lastMessage.setAttachedFiles(attachedFiles);
                    chatMessages.set(chatMessages.size() - 1, lastMessage);
                } else {
                    lastMessage = new ChatMessageViewModel(message.text(), isUserMessage ? MessageType.USER : MessageType.AI, attachedFiles);
                    chatMessages.add(lastMessage);
                }
                if (isUserMessage) {
                    statusMessage.set("User message added");
                } else {
                    statusMessage.set("AI response received");
                }
            }
            
        });
    }

    @Override
    public void onError(String errorMessage, Exception exception) {
        Platform.runLater(() -> {
            ChatMessageViewModel errorMsg = new ChatMessageViewModel(errorMessage, MessageType.ERROR, List.of());
            addMessage(errorMsg);
            statusMessage.set("Error occurred: " + errorMessage);
        });
    }

    @Override
    public void onMemoryCleared() {
        Platform.runLater(() -> {
            chatMessages.clear();
            statusMessage.set("Chat memory cleared");
        });
    }

    @Override
    public void onPartialMessage(String message) {
        Platform.runLater(() -> {
            var lastMessage = chatMessages.get(chatMessages.size() - 1);
            var replaceLast = lastMessage.getType() == MessageType.PARTIAL;
            if (replaceLast) {
                lastMessage.setContent(lastMessage.getContent() + message);
                chatMessages.set(chatMessages.size() - 1, lastMessage);
            } else {
                lastMessage = new ChatMessageViewModel(message, MessageType.PARTIAL, List.of());
                chatMessages.add(lastMessage);
            }
        });
    }

    /**
     * Shuts down the ViewModel and command manager
     */
    public void shutdown() {
        commandManager.shutdown();
        logger.info("ChatViewModel shutdown");
    }

    public void copyMessage(ChatMessageViewModel message) {
        try {
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();
            content.putString(message.getContent());
            clipboard.setContent(content);
            
            statusMessage.set("Message copied to clipboard");            
        } catch (Exception e) {
            logger.error("Failed to copy message to clipboard", e);
            statusMessage.set("Failed to copy message");
        }
    }
}
