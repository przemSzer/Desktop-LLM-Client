package dev.local.ai.core.models.anthropic;

import dev.langchain4j.model.anthropic.AnthropicTokenUsage;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogAnthropicCacheInfo implements ChatModelListener {

    private static final Logger logger = LoggerFactory.getLogger(LogAnthropicCacheInfo.class);

    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        logAnthropicCacheUsage(responseContext.chatResponse());
    }

    private void logAnthropicCacheUsage(ChatResponse response) {
        if (response.tokenUsage() instanceof AnthropicTokenUsage anthropicUsage) {
            logger.debug(
                    "Anthropic prompt cache: created={}, read={}, uncachedInput={}, output={}",
                    anthropicUsage.cacheCreationInputTokens(),
                    anthropicUsage.cacheReadInputTokens(),
                    anthropicUsage.inputTokenCount(),
                    anthropicUsage.outputTokenCount());
        }
    }

}
