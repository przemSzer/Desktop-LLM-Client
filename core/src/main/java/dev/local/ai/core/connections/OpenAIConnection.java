package dev.local.ai.core.connections;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.jspecify.annotations.NonNull;

@JsonTypeName("openai")
public record OpenAIConnection(
    String id,
    String name,
    String description,
    String apiKey
) implements ModelProviderConnection {
    
    public OpenAIConnection(String name, String description) {
        this(java.util.UUID.randomUUID().toString(), name, description, "https://api.openai.com/v1");
    }

    @Override
    public ConnectionProvider providerType() {
        return ConnectionProvider.OPENAI;
    }

    @Override
    public String toString() {
        return "OpenAIConnection{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
