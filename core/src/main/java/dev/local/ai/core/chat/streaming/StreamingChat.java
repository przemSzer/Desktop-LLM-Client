package dev.local.ai.core.chat.streaming;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.*;
import dev.local.ai.core.chat.ILLMChat;
import dev.local.ai.core.chat.LLMChangedEvent;
import dev.local.ai.core.chat.messages.Message;
import dev.local.ai.core.chat.messages.Statistics;
import dev.local.ai.core.events.CoreEventBus;
import dev.local.ai.core.events.EventListener;
import dev.local.ai.core.models.StreamingChatModelsProvider;
import dev.local.ai.core.tools.ICancellable;
import dev.local.ai.core.tools.IToolExecutor;
import dev.local.ai.core.tools.ToolHelper;
import io.reactivex.rxjava4.core.Observable;
import io.reactivex.rxjava4.subjects.PublishSubject;
import io.reactivex.rxjava4.subjects.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class StreamingChat implements ILLMChat, AutoCloseable {

    private StreamingChatModel chatModel;
    private final ChatMemory chatMemory;
    private final StreamingChatModelsProvider chatModelsProvider;
    private static final Logger logger = LoggerFactory.getLogger(StreamingChat.class);
    private final MessageToChatMessageConverter messageToChatMessageConverter;
    private final IToolExecutor toolExecutor;
    private final AtomicBoolean stopRequested = new AtomicBoolean(false);

    private final CoreEventBus eventBus;
    private final EventListener<LLMChangedEvent> llmChangedListener = this::onLLMChanged;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final Subject<ChatEvent> events = PublishSubject.<ChatEvent>create().toSerialized();

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
        this.toolExecutor = toolExecutor;
        this.messageToChatMessageConverter = new MessageToChatMessageConverter();
        logger.info("StreamingChat instance created with model: {}", initialChatModel != null ? initialChatModel.getClass().getSimpleName(): "null");
    }

    public Observable<ChatEvent> events() {
        return events.hide();
    }

    public void stop() {
        logger.info("Stop requested");
        stopRequested.set(true);
        if (toolExecutor instanceof ICancellable cancellable) {
            cancellable.cancel();
        }
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            eventBus.unsubscribe(LLMChangedEvent.EVENT_TYPE, llmChangedListener);
            events.onComplete();
            if (toolExecutor instanceof AutoCloseable ac){
                try {
                    ac.close();
                } catch (Exception e) {
                    logger.error("Can not close tool executor", e);
                }
            }
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
            emit(new ChatEvent.UserMessageAdded(newRequestId.toString(), message));

            var request = prepareChatRequest();
            logger.info("Sending chat request, with {} messages", request.messages().size());
            chatModel.chat(
                    request,
                    new StreamingResponseHandler(
                            chatMemory,
                            toolExecutor,
                            newRequestId
                    )
                );

        } catch (Exception e) {
            logger.error("Error processing message: {}", message, e);
            emit(new ChatEvent.ChatError("Failed to process message: " + e.getMessage()));
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

    private void emit(ChatEvent event) {
        if (!closed.get()) {
            events.onNext(event);
        }
    }

    private static String argumentsText(ToolExecutionRequest request) {
        return ToolHelper.getArgumentsIgnoringError(request).entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining(", "));
    }

    private class StreamingResponseHandler implements StreamingChatResponseHandler{

        private final ChatMemory chatMemory;
        private final IToolExecutor toolExecutor;
        private String currentRequestId ;

        public StreamingResponseHandler(ChatMemory chatMemory, IToolExecutor toolExecutor, UUID initialRequestId) {
            this.chatMemory = chatMemory;
            this.toolExecutor = toolExecutor;
            currentRequestId = initialRequestId.toString();
        }

        @Override
        public void onPartialResponse(PartialResponse partialResponse, PartialResponseContext context) {
            if (stopRequested.get()) {
                stop(context.streamingHandle());
            }
            logger.trace("Partial response reqId {}, value: {}",currentRequestId, partialResponse);
            emit(new ChatEvent.PartialText(currentRequestId, partialResponse.text()));
        }

        private void stop(StreamingHandle streamingHandle) {
            logger.info("Stop requested, cancelling streaming response for request: {}", currentRequestId);
            streamingHandle.cancel();
            emit(new ChatEvent.TurnCancelled(currentRequestId));
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
            emit(new ChatEvent.PartialThinking(currentRequestId, partialThinking.text()));
            logger.trace("Partial thinking reqId {}, value: {}", currentRequestId, partialThinking.text());
        }


        @Override
        public void onCompleteResponse(ChatResponse response) {
            chatMemory.add(response.aiMessage());
            if (response.aiMessage().hasToolExecutionRequests()){
                logger.debug("Tool execution requests: {}", response.aiMessage().toolExecutionRequests());
                executeTools(response.aiMessage().toolExecutionRequests());
                if (stopRequested.get()) {
                    logger.debug("Stop requested, cancelling streaming response for request: {}", currentRequestId);
                    emit(new ChatEvent.TurnCancelled(currentRequestId));
                    return;
                }
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
                emit(new ChatEvent.AiMessageCompleted(currentRequestId, aiMessage));
            }

            logger.info("Message processed successfully. AI response added to memory.");
        }

        private void executeTools(List<ToolExecutionRequest> toolExecutionRequests) {
            logger.debug("Processing {} tool execution requests", toolExecutionRequests.size());
            toolExecutionRequests.forEach(toolExecutionRequest ->
                emit(new ChatEvent.ToolCallRequested(
                        currentRequestId,
                        toolExecutionRequest.id(),
                        toolExecutionRequest.name(),
                        argumentsText(toolExecutionRequest)
                ))
            );
            toolExecutor.execute(toolExecutionRequests, this::toolExecutionFinishedProperly);
        }

        private void toolExecutionFinishedProperly(ToolExecutionResultMessage result) {
            chatMemory.add(result);
            emit(new ChatEvent.ToolFinished(
                    currentRequestId,
                    result.id(),
                    result.toolName(),
                    result.text(),
                    result.isError()
            ));
        }

        @Override
        public void onCompleteToolCall(CompleteToolCall completeToolCall) {
            logger.info("Tool call completed: {} with id {}",
                    completeToolCall.toolExecutionRequest().name(),
                    completeToolCall.toolExecutionRequest().id()
            );
        }

        @Override
        public void onError(Throwable error) {
            logger.error("Error processing message: {}", error.getMessage(), error);
            emit(new ChatEvent.ChatError("Failed to process message: " + error.getMessage()));
        }
    }

    @Override
    public void clearMemory() {
        chatMemory.clear();
        emit(new ChatEvent.MemoryCleared());
        logger.info("Chat memory cleared");
    }

    @Override
    public void emptyNonSystemMessages() {
        var systemMessages = chatMemory.messages().stream()
                .filter(SystemMessage.class::isInstance)
                .toList();
        chatMemory.set(new ArrayList<>(systemMessages));
        emit(new ChatEvent.MemoryCleared());
        logger.info("Non-system chat messages removed; system message retained where present");
    }

    @Override
    public int getMessageCount() {
        return chatMemory.messages().size();
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
