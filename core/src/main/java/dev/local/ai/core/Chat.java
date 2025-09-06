package dev.local.ai.core;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.local.ai.core.chat.LLMChangedEvent;
import dev.local.ai.core.events.CoreEventBusProvider;
import dev.local.ai.core.events.EventListener;
import dev.local.ai.core.models.LLMInfoAndConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a chat conversation with messages and metadata.
 */
public class Chat implements ILLMChat, EventListener<LLMChangedEvent> {
    private static final Logger logger = LoggerFactory.getLogger(Chat.class);
    
    private ChatModel chatModel;
    private final ChatMemory chatMemory;
    private ChatListener callback;
    
    public Chat(ChatModel chatModel) {
        this.chatModel = chatModel;
        this.chatMemory = MessageWindowChatMemory.withMaxMessages(100);
        CoreEventBusProvider.getInstance().subscribe(LLMChangedEvent.EVENT_TYPE, this);
        logger.info("Chat instance created with model: {}", chatModel.getClass().getSimpleName());
    }

    @Override
    public void onEvent(LLMChangedEvent event) {
        logger.info("LLMChangedEvent received: {}", event.getModelInfo());
        changeModel(event.getModelInfo());
    }

    void changeModel(LLMInfoAndConnection modelInfo) {
        throw new UnsupportedOperationException("Not implemented");        
    }

    public String getSystemMessage() {
        return chatMemory.messages().stream()
            .filter(message -> message instanceof SystemMessage)
            .map(message -> ((SystemMessage) message).text())
            .findFirst()
            .orElse("");
    }

    public void setSystemMessage(String message) {
        if (message == null || message.isEmpty()){
            logger.debug("Removing system message, since it is null or empty");
            chatMemory.messages().removeIf(m -> m instanceof SystemMessage);
            logger.info("System message removed");
            return;
        }else{
            var newSystemMessage = new SystemMessage(message);
            chatMemory.add(newSystemMessage);
            logger.info("System message updated to: {}", message);
        }
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
    public void setCallback(ChatListener callback) {
        this.callback = callback;
        logger.debug("Chat callback {} set", callback != null ? "was" : "was not");
    }
    
    /**
     * Gets the current number of messages in chat memory
     * @return number of messages
     */
    public int getMessageCount() {
        return chatMemory.messages().size();
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