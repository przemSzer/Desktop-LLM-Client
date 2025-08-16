package dev.local.ai.ui.chat.viewmodel;

import dev.local.ai.core.Chat;
import dev.local.ai.core.ChatCallback;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
public class ChatViewModel implements ChatCallback {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatViewModel.class);
    
    // Observable properties for data binding
    private final ListProperty<ChatMessage> chatMessages;
    private final StringProperty inputMessage;
    private final StringProperty statusMessage;
    private final BooleanProperty canUndo;
    private final BooleanProperty canRedo;
    
    // Model and command management
    private final Chat chat;
    private final CommandManager commandManager;
    
    public ChatViewModel(Chat chat, CommandManager commandManager) {
        this.chat = chat;
        this.commandManager = commandManager;
        
        // Initialize observable properties
        this.chatMessages = new SimpleListProperty<>(FXCollections.observableArrayList());
        this.inputMessage = new SimpleStringProperty("");
        this.statusMessage = new SimpleStringProperty("Ready");
        this.canUndo = new SimpleBooleanProperty(false);
        this.canRedo = new SimpleBooleanProperty(false);
        
        // Set this ViewModel as the callback for the Chat model
        chat.setCallback(this);
        
        // Bind undo/redo properties to command manager state
        setupCommandBindings();
        
        logger.info("ChatViewModel initialized");
    }
    
    private void setupCommandBindings() {
        // Update undo/redo state when commands are executed
        canUndo.bind(Bindings.createBooleanBinding(
            commandManager::canUndo,
            chatMessages
        ));
        
        canRedo.bind(Bindings.createBooleanBinding(
            commandManager::canRedo,
            chatMessages
        ));
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
            // Clear input immediately
            setInputMessage("");
            
            // Update status
            statusMessage.set("Sending message...");
            
            // Create and execute the send message command
            SendUserMessageToLLMCommand command = new SendUserMessageToLLMCommand(chat, message);
            boolean success = commandManager.executeCommand(command);
            
            if (success) {
                logger.info("SendMessageCommand executed successfully: {}", message);
            } else {
                statusMessage.set("Failed to send message");
                logger.error("SendMessageCommand failed: {}", message);
            }
            
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
            MessageType messageType = isUserMessage ? MessageType.USER : MessageType.AI;
            addMessage(new ChatMessage(message, messageType));
            
            if (isUserMessage) {
                statusMessage.set("User message added");
            } else {
                statusMessage.set("AI response received");
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
    
    /**
     * Shuts down the ViewModel and command manager
     */
    public void shutdown() {
        commandManager.shutdown();
        logger.info("ChatViewModel shutdown");
    }
}
