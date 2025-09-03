# Ollama Connection Implementation

This package contains the implementation for Ollama connection management, including a dialog form for creating new Ollama connections.

## Components

### OllamaConnectionDialog
- **Purpose**: Main dialog class that implements `INewConnectionDialog`
- **Features**: 
  - Shows a modal dialog with Ollama connection form
  - Handles save/cancel operations
  - Integrates with `ConnectionsStore` for persistence
  - Provides error handling and user feedback

### OllamaConnectionForm
- **Purpose**: Controller for the Ollama connection form
- **Features**:
  - Form validation (name, URL, port)
  - Default values for Ollama (localhost:11434)
  - Save/Cancel button handling
  - Input validation with visual feedback

### OllamaConnectionForm.fxml
- **Purpose**: FXML layout definition for the connection form
- **Fields**:
  - Name (TextField)
  - Description (TextArea)
  - URL (TextField) - defaults to "http://localhost"
  - Port (TextField) - defaults to "11434"
  - Save/Cancel buttons

## Usage

### Running the Demo

```bash
cd ui
mvn compile exec:java -Dexec.mainClass="dev.local.ai.ui.connection.ollama.OllamaConnectionDialogDemo"
```

### Integration

The dialog is automatically integrated into the main connections view:

1. User clicks "Select Provider" → "Ollama"
2. `OllamaConnectionDialog.show()` is called
3. Form is displayed with Ollama-specific fields
4. User fills in connection details
5. On save, connection is persisted to `ConnectionsStore`
6. Connections list is refreshed automatically

## Form Validation

- **Name**: Required, cannot be empty
- **URL**: Required, cannot be empty
- **Port**: Required, must be a valid integer between 1-65535
- **Description**: Optional

## Default Values

- **URL**: `http://localhost`
- **Port**: `11434` (standard Ollama port)

## Error Handling

- Form validation with visual feedback (red borders)
- Error dialogs for save failures
- Logging for debugging purposes

## Testing

Run the unit tests:

```bash
cd ui
mvn test -Dtest=OllamaConnectionFormTest
```

## Dependencies

- JavaFX 24.0.1
- SLF4J for logging
- Core connections package (`dev.local.ai.core.connections`)

## Future Enhancements

- Connection testing before saving
- URL validation (check if Ollama is reachable)
- Model selection dropdown
- Advanced configuration options
- Connection editing (currently only supports creation)
