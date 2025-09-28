package dev.local.ai.ui.files.viewmodel;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.local.ai.core.documents.DocumentDescription;
import dev.local.ai.ui.commands.CommandManager;
import dev.local.ai.ui.commands.CommandManagerProvider;
import dev.local.ai.ui.files.commands.PrepareFileToBeUsedByLLM;

/**
 * ViewModel for the FileAttachmentControl following MVVM pattern.
 * Manages the observable data and commands for file attachment functionality.
 * This ViewModel is GUI-agnostic and contains no JavaFX UI dependencies.
 */
public class FileAttachmentViewModel {

    private static final Logger logger = LoggerFactory.getLogger(FileAttachmentViewModel.class);

    // Observable properties for data binding
    private final ListProperty<AttachedFileViewModel> attachedFiles;
    private final StringProperty filesCountText;
    private final StringProperty filesCountStyle;
    private final BooleanProperty hasFiles;
    private final IntegerProperty filesCount;
    private final StringProperty statusMessage;
    private final CommandManager commandManager;
    // File selection callback - allows ViewModel to request file selection from UI
    private Supplier<File> fileSelectionCallback;

    public FileAttachmentViewModel() {
        this.commandManager = CommandManagerProvider.get();
        // Initialize observable properties
        this.attachedFiles = new SimpleListProperty<>(FXCollections.observableArrayList());
        this.filesCountText = new SimpleStringProperty("No files attached");
        this.filesCountStyle = new SimpleStringProperty("-fx-text-fill: gray;");
        this.hasFiles = new SimpleBooleanProperty(false);
        this.filesCount = new SimpleIntegerProperty(0);
        this.statusMessage = new SimpleStringProperty("Ready");

        // Set up property bindings
        setupPropertyBindings();

        logger.info("FileAttachmentViewModel initialized");
    }

    /**
     * Sets up automatic property bindings to keep derived properties in sync
     */
    private void setupPropertyBindings() {
        // Bind files count to the size of attached files list
        filesCount.bind(attachedFiles.sizeProperty());
        
        // Bind hasFiles to whether the list is empty
        hasFiles.bind(attachedFiles.emptyProperty().not());
        
        // Update files count text and style when files count changes
        filesCount.addListener((obs, oldVal, newVal) -> updateFilesCountDisplay(newVal.intValue()));
    }

    /**
     * Updates the files count display text and style based on the number of files
     */
    private void updateFilesCountDisplay(int count) {
        if (count == 0) {
            filesCountText.set("No files attached");
            filesCountStyle.set("-fx-text-fill: gray;");
        } else if (count == 1) {
            filesCountText.set("1 file attached");
            filesCountStyle.set("-fx-text-fill: blue;");
        } else {
            filesCountText.set(count + " files attached");
            filesCountStyle.set("-fx-text-fill: blue;");
        }
    }

    // Property accessors
    public ListProperty<AttachedFileViewModel> attachedFilesProperty() {
        return attachedFiles;
    }

    public ObservableList<AttachedFileViewModel> getAttachedFiles() {
        return attachedFiles.get();
    }

    public StringProperty filesCountTextProperty() {
        return filesCountText;
    }

    public String getFilesCountText() {
        return filesCountText.get();
    }

    public StringProperty filesCountStyleProperty() {
        return filesCountStyle;
    }

    public String getFilesCountStyle() {
        return filesCountStyle.get();
    }

    public BooleanProperty hasFilesProperty() {
        return hasFiles;
    }

    public boolean hasFiles() {
        return hasFiles.get();
    }

    public IntegerProperty filesCountProperty() {
        return filesCount;
    }

    public int getFilesCount() {
        return filesCount.get();
    }

    public StringProperty statusMessageProperty() {
        return statusMessage;
    }

    public String getStatusMessage() {
        return statusMessage.get();
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage.set(statusMessage);
    }

    /**
     * Sets the file selection callback for UI-specific file selection
     * @param fileSelectionCallback a supplier that returns a selected file or null
     */
    public void setFileSelectionCallback(Supplier<File> fileSelectionCallback) {
        this.fileSelectionCallback = fileSelectionCallback;
    }

