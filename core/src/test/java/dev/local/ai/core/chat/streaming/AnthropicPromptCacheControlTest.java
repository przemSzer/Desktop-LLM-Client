package dev.local.ai.core.chat.streaming;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AnthropicPromptCacheControlTest {

    @Test
    void marksOnlyTheLastConversationMessage() {
        var system = SystemMessage.from("You are helpful.");
        var firstUser = UserMessage.from("first");
        var ai = AiMessage.from("answer");
        var secondUser = UserMessage.from("second");

        var result = AnthropicPromptCacheControl.withBreakpointOnLastMessage(
                List.of(system, firstUser, ai, secondUser));

        assertThat(result).hasSize(4);
        assertThat(result.get(0)).isSameAs(system);
        assertThat(((UserMessage) result.get(1)).attributes())
                .doesNotContainKey(AnthropicPromptCacheControl.CACHE_CONTROL_ATTRIBUTE);
        assertThat(((AiMessage) result.get(2)).attributes())
                .doesNotContainKey(AnthropicPromptCacheControl.CACHE_CONTROL_ATTRIBUTE);
        assertThat(((UserMessage) result.get(3)).attributes())
                .containsEntry(AnthropicPromptCacheControl.CACHE_CONTROL_ATTRIBUTE, AnthropicPromptCacheControl.EPHEMERAL);
        assertThat(secondUser.attributes())
                .doesNotContainKey(AnthropicPromptCacheControl.CACHE_CONTROL_ATTRIBUTE);
    }

    @Test
    void marksLastToolResultInAnAgentLoop() {
        var user = UserMessage.from("search this");
        var ai = AiMessage.builder()
                .toolExecutionRequests(List.of(
                        ToolExecutionRequest.builder().id("call-1").name("search").arguments("{}").build()))
                .build();
        var toolResult = ToolExecutionResultMessage.from("call-1", "search", "found it");

        var result = AnthropicPromptCacheControl.withBreakpointOnLastMessage(List.of(user, ai, toolResult));

        assertThat(((UserMessage) result.get(0)).attributes())
                .doesNotContainKey(AnthropicPromptCacheControl.CACHE_CONTROL_ATTRIBUTE);
        assertThat(((AiMessage) result.get(1)).attributes())
                .doesNotContainKey(AnthropicPromptCacheControl.CACHE_CONTROL_ATTRIBUTE);
        assertThat(((ToolExecutionResultMessage) result.get(2)).attributes())
                .containsEntry(AnthropicPromptCacheControl.CACHE_CONTROL_ATTRIBUTE, AnthropicPromptCacheControl.EPHEMERAL);
        assertThat(toolResult.attributes())
                .doesNotContainKey(AnthropicPromptCacheControl.CACHE_CONTROL_ATTRIBUTE);
    }

    @Test
    void preservesExistingAiMessageAttributes() {
        var ai = AiMessage.builder()
                .text("thinking answer")
                .thinking("hmm")
                .attributes(Map.of("thinking_signature", "sig-1"))
                .build();

        var result = AnthropicPromptCacheControl.withBreakpointOnLastMessage(List.of(ai));

        assertThat(((AiMessage) result.getFirst()).attributes())
                .containsEntry("thinking_signature", "sig-1")
                .containsEntry(AnthropicPromptCacheControl.CACHE_CONTROL_ATTRIBUTE, AnthropicPromptCacheControl.EPHEMERAL);
        assertThat(((AiMessage) result.getFirst()).thinking()).isEqualTo("hmm");
    }

    @Test
    void leavesSystemOnlyHistoryUnchanged() {
        var system = SystemMessage.from("You are helpful.");

        var result = AnthropicPromptCacheControl.withBreakpointOnLastMessage(List.of(system));

        assertThat(result).containsExactly(system);
    }
}
