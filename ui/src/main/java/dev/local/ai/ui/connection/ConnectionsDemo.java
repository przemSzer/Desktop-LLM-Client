package dev.local.ai.ui.connection;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Demo application to showcase the ConnectionsView implementation.
 * This can be used to test the connections functionality independently.
 */
public class ConnectionsDemo extends Application {
    
    @Override
    public void start(Stage primaryStage) throws IOException {
        // Load the FXML
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ConnectionsView.fxml"));
        VBox root = loader.load();
        
        // Create the scene
        Scene scene = new Scene(root);
        
        // Set up the stage
        primaryStage.setTitle("Model Providers Connections");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(600);
        primaryStage.setMinHeight(400);
        
        // Show the stage
        primaryStage.show();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
