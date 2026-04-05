package dev.local.ai.core.chat.messages;

import dev.langchain4j.model.output.TokenUsage;

public record Statistics(int inputTokens, int outputTokens, int totalTokens) {

    public static Statistics fromTokenUsage(TokenUsage usage) {
        if (usage == null) {
            return new Statistics(0, 0, 0);
        }
        int input = nullableInt(usage.inputTokenCount());
        int output = nullableInt(usage.outputTokenCount());
        Integer total = usage.totalTokenCount();
        int totalTokens = total != null ? total : input + output;
        return new Statistics(input, output, totalTokens);
    }

    private static int nullableInt(Integer value) {
        return value != null ? value : 0;
    }
}
