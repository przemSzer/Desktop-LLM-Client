package dev.local.ai.ui.chat.viewmodel;

import dev.local.ai.core.ChatListener;
import dev.local.ai.core.ILLMChat;
import dev.local.ai.core.IPartialMessageAware;
import dev.local.ai.core.IPartialMessagesListener;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.local.ai.ui.chat.model.ChatMessage;
import dev.local.ai.ui.chat.model.ChatMessage.MessageType;
import dev.local.ai.ui.commands.CommandManager;
import dev.local.ai.ui.chat.command.SendUserMessageToLLMCommand;
import dev.local.ai.ui.chat.command.ClearChatCommand;

/**
 * ViewModel for the Chat UI following MVVM pattern.
 * Manages the observable data and commands for the chat interface.
 */
public class ChatViewModel implements ChatListener, IPartialMessagesListener {

    private static final Logger logger = LoggerFactory.getLogger(ChatViewModel.class);

    // Observable properties for data binding
    private final ListProperty<ChatMessage> chatMessages;
    private SimpleStringProperty systemMessage;
    private final StringProperty inputMessage;
    private final StringProperty statusMessage;
    private final BooleanProperty canUndo;
    private final BooleanProperty canRedo;

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
            if (newVal == null) {
                chat.setSystemMessage("");
            } else if (!newVal.equals(oldVal)) {
                chat.setSystemMessage(newVal);
            }
        });
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
    public ListProperty<ChatMessage> chatMessagesProperty() {
        return chatMessages;
    }

    public ObservableList<ChatMessage> getChatMessages() {
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

    // Commands
    public void sendMessage() {
        String message = getInputMessage().trim();
        if (message.isEmpty()) {
            logger.debug("Empty message ignored");
            return;
        }

        try {

            setInputMessage("");

            // Update status
            statusMessage.set("Sending message...");

            var command = new SendUserMessageToLLMCommand(chat, message);
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

    private void addMessage(ChatMessage message) {
        chatMessages.add(message);
        logger.debug("Message added to view model: {}", message);
    }

    public int getMessageCount() {
        return chat.getMessageCount();
    }

    // ChatCallback implementation
    @Override
    public void onMessageAdded(String message, boolean isUserMessage) {
        Platform.runLater(() -> {
            if (chatMessages.isEmpty()) {
                var lastMessage = new ChatMessage(message, isUserMessage ? MessageType.USER : MessageType.AI);
                chatMessages.add(lastMessage);
            } else {
                var lastMessage = chatMessages.get(chatMessages.size() - 1);
                var replaceLastOneSincePartial = lastMessage.getType() == MessageType.PARTIAL;
                if (replaceLastOneSincePartial) {
                    lastMessage.setContent(message);
                    lastMessage.setType(MessageType.AI);
                    chatMessages.set(chatMessages.size() - 1, lastMessage);
                } else {
                    lastMessage = new ChatMessage(message, isUserMessage ? MessageType.USER : MessageType.AI);
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
            ChatMessage errorMsg = new ChatMessage(errorMessage, MessageType.ERROR);
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
                lastMessage = new ChatMessage(message, MessageType.PARTIAL);
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

    public void copyMessage(ChatMessage message) {
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
