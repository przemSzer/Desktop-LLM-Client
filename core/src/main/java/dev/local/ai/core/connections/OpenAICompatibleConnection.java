package dev.local.ai.core.connections;

import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Connection to a server that speaks the OpenAI chat completions API
 * at a caller-supplied base URL, such as LM Studio, vLLM, or OpenRouter.
 * {@code baseUrl} must already include the {@code /v1} prefix, because the
 * client appends {@code chat/completions} and {@code models} to it.
 */
@JsonTypeName("openai-compatible")
public record OpenAICompatibleConnection(
    String id,
    String name,
    String description,
    String baseUrl,
    String apiKey
) implements ModelProviderConnection {

    public OpenAICompatibleConnection {
        baseUrl = stripTrailingSlashes(baseUrl);
        if (apiKey != null && apiKey.isBlank()) {
            apiKey = "";
        }
    }

    public OpenAICompatibleConnection(String name, String description) {
        this(java.util.UUID.randomUUID().toString(), name, description, "http://localhost:1234/v1", "");
    }

    /**
     * @return the API key, or {@code null} when it was left blank so the HTTP
     *         client omits the {@code Authorization} header
     */
    public String apiKeyOrNull() {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        return apiKey;
    }

    @Override
    public ConnectionProvider providerType() {
        return ConnectionProvider.OPENAI_COMPATIBLE;
    }

    @Override
    public String toString() {
        return "OpenAICompatibleConnection{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", baseUrl='" + baseUrl + '\'' +
                '}';
    }

    private static String stripTrailingSlashes(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        int end = trimmed.length();
        while (end > 0 && trimmed.charAt(end - 1) == '/') {
            end--;
        }
        return trimmed.substring(0, end);
    }
}
