package dev.local.ai.core.chat.streaming;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.CompleteToolCall;
import dev.langchain4j.model.chat.response.PartialResponse;
import dev.langchain4j.model.chat.response.PartialResponseContext;
import dev.langchain4j.model.chat.response.PartialThinking;
import dev.langchain4j.model.chat.response.PartialThinkingContext;
import dev.langchain4j.model.chat.response.PartialToolCall;
import dev.langchain4j.model.chat.response.PartialToolCallContext;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.local.ai.core.chat.IChatListener;
import dev.local.ai.core.chat.ILLMChat;
import dev.local.ai.core.chat.LLMChangedEvent;
import dev.local.ai.core.chat.messages.Message;
import dev.local.ai.core.chat.messages.Statistics;
import dev.local.ai.core.events.CoreEventBus;
import dev.local.ai.core.models.StreamingChatModelsProvider;
import dev.local.ai.core.tools.IToolProvider;
import dev.local.ai.core.tools.ToolHelper;

public class StreamingChat implements ILLMChat,IPartialMessageAware{

    private StreamingChatModel chatModel;
    private final ChatMemory chatMemory;
    private IChatListener callback;
    private IPartialMessagesListener partialMessageListener;
    private final StreamingChatModelsProvider chatModelsProvider;
    private static final Logger logger = LoggerFactory.getLogger(StreamingChat.class);
    private final MessageToChatMessageConverter messageToChatMessageConverter;
    private IToolProvider toolProvider;
    private AtomicBoolean stopRequested = new AtomicBoolean(false);
    
    public StreamingChat(StreamingChatModel chatModel,
                         IToolProvider toolProvider,
                         CoreEventBus eventBus,
                         StreamingChatModelsProvider chatModelsProvider) {
        this.chatModel = chatModel;
        this.chatMemory = MessageWindowChatMemory.builder()
                        .alwaysKeepSystemMessageFirst(true)
                        .maxMessages(1000)
                        .build();
        this.chatModelsProvider = chatModelsProvider;
        logger.info("StreamingChat instance created with model: {}", chatModel.getClass().getSimpleName());
        eventBus.subscribe(LLMChangedEvent.EVENT_TYPE, this::onLLMChanged);
        eventBus.subscribe(StopRequestEvent.EVENT_TYPE, this::onStopRequest);
        this.toolProvider = toolProvider;
        this.messageToChatMessageConverter = new MessageToChatMessageConverter();
    }

    @Override
    public String getSystemMessage() {
        return chatMemory
            .messages()
            .stream()
            .filter(SystemMessage.class::isInstance)
            .map(message -> ((SystemMessage) message).text())
            .findFirst()
            .orElse("");
    }
    

    @Override
    public void sendMessage(Message message) {
        logger.debug("Sending message: {}", message);
        try {
            stopRequested.set(false);
            addNewMessageToChatMemory(message);
                        
            // Notify callback about user message
            if (callback != null) {
                callback.onMessageAdded(message, true);
            }
            
            var request = prepareChatRequest();
            logger.info("Sending chat request, with {} messages", request.messages().size());
            this.chatModel.chat(
                    request,
                    new StreamingResponseHandler(chatMemory, callback, partialMessageListener, this.toolProvider)
                );
            
        } catch (Exception e) {
            logger.error("Error processing message: {}", message, e);
            
            // Notify callback about error
            if (callback != null) {
                callback.onError("Failed to process message: " + e.getMessage(), e);
            }
        }
    }

    private ChatRequest prepareChatRequest() {
        return ChatRequest.builder()
            .messages(chatMemory.messages())
            .toolSpecifications(toolProvider.getToolSpecifications())
            .build();        
    }

    private void addNewMessageToChatMemory(Message message) {
        messageToChatMessageConverter.convert(message)
            .ifPresentOrElse(chatMemory::add, () -> logger.warn("Message converter returned empty optional for message: {}", message));    
    }

    private void onLLMChanged(LLMChangedEvent event) {
        logger.info("LLMChangedEvent received: {}", event.getModelInfo());
        this.chatModel = chatModelsProvider.createStreamingChatModel(event.getModelInfo());
    }

    private void onStopRequest(StopRequestEvent event) {
        logger.info("StopRequestEvent received: {}", event.getEventId());
        stopRequested.set(true);
    }

    private class StreamingResponseHandler implements StreamingChatResponseHandler{

        private final IChatListener callback;
        private final ChatMemory chatMemory;
        private final IPartialMessagesListener partialMessageListener;
        private final IToolProvider toolProvider;
        private final StringBuffer partialThinkingBuffer = new StringBuffer();

