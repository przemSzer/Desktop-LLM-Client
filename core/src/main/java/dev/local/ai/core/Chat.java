package dev.local.ai.core;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a chat conversation with messages and metadata.
 */
public class Chat {
    private static final Logger logger = LoggerFactory.getLogger(Chat.class);
    
    private final ChatModel chatModel;
    private final ChatMemory chatMemory;
    
    // Callback for notifying about changes
    private ChatCallback callback;
    
    public Chat(ChatModel chatModel) {
        this.chatModel = chatModel;
        this.chatMemory = MessageWindowChatMemory.withMaxMessages(100);
        logger.info("Chat instance created with model: {}", chatModel.getClass().getSimpleName());
    }

    public void sendMessage(String message) {
        logger.debug("Sending message: {}", message);
        try {
            var newMessage = new UserMessage(message);
            chatMemory.add(newMessage);
            
            // Notify callback about user message
            if (callback != null) {
                callback.onMessageAdded(message, true);
            }
            
            var response = chatModel.chat(chatMemory.messages());
            chatMemory.add(response.aiMessage());
            
            // Notify callback about AI response
            if (callback != null) {
                callback.onMessageAdded(response.aiMessage().text(), false);
            }
            
            logger.info("Message processed successfully. AI response added to memory.");
        } catch (Exception e) {
            logger.error("Error processing message: {}", message, e);
            
            // Notify callback about error
            if (callback != null) {
                callback.onError("Failed to process message: " + e.getMessage(), e);
            }
            
            throw e;
        }
    }
    
    /**
     * Sets the callback for receiving chat events
     * @param callback the callback to set, or null to remove
     */
    public void setCallback(ChatCallback callback) {
        this.callback = callback;
        logger.debug("Chat callback {} set", callback != null ? "was" : "was not");
    }
    
    /**
     * Gets the current number of messages in chat memory
     * @return number of messages
     */
    public int getMessageCount() {
        int count = chatMemory.messages().size();
        logger.debug("Current message count in memory: {}", count);
        return count;
    }
    
    /**
     * Clears the chat memory
     */
    public void clearMemory() {
        chatMemory.clear();
        
        // Notify callback about memory cleared
        if (callback != null) {
            callback.onMemoryCleared();
        }
        
        logger.info("Chat memory cleared");
    }
} 