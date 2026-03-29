package dev.local.ai.ui.chat.controls;

import dev.local.ai.ui.chat.viewmodel.ChatMessageViewModel;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class PartialMessageControl extends VBox {
    
    private final Label messageType;
    private final SelectableText content;
    
    public PartialMessageControl() {
        super(8);
        getStyleClass().addAll("message-bubble", "partial-message");
        
        messageType = new Label("LLM");
        messageType.setWrapText(true);
        messageType.getStyleClass().addAll("message-type-label", "partial-label");
        
        content = new SelectableText();
        content.getStyleClass().add("partial-content");
        
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