        public StreamingResponseHandler(ChatMemory chatMemory, IChatListener callback, IPartialMessagesListener partialMessageListener, IToolProvider toolProvider) {
            this.chatMemory = chatMemory;
            this.callback = callback;
            this.partialMessageListener = partialMessageListener;
            this.toolProvider = toolProvider;
        }

        @Override
        public void onPartialResponse(PartialResponse partialResponse, PartialResponseContext context) {
            if (partialMessageListener != null) {
                partialMessageListener.onPartialMessage(partialResponse.text());
            }
            if (stopRequested.get()) {
                logger.info("Stop requested, cancelling streaming");
                context.streamingHandle().cancel();
                callback.onCancel();
            }
        }

        @Override
        public void onPartialToolCall(PartialToolCall partialToolCall, PartialToolCallContext context) {
            if (stopRequested.get()) {
                logger.info("Stop requested, cancelling streaming");
                context.streamingHandle().cancel();
                callback.onCancel();
            }
        }
        
        @Override
        public void onPartialThinking(PartialThinking partialThinking, PartialThinkingContext context) {
            if (stopRequested.get()) {
                logger.info("Stop requested, cancelling streaming in partial thinking");
                context.streamingHandle().cancel();
                callback.onCancel();
            }
            if (partialMessageListener != null) {                
                partialThinkingBuffer.append(partialThinking.text());
            }
        }



        @Override
        public void onCompleteResponse(ChatResponse response) {
            chatMemory.add(response.aiMessage());
            if (partialThinkingBuffer.length() > 0) {
                logger.info("Partial thinking: {}", partialThinkingBuffer.toString());
                partialThinkingBuffer.setLength(0);
            }
            if (response.aiMessage().hasToolExecutionRequests()){
                logger.info("Tool execution requests: {}", response.aiMessage().toolExecutionRequests());
                executeTools(response.aiMessage().toolExecutionRequests());
                var request = prepareChatRequest();
                chatModel.chat(
                    request,
                    this
                );
            }else{
                logger.info("AI response finished, usage: {}", response.tokenUsage());
                var statistics = Statistics.fromTokenUsage(response.tokenUsage());
                var aiMessage = Message.ai(response.aiMessage().text(),statistics);
                callback.onMessageAdded(aiMessage, false);
            }
            
            logger.info("Message processed successfully. AI response added to memory.");
        }

        private void executeTools(List<ToolExecutionRequest> toolExecutionRequests) {
            logger.debug("Processing {} tool execution requests", toolExecutionRequests.size());
            for (var toolExecutionRequest : toolExecutionRequests) {                
                logger.debug("Processing tool execution request: {}", toolExecutionRequest);
                toolProvider.getToolExecutors()
                    .stream()
                    .flatMap(toolExecutor -> toolExecutor.execute(toolExecutionRequest).stream())
                    .forEach(result ->toolExecutionFinishedProperly(result, toolExecutionRequest));                
            }
        }
            
        private void toolExecutionFinishedProperly(ToolExecutionResultMessage result, ToolExecutionRequest toolExecutionRequest) {
            logger.info("Tool execution returned: {}", result);
            callback.onMessageAdded(
                Message.toolCall(result.toolName(), ToolHelper.getArgumentsIgnoringError(toolExecutionRequest)), false
            );
            logger.info("Tool execution returned: {}", result);
            chatMemory.add(result);
            callback.onMessageAdded(Message.toolResult(result.text(), List.of()), false);
        }

        @Override
        public void onCompleteToolCall(CompleteToolCall completeToolCall) {
            logger.info("Tool call completed: {}", completeToolCall.toolExecutionRequest().name());
        }

        @Override
        public void onError(Throwable error) {
            logger.error("Error processing message: {}", error.getMessage(), error);
            if (error instanceof Exception errorAsException){
                callback.onError("Failed to process message: " + error.getMessage(), errorAsException);
            }
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
    public void setCallback(IChatListener callback) {
        this.callback = callback;
    }

    @Override
    public void setPartialMessageListener(IPartialMessagesListener listener) {
        this.partialMessageListener = listener;
    }

    @Override
    public void setSystemMessage(Message message) {
        var chatMessageMaybe = messageToChatMessageConverter.convert(message);
        if (chatMessageMaybe.isEmpty()) {
            logger.debug("System message will be removed because it is empty: {}", message);
            var messagesWithoutSystemMessage = chatMemory.messages().stream().filter(m -> !(m instanceof SystemMessage)).toList();
            chatMemory.set(messagesWithoutSystemMessage);
            return;
        }
        var chatMessage = chatMessageMaybe.get();
        if (chatMessage instanceof SystemMessage) {
            logger.info("System message updated to: {}", message);
            chatMemory.add(chatMessage);        
        }else{
            logger.warn("Message converter returned non-system message: {}", chatMessage);
        }
    }

}
