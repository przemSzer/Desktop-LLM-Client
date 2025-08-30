package dev.local.ai.ui.chat.controls;

import dev.local.ai.ui.chat.model.ChatMessage;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import one.jpro.platform.mdfx.MarkdownView;

import java.io.IOException;

public class AIMessageControl extends VBox {
    
    @FXML
    private Label messageType;
    
    @FXML
    private MarkdownView markdownView;
    
    public AIMessageControl() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AIMessageControl.fxml"));
        loader.setController(this);
        try {
            VBox loadedContent = loader.load();
            // Add the loaded content to this instance
            getChildren().addAll(loadedContent.getChildren());
            // Copy the styles and properties from the loaded content
            getStyleClass().addAll(loadedContent.getStyleClass());
            setSpacing(loadedContent.getSpacing());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load AIMessageControl FXML", e);
        }
    }
    
    public void setMessage(ChatMessage message) {
        if (message != null) {
            markdownView.setMdString(message.getContent());
        }
    }
}
