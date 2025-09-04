package dev.local.ai.core.models;

import dev.local.ai.core.connections.OpenAIConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Service for loading available models from OpenAI.
 * Uses the OpenAI API to fetch the list of available models.
 */
public class OpenAIModelService implements ModelService {
    
    private static final Logger logger = LoggerFactory.getLogger(OpenAIModelService.class);
    private OpenAIConnection connection;
    
    public OpenAIModelService(OpenAIConnection connection) {
        this.connection = connection;
    }
        
    @Override
    public List<ModelInfo> loadModels() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'loadModels'");
    }
}
