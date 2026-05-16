package dev.local.ai.core.storage.conversations;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ConversationMetadata(
        String id,
        String title,
        Instant createdAt,
        Instant updatedAt,
        ForkInfo forkedFrom
) {

    public ConversationMetadata withUpdatedAt(Instant updatedAt) {
        return new ConversationMetadata(id, title, createdAt, updatedAt, forkedFrom);
    }

    public ConversationMetadata withTitle(String title) {
        return new ConversationMetadata(id, title, createdAt, updatedAt, forkedFrom);
    }

    public ConversationSummary toSummary() {
        return new ConversationSummary(id, title, createdAt, updatedAt);
    }
}
