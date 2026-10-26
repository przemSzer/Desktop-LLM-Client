package dev.local.ai.core.chat.streaming;

import dev.local.ai.core.chat.messages.Message;

public sealed interface ChatEvent permits
        ChatEvent.UserMessageAdded,
        ChatEvent.PartialText,
        ChatEvent.PartialThinking,
        ChatEvent.AiMessageCompleted,
        ChatEvent.ToolCallRequested,
        ChatEvent.ToolFinished,
        ChatEvent.ChatError,
        ChatEvent.TurnCancelled,
        ChatEvent.MemoryCleared {

    record UserMessageAdded(String requestId, Message message) implements ChatEvent {
    }

    record PartialText(String requestId, String delta) implements ChatEvent {
    }

    record PartialThinking(String requestId, String delta) implements ChatEvent {
    }

    record AiMessageCompleted(String requestId, Message message) implements ChatEvent {
    }

    record ToolCallRequested(String requestId, String toolRequestId, String name, String argumentsText) implements ChatEvent {
    }

    record ToolFinished(String requestId, String toolRequestId, String toolName, String text, boolean error) implements ChatEvent {
    }

    record ChatError(String message) implements ChatEvent {
    }

    record TurnCancelled(String requestId) implements ChatEvent {
    }

    record MemoryCleared() implements ChatEvent {
    }
}
