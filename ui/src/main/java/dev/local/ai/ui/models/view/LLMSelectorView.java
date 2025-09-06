package dev.local.ai.ui.models.view;

import dev.local.ai.ui.connection.viewmodel.ConnectionViewModel;
import dev.local.ai.ui.models.model.LLMInfoViewModel;
import dev.local.ai.ui.models.viewmodel.LLMSelectorViewModel;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the Model Selector view following MVVM pattern.
 * Handles UI events and delegates to the ViewModel.
 */
public class LLMSelectorView extends HBox implements Initializable {
    
    private static final Logger logger = LoggerFactory.getLogger(LLMSelectorView.class);
        
    @FXML
    private ComboBox<ConnectionViewModel> connectionComboBox;
    
    @FXML
    private ProgressIndicator loadingIndicator;
    
    @FXML
    private ComboBox<LLMInfoViewModel> modelComboBox;
            
    private LLMSelectorViewModel viewModel;
    
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

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing ModelSelectorController");
        
        // Initialize ViewModel
        viewModel = new LLMSelectorViewModel();
        
        // Setup data binding
        setupDataBinding();
        
        // Setup UI event handlers
        setupEventHandlers();
        
        logger.info("ModelSelectorController initialized successfully");
    }
    
    private void setupDataBinding() {
        // Bind connections to ComboBox
        connectionComboBox.setItems(viewModel.getConnections());
        connectionComboBox.valueProperty().bindBidirectional(viewModel.selectedConnectionProperty());
        
        // Bind models to ComboBox
        modelComboBox.setItems(viewModel.getAvailableModels());
        modelComboBox.itemsProperty().bind(viewModel.availableModelsProperty());
        modelComboBox.valueProperty().bindBidirectional(viewModel.selectedModelProperty());
                
        // Bind loading indicator
        loadingIndicator.visibleProperty().bind(viewModel.isLoadingModelsProperty());
        
        // Setup cell factories for better display
        setupCellFactories();
    }
    
    private void setupCellFactories() {
        // Connection ComboBox cell factory
        connectionComboBox.setCellFactory(listView -> new ListCell<ConnectionViewModel>() {
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
        });
        
        // Connection ComboBox button cell factory
        connectionComboBox.setButtonCell(new ListCell<ConnectionViewModel>() {
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
        });
        
        modelComboBox.setCellFactory(listView -> new ListCell<LLMInfoViewModel>() {
            @Override
            protected void updateItem(LLMInfoViewModel item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });
                
        modelComboBox.setButtonCell(new ListCell<LLMInfoViewModel>() {
            @Override
            protected void updateItem(LLMInfoViewModel item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });
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
                logger.info("Model selected: {}", selectedModel.getName());
            }
        });
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
