package dev.local.ai.core.chat.streaming;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Anthropic prompt caching is prefix-based and allows at most four {@code cache_control}
 * breakpoints. LangChain4j maps the {@code cache_control=ephemeral} message attribute onto
 * the last content block of that message.
 * <p>
 * Marking every stored message would exceed the breakpoint limit and is ignored on older
 * LangChain4j versions for {@link AiMessage} / {@link ToolExecutionResultMessage}.
 * Applying the marker only to the last conversation message of the outgoing request lets
 * Anthropic reuse the previous turn's cached prefix (20-block lookback) while writing a
 * new entry for the growing tail.
 */
final class AnthropicPromptCacheControl {

    static final String CACHE_CONTROL_ATTRIBUTE = "cache_control";
    static final String EPHEMERAL = "ephemeral";

    private AnthropicPromptCacheControl() {
    }

    static List<ChatMessage> withBreakpointOnLastMessage(List<ChatMessage> messages) {
        int lastCacheableIndex = lastCacheableIndex(messages);
        if (lastCacheableIndex < 0) {
            return messages;
        }
        var copy = new ArrayList<>(messages);
        copy.set(lastCacheableIndex, withCacheControl(messages.get(lastCacheableIndex)));
        return copy;
    }

    private static int lastCacheableIndex(List<ChatMessage> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (isCacheable(messages.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isCacheable(ChatMessage message) {
        return message instanceof UserMessage
                || message instanceof AiMessage
                || message instanceof ToolExecutionResultMessage;
    }

    private static ChatMessage withCacheControl(ChatMessage message) {
        return switch (message) {
            case UserMessage userMessage -> copyUserMessageWithCacheControl(userMessage);
            case AiMessage aiMessage -> aiMessage.toBuilder()
                    .attributes(attributesWithCacheControl(aiMessage.attributes()))
                    .build();
            case ToolExecutionResultMessage toolResult -> toolResult.toBuilder()
                    .attributes(attributesWithCacheControl(toolResult.attributes()))
                    .build();
            default -> message;
        };
    }

    private static UserMessage copyUserMessageWithCacheControl(UserMessage original) {
        UserMessage copy = original.name() == null
                ? new UserMessage(original.contents())
                : new UserMessage(original.name(), original.contents());
        if (original.attributes() != null) {
            copy.attributes().putAll(original.attributes());
        }
        copy.attributes().put(CACHE_CONTROL_ATTRIBUTE, EPHEMERAL);
        return copy;
    }

    private static Map<String, Object> attributesWithCacheControl(Map<String, Object> original) {
        Map<String, Object> attributes = original != null ? new HashMap<>(original) : new HashMap<>();
        attributes.put(CACHE_CONTROL_ATTRIBUTE, EPHEMERAL);
        return attributes;
    }
}
