package dev.local.ai.core.connections;

import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Ollama connection configuration with Ollama-specific parameters.
 */
@JsonTypeName("ollama")
public record OllamaConnection(
    String id,
    String name,
    String description,
    String baseUrl
) implements ModelProviderConnection {
    
    public OllamaConnection(String name, String description) {
        this(java.util.UUID.randomUUID().toString(), name, description, "http://localhost:11434");
    }
    
    @Override
    public ConnectionProvider providerType() {
        return ConnectionProvider.OLLAMA;
    }
}
