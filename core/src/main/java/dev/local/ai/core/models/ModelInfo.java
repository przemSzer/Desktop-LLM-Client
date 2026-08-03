package dev.local.ai.core.models;

public record ModelInfo(
        String id,
        String name,
        String description,
        int maxInputTokens,
        int maxOutputTokens
) {
    public static final int NOT_SPECIFIED = -1;
}
