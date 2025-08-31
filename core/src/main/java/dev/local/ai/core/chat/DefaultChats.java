package dev.local.ai.core.chat;

import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.local.ai.core.Chat;
import dev.local.ai.core.ILLMChat;
import dev.local.ai.core.StreamingChat;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultChats {
    private static final Logger logger = LoggerFactory.getLogger(DefaultChats.class);

    public static ILLMChat defaultChat() {
        return localOllamaGemma3n_Streaming();
    }

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
            .timeout(Duration.ofMinutes(5))
            .build();
            
        logger.info("OpenAI chat model created successfully with model: gpt-4o-mini");
        return new Chat(chatModel);
    }

    public static ILLMChat openAIGPT4oMiniStreaming() {
        logger.info("Creating default chat instance with OpenAI model");
        
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            logger.error("OPENAI_API_KEY environment variable is not set");
            throw new IllegalStateException("OPENAI_API_KEY environment variable is required");
        }
        
        OpenAiStreamingChatModel chatModel = OpenAiStreamingChatModel.builder()
            .apiKey(apiKey)
            .modelName("gpt-4o-mini")
            .timeout(Duration.ofMinutes(5))
            .build();
            
        logger.info("OpenAI chat model created successfully with model: gpt-4o-mini");
        return new StreamingChat(chatModel);
    }

    public static ILLMChat localOllamaGemma3_270m() {
        logger.info("Creating local Ollama chat instance");
        var chatModel = OllamaChatModel.builder()
            .baseUrl("http://localhost:11434")
            .logRequests(true)
            .logResponses(true)
            .modelName("gemma3:270m")
            .timeout(Duration.ofMinutes(5))
            .build();
        return new Chat(chatModel);
    }

    public static ILLMChat localOllamaGemma3_270mStreaming() {
        logger.info("Creating local Ollama chat instance");
        OllamaStreamingChatModel chatModel = OllamaStreamingChatModel.builder()
            .baseUrl("http://localhost:11434")
            .logRequests(true)
            .logResponses(true)
            .modelName("gemma3:270m")
            .timeout(Duration.ofMinutes(5))
            .build();
        return new StreamingChat(chatModel);
    }

    public static ILLMChat localOllamaGemma3n_Streaming() {
        logger.info("Creating local Ollama Gemma3n chat instance");
        OllamaStreamingChatModel chatModel = OllamaStreamingChatModel.builder()
            .baseUrl("http://localhost:11434")
            .logRequests(true)
            .logResponses(true)
            .modelName("gemma3n:latest")
            .timeout(Duration.ofMinutes(5))
            .build();
        return new StreamingChat(chatModel);
    }

    public static ILLMChat localOllamaPhi_streaming() {
        logger.info("Creating local Ollama Phi chat instance");
        OllamaStreamingChatModel chatModel = OllamaStreamingChatModel.builder()
            .baseUrl("http://localhost:11434")
            .logRequests(true)
            .logResponses(true)
            .modelName("phi4:latest")
            .timeout(Duration.ofMinutes(5))
            .build();
        return new StreamingChat(chatModel);
    }
}
