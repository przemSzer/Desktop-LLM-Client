package dev.local.ai.core.chat.messages;

import dev.langchain4j.model.anthropic.AnthropicTokenUsage;
import dev.langchain4j.model.googleai.GoogleAiGeminiTokenUsage;
import dev.langchain4j.model.openai.OpenAiTokenUsage;
import dev.langchain4j.model.output.TokenUsage;

public final class TokenUsageStatisticsExtractor {

    public Statistics from(TokenUsage usage) {
        if (usage == null) {
            return new Statistics(0, 0, 0);
        }
        int input = count(usage.inputTokenCount());
        int output = count(usage.outputTokenCount());
        Integer reportedTotal = usage.totalTokenCount();
        int total = reportedTotal != null ? reportedTotal : input + output;
        return switch (usage) {
            case AnthropicTokenUsage anthropic -> new Statistics(
                    input,
                    output,
                    total,
                    anthropic.cacheReadInputTokens(),
                    anthropic.cacheCreationInputTokens(),
                    false);
            case OpenAiTokenUsage openAi -> fromOpenAi(input, output, total, openAi);
            case GoogleAiGeminiTokenUsage gemini -> new Statistics(
                    input,
                    output,
                    total,
                    gemini.cachedContentTokenCount(),
                    null,
                    true);
            default -> new Statistics(input, output, total);
        };
    }

    private static Statistics fromOpenAi(int input, int output, int total, OpenAiTokenUsage usage) {
        OpenAiTokenUsage.InputTokensDetails details = usage.inputTokensDetails();
        Integer cacheRead = details == null ? null : details.cachedTokens();
        Integer cacheWrite = details == null ? null : details.cacheWriteTokens();
        return new Statistics(input, output, total, cacheRead, cacheWrite, true);
    }

    private static int count(Integer value) {
        return value != null ? value : 0;
    }
}
