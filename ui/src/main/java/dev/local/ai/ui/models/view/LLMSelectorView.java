package dev.local.ai.ui.models.view;

import dev.local.ai.core.connections.ConnectionsStore;
import dev.local.ai.core.events.CoreEventBus;
import dev.local.ai.ui.connection.viewmodel.ConnectionViewModel;
import dev.local.ai.ui.models.model.LLMInfoViewModel;
import dev.local.ai.ui.models.viewmodel.LLMSelectorViewModel;
import dev.local.ai.ui.utils.MainStageProvider;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;

public class LLMSelectorView extends HBox {
    
    private static final Logger logger = LoggerFactory.getLogger(LLMSelectorView.class);
        
    @FXML
    private ComboBox<ConnectionViewModel> connectionComboBox;
    
    @FXML
    private ProgressIndicator loadingIndicator;
    
    @FXML
    private ComboBox<LLMInfoViewModel> modelComboBox;
    
    @FXML
    private Button manageConnectionsButton;
            
    private LLMSelectorViewModel viewModel;
    private Callback<Class<?>, Object> controllerFactory;
    private MainStageProvider mainStageProvider;
    
    public LLMSelectorView() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("ModelSelectorView.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException e) {
            logger.error("Failed to load ModelSelectorView FXML", e);
            throw new RuntimeException("Failed to load ModelSelectorView FXML", e);
        }
    }

    public void init(ConnectionsStore connectionsStore,
                     CoreEventBus eventBus,
                     Callback<Class<?>, Object> controllerFactory,
                     MainStageProvider mainStageProvider) {
        try {
            logger.info("Initializing LLMSelectorView");

            this.viewModel = new LLMSelectorViewModel(connectionsStore, eventBus);
            this.controllerFactory = controllerFactory;
            this.mainStageProvider = mainStageProvider;

            setupDataBinding();
            setupEventHandlers();

            logger.info("LLMSelectorView initialized successfully");
        } catch (Exception e) {
            logger.error("Error initializing LLMSelectorView", e);
        }
    }
    
    private void setupDataBinding() {
        logger.debug("Setting up data binding...");
        
        connectionComboBox.setItems(viewModel.getConnections());
        connectionComboBox.valueProperty().bindBidirectional(viewModel.selectedConnectionProperty());
        logger.debug("Connection binding established");
        
        modelComboBox.setItems(viewModel.getAvailableModels());
        modelComboBox.itemsProperty().bind(viewModel.availableModelsProperty());    
        modelComboBox.valueProperty().bindBidirectional(viewModel.selectedModelProperty());        
        modelComboBox.setEditable(false);
        
        logger.debug("Model binding established");
                
        loadingIndicator.visibleProperty().bind(viewModel.isLoadingModelsProperty());
        
        setupCellFactories();
        
        logger.debug("Data binding setup complete");
    }
    
    private static class ConnectionCell extends ListCell<ConnectionViewModel> {
        @Override
        protected void updateItem(ConnectionViewModel item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                setText(item.getName());
            }
        }
    }

    private static class ConectionButtonCell extends ListCell<ConnectionViewModel> {
        @Override
        protected void updateItem(ConnectionViewModel item, boolean empty) {
            super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.getName());
                    // Note: Icon display would require ImageView wrapper
                }
        }
    }

    private static class ModelCell extends ListCell<LLMInfoViewModel> {
        @Override
        protected void updateItem(LLMInfoViewModel item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
            } else {
                setText(item.getName());
            }
        }
    }

    private static class ModelButtonCell extends ListCell<LLMInfoViewModel> {
        @Override
        protected void updateItem(LLMInfoViewModel item, boolean empty) {
            super.updateItem(item, empty);            
            if (empty || item == null) {
                setText(null);
            } else {
                setText(item.getName());
            }
        }
    }
    private void setupCellFactories() {        
        connectionComboBox.setCellFactory(listView -> new ConnectionCell());    
        connectionComboBox.setButtonCell(new ConectionButtonCell());
        
        modelComboBox.setCellFactory(listView -> new ModelCell());                
        modelComboBox.setButtonCell(new ModelButtonCell());
    }
    
    private void setupEventHandlers() {
        // Connection selection change handler
        connectionComboBox.setOnAction(event -> {
            ConnectionViewModel selectedConnection = connectionComboBox.getValue();
            if (selectedConnection != null) {
                logger.info("Connection selected: {}", selectedConnection.getName());
            }
        });
        
        // Model selection change handler
        modelComboBox.setOnAction(event -> {
            LLMInfoViewModel selectedModel = modelComboBox.getValue();
            if (selectedModel != null) {
                logger.info("ComboBox onAction: Model selected: {}", selectedModel.getName());
                logger.debug("ComboBox onAction: ViewModel selectedModel = {}", viewModel.getSelectedModel());
                logger.debug("ComboBox editable: {}", modelComboBox.isEditable());
                
                // Ensure the ViewModel is updated (backup for binding issues)
                if (!selectedModel.equals(viewModel.getSelectedModel())) {
                    logger.warn("Binding mismatch detected! Manually updating ViewModel");
                    viewModel.setSelectedModel(selectedModel);
                }
            }
        });
        
        // Additional listener to track ComboBox value changes
        modelComboBox.valueProperty().addListener((obs, oldValue, newValue) -> logger.debug("ComboBox valueProperty changed from {} to {}", oldValue, newValue));
        
        // Manage connections button handler
        manageConnectionsButton.setOnAction(event -> showConnectionsDialog());
    }
    
    private void showConnectionsDialog() {
        try {
            URL fxmlUrl = getClass().getResource("/fxml/ConnectionsView.fxml");
            if (fxmlUrl == null) {
                logger.error("ConnectionsView.fxml not found on classpath");
                return;
            }
            
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            if (controllerFactory != null) {
                loader.setControllerFactory(controllerFactory);
            }
            Parent root = loader.load();
            
            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(mainStageProvider.getMainWindow());
            dialogStage.setTitle("Manage Connections");
            dialogStage.setScene(new Scene(root));
            dialogStage.setMinWidth(600);
            dialogStage.setMinHeight(400);
            
            dialogStage.showAndWait();
            
            // Refresh connections after dialog is closed
            viewModel.refreshConnections();
            logger.info("Connections refreshed after dialog closed");
            
        } catch (IOException e) {
            logger.error("Failed to open Connections dialog", e);
        }
    }
    
    
    // Public getters for external access
    public LLMSelectorViewModel getViewModel() {
        return viewModel;
    }
    
    public ConnectionViewModel getSelectedConnection() {
        return viewModel.getSelectedConnection();
    }
    
    public LLMInfoViewModel getSelectedModel() {
        return viewModel.getSelectedModel();
    }
    
    public void setSelectedConnection(ConnectionViewModel connection) {
        viewModel.setSelectedConnection(connection);
    }
    
    public void setSelectedModel(LLMInfoViewModel model) {
        viewModel.setSelectedModel(model);
    }
}
