package dev.local.ai.core.chat.messages;

import dev.langchain4j.model.anthropic.AnthropicTokenUsage;
import dev.langchain4j.model.googleai.GoogleAiGeminiTokenUsage;
import dev.langchain4j.model.openai.OpenAiTokenUsage;
import dev.langchain4j.model.output.TokenUsage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenUsageStatisticsExtractorTest {

    private final TokenUsageStatisticsExtractor statistics = new TokenUsageStatisticsExtractor();

    @Test
    void should_keep_anthropic_cache_outside_input() {
        TokenUsage usage = AnthropicTokenUsage.builder()
                .inputTokenCount(12)
                .outputTokenCount(4)
                .cacheReadInputTokens(800)
                .cacheCreationInputTokens(200)
                .build();

        assertThat(statistics.from(usage))
                .isEqualTo(new Statistics(12, 4, 16, 800, 200, false));
    }

    @Test
    void should_treat_openai_cache_as_part_of_input() {
        TokenUsage usage = OpenAiTokenUsage.builder()
                .inputTokenCount(1000)
                .outputTokenCount(20)
                .totalTokenCount(1020)
                .inputTokensDetails(OpenAiTokenUsage.InputTokensDetails.builder()
                        .cachedTokens(800)
                        .cacheWriteTokens(50)
                        .build())
                .build();

        assertThat(statistics.from(usage))
                .isEqualTo(new Statistics(1000, 20, 1020, 800, 50, true));
    }

    @Test
    void should_leave_openai_cache_empty_when_details_are_missing() {
        TokenUsage usage = OpenAiTokenUsage.builder()
                .inputTokenCount(30)
                .outputTokenCount(5)
                .totalTokenCount(35)
                .build();

        assertThat(statistics.from(usage))
                .isEqualTo(new Statistics(30, 5, 35, null, null, true));
    }

    @Test
    void should_read_gemini_cached_content_without_a_write_count() {
        TokenUsage usage = GoogleAiGeminiTokenUsage.builder()
                .inputTokenCount(500)
                .outputTokenCount(10)
                .totalTokenCount(510)
                .cachedContentTokenCount(400)
                .build();

        assertThat(statistics.from(usage))
                .isEqualTo(new Statistics(500, 10, 510, 400, null, true));
    }

    @Test
    void should_ignore_cache_when_provider_does_not_report_it() {
        assertThat(statistics.from(new TokenUsage(7, 3)))
                .isEqualTo(new Statistics(7, 3, 10));
        assertThat(statistics.from(null))
                .isEqualTo(new Statistics(0, 0, 0));
    }
}
