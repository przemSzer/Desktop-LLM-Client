package dev.local.ai.core.connections;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("anthropic")
public record AnthropicConnection(
    String id,
    String name,
    String description,
    String apiKey
) implements ModelProviderConnection {

    public AnthropicConnection(String name, String description) {
        this(java.util.UUID.randomUUID().toString(), name, description, "");
    }

    @Override
    public ConnectionProvider providerType() {
        return ConnectionProvider.ANTHROPIC;
    }

    @Override
    public String toString() {
        return "AnthropicConnection{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
