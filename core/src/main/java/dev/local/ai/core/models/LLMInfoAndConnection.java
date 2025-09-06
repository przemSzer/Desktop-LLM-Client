package dev.local.ai.core.models;

import dev.local.ai.core.connections.ModelProviderConnection;

public record LLMInfoAndConnection(ModelInfo modelInfo, ModelProviderConnection connection) {
}
