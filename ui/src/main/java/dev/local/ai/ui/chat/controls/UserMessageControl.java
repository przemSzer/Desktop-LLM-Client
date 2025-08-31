package dev.local.ai.ui.chat.controls;

import dev.local.ai.ui.chat.model.ChatMessage;
import dev.local.ai.ui.chat.viewmodel.ChatViewModel;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserMessageControl extends VBox {
    
    @FXML
    private Label messageType;
    
    @FXML
    private Label content;

    @FXML
    private Button copyMessageButton;

    private final Logger logger = LoggerFactory.getLogger(UserMessageControl.class);
    
    public UserMessageControl(ChatMessage message, ChatViewModel chatViewModel) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/UserMessageControl.fxml"));
        loader.setController(this);
        try {
            VBox loadedContent = loader.load();
            getChildren().addAll(loadedContent.getChildren());
            getStyleClass().addAll(loadedContent.getStyleClass());
            setSpacing(loadedContent.getSpacing());
            
            if (message != null) {
                content.setText(message.getContent());
            }
            
            copyMessageButton.setOnAction(event -> chatViewModel.copyMessage(message));
        } catch (IOException e) {
            logger.error("Failed to load UserMessageControl FXML", e);
            throw new RuntimeException("Failed to load UserMessageControl FXML", e);
        }
    }
}
