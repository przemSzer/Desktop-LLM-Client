package dev.local.ai.core.models;

import dev.local.ai.core.connections.OllamaConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import dev.langchain4j.model.ollama.OllamaModels;

/**
 * Service for loading available models from Ollama.
 * Uses the Ollama API to fetch the list of available models.
 */
public class OllamaModelService implements AvailableModelsService {
    
    private static final Logger logger = LoggerFactory.getLogger(OllamaModelService.class);    
    private OllamaConnection connection;
    
    public OllamaModelService(OllamaConnection connection) {
        this.connection = connection;
    }
    
    @Override
    public List<ModelInfo> loadModels() {
        try{
            logger.info("Loading models from Ollama at: {}", this.connection.baseUrl());
            var ollamaModelsProvider = OllamaModels.builder()
                .baseUrl(this.connection.baseUrl())
                .maxRetries(5)
                .build();
            var models = ollamaModelsProvider.availableModels().content();
            logger.info("Loaded {} models from Ollama", models.size());
            return models.stream().map(model -> new ModelInfo(model.getName(), model.getName(), "Ollama model: " + model.getDetails().getFamily())).toList();
        } catch (Exception e) {
            logger.error("Error loading models from Ollama", e);
            return List.of();
        }        
    }    
}
