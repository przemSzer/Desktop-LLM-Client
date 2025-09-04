# Models Module

This module implements a model selector control for AI providers following the MVVM (Model-View-ViewModel) pattern.

## Architecture

The implementation follows the MVVM pattern with clear separation of concerns:

- **Model** (`ModelInfoViewModel.java`): JavaFX wrapper for core ModelInfo with observable properties
- **ViewModel** (`ModelSelectorViewModel.java`): Manages observable data and business logic
- **View** (`ModelSelector.fxml`): Defines the UI layout
- **Controller** (`ModelSelectorController.java`): Handles UI events and delegates to ViewModel
- **Services** (in core module): `ModelService`, `OllamaModelService`, `OpenAIModelService` handle model loading from different providers

## Features

- **Connection Selection**: Select from available connections (Ollama, OpenAI)
- **Lazy Loading**: Models are loaded asynchronously when a connection is selected
- **Provider Support**: Currently supports Ollama and OpenAI (extensible for more providers)
- **Status Updates**: Shows current status and loading indicators
- **Refresh Capability**: Manual refresh for connections and models

## Package Structure

```
dev.local.ai.ui.models/
├── model/
│   └── ModelInfoViewModel.java        # JavaFX wrapper for core ModelInfo
├── viewmodel/
│   └── ModelSelectorViewModel.java    # ViewModel for model selector
├── controller/
│   └── ModelSelectorController.java   # Controller for model selector view
├── ModelSelectorDemo.java             # Demo application
└── README.md                          # This file

dev.local.ai.core.models/ (in core module)
├── ModelInfo.java                     # Core model data class (no JavaFX dependencies)
├── ModelService.java                  # Service interface for model loading
├── OllamaModelService.java            # Ollama-specific model service
└── OpenAIModelService.java            # OpenAI-specific model service
```

## Usage

### Running the Demo

```bash
cd ui
mvn compile exec:java -Dexec.mainClass="dev.local.ai.ui.models.ModelSelectorDemo"
```

### Integration

To integrate the model selector into your main application:

1. Load the FXML:
```java
FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ModelSelector.fxml"));
VBox modelSelector = loader.load();
ModelSelectorController controller = loader.getController();
```

2. Access the ViewModel:
```java
ModelSelectorViewModel viewModel = controller.getViewModel();
ConnectionViewModel selectedConnection = controller.getSelectedConnection();
ModelInfoViewModel selectedModel = controller.getSelectedModel();
```

3. Listen for changes:
```java
viewModel.selectedConnectionProperty().addListener((obs, old, newVal) -> {
    if (newVal != null) {
        System.out.println("Connection selected: " + newVal.getName());
    }
});

viewModel.selectedModelProperty().addListener((obs, old, newVal) -> {
    if (newVal != null) {
        System.out.println("Model selected: " + newVal.getName());
        // Access core model info if needed
        ModelInfo coreModel = newVal.getCoreModelInfo();
    }
});
```

## Model Loading

The model loading is implemented with lazy loading:

1. **Connection Selection**: When a connection is selected, the appropriate model service is identified
2. **Async Loading**: Models are loaded asynchronously using CompletableFuture
3. **UI Updates**: The UI is updated on the JavaFX Application Thread using Platform.runLater()
4. **Error Handling**: Failed model loading is handled gracefully with user feedback

## Extending for New Providers

To add support for a new AI provider:

1. Create a new model service in the core module implementing `ModelService`:
```java
// In core/src/main/java/dev/local/ai/core/models/
public class NewProviderModelService implements ModelService {
    @Override
    public CompletableFuture<List<ModelInfo>> loadModels(ModelProviderConnection connection) {
        // Implementation for loading models from new provider
    }
    
    @Override
    public boolean canHandle(ModelProviderConnection connection) {
        return connection.providerType() == ConnectionProvider.NEW_PROVIDER;
    }
}
```

2. Add the service to the ModelSelectorViewModel:
```java
this.modelServices = List.of(
    new OllamaModelService(),
    new OpenAIModelService(),
    new NewProviderModelService()  // Add new service
);
```

## Dependencies

- JavaFX for UI components
- SLF4J for logging
- Core module for connection management
- HTTP Client for API calls to AI providers

## Error Handling

The module includes comprehensive error handling:

- Network timeouts and connection failures
- Invalid API responses
- Missing API keys or configuration
- Graceful fallback to default models when API calls fail

## Future Enhancements

1. **Caching**: Cache loaded models to avoid repeated API calls
2. **Model Details**: Display additional model information (size, capabilities, etc.)
3. **Favorites**: Allow users to mark favorite models
4. **Search/Filter**: Add search and filtering capabilities for models
5. **Model Validation**: Validate model availability before selection
