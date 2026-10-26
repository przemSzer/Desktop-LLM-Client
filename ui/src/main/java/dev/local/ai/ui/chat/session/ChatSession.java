package dev.local.ai.ui.chat.session;

import dev.langchain4j.memory.ChatMemory;
import dev.local.ai.core.chat.streaming.StreamingChat;
import dev.local.ai.core.tools.gates.WaitForApprovalGate;

public record ChatSession(
        String conversationId,
        ChatMemory chatMemory,
        StreamingChat chat,
        WaitForApprovalGate approval
        ) implements AutoCloseable
{
    @Override
    public void close() {
        chat.close();
        approval.close();
    }
}
