package dev.local.ai.ui.utils;

import javafx.stage.Stage;
import javafx.stage.Window;

public final class MainStageProvider {

    private Stage mainStage;

    public void setMainStage(Stage mainStage) {
        this.mainStage = mainStage;
    }

    public Stage getMainStage() {
        if (mainStage == null) {
            throw new IllegalStateException("Main stage not initialized");
        }
        return mainStage;
    }

    public Window getMainWindow() {
        return getMainStage();
    }
}
