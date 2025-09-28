package dev.local.ai.ui.files.viewmodel;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.io.File;

import dev.local.ai.core.documents.DocumentDescription;

/**
 * ViewModel for individual attached files following MVVM pattern.
 * Represents a single file attachment with its associated metadata and status.
 * This ViewModel is GUI-agnostic and contains no JavaFX UI dependencies.
 */
public class AttachedFileViewModel {

    // Observable properties for data binding
    private final ObjectProperty<File> file;
    private final ObjectProperty<DocumentDescription> description;
    private final ObjectProperty<FileStatus> status;

    public AttachedFileViewModel(DocumentDescription description, FileStatus status) {
        this.file = new SimpleObjectProperty<>(description.file());
        this.description = new SimpleObjectProperty<>(description);
        this.status = new SimpleObjectProperty<>(status);
    }

    public AttachedFileViewModel(DocumentDescription description) {
        this(description, FileStatus.VALID);
    }

    // Property accessors for file
    public ObjectProperty<File> fileProperty() {
        return file;
    }

    public File getFile() {
        return file.get();
    }

    public void setFile(File file) {
        this.file.set(file);
    }

    // Property accessors for description
    public ObjectProperty<DocumentDescription> descriptionProperty() {
        return description;
    }

    public DocumentDescription getDescription() {
        return description.get();
    }

    public void setDescription(DocumentDescription description) {
        this.description.set(description);
    }

    // Property accessors for status
    public ObjectProperty<FileStatus> statusProperty() {
        return status;
    }

    public FileStatus getStatus() {
        return status.get();
    }

    public void setStatus(FileStatus status) {
        this.status.set(status);
    }

    /**
     * Gets the file name for display purposes
     * @return the file name or "Unknown" if file is null
     */
    public String getFileName() {
        File fileObj = getFile();
        return fileObj != null ? fileObj.getName() : "Unknown";
    }

    /**
     * Gets the file title from the description for display purposes
     * @return the file title or file name if description is null
     */
    public String getDisplayTitle() {
        DocumentDescription desc = getDescription();
        if (desc != null && desc.title() != null) {
            return desc.title();
        }
        return getFileName();
    }

    /**
     * Checks if the file is ready for use
     * @return true if status is VALID, false otherwise
     */
    public boolean isReady() {
        return getStatus() != null && getStatus().isReady();
    }

    /**
     * Checks if the file is currently being processed
     * @return true if status indicates processing, false otherwise
     */
    public boolean isProcessing() {
        return getStatus() != null && getStatus().isProcessing();
    }

    /**
     * Checks if the file has an error status
     * @return true if status indicates an error, false otherwise
     */
    public boolean hasError() {
        return getStatus() != null && getStatus().hasError();
    }

    @Override
    public String toString() {
        return String.format("AttachedFileViewModel{file=%s, description=%s, status='%s'}", 
                           getFile(), getDescription(), getStatus());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        AttachedFileViewModel that = (AttachedFileViewModel) obj;
        
        File thisFile = getFile();
        File thatFile = that.getFile();
        
        if (thisFile == null ? thatFile != null : !thisFile.equals(thatFile)) {
            return false;
        }
        
        DocumentDescription thisDesc = getDescription();
        DocumentDescription thatDesc = that.getDescription();
        
        return thisDesc == null ? thatDesc == null : thisDesc.equals(thatDesc);
    }

    @Override
    public int hashCode() {
        File fileObj = getFile();
        DocumentDescription desc = getDescription();
        
        int result = fileObj != null ? fileObj.hashCode() : 0;
        result = 31 * result + (desc != null ? desc.hashCode() : 0);
        return result;
    }
}
