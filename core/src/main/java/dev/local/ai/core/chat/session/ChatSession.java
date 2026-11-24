package dev.local.ai.core.chat.session;

import dev.langchain4j.memory.ChatMemory;
import dev.local.ai.core.chat.streaming.StreamingChat;
import dev.local.ai.core.tools.gates.WaitForApprovalGate;
import dev.local.ai.core.tools.session.SessionToolApprovalConfiguration;
import dev.local.ai.core.tools.session.SessionToolSelection;

public record ChatSession(
        String conversationId,
        ChatMemory chatMemory,
        StreamingChat chat,
        WaitForApprovalGate approval,
        SessionToolApprovalConfiguration toolApproval,
        SessionToolSelection toolSelection
) implements AutoCloseable
{
    @Override
    public void close() {
        chat.close();
        approval.close();
        toolSelection.close();
    }
}
