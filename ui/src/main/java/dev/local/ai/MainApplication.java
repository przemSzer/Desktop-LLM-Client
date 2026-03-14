package dev.local.ai;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.local.ai.ui.utils.HostServicesProvider;

import java.io.IOException;

public class MainApplication extends Application {
    
    private static final Logger logger = LoggerFactory.getLogger(MainApplication.class);
    
    @Override
    public void start(Stage primaryStage) {
        try {
            logger.info("Starting Chat Application");
            HostServicesProvider.getInstance().setHostServices(this.getHostServices());
            Platform.setImplicitExit(true);
            // Load the FXML file
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ChatWindow.fxml"));
            Parent root = loader.load();
            
            Scene scene = new Scene(root, 600, 400);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            
            // Set up the primary stage
            primaryStage.setTitle("LLM Chat");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(900);
            primaryStage.setMinHeight(800);
            
            // Show the window
            primaryStage.show();
            logger.info("Chat Application started successfully");
            
        } catch (IOException e) {
            logger.error("Failed to load FXML file", e);
            throw new RuntimeException("Failed to start application", e);
        }
    }
    
    @Override
    public void stop() {
        logger.info("Chat Application stopping");
        System.exit(0);
    }
    
    public static void main(String[] args) {
        GlobalExceptionHandler.install();
        launch(args);
    }
} 