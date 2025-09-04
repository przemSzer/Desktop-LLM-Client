package dev.local.ai.core.models;

import java.util.List;

/**
 * Service interface for loading available models from different AI providers.
 * This interface abstracts the model loading logic and allows for different
 * implementations for different providers (Ollama, OpenAI, etc.).
 */
public interface ModelService {
    
    /**
     * Loads available models for the given connection.
     * 
     * @param connection The connection to load models for
     * @return A list of available models
     */
    List<ModelInfo> loadModels();    
}
