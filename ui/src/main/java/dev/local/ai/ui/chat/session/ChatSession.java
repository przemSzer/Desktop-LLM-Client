package dev.local.ai.ui.chat.session;

import dev.langchain4j.memory.ChatMemory;
import dev.local.ai.core.chat.streaming.StreamingChat;
import dev.local.ai.core.tools.gates.IApprovalProvider;

import java.util.function.Consumer;

public record ChatSession(
        String conversationId,
        ChatMemory chatMemory,
        StreamingChat chat,
        Consumer<IApprovalProvider> setApprovalProvider
        ) implements AutoCloseable
{
    @Override
    public void close() {
        chat.close();
    }
}
