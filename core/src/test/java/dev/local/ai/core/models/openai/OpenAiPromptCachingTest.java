package dev.local.ai.core.models.openai;

import dev.langchain4j.model.openai.OpenAiPromptCacheOptions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiPromptCachingTest {

    @Test
    void should_enable_implicit_cache_when_model_is_gpt_5_6_or_newer() {
        assertThat(OpenAiPromptCaching.forChat("gpt-5.6"))
                .isEqualTo(implicitCache());
        assertThat(OpenAiPromptCaching.forChat("gpt-5.6-mini"))
                .isEqualTo(implicitCache());
        assertThat(OpenAiPromptCaching.forChat("ft:gpt-5.7:org:custom"))
                .isEqualTo(implicitCache());
        assertThat(OpenAiPromptCaching.forChat("gpt-6"))
                .isEqualTo(implicitCache());
    }

    @Test
    void should_leave_automatic_cache_untouched_when_model_predates_gpt_5_6() {
        assertThat(OpenAiPromptCaching.forChat("gpt-4o"))
                .isNull();
        assertThat(OpenAiPromptCaching.forChat("gpt-4.1"))
                .isNull();
        assertThat(OpenAiPromptCaching.forChat("gpt-5"))
                .isNull();
        assertThat(OpenAiPromptCaching.forChat("gpt-5-mini"))
                .isNull();
        assertThat(OpenAiPromptCaching.forChat("gpt-5.4"))
                .isNull();
        assertThat(OpenAiPromptCaching.forChat("o3"))
                .isNull();
    }

    @Test
    void should_leave_cache_options_unset_when_model_name_is_missing() {
        assertThat(OpenAiPromptCaching.forChat(null))
                .isNull();
        assertThat(OpenAiPromptCaching.forChat("  "))
                .isNull();
    }

    private static OpenAiPromptCacheOptions implicitCache() {
        return OpenAiPromptCacheOptions.builder()
                .mode(OpenAiPromptCacheOptions.MODE_IMPLICIT)
                .ttl(OpenAiPromptCacheOptions.TTL_30M)
                .build();
    }
}
