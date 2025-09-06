package dev.local.ai.ui.models;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.local.ai.ui.models.view.LLMSelectorView;

/**
 * Demo application for the Model Selector control.
 * Shows how to use the ModelSelector in a standalone application.
 */
public class LLMSelectorDemo extends Application {
    
    private static final Logger logger = LoggerFactory.getLogger(LLMSelectorDemo.class);
    
    @Override
    public void start(Stage primaryStage) {
        try {
            logger.info("Starting ModelSelectorDemo");
                     
            // Get the controller
            LLMSelectorView view = new LLMSelectorView();
            
            // Log controller initialization
            logger.info("ModelSelectorController initialized");
            
            // Create scene and stage
            Scene scene = new Scene(view, 400, 300);
            primaryStage.setTitle("Model Selector Demo");
            primaryStage.setScene(scene);
            primaryStage.setResizable(false);
            
            // Add close confirmation
            primaryStage.setOnCloseRequest(event -> {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Exit Application");
                alert.setHeaderText("Are you sure you want to exit?");
                alert.setContentText("This will close the Model Selector Demo.");
                
                if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                    logger.info("Application closed by user");
                } else {
                    event.consume();
                }
            });
            
            // Show the stage
            primaryStage.show();
            
            logger.info("ModelSelectorDemo started successfully");
            
        } catch (Exception e) {
            logger.error("Failed to start ModelSelectorDemo", e);
            
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to start application");
            alert.setContentText("An error occurred while starting the Model Selector Demo: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
