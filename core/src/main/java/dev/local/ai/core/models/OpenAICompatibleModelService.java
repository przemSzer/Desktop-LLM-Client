package dev.local.ai.core.models;

import dev.langchain4j.model.catalog.ModelDescription;
import dev.langchain4j.model.openai.OpenAiModelCatalog;
import dev.local.ai.core.connections.OpenAICompatibleConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

/**
 * Loads models from an OpenAI-compatible {@code GET /v1/models} endpoint.
 */
public class OpenAICompatibleModelService implements AvailableModelsService {

    private static final Logger logger = LoggerFactory.getLogger(OpenAICompatibleModelService.class);

    private final OpenAICompatibleConnection connection;

    public OpenAICompatibleModelService(OpenAICompatibleConnection connection) {
        this.connection = connection;
    }

    @Override
    public List<ModelInfo> loadModels() {
        var builder = OpenAiModelCatalog.builder()
            .baseUrl(connection.baseUrl())
            .connectTimeout(Duration.ofSeconds(10))
            .readTimeout(Duration.ofMinutes(2));
        var apiKey = connection.apiKeyOrNull();
        if (apiKey != null) {
            builder.apiKey(apiKey);
        }
        var result = builder.build().listModels().stream()
            .map(this::toModelInfo)
            .toList();
        logger.debug("Loaded {} models from OpenAI-compatible endpoint {}", result.size(), connection.baseUrl());
        return result;
    }

    private ModelInfo toModelInfo(ModelDescription model) {
        String id = model.name();
        String displayName = model.displayName();
        String name = displayName != null && !displayName.isBlank() ? displayName : id;
        return new ModelInfo(
            id,
            name,
            "OpenAI compatible model: " + id,
            ModelInfo.NOT_SPECIFIED,
            ModelInfo.NOT_SPECIFIED
        );
    }
}
