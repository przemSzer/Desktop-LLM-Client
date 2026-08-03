package dev.local.ai.core.models;

import dev.local.ai.core.connections.AnthropicConnection;
import dev.local.ai.core.connections.GoogleConnection;
import dev.local.ai.core.connections.ModelProviderConnection;
import dev.local.ai.core.connections.OllamaConnection;
import dev.local.ai.core.connections.OpenAIConnection;

public class AvailableModelsServiceFactory {

    public AvailableModelsServiceFactory() {
    }

    public AvailableModelsService forConnection(ModelProviderConnection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("Connection cannot be null");
        }
        return switch (connection) {
            case OllamaConnection ollamaConnection -> new OllamaModelService(ollamaConnection);
            case OpenAIConnection openAIConnection -> new OpenAIModelService(openAIConnection);
            case GoogleConnection googleConnection -> new GoogleGeminiModelService(googleConnection);
            case AnthropicConnection anthropicConnection -> new AnthropicModelService(anthropicConnection);
            default -> throw new IllegalArgumentException("Unsupported connection type: " + connection.getClass());
        };
    }
}