    // Command methods for file operations

    /**
     * Initiates file selection through the UI callback and adds the selected file
     * @return true if a file was selected and added, false otherwise
     */
    public boolean selectAndAddFile() {
        if (fileSelectionCallback == null) {
            logger.warn("File selection callback not set");
            setStatusMessage("File selection not available");
            return false;
        }

        try {
            File selectedFile = fileSelectionCallback.get();
            if (selectedFile != null) {
                AttachedFileViewModel fileViewModel = new AttachedFileViewModel(new DocumentDescription(selectedFile.getName(), null, null, selectedFile), FileStatus.LOADING);
                if (addFile(fileViewModel)){
                    // Execute the command with the file from the ViewModel
                    commandManager.executeCommandAsync(new PrepareFileToBeUsedByLLM(selectedFile, (status, description) -> {
                        Platform.runLater(() -> {
                            fileViewModel.statusProperty().set(status);
                            fileViewModel.descriptionProperty().set(description);
                        });
                    }));
                    return true;
                } else {
                    logger.error("Failed to add file: {}", selectedFile.getName());
                    setStatusMessage("Failed to add file: " + selectedFile.getName());
                    return false;
                }
            } else {
                logger.info("No file selected by user");
                setStatusMessage("No file selected");
                return false;
            }
        } catch (Exception e) {
            logger.error("Error during file selection", e);
            setStatusMessage("Error during file selection: " + e.getMessage());
            return false;
        }
    }

    /**
     * Adds a file to the attached files list if it's not already present
     * @param fileViewModel the file ViewModel to add
     * @return true if the file was added, false if it was already present
     */
    public boolean addFile(AttachedFileViewModel fileViewModel) {
        if (fileViewModel == null) {
            logger.warn("Attempted to add null file ViewModel");
            return false;
        }

        var fileAlreadyAttached = attachedFiles.get()
            .stream()
            .anyMatch(f -> f.getDescription().file().equals(fileViewModel.getDescription().file()));
        if (fileAlreadyAttached) {
            logger.info("File already attached: {}", fileViewModel.getDescription().file().getName());
            setStatusMessage("File already attached: " + fileViewModel.getDescription().file().getName());
            return false;
        }

        attachedFiles.add(fileViewModel);
        logger.info("File attached: {}", fileViewModel.getDescription().file().getName());
        setStatusMessage("File attached: " + fileViewModel.getDescription().file().getName());
        return true;
    }

    /**
     * Removes a specific file from the attached files list
     * @param fileViewModel the file ViewModel to remove
     * @return true if the file was removed, false if it wasn't in the list
     */
    public boolean removeFile(AttachedFileViewModel fileViewModel) {
        if (fileViewModel == null) {
            logger.warn("Attempted to remove null file ViewModel");
            return false;
        }

        boolean removed = attachedFiles.remove(fileViewModel);
        if (removed) {
            logger.info("File removed: {}", fileViewModel.getFileName());
            setStatusMessage("File removed: " + fileViewModel.getFileName());
        } else {
            logger.info("File not found in attached files: {}", fileViewModel.getFileName());
            setStatusMessage("File not found: " + fileViewModel.getFileName());
        }
        return removed;
    }

    /**
     * Clears all attached files
     */
    public void clearAllFiles() {
        int count = attachedFiles.size();
        attachedFiles.clear();
        logger.info("All {} files cleared", count);
        setStatusMessage("All files cleared");
    }

    /**
     * Checks if a specific file is already attached
     * @param fileViewModel the file ViewModel to check
     * @return true if the file is attached, false otherwise
     */
    public boolean containsFile(AttachedFileViewModel fileViewModel) {
        return fileViewModel != null && attachedFiles.contains(fileViewModel);
    }

    /**
     * Gets a copy of the attached files as a regular List
     * @return a new ArrayList containing all attached file ViewModels
     */
    public List<AttachedFileViewModel> getAttachedFilesList() {
        return new ArrayList<>(attachedFiles.get());
    }

    
    
}
