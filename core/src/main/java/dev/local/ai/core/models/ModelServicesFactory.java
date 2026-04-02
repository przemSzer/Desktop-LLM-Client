package dev.local.ai.core.models;

import dev.local.ai.core.connections.GoogleConnection;
import dev.local.ai.core.connections.ModelProviderConnection;
import dev.local.ai.core.connections.OllamaConnection;
import dev.local.ai.core.connections.OpenAIConnection;

public class ModelServicesFactory {

    public static AvailableModelsService forConnection(ModelProviderConnection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("Connection cannot be null");
        }
        switch(connection){
            case OllamaConnection ollamaConnection:
                return new OllamaModelService(ollamaConnection);
            case OpenAIConnection openAIConnection:
                return new OpenAIModelService(openAIConnection);
            case GoogleConnection googleConnection:
                return new GoogleGeminiModelService(googleConnection);
            default:
                throw new IllegalArgumentException("Unsupported connection type: " + connection.getClass());
        }
    }
}
