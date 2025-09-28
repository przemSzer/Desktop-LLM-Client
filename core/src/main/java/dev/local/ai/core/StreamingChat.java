package dev.local.ai.core;

import java.util.List;

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
import dev.local.ai.core.chat.messages.Message;
import dev.local.ai.core.documents.DocumentDescription;
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
    public void sendMessage(Message message) {
        logger.debug("Sending message: {}", message);
        try {
            UserMessage newMessage = null;
            if (message.files().isEmpty()){
                newMessage = new UserMessage(message.text());
                chatMemory.add(newMessage);
            }else{
                StringBuffer buffer = new StringBuffer(message.text());
                for (var file : message.files()) {
                    var fileContent = createMessageWithFiles(file);
                    buffer.append(fileContent);
                }
                newMessage = new UserMessage(buffer.toString());
                chatMemory.add(newMessage);
            }
                        
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

    private String createMessageWithFiles(DocumentDescription file) {            
        var buffer = new StringBuilder();
        buffer.append("\n");
        buffer.append("<file name=\"").append(file.title()).append("\" type=\"").append(file.type().toString()).append("\">\n");
        buffer.append(file.text()).append("\n");
        buffer.append("</file>");                
        buffer.append("\n");
        return buffer.toString();
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
                Message aiMessage = new Message(response.aiMessage().text(), List.of());
                callback.onMessageAdded(aiMessage, false);
            }
            
            logger.info("Message processed successfully. AI response added to memory.");
        }

        @Override
        public void onError(Throwable error) {
            logger.error("Error processing message: {}", error.getMessage(), error);
            if (error instanceof Exception){
                callback.onError("Failed to process message: " + error.getMessage(), (Exception) error);
            }
            //TODO: Handle other types of critical errors
            else{
                callback.onError("Failed to process message: " + error.getMessage(), new Exception(error));
            }
        }
    }

    @Override
    public void clearMemory() {
        chatMemory.clear();
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

    @Override
    public void setSystemMessage(Message message) {
        SystemMessage newMessage = null;
        if (message.files().isEmpty()){
            newMessage = new SystemMessage(message.text());
            chatMemory.add(newMessage);
        }else{
            StringBuffer buffer = new StringBuffer(message.text());
            for (var file : message.files()) {
                var fileContent = createMessageWithFiles(file);
                buffer.append(fileContent);
            }
            newMessage = new SystemMessage(buffer.toString());
            chatMemory.add(newMessage);
        }
        logger.info("System message updated to: {}", message.text().substring(0, Math.min(message.text().length(), 100)));
    }

}
