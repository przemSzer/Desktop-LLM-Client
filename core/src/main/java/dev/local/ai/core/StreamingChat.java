package dev.local.ai.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.local.ai.core.chat.LLMChangedEvent;
import dev.local.ai.core.events.CoreEventBusProvider;
import dev.local.ai.core.events.EventListener;
import dev.local.ai.core.models.StreamingChatModelsProvider;
import dev.local.ai.core.models.LLMInfoAndConnection;

public class StreamingChat implements ILLMChat,IPartialMessageAware, EventListener<LLMChangedEvent>{

    private StreamingChatModel chatModel;
    private final ChatMemory chatMemory;
    private ChatListener callback;
    private IPartialMessagesListener partialMessageListener;
    private final StreamingChatModelsProvider chatModelsProvider;
    private static final Logger logger = LoggerFactory.getLogger(StreamingChat.class);
    
    public StreamingChat(StreamingChatModel chatModel) {
        this.chatModel = chatModel;
        this.chatMemory = MessageWindowChatMemory.withMaxMessages(100);
        this.chatModelsProvider = new StreamingChatModelsProvider();
        logger.info("StreamingChat instance created with model: {}", chatModel.getClass().getSimpleName());
        CoreEventBusProvider.getInstance().subscribe(LLMChangedEvent.EVENT_TYPE, this);
    }

    @Override
    public String getSystemMessage() {
        return chatMemory.messages().stream()
            .filter(message -> message instanceof SystemMessage)
            .map(message -> ((SystemMessage) message).text())
            .findFirst()
            .orElse("");
    }
    
    @Override
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

    @Override
    public void sendMessage(String message) {
        logger.debug("Sending message: {}", message);
        try {
            var newMessage = new UserMessage(message);
            chatMemory.add(newMessage);
            
            // Notify callback about user message
            if (callback != null) {
                callback.onMessageAdded(message, true);
            }
            
            this.chatModel.chat(
                    chatMemory.messages(), 
                    new StreamingResponseHandler(chatMemory, callback, partialMessageListener)
                );
            
        } catch (Exception e) {
            logger.error("Error processing message: {}", message, e);
            
            // Notify callback about error
            if (callback != null) {
                callback.onError("Failed to process message: " + e.getMessage(), e);
            }
            
            throw e;
        }
    }

    @Override
    public void onEvent(LLMChangedEvent event) {
        logger.info("LLMChangedEvent received: {}", event.getModelInfo());
        changeModel(event.getModelInfo());
    }

    void changeModel(LLMInfoAndConnection modelInfo) {
        this.chatModel = chatModelsProvider.createStreamingChatModel(modelInfo);
    }

    private static class StreamingResponseHandler implements StreamingChatResponseHandler{

        private final ChatListener callback;
        private final ChatMemory chatMemory;
        private final IPartialMessagesListener partialMessageListener;

        public StreamingResponseHandler(ChatMemory chatMemory, ChatListener callback, IPartialMessagesListener partialMessageListener) {
            this.chatMemory = chatMemory;
            this.callback = callback;
            this.partialMessageListener = partialMessageListener;
        }

        @Override
        public void onPartialResponse(String partialResponse) {
            if (partialMessageListener != null) {
                partialMessageListener.onPartialMessage(partialResponse);
            }
        }

        @Override
        public void onCompleteResponse(ChatResponse response) {
            chatMemory.add(response.aiMessage());
            if (callback != null) {
                callback.onMessageAdded(response.aiMessage().text(), false);
            }
            
            logger.info("Message processed successfully. AI response added to memory.");
        }

        @Override
        public void onError(Throwable error) {
            if (error instanceof Exception){
                callback.onError("Failed to process message: " + error.getMessage(), (Exception) error);
            }else{
                callback.onError("Failed to process message: " + error.getMessage(), new Exception(error));
            }
        }
    }

    @Override
    public void clearMemory() {
        if (callback != null) {
            callback.onMemoryCleared();
        }
        
        logger.info("Chat memory cleared");
    }

    @Override
    public int getMessageCount() {
        return chatMemory.messages().size();
    }

    @Override
    public void setCallback(ChatListener callback) {
        this.callback = callback;
    }

    @Override
    public void setPartialMessageListener(IPartialMessagesListener listener) {
        this.partialMessageListener = listener;
    }

}
