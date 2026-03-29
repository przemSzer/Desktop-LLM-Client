package dev.local.ai.ui.chat.controls;

import dev.local.ai.ui.chat.viewmodel.ChatMessageViewModel;
import dev.local.ai.ui.chat.viewmodel.ChatViewModel;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ToolMessageControl extends VBox {
    
    @FXML
    private Label messageType;
    
    @FXML
    private SelectableText toolContent;

    @FXML
    private VBox messageActions;

    @FXML
    private Button copyMessageButton;

    private final Logger logger = LoggerFactory.getLogger(ToolMessageControl.class);
    
    public ToolMessageControl(ChatMessageViewModel message, ChatViewModel chatViewModel) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("ToolMessageControl.fxml"));
        loader.setController(this);
        try {
            VBox loadedContent = loader.load();
            getChildren().addAll(loadedContent.getChildren());
            getStyleClass().addAll(loadedContent.getStyleClass());
            setSpacing(loadedContent.getSpacing());
            copyMessageButton.setOnAction(event -> chatViewModel.copyMessage(message));
            toolContent.setText(partialContent(message.getContent()));
        } catch (IOException e) {
            logger.error("Failed to load ToolMessageControl FXML", e);
            throw new RuntimeException("Failed to load ToolMessageControl FXML", e);
        }
    }
    
    private String partialContent(String content) {
        if (content == null) {
            return "";
        }
        if (content.length() > 100) {
            return content.substring(0, 100) + "...";
        }
        return content.substring(0, content.length());
    }

    public ToolMessageControl() {
        this(new ChatMessageViewModel("Tool execution result", ChatMessageViewModel.MessageType.TOOL_RESULT, List.of()), null);
    }
    
}
