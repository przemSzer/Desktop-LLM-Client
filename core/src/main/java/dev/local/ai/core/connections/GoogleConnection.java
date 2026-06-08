package dev.local.ai.core.connections;

import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Google AI (Gemini) connection configuration.
 */
@JsonTypeName("google")
public record GoogleConnection(
    String id,
    String name,
    String description,
    String apiKey
) implements ModelProviderConnection {

    public GoogleConnection(String name, String description) {
        this(java.util.UUID.randomUUID().toString(), name, description, "");
    }

    @Override
    public ConnectionProvider providerType() {
        return ConnectionProvider.GOOGLE;
    }

    @Override
    public String toString() {
        return "GoogleConnection{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
