package dev.local.ai.ui.chat.controls;

import dev.local.ai.ui.chat.model.ChatMessage;
import dev.local.ai.ui.chat.viewmodel.ChatViewModel;
import javafx.scene.control.ListCell;

public class MessageCell extends ListCell<ChatMessage> {

    private final ChatViewModel chatViewModel;

    public MessageCell(ChatViewModel chatViewModel) {
        this.chatViewModel = chatViewModel;
    }

    @Override
    protected void updateItem(ChatMessage item, boolean empty) {
        super.updateItem(item, empty);
        
        if (empty || item == null) {
            setText(null);
            setGraphic(null);
            setStyle("");
        } else {
            // Apply different styles based on message type
            switch (item.getType()) {
                case USER:
                    var userControl = new UserMessageControl();
                    userControl.setMessage(item);
                    setGraphic(userControl);
                    setText(null);
                    break;
                case AI:
                    var aiControl = new AIMessageControl(item, chatViewModel);
                    setGraphic(aiControl);
                    setText(null);
                    break;
                case ERROR:
                    setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    setGraphic(null);
                    setText("Error: " + item.getContent());
                    break;
                case SYSTEM:
                case PARTIAL:
                    var partialControl = new PartialMessageControl();
                    partialControl.setMessage(item);
                    setGraphic(partialControl);
                    setText(null);
                    break;
                default:
                    setStyle("");
                    setGraphic(null);
                    setText(item.getContent());
            }
        }
    }
}
