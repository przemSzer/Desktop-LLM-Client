package dev.local.ai.ui.chat.controls;

import dev.local.ai.ui.chat.model.ChatMessage;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;

public class MessageCell extends ListCell<ChatMessage> {

    private final Label messageLabel;

    public MessageCell() {
        super(); 
        messageLabel = new Label();
        messageLabel.setWrapText(true);
        messageLabel.setPrefWidth(300);
        messageLabel.setPrefHeight(100);
        getChildren().add(messageLabel);
    }



    @Override
    protected void updateItem(ChatMessage item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setText(null);
            setStyle("");
        } else {
            setText(item.toString());            
            
            // Apply different styles based on message type
            switch (item.getType()) {
                case USER:
                    setStyle("-fx-text-fill: blue; -fx-font-weight: bold;");
                    break;
                case AI:
                    setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                    break;
                case ERROR:
                    setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    break;
                case SYSTEM,PARTIAL:
                    setStyle("-fx-text-fill: gray; -fx-font-style: italic;");
                    break;
                default:
                    setStyle("");
            }
        }
    }
}
