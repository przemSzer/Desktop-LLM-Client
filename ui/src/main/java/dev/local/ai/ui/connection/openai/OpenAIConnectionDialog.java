package dev.local.ai.ui.connection.openai;

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

import dev.local.ai.core.connections.OpenAIConnection;
import dev.local.ai.ui.connection.INewConnectionDialog;

/**
 * Dialog for creating new OpenAI connections.
 * Implements the INewConnectionDialog interface and shows a form for OpenAI configuration.
 */
public class OpenAIConnectionDialog implements INewConnectionDialog<OpenAIConnection> {
    
    private static final Logger logger = LoggerFactory.getLogger(OpenAIConnectionDialog.class);
    
    private static final String FAILED_TO_SAVE_CONNECTION = "Failed to save connection";
    
    private Stage dialogStage;
    private OpenAIConnectionForm formController;
    private Function<OpenAIConnection, Boolean> onSaveCallback;
    private Runnable onCancelCallback;
    
    @Override
    public void show() {
        try {
            // Create the dialog stage
            dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initStyle(StageStyle.DECORATED);
            dialogStage.setTitle("New OpenAI Connection");
            dialogStage.setResizable(false);
            
            // Load the FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/openai/OpenAIConnectionForm.fxml"));
            Scene scene = new Scene(loader.load());
            
            // Get the controller
            formController = loader.getController();
            
            // Set up callbacks
            formController.setOnSave(this::handleSave);
            formController.setOnCancel(this::handleCancel);
            
            // Set the scene and show
            dialogStage.setScene(scene);
            dialogStage.showAndWait();
            
        } catch (IOException e) {
            logger.error("Failed to load OpenAI connection form", e);
            showErrorDialog("Failed to load connection form", e.getMessage());
        }
    }
    
    private void handleSave() {
        try {
            // Get form data
            String name = formController.getName();
            String description = formController.getDescription();
            String apiKey = formController.getApiKey();
            
            // Create connection record
            OpenAIConnection connection = new OpenAIConnection(
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
            
            logger.info("Saved new OpenAI connection: {} (API Key: {})", name, apiKey.length() > 8 ? apiKey.substring(0, 8) + "..." : apiKey);
            
        } catch (Exception e) {
            logger.error("Failed to save OpenAI connection", e);
            showErrorDialog(FAILED_TO_SAVE_CONNECTION, e.getMessage());
        }
    }
    
    private void handleCancel() {
        logger.debug("OpenAI connection dialog cancelled");
        
        // Close dialog
        dialogStage.close();
        
        // Call the cancel callback
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
    public void onSave(Function<OpenAIConnection, Boolean> onSave) {
        this.onSaveCallback = onSave;
    }

    @Override
    public void onCancel(Runnable onCancel) {
        this.onCancelCallback = onCancel;
    }
}
