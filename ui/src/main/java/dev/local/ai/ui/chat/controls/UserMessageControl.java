package dev.local.ai.ui.chat.controls;

import dev.local.ai.ui.chat.model.ChatMessage;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;

public class UserMessageControl extends VBox {
    
    private final Label messageType;
    private final Label content;
    
    public UserMessageControl() {
        super(10); // 10px spacing between elements
        
        messageType = new Label("You");
        messageType.setWrapText(true);
        messageType.setStyle("-fx-text-fill: blue; -fx-font-weight: bold;");
        
        content = new Label();
        content.setWrapText(true);
        content.setStyle("-fx-text-fill: blue; -fx-font-weight: bold;");
        
        // Make content expand to fill available space
        HBox.setHgrow(content, Priority.ALWAYS);
        
        getChildren().addAll(messageType, content);
    }
    
    public void setMessage(ChatMessage message) {
        if (message != null) {
            content.setText(message.getContent());
        }
    }
}
