package dev.local.ai.ui.files.controls;

import javafx.beans.property.ListProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.local.ai.ui.files.viewmodel.FileAttachmentViewModel;
import dev.local.ai.ui.commands.CommandManager;
import dev.local.ai.ui.files.viewmodel.AttachedFileViewModel;

public class FileAttachmentControl extends VBox {
    
    @FXML
    private Button addFileButton;
    
    @FXML
    private Button clearFilesButton;
    
    @FXML
    private ListView<AttachedFileViewModel> attachedFilesListView;
    
    @FXML
    private Label filesCountLabel;
    
    private final Logger logger = LoggerFactory.getLogger(FileAttachmentControl.class);
    private FileAttachmentViewModel viewModel;
    
    public FileAttachmentControl() {        
        FXMLLoader loader = new FXMLLoader(getClass().getResource("FileAttachmentControl.fxml"));
        loader.setController(this);
        try {
            VBox loadedContent = loader.load();
            getChildren().addAll(loadedContent.getChildren());
            getStyleClass().addAll(loadedContent.getStyleClass());
            setSpacing(loadedContent.getSpacing());
                    
        } catch (IOException e) {
            logger.error("Failed to load FileAttachmentControl FXML", e);
            throw new RuntimeException("Failed to load FileAttachmentControl FXML", e);
        }
    }

    public void init(CommandManager commandManager) {
        this.viewModel = new FileAttachmentViewModel(commandManager);
        initializeControls();
    }

    private void initializeControls() {
        // Set up data binding to ViewModel
        setupDataBinding();
        
        // Set up file selection callback in ViewModel
        setupFileSelectionCallback();
        
        // Set up cell factory to use AttachedFileView control
        attachedFilesListView.setCellFactory(listView -> new javafx.scene.control.ListCell<AttachedFileViewModel>() {
            @Override
            protected void updateItem(AttachedFileViewModel fileViewModel, boolean empty) {
                super.updateItem(fileViewModel, empty);
                if (empty || fileViewModel == null) {
                    setGraphic(null);
                } else {
                    // Create AttachedFileView control for this file
                    AttachedFileView fileView = new AttachedFileView(fileViewModel);
                    
                    // Set up remove callback
                    fileView.setOnRemoveCallback(() -> {
                        // Remove the file from the ViewModel
                        viewModel.removeFile(fileViewModel);
                    });
                    
                    setGraphic(fileView);
                }
            }
        });
        
        // Set up event handlers
        setupEventHandlers();
    }
    
    /**
     * Sets up data binding between UI controls and ViewModel properties
     */
    private void setupDataBinding() {
        attachedFilesListView.itemsProperty().bind(viewModel.attachedFilesProperty());
        filesCountLabel.textProperty().bind(viewModel.filesCountTextProperty());
        
        viewModel.hasFilesProperty().addListener((obs, wasFiles, hasFiles) -> {
            if (hasFiles) {
                filesCountLabel.getStyleClass().add("has-files");
            } else {
                filesCountLabel.getStyleClass().remove("has-files");
            }
        });
    }
    
    /**
     * Sets up the file selection callback in the ViewModel
     */
    private void setupFileSelectionCallback() {
        viewModel.setFileSelectionCallback(() -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select File to Attach");
            
            // Get the current stage from the scene
            Stage stage = (Stage) getScene().getWindow();
            File selectedFile = fileChooser.showOpenDialog(stage);
            
            if (selectedFile != null) {
                // Create AttachedFileViewModel with LOADING status initially
                return selectedFile;
            }
            
            return null;
        });
    }
    
    /**
     * Sets up event handlers for UI controls
     */
    private void setupEventHandlers() {
        // Add file button action
        addFileButton.setOnAction(event -> addFile());
        
        // Clear files button action
        clearFilesButton.setOnAction(event -> clearFiles());
    }
    
    private void addFile() {
        // Delegate file selection and addition to ViewModel
        viewModel.selectAndAddFile();
    }
    
    private void clearFiles() {
        // Delegate to ViewModel
        viewModel.clearAllFiles();
    }
    
    // Public API methods - delegate to ViewModel
    
    /**
     * Gets the list of attached file ViewModels
     * @return list of attached file ViewModels
     */
    public List<AttachedFileViewModel> getAttachedFiles() {
        return viewModel.getAttachedFilesList();
    }
    
    public ListProperty<AttachedFileViewModel> attachedFilesProperty() {
        return viewModel.attachedFilesProperty();
    }

    /**
     * Gets the list of attached files (extracts File objects from ViewModels)
     * @return list of attached files
     */
    public List<File> getAttachedFileObjects() {
        return viewModel.getAttachedFilesList().stream()
                .map(AttachedFileViewModel::getFile)
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Checks if any files are attached
     * @return true if files are attached, false otherwise
     */
    public boolean hasAttachedFiles() {
        return viewModel.hasFiles();
    }
    
    /**
     * Removes a specific file ViewModel from the attached files
     * @param fileViewModel the file ViewModel to remove
     * @return true if the file was removed, false if it wasn't found
     */
    public boolean removeFile(AttachedFileViewModel fileViewModel) {
        return viewModel.removeFile(fileViewModel);
    }
    
    /**
     * Removes a specific file from the attached files (finds by File object)
     * @param file the file to remove
     * @return true if the file was removed, false if it wasn't found
     */
    public boolean removeFile(File file) {
        return viewModel.getAttachedFilesList().stream()
                .filter(vm -> file.equals(vm.getFile()))
                .findFirst()
                .map(viewModel::removeFile)
                .orElse(false);
    }
    
    /**
     * Gets the ViewModel instance for advanced operations
     * @return the FileAttachmentViewModel instance
     */
    public FileAttachmentViewModel getViewModel() {
        return viewModel;
    }
}
