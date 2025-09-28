package dev.local.ai.ui.files.controls;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.local.ai.ui.files.viewmodel.AttachedFileViewModel;
import dev.local.ai.ui.files.viewmodel.FileStatus;

/**
 * Custom control for displaying an attached file with its status and actions.
 * This control is loaded from FXML and follows MVVM pattern.
 */
public class AttachedFileView extends HBox {
    
    @FXML
    private Label fileNameLabel;
    
    @FXML
    private Label statusLabel;
    
    @FXML
    private ProgressIndicator progressIndicator;
    
    @FXML
    private Button removeButton;
    
    @FXML
    private Label fileTypeLabel;
    
    private final Logger logger = LoggerFactory.getLogger(AttachedFileView.class);
    private AttachedFileViewModel fileViewModel;
    private Runnable onRemoveCallback;
    
    public AttachedFileView() {
        this(null);
    }
    
    public AttachedFileView(AttachedFileViewModel fileViewModel) {
        this.fileViewModel = fileViewModel;
        
        FXMLLoader loader = new FXMLLoader(getClass().getResource("AttachedFileView.fxml"));
        loader.setController(this);
        try {
            HBox loadedContent = loader.load();
            getChildren().addAll(loadedContent.getChildren());
            getStyleClass().addAll(loadedContent.getStyleClass());
            setSpacing(loadedContent.getSpacing());
            
            initializeControls();
        } catch (IOException e) {
            logger.error("Failed to load AttachedFileView FXML", e);
            throw new RuntimeException("Failed to load AttachedFileView FXML", e);
        }
    }
    
    private void initializeControls() {
        // Set up data binding if ViewModel is provided
        if (fileViewModel != null) {
            setupDataBinding();
        }
        
    }
    
    /**
     * Sets up data binding between UI controls and ViewModel properties
     */
    private void setupDataBinding() {
        if (fileViewModel == null) return;
        
        // Bind file name label to ViewModel's display title
        fileNameLabel.textProperty().bind(fileViewModel.fileProperty()
            .map(file -> file != null ? file.getName() : fileViewModel.getFileName()));
        
        // Bind status label to ViewModel's status
        statusLabel.textProperty().bind(fileViewModel.statusProperty()
            .map(status -> status != null ? status.getDisplayName() : "Unknown"));
        
        // Bind progress indicator visibility to processing status
        progressIndicator.visibleProperty().bind(fileViewModel.statusProperty()
            .map(status -> status != null && status.isProcessing()));
        
        // Update status label style based on status
        fileViewModel.statusProperty().addListener((obs, oldStatus, newStatus) -> {
            updateStatusStyle(newStatus);
        });
        
        fileTypeLabel.textProperty().bind(
            fileViewModel.descriptionProperty()
                .map(description -> description != null && description.type() != null ? description.type().toString() : "Unknown")
            );
        
        // Initial style update
        updateStatusStyle(fileViewModel.getStatus());
    }
    
    /**
     * Updates the status label style based on the file status
     */
    private void updateStatusStyle(FileStatus status) {
        if (status == null) {
            statusLabel.setStyle("");
            return;
        }
        
        switch (status) {
            case VALID:
                statusLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                break;
            case LOADING:
            case PREPARING:
                statusLabel.setStyle("-fx-text-fill: blue; -fx-font-weight: bold;");
                break;
            case ERROR:
                statusLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                break;
            default:
                statusLabel.setStyle("");
        }
    }
    
 
    /**
     * Handles the remove file action
     * This method is called from FXML and must be public
     */
    @FXML
    public void removeFile() {
        if (fileViewModel != null) {
            logger.info("Remove file requested: {}", fileViewModel.getFileName());
            // Call the callback if it's set
            if (onRemoveCallback != null) {
                onRemoveCallback.run();
            }
        }
    }
    
    /**
     * Sets the file ViewModel for this control
     * @param fileViewModel the ViewModel to bind to
     */
    public void setFileViewModel(AttachedFileViewModel fileViewModel) {
        this.fileViewModel = fileViewModel;
        if (fileViewModel != null) {
            setupDataBinding();
        }
    }
    
    /**
     * Gets the file ViewModel bound to this control
     * @return the ViewModel or null if not set
     */
    public AttachedFileViewModel getFileViewModel() {
        return fileViewModel;
    }
    
    /**
     * Sets a callback for when the remove button is clicked
     * @param onRemoveCallback the callback to execute when remove is clicked
     */
    public void setOnRemoveCallback(Runnable onRemoveCallback) {
        this.onRemoveCallback = onRemoveCallback;
    }
    
    /**
     * Updates the display with a new file ViewModel
     * @param fileViewModel the new ViewModel to display
     */
    public void updateFile(AttachedFileViewModel fileViewModel) {
        setFileViewModel(fileViewModel);
    }
}
