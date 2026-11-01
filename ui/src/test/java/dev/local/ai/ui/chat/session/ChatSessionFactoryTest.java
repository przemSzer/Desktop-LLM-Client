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
import dev.local.ai.core.tools.settings.ToolSettings;
import dev.local.ai.core.tools.settings.ToolSettingsReader;
import dev.local.ai.core.tools.settings.ToolSettingsWriter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ChatSessionFactoryTest {

    @TempDir
    Path chatsDir;

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

    private ChatSessionFactory factory;
    private ToolSettingsWriter writer;

    @BeforeEach
    void setUp() {
        var modelInfo = new ModelInfo("model-id", "model-name", "desc",-1,-1);
        var connection = new OllamaConnection("conn-id", "test-conn", "test", "http://localhost:11434");
        var llm = new LLMInfoAndConnection(modelInfo, connection);
        given(lastSelectedModel.get()).willReturn(Optional.of(llm));
        given(modelsProvider.createStreamingChatModel(llm))
                .willReturn(firstStreamingModel, secondStreamingModel);

        writer = new ToolSettingsWriter();
        factory = new ChatSessionFactory(
                chatMemoryStore,
                lastSelectedModel,
                modelsProvider,
                toolProvider,
                eventBus,
                chatsDir,
                new ToolSettingsReader(),
                writer
        );
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

        assertSame(sessionB.chat(), sessionB.chat());
    }

    @Test
    void should_auto_approve_tool_listed_in_conversation_settings() {
        writer.write(
                chatsDir.resolve("conv-A").resolve(ChatSessionFactory.TOOL_SETTINGS_FILE),
                new ToolSettings(List.of("downloadWebPage"))
        );

        ChatSession session = factory.openConversation("conv-A");

        assertThat(session.toolApproval().alwaysAllowedTools())
                .containsExactly("downloadWebPage");
    }

    @Test
    void should_not_inherit_always_allowed_tools_from_another_conversation() {
        writer.write(
                chatsDir.resolve("conv-A").resolve(ChatSessionFactory.TOOL_SETTINGS_FILE),
                new ToolSettings(List.of("downloadWebPage"))
        );

        ChatSession sessionA = factory.openConversation("conv-A");
        ChatSession sessionB = factory.openConversation("conv-B");

        assertThat(sessionA.toolApproval().alwaysAllowedTools())
                .containsExactly("downloadWebPage");
        assertThat(sessionB.toolApproval().alwaysAllowedTools())
                .isEmpty();
    }
}
