package dev.local.ai.core.models;

import dev.langchain4j.model.anthropic.AnthropicModelCatalog;
import dev.langchain4j.model.catalog.ModelDescription;
import dev.local.ai.core.connections.AnthropicConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

public class AnthropicModelService implements AvailableModelsService {

    private static final Logger logger = LoggerFactory.getLogger(AnthropicModelService.class);

    private final AnthropicConnection connection;

    public AnthropicModelService(AnthropicConnection connection) {
        this.connection = connection;
    }

    @Override
    public List<ModelInfo> loadModels() {
        try {
            AnthropicModelCatalog catalog = AnthropicModelCatalog.builder()
                .apiKey(connection.apiKey())
                .timeout(Duration.ofMinutes(2))
                .build();
            var result = catalog.listModels().stream()
                .map(this::toModelInfo)
                .toList();
            logger.debug("Loaded {} models from Anthropic for connection {}", result.size(), connection.name());
            return result;
        } catch (Exception e) {
            logger.error("Failed to list Anthropic models", e);
            throw new RuntimeException("Failed to list Anthropic models: " + e.getMessage(), e);
        }
    }

    private ModelInfo toModelInfo(ModelDescription model) {
        String id = model.name();
        String name = model.displayName() != null && !model.displayName().isBlank() ? model.displayName() : id;
        String description = model.description() != null && !model.description().isBlank()
            ? model.description()
            : "Anthropic Claude model";
        return new ModelInfo(id, name, description);
    }
}
