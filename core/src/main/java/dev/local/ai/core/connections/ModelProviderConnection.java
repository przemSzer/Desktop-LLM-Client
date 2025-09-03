package dev.local.ai.core.connections;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = OllamaConnection.class, name = "ollama"),
    @JsonSubTypes.Type(value = OpenAIConnection.class, name = "openai")
})
public sealed interface ModelProviderConnection 
    permits OllamaConnection, OpenAIConnection {
    String id();
    String name();
    String description();
    ConnectionProvider providerType();
}
