package dev.local.ai.core.models;

import java.time.Duration;

import dev.langchain4j.http.client.HttpClientBuilder;
import dev.langchain4j.http.client.HttpClientBuilderLoader;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.local.ai.core.connections.GoogleConnection;
import dev.local.ai.core.connections.OllamaConnection;
import dev.local.ai.core.connections.OpenAIConnection;

public class StreamingChatModelsProvider {

    public StreamingChatModel createStreamingChatModel(LLMInfoAndConnection modelInfo) {
        switch(modelInfo.connection()){
            case OllamaConnection ollamaConnection:
                return ollamaChatModel(modelInfo);
            case OpenAIConnection openAIConnection:
                return openAIChatModel(modelInfo);
            case GoogleConnection googleConnection:
                return googleGeminiChatModel(modelInfo);
            default:
                throw new IllegalArgumentException("Unsupported connection type: " + modelInfo.connection().getClass());
        }    
    }

    private StreamingChatModel openAIChatModel(LLMInfoAndConnection modelInfo) {
        var openAIConnection = (OpenAIConnection) modelInfo.connection();
        return OpenAiStreamingChatModel
            .builder()
            .apiKey(openAIConnection.apiKey())
            .modelName(modelInfo.modelInfo().name())
            .returnThinking(true)
            .timeout(Duration.ofMinutes(5))
            .build();
    }

    private StreamingChatModel ollamaChatModel(LLMInfoAndConnection modelInfo) {
        var ollamaConnection = (OllamaConnection) modelInfo.connection();
        var httpClientBuilder = HttpClientBuilderLoader.loadHttpClientBuilder()
            .readTimeout(Duration.ofSeconds(20))
            .connectTimeout(Duration.ofMinutes(5));
        return OllamaStreamingChatModel
            .builder()
            .baseUrl(ollamaConnection.baseUrl())
            .logRequests(true)
            .logResponses(true)
            .modelName(modelInfo.modelInfo().name())
            .think(true)
            .returnThinking(true)
            .httpClientBuilder(httpClientBuilder)
            .timeout(Duration.ofMinutes(5))
            .build();
    }

    private StreamingChatModel googleGeminiChatModel(LLMInfoAndConnection modelInfo) {
        var googleConnection = (GoogleConnection) modelInfo.connection();
        return GoogleAiGeminiStreamingChatModel
            .builder()
            .apiKey(googleConnection.apiKey())
            .modelName(modelInfo.modelInfo().id())
            .timeout(Duration.ofMinutes(5))
            .build();
    }

}
