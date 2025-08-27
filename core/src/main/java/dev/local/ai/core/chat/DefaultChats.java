package dev.local.ai.core.chat;

import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.local.ai.core.Chat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultChats {
    private static final Logger logger = LoggerFactory.getLogger(DefaultChats.class);

    public static Chat openAIGPT4oMini() {
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

    public static Chat localOllamaGemma3_270m() {
        logger.info("Creating local Ollama chat instance");
        var chatModel = OllamaChatModel.builder()
            .baseUrl("http://localhost:11434")
            .logRequests(true)
            .logResponses(true)
            .modelName("gemma3:270m")
            .build();
        return new Chat(chatModel);
    }
}
