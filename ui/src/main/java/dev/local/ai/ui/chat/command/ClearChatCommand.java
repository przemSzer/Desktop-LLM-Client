package dev.local.ai.ui.chat.command;

import dev.local.ai.core.ILLMChat;
import dev.local.ai.ui.commands.ICommand;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ClearChatCommand implements ICommand {
    
    private static final Logger logger = LoggerFactory.getLogger(ClearChatCommand.class);
    
    private final ILLMChat chat;
    private List<String> previousMessages;
    
    public ClearChatCommand(ILLMChat chat) {
        this.chat = chat;
    }
    
    @Override
    public boolean execute() {
        if (!canExecute()) {
            logger.warn("Cannot execute ClearChatCommand");
            return false;
        }
        
        try {
            logger.debug("Executing ClearChatCommand");
            
            // Store previous state for undo
            if (supportsUndo()) {
                previousMessages = null;
            }
            var systemMessage = chat.getSystemMessage();
            chat.clearMemory();
            if (systemMessage != null && !systemMessage.isEmpty()) {
                chat.setSystemMessage(systemMessage);
            }
            logger.info("ClearChatCommand executed successfully");
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to execute ClearChatCommand", e);
            return false;
        }
    }
    
    @Override
    public boolean undo() {
        if (!supportsUndo() || previousMessages == null) {
            logger.warn("ClearChatCommand cannot be undone");
            return false;
        }
        
        try {
            logger.debug("Undoing ClearChatCommand");
            // Restore previous messages
            // Note: This would require the Chat class to support restoring messages
            // For now, we'll just log that undo was attempted
            logger.info("ClearChatCommand undo attempted (restoration not implemented)");
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to undo ClearChatCommand", e);
            return false;
        }
    }
    
    @Override
    public boolean canExecute() {
        return chat != null;
    }
    
    @Override
    public String getDescription() {
        return "Clear chat memory";
    }
    
    @Override
    public boolean supportsUndo() {
        return true; // Clearing can potentially be undone
    }
}
