package dev.local.ai.core.chat.streaming;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.local.ai.core.chat.LLMChangedEvent;
import dev.local.ai.core.connections.OllamaConnection;
import dev.local.ai.core.events.CoreEventBus;
import dev.local.ai.core.events.Event;
import dev.local.ai.core.events.EventListener;
import dev.local.ai.core.models.LLMInfoAndConnection;
import dev.local.ai.core.models.ModelInfo;
import dev.local.ai.core.models.StreamingChatModelsProvider;
import dev.local.ai.core.tools.IToolProvider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class StreamingChatLifecycleTest {

    @Mock
    private StreamingChatModel chatModel;

    @Mock
    private ChatMemory chatMemory;

    @Mock
    private IToolProvider toolProvider;

    @Mock
    private CoreEventBus eventBus;

    @Mock
    private StreamingChatModelsProvider modelsProvider;

    @SuppressWarnings({ "rawtypes" })
    @Captor
    private ArgumentCaptor<EventListener> llmListenerCaptor;

    @SuppressWarnings({ "rawtypes" })
    @Captor
    private ArgumentCaptor<EventListener> stopListenerCaptor;

    private StreamingChat streamingChat;

    @BeforeEach
    void setUp() {
        var initialConnection = new LLMInfoAndConnection(
                new ModelInfo("id", "name", "description", -1,-1),
                new OllamaConnection("ollama", "ollama desc")
        );
        streamingChat = new StreamingChat(chatModel, initialConnection, chatMemory, toolProvider, eventBus, modelsProvider);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldSubscribeListenersOnConstruction() {
        then(eventBus).should().subscribe(eq(LLMChangedEvent.EVENT_TYPE), any(EventListener.class));
        then(eventBus).should().subscribe(eq(StopRequestEvent.EVENT_TYPE), any(EventListener.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldUnsubscribeSameListenerInstancesOnClose() {
        InOrder order = inOrder(eventBus);
        order.verify(eventBus).subscribe(eq(LLMChangedEvent.EVENT_TYPE), llmListenerCaptor.capture());
        order.verify(eventBus).subscribe(eq(StopRequestEvent.EVENT_TYPE), stopListenerCaptor.capture());
        EventListener<? extends Event> capturedLlmListener = llmListenerCaptor.getValue();
        EventListener<? extends Event> capturedStopListener = stopListenerCaptor.getValue();

        streamingChat.close();

        then(eventBus).should().unsubscribe(LLMChangedEvent.EVENT_TYPE, capturedLlmListener);
        then(eventBus).should().unsubscribe(StopRequestEvent.EVENT_TYPE, capturedStopListener);
    }

    @Test
    void shouldBeIdempotentOnRepeatedClose() {
        streamingChat.close();
        streamingChat.close();

        then(eventBus).should(times(1))
                .unsubscribe(eq(LLMChangedEvent.EVENT_TYPE), any(EventListener.class));
        then(eventBus).should(times(1))
                .unsubscribe(eq(StopRequestEvent.EVENT_TYPE), any(EventListener.class));
    }
}
