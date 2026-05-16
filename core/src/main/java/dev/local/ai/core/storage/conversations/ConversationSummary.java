package dev.local.ai.core.storage.conversations;

import java.time.Instant;

public record ConversationSummary(String id, String title, Instant createdAt, Instant updatedAt) {
}
