package dev.local.ai.ui.models.errors;

public class ModelsInfoDownloadFailed extends RuntimeException {
    public ModelsInfoDownloadFailed(String message, Throwable cause) {
        super(message, cause);
    }
}
