package dev.local.ai.ui.chat.controls;

import dev.local.ai.ui.chat.viewmodel.ChatMessageViewModel;
import dev.local.ai.ui.chat.viewmodel.ChatViewModel;
import javafx.scene.control.ListCell;
import javafx.scene.layout.VBox;

public class MessageCell extends ListCell<ChatMessageViewModel> {

    private final ChatViewModel chatViewModel;

    public MessageCell(ChatViewModel chatViewModel) {
        this.chatViewModel = chatViewModel;
        setFocusTraversable(false);
    }

    @Override
    protected void updateItem(ChatMessageViewModel item, boolean empty) {
        super.updateItem(item, empty);
        
        if (empty || item == null) {
            setText(null);
            setGraphic(null);
        } else {
            // Apply different styles based on message type
            switch (item.getType()) {
                case USER:
                    var userControl = new UserMessageControl(item, chatViewModel);
                    setGraphic(userControl);
                    setText(null);
                    break;
                case AI:
                    var aiControl = new AIMessageControl(item, chatViewModel);
                    setGraphic(aiControl);
                    setText(null);
                    break;
                case ERROR:
                    VBox errorBox = new VBox(4);
                    errorBox.getStyleClass().addAll("message-bubble", "error-message");
                    SelectableText errorText = new SelectableText("Error: " + item.getContent());
                    errorText.getStyleClass().add("error-content");
                    errorBox.getChildren().add(errorText);
                    setGraphic(errorBox);
                    setText(null);
                    break;
                case TOOL_RESULT:
                case TOOL_CALL:
                    var toolCallControl = new ToolMessageControl(item, chatViewModel);
                    setGraphic(toolCallControl);
                    setText(null);
                    break;                
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
