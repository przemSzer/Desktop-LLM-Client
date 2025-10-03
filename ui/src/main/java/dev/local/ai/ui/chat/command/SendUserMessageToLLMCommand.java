package dev.local.ai.ui.chat.command;

import dev.local.ai.core.chat.ILLMChat;
import dev.local.ai.core.chat.messages.Message;
import dev.local.ai.core.documents.DocumentDescription;
import dev.local.ai.ui.commands.ICommand;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SendUserMessageToLLMCommand implements ICommand {
    
    private static final Logger logger = LoggerFactory.getLogger(SendUserMessageToLLMCommand.class);
    
    private final ILLMChat chat;
    private final String message;
    private final List<DocumentDescription> files;
    private boolean executed = false;
    
    public SendUserMessageToLLMCommand(ILLMChat chat, String message, List<DocumentDescription> files) {
        this.chat = chat;
        this.message = message;
        this.files = files;
    }
    
    @Override
    public boolean execute() {
        if (!canExecute()) {
            logger.warn("Cannot execute SendMessageCommand: {}", message);
            return false;
        }
        
        try {
            logger.debug("Executing SendMessageCommand: {}", message);
            chat.sendMessage(new Message(message, files));
            executed = true;
            logger.info("SendMessageCommand executed successfully: {}", message);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to execute SendMessageCommand: {}", message, e);
            return false;
        }
    }
    
    @Override
    public boolean undo() {
        if (!supportsUndo()) {
            logger.warn("SendMessageCommand does not support undo");
            return false;
        }
        
        // Note: In a real chat system, you might want to implement
        // message removal or editing instead of true undo
        logger.debug("Undo not supported for SendMessageCommand");
        return false;
    }
    
    @Override
    public boolean canExecute() {
        return chat != null && message != null && !message.trim().isEmpty();
    }
    
    @Override
    public String getDescription() {
        return "Send message: " + (message != null ? message.substring(0, Math.min(50, message.length())) + "..." : "null");
    }
    
    @Override
    public boolean supportsUndo() {
        return false; // Sending a message cannot be undone
    }
    
    // Getters
    public String getMessage() {
        return message;
    }
    
    public boolean isExecuted() {
        return executed;
    }
}
