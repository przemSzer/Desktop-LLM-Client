# Connections Module

This module implements a connections management system for AI providers following the MVVM (Model-View-ViewModel) pattern.

## Architecture

The implementation follows the MVVM pattern with clear separation of concerns:

- **Model** (`Connection.java`): Represents connection data with common parameters (icon, name, description)
- **ViewModel** (`ConnectionsViewModel.java`): Manages observable data and business logic
- **View** (`ConnectionsView.fxml`): Defines the UI layout
- **Controller** (`ConnectionsController.java`): Handles UI events and delegates to ViewModel

## Features

- **Action Panel**: Contains "Add new connection" label, provider selection menu, and delete button
- **Provider Support**: Currently supports OpenAI and Ollama (extensible for more providers)
- **Table View**: Displays connections with icon, name, and description columns
- **Selection Management**: Supports selecting and deleting connections
- **Status Updates**: Shows current status and selected connection information

## Package Structure

```
dev.local.ai.ui.connection/
├── model/
│   └── Connection.java                 # Base connection model
├── viewmodel/
│   └── ConnectionsViewModel.java       # ViewModel for connections
├── controller/
│   └── ConnectionsController.java      # Controller for connections view
├── openai/
│   └── OpenAIConnection.java          # OpenAI-specific implementation (placeholder)
├── ollama/
│   └── OllamaConnection.java          # Ollama-specific implementation (placeholder)
├── ConnectionsDemo.java                # Demo application
└── README.md                          # This file
```

## Usage

### Running the Demo

```bash
cd ui
mvn compile exec:java -Dexec.mainClass="dev.local.ai.ui.connection.ConnectionsDemo"
```

### Integration

To integrate the connections view into your main application:

1. Load the FXML:
```java
FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ConnectionsView.fxml"));
VBox connectionsView = loader.load();
```

2. Access the controller if needed:
```java
ConnectionsController controller = loader.getController();
```

## Current Status

- ✅ MVVM pattern implementation
- ✅ Basic UI with action panel and table view
- ✅ Provider type enumeration (OpenAI, Ollama)
- ✅ Connection management (add, delete, select)
- ✅ Data binding and observable properties
- ✅ Sample data for demonstration
- 🔄 Dialog implementation for new connections (TODO)
- 🔄 Provider-specific configuration (TODO)

## Next Steps

1. **Implement Connection Dialogs**: Create dialogs for adding new connections with provider-specific fields
2. **Provider-Specific Logic**: Implement actual connection logic for OpenAI and Ollama
3. **Configuration Persistence**: Add ability to save/load connection configurations
4. **Connection Testing**: Add functionality to test connections before saving
5. **Validation**: Add input validation for connection parameters

## Testing

Run the unit tests:

```bash
cd ui
mvn test -Dtest=ConnectionsViewModelTest
```

## Dependencies

- JavaFX 24.0.1
- SLF4J for logging
- JUnit 5 for testing
