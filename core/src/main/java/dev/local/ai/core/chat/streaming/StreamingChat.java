package dev.local.ai.core.chat.streaming;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.*;
import dev.local.ai.core.chat.IChatListener;
import dev.local.ai.core.chat.ILLMChat;
import dev.local.ai.core.chat.LLMChangedEvent;
import dev.local.ai.core.chat.messages.Message;
import dev.local.ai.core.chat.messages.MessageType;
import dev.local.ai.core.chat.messages.Statistics;
import dev.local.ai.core.events.CoreEventBus;
import dev.local.ai.core.events.EventListener;
import dev.local.ai.core.models.StreamingChatModelsProvider;
import dev.local.ai.core.tools.IToolExecutor;
import dev.local.ai.core.tools.ToolHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class StreamingChat implements ILLMChat, IPartialMessageAware, AutoCloseable {

    private StreamingChatModel chatModel;
    private final ChatMemory chatMemory;
    private IChatListener callback;
    private IPartialMessagesListener partialMessageListener;
    private final StreamingChatModelsProvider chatModelsProvider;
    private static final Logger logger = LoggerFactory.getLogger(StreamingChat.class);
    private final MessageToChatMessageConverter messageToChatMessageConverter;
    private final IToolExecutor toolExecutor;
    private final AtomicBoolean stopRequested = new AtomicBoolean(false);

    private final CoreEventBus eventBus;
    private final EventListener<LLMChangedEvent> llmChangedListener = this::onLLMChanged;
    private final EventListener<StopRequestEvent> stopRequestListener = this::onStopRequest;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    //TODO: chatModel in fact should be initial chatModel, 
    // but it also should be gathered from chatModelsProvider
    public StreamingChat(StreamingChatModel initialChatModel,
                         ChatMemory chatMemory,
                         IToolExecutor toolExecutor,
                         CoreEventBus eventBus,
                         StreamingChatModelsProvider chatModelsProvider) {
        this.chatModel = initialChatModel;
        this.chatMemory = chatMemory;
        this.chatModelsProvider = chatModelsProvider;
        this.eventBus = eventBus;
        eventBus.subscribe(LLMChangedEvent.EVENT_TYPE, llmChangedListener);
        eventBus.subscribe(StopRequestEvent.EVENT_TYPE, stopRequestListener);
        this.toolExecutor = toolExecutor;
        this.messageToChatMessageConverter = new MessageToChatMessageConverter();
        logger.info("StreamingChat instance created with model: {}", initialChatModel != null ? initialChatModel.getClass().getSimpleName(): "null");
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            eventBus.unsubscribe(LLMChangedEvent.EVENT_TYPE, llmChangedListener);
            eventBus.unsubscribe(StopRequestEvent.EVENT_TYPE, stopRequestListener);
            logger.info("StreamingChat closed and unsubscribed from CoreEventBus");            
        }
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
                        
            UUID newRequestId = UUID.randomUUID();
            if (callback != null) {
                callback.onMessageAdded(message, newRequestId.toString());
            }
            
            var request = prepareChatRequest();
            logger.info("Sending chat request, with {} messages", request.messages().size());
            chatModel.chat(
                    request,
                    new StreamingResponseHandler(
                            chatMemory,
                            callback,
                            partialMessageListener,
                            toolExecutor,
                            newRequestId
                    )
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
            .toolSpecifications(toolExecutor.toolSpecifications())
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
        private final IToolExecutor toolExecutor;
        private String currentRequestId ;

        public StreamingResponseHandler(ChatMemory chatMemory, IChatListener callback, IPartialMessagesListener partialMessageListener, IToolExecutor toolExecutor, UUID initialRequestId) {
            this.chatMemory = chatMemory;
            this.callback = callback;
            this.partialMessageListener = partialMessageListener;
            this.toolExecutor = toolExecutor;
            currentRequestId = initialRequestId.toString();
        }

        @Override
        public void onPartialResponse(PartialResponse partialResponse, PartialResponseContext context) {
            if (stopRequested.get()) {
                stop(context.streamingHandle());
            }
            if (partialMessageListener != null) {
                logger.trace("Partial response reqId {}, value: {}",currentRequestId, partialResponse);
                partialMessageListener.onPartialMessage(partialResponse.text(), MessageType.PARTIAL, currentRequestId);
            }
        }

        private void stop(StreamingHandle streamingHandle) {
            logger.info("Stop requested, cancelling streaming response for request: {}", currentRequestId);
            streamingHandle.cancel();
            callback.onCancel();
        }

        @Override
        public void onPartialToolCall(PartialToolCall partialToolCall, PartialToolCallContext context) {
            if (stopRequested.get()) {
                stop(context.streamingHandle());
            }
        }
        
        @Override
        public void onPartialThinking(PartialThinking partialThinking, PartialThinkingContext context) {
            if (stopRequested.get()) {
                stop(context.streamingHandle());
            }
            if (partialMessageListener != null) {
                partialMessageListener.onPartialMessage(partialThinking.text(), MessageType.PARTIAL_THINKING, currentRequestId);
            }
            logger.trace("Partial thinking reqId {}, value: {}", currentRequestId, partialThinking.text());
        }


        @Override
        public void onCompleteResponse(ChatResponse response) {
            chatMemory.add(response.aiMessage());
            if (response.aiMessage().hasToolExecutionRequests()){
                logger.debug("Tool execution requests: {}", response.aiMessage().toolExecutionRequests());
                executeTools(response.aiMessage().toolExecutionRequests());
                var request = prepareChatRequest();
                currentRequestId = UUID.randomUUID().toString();
                chatModel.chat(
                    request,
                    this
                );
            }else{
                logger.info("AI response finished, usage: {}", response.tokenUsage());
                var statistics = Statistics.fromTokenUsage(response.tokenUsage());
                var aiMessage = Message.ai(response.aiMessage().text(),statistics);
                callback.onMessageAdded(aiMessage, currentRequestId);
            }
            
            logger.info("Message processed successfully. AI response added to memory.");
        }

        private void executeTools(List<ToolExecutionRequest> toolExecutionRequests) {
            logger.debug("Processing {} tool execution requests", toolExecutionRequests.size());
            toolExecutionRequests.forEach(toolExecutionRequest ->
                callback.onMessageAdded(
                        Message.toolCall(
                                toolExecutionRequest.name(),
                                ToolHelper.getArgumentsIgnoringError(toolExecutionRequest),
                                toolExecutionRequest.id()
                        ),
                        currentRequestId
                )
            );
            toolExecutor.execute(toolExecutionRequests)
                    .forEach(this::toolExecutionFinishedProperly);
        }
            
        private void toolExecutionFinishedProperly(ToolExecutionResultMessage result) {
            chatMemory.add(result);
            callback.onMessageAdded(Message.toolResult(result.text(), List.of()), currentRequestId);
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
    public void emptyNonSystemMessages() {
        var systemMessages = chatMemory.messages().stream()
                .filter(SystemMessage.class::isInstance)
                .toList();
        chatMemory.set(new ArrayList<>(systemMessages));
        if (callback != null) {
            callback.onMemoryCleared();
        }
        logger.info("Non-system chat messages removed; system message retained where present");
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
