package dev.local.ai.ui.connection.openaicompatible;

import java.io.IOException;
import java.util.function.Function;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.local.ai.core.connections.OpenAICompatibleConnection;
import dev.local.ai.ui.connection.INewConnectionDialog;

public class OpenAICompatibleConnectionDialog implements INewConnectionDialog<OpenAICompatibleConnection> {

    private static final Logger logger = LoggerFactory.getLogger(OpenAICompatibleConnectionDialog.class);
    private static final String FAILED_TO_SAVE_CONNECTION = "Failed to save connection";

    private Stage dialogStage;
    private OpenAICompatibleConnectionForm formController;
    private Function<OpenAICompatibleConnection, Boolean> onSaveCallback;
    private Runnable onCancelCallback;

    @Override
    public void show() {
        try {
            dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initStyle(StageStyle.DECORATED);
            dialogStage.setTitle("New OpenAI Compatible Connection");
            dialogStage.setResizable(false);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/openaicompatible/OpenAICompatibleConnectionForm.fxml"));
            Scene scene = new Scene(loader.load());

            formController = loader.getController();
            formController.setOnSave(this::handleSave);
            formController.setOnCancel(this::handleCancel);

            dialogStage.setScene(scene);
            dialogStage.showAndWait();

        } catch (IOException e) {
            logger.error("Failed to load OpenAI compatible connection form", e);
            showErrorDialog("Failed to load connection form", e.getMessage());
        }
    }

    private void handleSave() {
        try {
            String name = formController.getName();
            String description = formController.getDescription();
            String baseUrl = formController.getBaseUrl();
            String apiKey = formController.getApiKey();

            OpenAICompatibleConnection connection = new OpenAICompatibleConnection(
                java.util.UUID.randomUUID().toString(),
                name,
                description,
                baseUrl,
                apiKey
            );

            if (onSaveCallback != null) {
                var result = onSaveCallback.apply(connection);
                if (result.booleanValue()) {
                    dialogStage.close();
                } else {
                    showErrorDialog(FAILED_TO_SAVE_CONNECTION, FAILED_TO_SAVE_CONNECTION);
                }
            }

            logger.info("Saved new OpenAI compatible connection: {} ({})", name, connection.baseUrl());

        } catch (Exception e) {
            logger.error("Failed to save OpenAI compatible connection", e);
            showErrorDialog(FAILED_TO_SAVE_CONNECTION, e.getMessage());
        }
    }

    private void handleCancel() {
        logger.debug("OpenAI compatible connection dialog cancelled");
        dialogStage.close();
        if (onCancelCallback != null) {
            onCancelCallback.run();
        }
    }

    private void showErrorDialog(String title, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @Override
    public void onSave(Function<OpenAICompatibleConnection, Boolean> onSave) {
        this.onSaveCallback = onSave;
    }

    @Override
    public void onCancel(Runnable onCancel) {
        this.onCancelCallback = onCancel;
    }
}
