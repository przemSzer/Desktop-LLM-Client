package dev.local.ai.core.chat.messages;

/**
 * Token counts for one model response.
 * <p>
 * {@code cacheReadTokens} and {@code cacheWriteTokens} are {@code null} when the provider did not
 * report them. Zero means the provider reported that nothing was read or written.
 * {@code cacheIncludedInInput} is true when those cache tokens are already part of
 * {@code inputTokens}, as with OpenAI and Gemini. Anthropic reports them separately, so its
 * {@code inputTokens} are only the uncached remainder.
 */
public record Statistics(
        int inputTokens,
        int outputTokens,
        int totalTokens,
        Integer cacheReadTokens,
        Integer cacheWriteTokens,
        boolean cacheIncludedInInput) {

    public Statistics(int inputTokens, int outputTokens, int totalTokens) {
        this(inputTokens, outputTokens, totalTokens, null, null, false);
    }
}
