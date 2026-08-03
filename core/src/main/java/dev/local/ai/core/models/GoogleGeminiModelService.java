package dev.local.ai.core.models;

import java.time.Duration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.langchain4j.model.catalog.ModelDescription;
import dev.langchain4j.model.catalog.ModelType;
import dev.langchain4j.model.googleai.GoogleAiGeminiModelCatalog;
import dev.local.ai.core.connections.GoogleConnection;

/**
 * Loads available Gemini models using the Google AI model catalog API.
 */
public class GoogleGeminiModelService implements AvailableModelsService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleGeminiModelService.class);

    private final GoogleConnection connection;

    public GoogleGeminiModelService(GoogleConnection connection) {
        this.connection = connection;
    }

    @Override
    public List<ModelInfo> loadModels() {
        try {
            GoogleAiGeminiModelCatalog catalog = GoogleAiGeminiModelCatalog.builder()
                .apiKey(connection.apiKey())
                .timeout(Duration.ofMinutes(2))
                .build();
            var result = catalog.listModels().stream()
                .filter(m -> m.type() == ModelType.CHAT)
                .map(this::toModelInfo)
                .toList();
            logger.debug("Loaded {} models from Google Gemini", result.size());
            return result;
        } catch (Exception e) {
            logger.error("Failed to list Gemini models", e);
            throw new RuntimeException("Failed to list Gemini models: " + e.getMessage(), e);
        }
    }

    private ModelInfo toModelInfo(ModelDescription m) {
        String id = m.name();
        String name = m.displayName() != null && !m.displayName().isBlank() ? m.displayName() : id;
        String description = m.description() != null && !m.description().isBlank()
            ? m.description()
            : "Google Gemini model";
        return new ModelInfo(id, name, description, m.maxInputTokens(), m.maxOutputTokens());
    }
}
