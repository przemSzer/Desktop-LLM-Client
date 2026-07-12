package dev.local.ai.ui.connection.anthropic;

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

import dev.local.ai.core.connections.AnthropicConnection;
import dev.local.ai.ui.connection.INewConnectionDialog;

public class AnthropicConnectionDialog implements INewConnectionDialog<AnthropicConnection> {

    private static final Logger logger = LoggerFactory.getLogger(AnthropicConnectionDialog.class);

    private static final String FAILED_TO_SAVE_CONNECTION = "Failed to save connection";

    private Stage dialogStage;
    private AnthropicConnectionForm formController;
    private Function<AnthropicConnection, Boolean> onSaveCallback;
    private Runnable onCancelCallback;

    @Override
    public void show() {
        try {
            dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initStyle(StageStyle.DECORATED);
            dialogStage.setTitle("New Anthropic Connection");
            dialogStage.setResizable(false);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/anthropic/AnthropicConnectionForm.fxml"));
            Scene scene = new Scene(loader.load());

            formController = loader.getController();
            formController.setOnSave(this::handleSave);
            formController.setOnCancel(this::handleCancel);

            dialogStage.setScene(scene);
            dialogStage.showAndWait();

        } catch (IOException e) {
            logger.error("Failed to load Anthropic connection form", e);
            showErrorDialog("Failed to load connection form", e.getMessage());
        }
    }

    private void handleSave() {
        try {
            String name = formController.getName();
            String description = formController.getDescription();
            String apiKey = formController.getApiKey();

            AnthropicConnection connection = new AnthropicConnection(
                java.util.UUID.randomUUID().toString(),
                name,
                description,
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

            logger.info("Saved new Anthropic connection: {}", name);

        } catch (Exception e) {
            logger.error("Failed to save Anthropic connection", e);
            showErrorDialog(FAILED_TO_SAVE_CONNECTION, e.getMessage());
        }
    }

    private void handleCancel() {
        logger.debug("Anthropic connection dialog cancelled");
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
    public void onSave(Function<AnthropicConnection, Boolean> onSave) {
        this.onSaveCallback = onSave;
    }

    @Override
    public void onCancel(Runnable onCancel) {
        this.onCancelCallback = onCancel;
    }
}
