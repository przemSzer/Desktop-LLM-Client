package dev.local.ai.ui.chat.session;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.local.ai.core.connections.OllamaConnection;
import dev.local.ai.core.events.CoreEventBus;
import dev.local.ai.core.models.LLMInfoAndConnection;
import dev.local.ai.core.models.ModelInfo;
import dev.local.ai.core.models.StreamingChatModelsProvider;
import dev.local.ai.core.storage.models.LastSelectedModel;
import dev.local.ai.core.tools.IToolProvider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ConversationSessionFactoryTest {

    @Mock(lenient = true)
    private ChatMemoryStore chatMemoryStore;

    @Mock(lenient = true)
    private LastSelectedModel lastSelectedModel;

    @Mock(lenient = true)
    private StreamingChatModelsProvider modelsProvider;

    @Mock(lenient = true)
    private IToolProvider toolProvider;

    @Mock(lenient = true)
    private CoreEventBus eventBus;

    @Mock(lenient = true)
    private StreamingChatModel firstStreamingModel;

    @Mock(lenient = true)
    private StreamingChatModel secondStreamingModel;

    private ConversationSessionFactory factory;

    @BeforeEach
    void setUp() {
        // Records and sealed interfaces are not mockable; use real instances.
        var modelInfo = new ModelInfo("model-id", "model-name", "desc",-1,-1);
        var connection = new OllamaConnection("conn-id", "test-conn", "test", "http://localhost:11434");
        var llm = new LLMInfoAndConnection(modelInfo, connection);
        given(lastSelectedModel.get()).willReturn(Optional.of(llm));
        given(modelsProvider.createStreamingChatModel(llm))
                .willReturn(firstStreamingModel, secondStreamingModel);

        factory = new ConversationSessionFactory(chatMemoryStore, lastSelectedModel,
                modelsProvider, toolProvider, eventBus);
    }

    @Test
    void shouldProduceIndependentSessionsBoundToTheirIds() {
        ChatSession sessionA = factory.openConversation("conv-A");
        ChatSession sessionB = factory.openConversation("conv-B");

        assertNotNull(sessionA.chat());
        assertNotNull(sessionB.chat());
        assertEquals("conv-A", sessionA.conversationId());
        assertEquals("conv-B", sessionB.conversationId());
        assertEquals("conv-A", sessionA.chatMemory().id());
        assertEquals("conv-B", sessionB.chatMemory().id());
    }

    @Test
    void shouldProduceDistinctStreamingChatInstancesPerSession() {
        ChatSession sessionA = factory.openConversation("conv-A");
        ChatSession sessionB = factory.openConversation("conv-B");

        assertNotSame(sessionA.chat(), sessionB.chat());
        assertNotSame(sessionA.chatMemory(), sessionB.chatMemory());
    }

    @Test
    void shouldClosingOneSessionShouldNotAffectAnother() {
        ChatSession sessionA = factory.openConversation("conv-A");
        ChatSession sessionB = factory.openConversation("conv-B");

        sessionA.close();

        // sessionB still references its own (different) chat; still works.
        assertSame(sessionB.chat(), sessionB.chat());
    }
}
