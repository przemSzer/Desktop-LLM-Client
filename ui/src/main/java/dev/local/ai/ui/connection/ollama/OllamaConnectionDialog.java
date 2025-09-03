package dev.local.ai.ui.connection.ollama;

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

import dev.local.ai.core.connections.OllamaConnection;
import dev.local.ai.ui.connection.INewConnectionDialog;

/**
 * Dialog for creating new Ollama connections.
 * Implements the INewConnectionDialog interface and shows a form for Ollama configuration.
 */
public class OllamaConnectionDialog implements INewConnectionDialog<OllamaConnection> {
    
    private static final Logger logger = LoggerFactory.getLogger(OllamaConnectionDialog.class);
    
    private Stage dialogStage;
    private OllamaConnectionForm formController;
    private Function<OllamaConnection, Boolean> onSaveCallback;
    private Runnable onCancelCallback;
    
    @Override
    public void show() {
        try {
            // Create the dialog stage
            dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initStyle(StageStyle.DECORATED);
            dialogStage.setTitle("New Ollama Connection");
            dialogStage.setResizable(false);
            
            // Load the FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ollama/OllamaConnectionForm.fxml"));
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
            logger.error("Failed to load Ollama connection form", e);
            showErrorDialog("Failed to load connection form", e.getMessage());
        }
    }
    
    private void handleSave() {
        try {
            // Get form data
            String name = formController.getName();
            String description = formController.getDescription();
            String url = formController.getUrl();
            
            // Create connection record
            OllamaConnection connection = new OllamaConnection(
                java.util.UUID.randomUUID().toString(),
                name, 
                description, 
                url
            );
            
            if (onSaveCallback != null) {
                var result = onSaveCallback.apply(connection);
                if (result) {
                    dialogStage.close();
                }else{
                    showErrorDialog("Failed to save connection", "Failed to save connection");
                }
            }
            
            logger.info("Saved new Ollama connection: {} (URL: {}, Port: {})", name, url);
            
            // Close dialog
            dialogStage.close();
            
            // Call the save callback
            
        } catch (Exception e) {
            logger.error("Failed to save Ollama connection", e);
            showErrorDialog("Failed to save connection", e.getMessage());
        }
    }
    
    private void handleCancel() {
        logger.debug("Ollama connection dialog cancelled");
        
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
    public void onSave(Function<OllamaConnection, Boolean> onSave) {
        this.onSaveCallback = onSave;
    }

    @Override
    public void onCancel(Runnable onCancel) {
        this.onCancelCallback = onCancel;
    }
}
