package dev.local.ai.ui.models.viewmodel;

import dev.local.ai.core.chat.LLMChangedEvent;
import dev.local.ai.core.connections.ConnectionsStore;
import dev.local.ai.core.connections.ModelProviderConnection;
import dev.local.ai.core.events.CoreEventBus;
import dev.local.ai.ui.connection.viewmodel.ConnectionViewModel;
import dev.local.ai.ui.models.model.LLMInfoViewModel;
import dev.local.ai.core.models.LLMInfoAndConnection;
import dev.local.ai.core.models.AvailableModelsService;
import dev.local.ai.core.models.ModelServicesFactory;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class LLMSelectorViewModel {
    
    private static final Logger logger = LoggerFactory.getLogger(LLMSelectorViewModel.class);
    
    private final ListProperty<ConnectionViewModel> connections;
    private final ObjectProperty<ConnectionViewModel> selectedConnection;
    private final ListProperty<LLMInfoViewModel> availableModels;
    private final ObjectProperty<LLMInfoViewModel> selectedModel;
    private final StringProperty statusMessage;
    private final BooleanProperty isLoadingModels;
    
    private final ConnectionsStore connectionsStore;

    private final CoreEventBus coreEventBus;

    private CompletableFuture<Void> lastLoadingTask;
    
    public LLMSelectorViewModel(ConnectionsStore connectionsStore, CoreEventBus coreEventBus) {
        this.connections = new SimpleListProperty<>(FXCollections.observableArrayList());
        this.selectedConnection = new SimpleObjectProperty<>();
        this.availableModels = new SimpleListProperty<>(FXCollections.observableArrayList());
        this.selectedModel = new SimpleObjectProperty<>();
        this.statusMessage = new SimpleStringProperty("Ready");
        this.isLoadingModels = new SimpleBooleanProperty(false);

        this.connectionsStore = connectionsStore;
        this.coreEventBus = coreEventBus;

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
        logger.debug("Setting up property listeners...");
        
        selectedConnection.addListener((obs, oldConnection, newConnection) -> {
            logger.debug("Selected connection changed from {} to {}", oldConnection, newConnection);
            if (newConnection != null) {
                loadModelsForConnection(newConnection);
            } else {               
                selectedModel.set(null);                
                availableModels.clear();
            }
        });
        
        selectedModel.addListener((obs, oldModel, newModel) -> {
            logger.debug("Selected model changed from {} to {}", oldModel, newModel);
            if (newModel != null) {
                try {
                    var selectedConn = getSelectedConnection();
                    if (selectedConn == null) {
                        logger.warn("Cannot publish LLMChangedEvent: no connection selected");
                        return;
                    }
                    
                    var connection = findConnectionById(selectedConn.getId());
                    if (connection == null) {
                        logger.warn("Cannot publish LLMChangedEvent: connection not found for ID: {}", selectedConn.getId());
                        return;
                    }
                    
                    var llmChangedEvent = new LLMChangedEvent(
                        getClass().getSimpleName(), 
                        new LLMInfoAndConnection(newModel.getCoreModelInfo(), connection)
                    );
                    logger.info("Publishing LLMChangedEvent for model: {}", newModel.getName());
                    coreEventBus.publish(llmChangedEvent);
                } catch (Exception e) {
                    logger.error("Error publishing LLMChangedEvent", e);
                }
            }
        });
        
        logger.debug("Property listeners setup complete");
    }
    
    private void loadModelsForConnection(ConnectionViewModel connectionViewModel) {
        setStatusMessage("Loading models for " + connectionViewModel.getName() + "...");
        isLoadingModels.set(true);
        selectedModel.set(null);
        availableModels.clear();
        ModelProviderConnection connection = findConnectionById(connectionViewModel.getId());
        
        if (connection == null) {
            setStatusMessage("Connection not found");
            isLoadingModels.set(false);
            return;
        }
        //Todo: inject the service instead of using the sigleton
        AvailableModelsService service = ModelServicesFactory.forConnection(connection);
                
        if (service == null) {
            setStatusMessage("No model service available for " + connection.providerType());
            isLoadingModels.set(false);
            return;
        }
        if (this.lastLoadingTask != null && !this.lastLoadingTask.isDone()) {
            logger.debug("Cancelling last loading task");
            this.lastLoadingTask.cancel(true);
        }
        this.lastLoadingTask = CompletableFuture.supplyAsync(service::loadModels)
            .thenAccept(models -> {
                var modelsForView = models.stream()
                        .map(LLMInfoViewModel::new)
                        .toList();
                Platform.runLater(() -> {                    
                    availableModels.set(FXCollections.observableArrayList(modelsForView));
                    logger.debug("Loaded {} models for {}", modelsForView.size(), connectionViewModel.getName());
                    setStatusMessage("Loaded " + models.size() + " models for " + connectionViewModel.getName());
                    
                    if (!modelsForView.isEmpty() && modelsForView.contains(selectedModel.get())){
                            selectedModel.set(selectedModel.get());
                        }

                    isLoadingModels.set(false);
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
        return connectionsStore.findById(connectionId).orElse(null);
    }
    
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
    
    public ListProperty<LLMInfoViewModel> availableModelsProperty() {
        return availableModels;
    }
    
    public ObservableList<LLMInfoViewModel> getAvailableModels() {
        return availableModels.get();
    }
    
    public ObjectProperty<LLMInfoViewModel> selectedModelProperty() {
        return selectedModel;
    }
    
    public LLMInfoViewModel getSelectedModel() {
        return selectedModel.get();
    }
    
    public void setSelectedModel(LLMInfoViewModel model) {
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
    
    public void refreshConnections() {
        loadConnections();
    }
}
