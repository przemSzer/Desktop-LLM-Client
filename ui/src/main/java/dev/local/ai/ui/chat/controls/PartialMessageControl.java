package dev.local.ai.ui.chat.controls;

import dev.local.ai.ui.chat.viewmodel.ChatMessageViewModel;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class PartialMessageControl extends VBox {
    
    private final Label messageType;
    private final Label content;
    
    public PartialMessageControl() {
        super(10); // 10px spacing between elements
        
        messageType = new Label("LLM");
        messageType.setWrapText(true);
        messageType.setStyle("-fx-text-fill: gray; -fx-font-style: italic;");
        
        content = new Label();
        content.setWrapText(true);
        content.setStyle("-fx-text-fill: gray; -fx-font-style: italic;");
        
        // Make content expand to fill available space
        HBox.setHgrow(this, Priority.SOMETIMES);
        VBox.setVgrow(this, Priority.ALWAYS);
        getChildren().addAll(messageType, content);
    }
    
    public void setMessage(ChatMessageViewModel message) {
        if (message != null) {
            content.textProperty().bind(message.contentProperty());
        }
    }
}
