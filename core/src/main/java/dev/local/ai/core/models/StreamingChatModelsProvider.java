package dev.local.ai.core.models;

import java.time.Duration;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.local.ai.core.connections.OllamaConnection;
import dev.local.ai.core.connections.OpenAIConnection;

public class StreamingChatModelsProvider {

    public StreamingChatModel createStreamingChatModel(LLMInfoAndConnection modelInfo) {
        switch(modelInfo.connection()){
            case OllamaConnection ollamaConnection:
                return ollamaChatModel(modelInfo);
            case OpenAIConnection openAIConnection:
                return openAIChatModel(modelInfo);
            default:
                throw new IllegalArgumentException("Unsupported connection type: " + modelInfo.connection().getClass());
        }    
    }

    private StreamingChatModel openAIChatModel(LLMInfoAndConnection modelInfo) {
        throw new UnsupportedOperationException("Unimplemented method 'openAIChatModel'");
    }

    private StreamingChatModel ollamaChatModel(LLMInfoAndConnection modelInfo) {
        var ollamaConnection = (OllamaConnection) modelInfo.connection();
        return OllamaStreamingChatModel
            .builder()
            .baseUrl(ollamaConnection.baseUrl())
            .logRequests(true)
            .logResponses(true)
            .modelName(modelInfo.modelInfo().name())
            .timeout(Duration.ofMinutes(5))
            .build();
    }
    
}
