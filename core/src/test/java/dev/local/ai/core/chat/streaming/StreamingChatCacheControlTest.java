package dev.local.ai.core.chat.streaming;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.local.ai.core.chat.messages.Message;
import dev.local.ai.core.events.CoreEventBus;
import dev.local.ai.core.models.StreamingChatModelsProvider;
import dev.local.ai.core.tools.IToolExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class StreamingChatCacheControlTest {

    @Mock
    private StreamingChatModel chatModel;

    @Mock
    private IToolExecutor toolExecutor;

    @Mock
    private CoreEventBus eventBus;

    @Mock
    private StreamingChatModelsProvider modelsProvider;

    @Captor
    private ArgumentCaptor<ChatRequest> chatRequestCaptor;

    private ChatMemory chatMemory;
    private StreamingChat streamingChat;

    @BeforeEach
    void setUp() {
        chatMemory = new InMemoryChatMemory();
        streamingChat = new StreamingChat(chatModel, chatMemory, toolExecutor, eventBus, modelsProvider);
    }

    @Test
    void sendMessageMarksOnlyTheLatestUserTurnForAnthropicCache() {
        chatMemory.add(SystemMessage.from("You are helpful."));
        chatMemory.add(UserMessage.from("previous question"));
        given(toolExecutor.toolSpecifications()).willReturn(List.of());

        streamingChat.sendMessage(new Message("new question"));

        then(chatModel).should().chat(chatRequestCaptor.capture(), any());
        var messages = chatRequestCaptor.getValue().messages();

        assertThat(messages).hasSize(3);
        assertThat(((UserMessage) messages.get(1)).attributes())
                .doesNotContainKey(AnthropicPromptCacheControl.CACHE_CONTROL_ATTRIBUTE);
        assertThat(((UserMessage) messages.get(2)).singleText()).isEqualTo("new question");
        assertThat(((UserMessage) messages.get(2)).attributes())
                .containsEntry(AnthropicPromptCacheControl.CACHE_CONTROL_ATTRIBUTE, AnthropicPromptCacheControl.EPHEMERAL);
        assertThat(chatMemory.messages().get(2))
                .isInstanceOf(UserMessage.class);
        assertThat(((UserMessage) chatMemory.messages().get(2)).attributes())
                .doesNotContainKey(AnthropicPromptCacheControl.CACHE_CONTROL_ATTRIBUTE);
    }

    private static final class InMemoryChatMemory implements ChatMemory {
        private final List<dev.langchain4j.data.message.ChatMessage> messages = new ArrayList<>();

        @Override
        public Object id() {
            return "test";
        }

        @Override
        public void add(dev.langchain4j.data.message.ChatMessage message) {
            messages.add(message);
        }

        @Override
        public List<dev.langchain4j.data.message.ChatMessage> messages() {
            return List.copyOf(messages);
        }

        @Override
        public void clear() {
            messages.clear();
        }
    }
}
