package dev.local.ai.ui.models.view;

import dev.local.ai.ui.connection.ConnectionsManagerDialog;
import dev.local.ai.ui.connection.viewmodel.ConnectionViewModel;
import dev.local.ai.ui.models.model.LLMInfoViewModel;
import dev.local.ai.ui.models.viewmodel.LLMSelectorViewModel;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class LLMSelectorView extends VBox {
    
    private static final Logger logger = LoggerFactory.getLogger(LLMSelectorView.class);

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

    private static class ConnectionButtonCell extends ListCell<ConnectionViewModel> {
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

    @FXML
    private ComboBox<ConnectionViewModel> connectionComboBox;
    
    @FXML
    private ProgressIndicator loadingIndicator;
    
    @FXML
    private ComboBox<LLMInfoViewModel> modelComboBox;
    
    @FXML
    private Button manageConnectionsButton;
            
    private LLMSelectorViewModel viewModel;
    private ConnectionsManagerDialog connectionsDialog;
    
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

    public void init(ConnectionsManagerDialog connectionsDialog,
                     LLMSelectorViewModel viewModel
    ) {
        try {
            logger.info("Initializing LLMSelectorView");
            this.viewModel = viewModel;
            this.connectionsDialog = connectionsDialog;

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
        loadingIndicator.managedProperty().bind(loadingIndicator.visibleProperty());
        
        setupCellFactories();
        
        logger.debug("Data binding setup complete");
    }

    private void setupCellFactories() {
        connectionComboBox.setCellFactory(listView -> new ConnectionCell());    
        connectionComboBox.setButtonCell(new ConnectionButtonCell());
        
        modelComboBox.setCellFactory(listView -> new ModelCell());                
        modelComboBox.setButtonCell(new ModelButtonCell());
    }
    
    private void setupEventHandlers() {
        manageConnectionsButton.setOnAction(event -> {
            connectionsDialog.show();
            viewModel.refreshConnections();
        });
    }
    
    
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

    ComboBox<ConnectionViewModel> getConnectionComboBox() {
        return connectionComboBox;
    }

    ComboBox<LLMInfoViewModel> getModelComboBox() {
        return modelComboBox;
    }

    ProgressIndicator getLoadingIndicator() {
        return loadingIndicator;
    }

    Button getManageConnectionsButton() {
        return manageConnectionsButton;
    }
}
