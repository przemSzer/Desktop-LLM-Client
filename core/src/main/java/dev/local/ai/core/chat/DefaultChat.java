package dev.local.ai.core.chat;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.local.ai.core.Chat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultChat {
    private static final Logger logger = LoggerFactory.getLogger(DefaultChat.class);

    public static Chat createDefaultChat() {
        logger.info("Creating default chat instance with OpenAI model");
        
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            logger.error("OPENAI_API_KEY environment variable is not set");
            throw new IllegalStateException("OPENAI_API_KEY environment variable is required");
        }
        
        var chatModel = OpenAiChatModel.builder()
            .apiKey(apiKey)
            .modelName("gpt-4o-mini")
            .build();
            
        logger.info("OpenAI chat model created successfully with model: gpt-4o-mini");
        return new Chat(chatModel);
    }
}
