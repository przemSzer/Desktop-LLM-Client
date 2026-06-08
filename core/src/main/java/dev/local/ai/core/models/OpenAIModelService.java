package dev.local.ai.core.models;

import dev.langchain4j.model.catalog.ModelDescription;
import dev.langchain4j.model.catalog.ModelType;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import dev.langchain4j.model.openai.OpenAiModelCatalog;
import dev.local.ai.core.connections.OpenAIConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Service for loading available models from OpenAI.
 * Uses the OpenAI API to fetch the list of available models.
 */
public class OpenAIModelService implements AvailableModelsService {
    
    private static final Logger logger = LoggerFactory.getLogger(OpenAIModelService.class);
    private OpenAIConnection connection;
    
    public OpenAIModelService(OpenAIConnection connection) {
        this.connection = connection;
    }
        
    @Override
    public List<ModelInfo> loadModels() {
        OpenAiModelCatalog catalog = OpenAiModelCatalog.builder()
            .apiKey(connection.apiKey())
            .build();        
        var result = catalog.listModels()
            .stream()
            // .filter(m -> m.type() == ModelType.CHAT)
            .map(this::toModelInfo)
            .toList();
        logger.debug("Loaded {} models from OpenAI", result.size());
        return result;
    }

    private ModelInfo toModelInfo(ModelDescription model) {
        return new ModelInfo(model.name(), model.displayName(), "OpenAI model: " + model.toString());
    }
}
