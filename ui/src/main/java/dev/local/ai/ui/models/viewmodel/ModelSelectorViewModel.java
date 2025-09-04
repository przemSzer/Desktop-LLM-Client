package dev.local.ai.ui.models.viewmodel;

import dev.local.ai.core.connections.ConnectionsStore;
import dev.local.ai.core.connections.ModelProviderConnection;
import dev.local.ai.ui.connection.viewmodel.ConnectionViewModel;
import dev.local.ai.ui.models.model.ModelInfoViewModel;
import dev.local.ai.core.models.ModelService;
import dev.local.ai.core.models.ModelServicesFactory;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * ViewModel for the Model Selector UI following MVVM pattern.
 * Manages the observable data and commands for the model selection interface.
 */
public class ModelSelectorViewModel {
    
    private static final Logger logger = LoggerFactory.getLogger(ModelSelectorViewModel.class);
    
    // Observable properties for data binding
    private final ListProperty<ConnectionViewModel> connections;
    private final ObjectProperty<ConnectionViewModel> selectedConnection;
    private final ListProperty<ModelInfoViewModel> availableModels;
    private final ObjectProperty<ModelInfoViewModel> selectedModel;
    private final StringProperty statusMessage;
    private final BooleanProperty isLoadingModels;
    
    // Dependencies
    private final ConnectionsStore connectionsStore;
    
    public ModelSelectorViewModel() {
        this.connections = new SimpleListProperty<>(FXCollections.observableArrayList());
        this.selectedConnection = new SimpleObjectProperty<>();
        this.availableModels = new SimpleListProperty<>(FXCollections.observableArrayList());
        this.selectedModel = new SimpleObjectProperty<>();
        this.statusMessage = new SimpleStringProperty("Ready");
        this.isLoadingModels = new SimpleBooleanProperty(false);
        
        // Initialize dependencies  
        this.connectionsStore = new ConnectionsStore();        
        // Load connections and setup listeners
        loadConnections();
        setupPropertyListeners();
        
        logger.info("ModelSelectorViewModel initialized");
    }
    
    private void loadConnections() {
        try {
            var mappedConnections = connectionsStore
                .readAll().stream()
                .map(connection -> new ConnectionViewModel(
                    connection.providerType(), 
                    connection.name(), 
                    connection.description(), 
                    connection.id()
                ))
                .toList();
            this.connections.set(FXCollections.observableArrayList(mappedConnections));
            setStatusMessage("Loaded " + mappedConnections.size() + " connections");
        } catch (Exception e) {
            logger.error("Failed to load connections", e);
            setStatusMessage("Failed to load connections: " + e.getMessage());
        }
    }
    
    private void setupPropertyListeners() {        
        selectedConnection.addListener((obs, oldConnection, newConnection) -> {
            if (newConnection != null) {
                loadModelsForConnection(newConnection);
            } else {               
                availableModels.clear();
                selectedModel.set(null);
            }
        });
    }
    
    private void loadModelsForConnection(ConnectionViewModel connectionViewModel) {
        setStatusMessage("Loading models for " + connectionViewModel.getName() + "...");
        isLoadingModels.set(true);
        availableModels.clear();
        selectedModel.set(null);
        ModelProviderConnection connection = findConnectionById(connectionViewModel.getId());
        
        if (connection == null) {
            setStatusMessage("Connection not found");
            isLoadingModels.set(false);
            return;
        }
        ModelService service = ModelServicesFactory.forConnection(connection);
                
        if (service == null) {
            setStatusMessage("No model service available for " + connection.providerType());
            isLoadingModels.set(false);
            return;
        }
        CompletableFuture.supplyAsync(() -> service.loadModels())
            .thenAccept(models -> {
                Platform.runLater(() -> {
                    var modelViewModels = models.stream()
                        .map(ModelInfoViewModel::new)
                        .toList();
                    availableModels.set(FXCollections.observableArrayList(modelViewModels));
                    setStatusMessage("Loaded " + models.size() + " models for " + connectionViewModel.getName());
                    isLoadingModels.set(false);
                    
                    // Auto-select first model if available
                    if (!modelViewModels.isEmpty()) {
                        selectedModel.set(modelViewModels.get(0));
                    }
                });
            })
            .exceptionally(throwable -> {
                Platform.runLater(() -> {
                    logger.error("Failed to load models for connection: " + connectionViewModel.getName(), throwable);
                    setStatusMessage("Failed to load models: " + throwable.getMessage());
                    isLoadingModels.set(false);
                });
                return null;
            });
    }
    
    private ModelProviderConnection findConnectionById(String connectionId) {
        return connectionsStore.readAll().stream()
            .filter(conn -> conn.id().equals(connectionId))
            .findFirst()
            .orElse(null);
    }
    
    // Property getters
    public ListProperty<ConnectionViewModel> connectionsProperty() {
        return connections;
    }
    
    public ObservableList<ConnectionViewModel> getConnections() {
        return connections.get();
    }
    
    public ObjectProperty<ConnectionViewModel> selectedConnectionProperty() {
        return selectedConnection;
    }
    
    public ConnectionViewModel getSelectedConnection() {
        return selectedConnection.get();
    }
    
    public void setSelectedConnection(ConnectionViewModel connection) {
        selectedConnection.set(connection);
    }
    
    public ListProperty<ModelInfoViewModel> availableModelsProperty() {
        return availableModels;
    }
    
    public ObservableList<ModelInfoViewModel> getAvailableModels() {
        return availableModels.get();
    }
    
    public ObjectProperty<ModelInfoViewModel> selectedModelProperty() {
        return selectedModel;
    }
    
    public ModelInfoViewModel getSelectedModel() {
        return selectedModel.get();
    }
    
    public void setSelectedModel(ModelInfoViewModel model) {
        selectedModel.set(model);
    }
    
    public StringProperty statusMessageProperty() {
        return statusMessage;
    }
    
    public String getStatusMessage() {
        return statusMessage.get();
    }
    
    public void setStatusMessage(String message) {
        statusMessage.set(message);
    }
    
    public BooleanProperty isLoadingModelsProperty() {
        return isLoadingModels;
    }
    
    public boolean isLoadingModels() {
        return isLoadingModels.get();
    }
    
    // Commands
    public void refreshConnections() {
        loadConnections();
    }
    
    public void refreshModels() {
        ConnectionViewModel currentConnection = getSelectedConnection();
        if (currentConnection != null) {
            loadModelsForConnection(currentConnection);
        }
    }
}
