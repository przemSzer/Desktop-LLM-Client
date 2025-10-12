package dev.local.ai.core.models;

import dev.langchain4j.model.openai.OpenAiChatModelName;
import dev.local.ai.core.connections.OpenAIConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
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
        return Arrays.asList(OpenAiChatModelName.values())                     
            .stream()
            .map(model -> new ModelInfo(model.toString(), model.toString(), "OpenAI model: " + model.toString())) // Convert to ModelInfo
            .toList();
    }
}
