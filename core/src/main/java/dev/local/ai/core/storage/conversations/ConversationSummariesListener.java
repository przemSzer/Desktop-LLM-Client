package dev.local.ai.core.storage.conversations;

import java.util.List;

@FunctionalInterface
public interface ConversationSummariesListener {

    void onConversationSummariesChanged(List<ConversationSummary> summaries);
}
