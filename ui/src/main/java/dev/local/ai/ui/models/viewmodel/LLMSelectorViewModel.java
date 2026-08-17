package dev.local.ai.ui.models.viewmodel;

import dev.local.ai.core.chat.LLMChangedEvent;
import dev.local.ai.core.connections.ConnectionsStore;
import dev.local.ai.core.connections.ModelProviderConnection;
import dev.local.ai.core.events.CoreEventBus;
import dev.local.ai.core.models.LLMInfoAndConnection;
import dev.local.ai.ui.connection.viewmodel.ConnectionViewModel;
import dev.local.ai.ui.models.ModelsInfoDownloadTask;
import dev.local.ai.ui.models.model.LLMInfoViewModel;
import dev.local.ai.ui.utils.IUIRunner;
import dev.local.ai.ui.utils.JavaFXUIRunner;
import io.reactivex.rxjava4.annotations.NonNull;
import io.reactivex.rxjava4.disposables.Disposable;
import io.reactivex.rxjava4.disposables.SerialDisposable;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class LLMSelectorViewModel implements AutoCloseable{

    private final IUIRunner uiRunner;

    public enum States {
        READY,
        ERROR,
        LOADING
    }

    private static final Logger logger = LoggerFactory.getLogger(LLMSelectorViewModel.class);
    
    private final ListProperty<ConnectionViewModel> connections;
    private final ObjectProperty<ConnectionViewModel> selectedConnection;
    private final ListProperty<LLMInfoViewModel> availableModels;
    private final ObjectProperty<LLMInfoViewModel> selectedModel;
    private final ObjectProperty<States> stateProperty;
    private final BooleanProperty isLoadingModels;
    
    private final ConnectionsStore connectionsStore;

    private final CoreEventBus coreEventBus;

    private final ModelsInfoDownloadTask modelsInfoDownloadTask;
    
    private final SerialDisposable serialDisposable = new SerialDisposable();
    private LLMInfoAndConnection lastPublishedSelection;

    public LLMSelectorViewModel(
            ConnectionsStore connectionsStore,
            CoreEventBus coreEventBus,
            ModelsInfoDownloadTask modelsInfoDownloadTask) {
        this(connectionsStore, coreEventBus, modelsInfoDownloadTask, new JavaFXUIRunner());
    }

    public LLMSelectorViewModel(
            ConnectionsStore connectionsStore,
            CoreEventBus coreEventBus,
            ModelsInfoDownloadTask modelsInfoDownloadTask,
            IUIRunner uiRunner) {
        this.modelsInfoDownloadTask = modelsInfoDownloadTask;
        this.connections = new SimpleListProperty<>(FXCollections.observableArrayList());
        this.selectedConnection = new SimpleObjectProperty<>();
        this.availableModels = new SimpleListProperty<>(FXCollections.observableArrayList());
        this.selectedModel = new SimpleObjectProperty<>();
        this.stateProperty = new SimpleObjectProperty<>(States.READY);
        this.isLoadingModels = new SimpleBooleanProperty(false);
        this.connectionsStore = connectionsStore;
        this.coreEventBus = coreEventBus;
        this.uiRunner = uiRunner;
        loadConnections();
        setupPropertyListeners();

        logger.info("ModelSelectorViewModel initialized");
    }

    private static ConnectionViewModel toConnectionViewModel(ModelProviderConnection connection) {
        return new ConnectionViewModel(
                connection.providerType(),
                connection.name(),
                connection.description(),
                connection.id()
        );
    }

    private void loadConnections() {
        try {
            var mappedConnections = connectionsStore
                .readAll()
                .stream()
                .map(LLMSelectorViewModel::toConnectionViewModel)
                .toList();
            this.connections.set(FXCollections.observableArrayList(mappedConnections));
            setState(States.READY);
        } catch (Exception e) {
            logger.error("Failed to load connections", e);
            setState(States.ERROR);
        }
    }

    private void setState(States states) {
        this.stateProperty.set(states);
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
                serialDisposable.set(Disposable.empty());
            }
        });
        
        selectedModel.addListener((obs, oldModel, newModel) -> {
            logger.debug("Selected model changed from {} to {}", oldModel, newModel);
            publishLlmChangedIfNeeded(newModel);
        });
        
        logger.debug("Property listeners setup complete");
    }

    private void publishLlmChangedIfNeeded(LLMInfoViewModel newModel) {
        if (newModel == null) {
            return;
        }
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

            var selection = new LLMInfoAndConnection(newModel.getCoreModelInfo(), connection);
            if (selection.equals(lastPublishedSelection)) {
                return;
            }

            lastPublishedSelection = selection;
            logger.info("Publishing LLMChangedEvent for model: {}", newModel.getName());
            coreEventBus.publish(new LLMChangedEvent(getClass().getSimpleName(), selection));
        } catch (Exception e) {
            logger.error("Error publishing LLMChangedEvent", e);
        }
    }

    private ModelProviderConnection findConnectionById(String connectionId) {
        return connectionsStore.findById(connectionId).orElse(null);
    }

    private void loadModelsForConnection(ConnectionViewModel connectionViewModel) {
        setState(States.LOADING);
        isLoadingModels.set(true);
        selectedModel.set(null);
        availableModels.clear();
        
        serialDisposable.set(
            modelsInfoDownloadTask.start(connectionViewModel.getId()
        ).subscribe(
            models -> uiRunner.run(loadingModelsFinished(models)),
            error -> uiRunner.run(loadingModelsFailed(connectionViewModel, error))
        ));
    }

    private Runnable loadingModelsFailed(ConnectionViewModel connectionViewModel, @NonNull Throwable error) {
        return () -> {
            logger.error("Failed to load models for connection: {}", connectionViewModel.getName(), error);
            setState(States.ERROR);
            isLoadingModels.set(false);
        };
    }

    private Runnable loadingModelsFinished(List<LLMInfoViewModel> models) {
        return () -> {
            availableModels.set(FXCollections.observableArrayList(models));
            setState(States.READY);
            isLoadingModels.set(false);
        };
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
    
    public ObjectProperty<States> statePropertyProperty() {
        return stateProperty;
    }

    public States getState() {
        return this.stateProperty.get();
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

    @Override
    public void close() throws Exception {
        serialDisposable.dispose();
    }
}
