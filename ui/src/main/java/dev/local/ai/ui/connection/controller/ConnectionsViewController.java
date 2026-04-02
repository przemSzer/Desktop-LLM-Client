package dev.local.ai.ui.connection.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.local.ai.core.connections.ConnectionProvider;
import dev.local.ai.ui.connection.viewmodel.ConnectionViewModel;
import dev.local.ai.ui.connection.viewmodel.ConnectionsViewModel;

/**
 * Controller for the Connections UI following MVVM pattern.
 * Handles only UI events and delegates business logic to ViewModel.
 */
public class ConnectionsViewController {
    
    private static final Logger logger = LoggerFactory.getLogger(ConnectionsViewController.class);
    
    @FXML
    private VBox rootVBox;
    
    @FXML
    private HBox actionPanel;
    
    @FXML
    private Label addNewConnectionLabel;
    
    @FXML
    private MenuButton providerMenuButton;
    
    @FXML
    private Button deleteButton;
    
    @FXML
    private TableView<ConnectionViewModel> connectionsTableView;
    
    @FXML
    private TableColumn<ConnectionViewModel, Image> iconColumn;
    
    @FXML
    private TableColumn<ConnectionViewModel, String> nameColumn;
    
    @FXML
    private TableColumn<ConnectionViewModel, String> descriptionColumn;
    
    @FXML
    private Label statusLabel;
    
    // ViewModel
    private ConnectionsViewModel connectionsViewModel;
    
    @FXML
    public void initialize() {
        logger.debug("Initializing ConnectionsController");
        
        connectionsViewModel = new ConnectionsViewModel();        
        setupDataBinding();
        setupEventHandlers();        
        setupTableColumns();
        
        logger.debug("ConnectionsController initialized.");
    }
    
    private void setupDataBinding() {
        connectionsTableView.itemsProperty().bind(connectionsViewModel.connectionsProperty());
        
        statusLabel.textProperty().bind(connectionsViewModel.statusMessageProperty());
        
        deleteButton.disableProperty().bind(
            connectionsViewModel.selectedConnectionProperty().isNull()
        );
        
        logger.debug("Data binding setup completed");
    }
    
    private void setupEventHandlers() {
        connectionsTableView.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                connectionsViewModel.setSelectedConnection(newSelection);
            }
        );
        
        deleteButton.setOnAction(event -> {
            logger.debug("Delete button clicked");
            connectionsViewModel.deleteSelectedConnection();
        });
        
        setupProviderMenu();
        
        logger.debug("Event handlers setup completed");
    }
    
    private void setupProviderMenu() {
        for (ConnectionProvider providerType : ConnectionProvider.values()) {
            MenuItem menuItem = new MenuItem(providerType.getDisplayName());
            menuItem.setOnAction(event -> {
                logger.debug("Provider selected: {}", providerType);
                addNewConnectionFor(providerType);
            });
            providerMenuButton.getItems().add(menuItem);
        }
    }
    
    private void addNewConnectionFor(ConnectionProvider providerType) {
        logger.info("Adding new connection for provider: {}", providerType);
        connectionsViewModel.newConnectionFor(providerType);
    }
    
    private void setupTableColumns() {
        iconColumn.setCellValueFactory(new PropertyValueFactory<>("icon"));
        iconColumn.setCellFactory(column -> new javafx.scene.control.TableCell<ConnectionViewModel, Image>() {
            private final ImageView imageView = new ImageView();
            
            @Override
            protected void updateItem(Image item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    imageView.setImage(item);
                    imageView.setFitWidth(24);
                    imageView.setFitHeight(24);
                    setGraphic(imageView);
                }
            }
        });
        
        // Name column
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));        
        
        // Description column
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        
        // Set column widths
        iconColumn.setPrefWidth(50);
        nameColumn.setPrefWidth(200);
        descriptionColumn.setPrefWidth(300);
        
        logger.debug("Table columns setup completed");
    }
}
