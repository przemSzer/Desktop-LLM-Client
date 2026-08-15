package dev.local.ai.ui.models.viewmodel;

import javafx.application.Platform;

public class JavaFXUIRunner implements IUIRunner {
    @Override
    public void run(Runnable action) {
        Platform.runLater(action);
    }
}
