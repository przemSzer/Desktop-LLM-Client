package dev.local.ai.ui.files.viewmodel;

/**
 * Enum representing the possible states of an attached file.
 * Provides type safety and clear definition of file processing states.
 */
public enum FileStatus {
    LOADING("Loading", "File is being loaded"),
    TYPE_DETECTED("Type detected", "File type detected"),
    PREPARING("Preparing", "File is being prepared for LLM"),
    VALID("Valid", "File is ready for use"),
    ERROR("Error", "File has an error");

    private final String displayName;
    private final String description;

    FileStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Gets the display name for UI purposes
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the description of this status
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Checks if this status indicates the file is ready for use
     * @return true if the file is ready, false otherwise
     */
    public boolean isReady() {
        return this == VALID;
    }

    /**
     * Checks if this status indicates the file is currently being processed
     * @return true if the file is being processed, false otherwise
     */
    public boolean isProcessing() {
        return this == LOADING || this == PREPARING;
    }

    /**
     * Checks if this status indicates an error
     * @return true if there's an error, false otherwise
     */
    public boolean hasError() {
        return this == ERROR;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
