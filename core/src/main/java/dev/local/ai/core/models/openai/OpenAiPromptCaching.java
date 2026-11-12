package dev.local.ai.core.models.openai;

import dev.langchain4j.model.openai.OpenAiPromptCacheOptions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OpenAI caches a repeated prefix automatically on models before {@code gpt-5.6}.
 * From {@code gpt-5.6} onward a cache entry is matched only at a breakpoint, and the request
 * has to opt in with {@code prompt_cache_options}. {@link OpenAiPromptCacheOptions#MODE_IMPLICIT}
 * asks OpenAI to put that breakpoint on the newest eligible message.
 * <p>
 * The only TTL OpenAI accepts for this option is {@link OpenAiPromptCacheOptions#TTL_30M}.
 * Sending the option to an older model is rejected, so those models keep the automatic cache.
 */
public final class OpenAiPromptCaching {

    private static final Pattern GPT_VERSION = Pattern.compile("(?i)gpt-(\\d+)(?:\\.(\\d+))?");
    private static final int BREAKPOINT_CACHE_MAJOR = 5;
    private static final int BREAKPOINT_CACHE_MINOR = 6;

    private OpenAiPromptCaching() {
    }

    public static OpenAiPromptCacheOptions forChat(String modelName) {
        if (!requiresPromptCacheOptions(modelName)) {
            return null;
        }
        return OpenAiPromptCacheOptions.builder()
                .mode(OpenAiPromptCacheOptions.MODE_IMPLICIT)
                .ttl(OpenAiPromptCacheOptions.TTL_30M)
                .build();
    }

    private static boolean requiresPromptCacheOptions(String modelName) {
        if (modelName == null || modelName.isBlank()) {
            return false;
        }
        Matcher matcher = GPT_VERSION.matcher(modelName);
        if (!matcher.find()) {
            return false;
        }
        int major = Integer.parseInt(matcher.group(1));
        if (major > BREAKPOINT_CACHE_MAJOR) {
            return true;
        }
        if (major < BREAKPOINT_CACHE_MAJOR) {
            return false;
        }
        String minor = matcher.group(2);
        return minor != null && Integer.parseInt(minor) >= BREAKPOINT_CACHE_MINOR;
    }
}
