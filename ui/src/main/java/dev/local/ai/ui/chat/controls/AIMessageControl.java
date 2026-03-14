package dev.local.ai.ui.chat.controls;

import dev.local.ai.ui.chat.viewmodel.ChatMessageViewModel;
import dev.local.ai.ui.chat.viewmodel.ChatViewModel;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import one.jpro.platform.mdfx.MarkdownView;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AIMessageControl extends VBox {
    
    @FXML
    private Label messageType;
    
    @FXML
    private AIMessageMarkdownView markdownView;

    @FXML
    private VBox messageActions;

    @FXML
    private Button copyMessageButton;

    private final Logger logger = LoggerFactory.getLogger(AIMessageControl.class);
    
    public AIMessageControl(ChatMessageViewModel message, ChatViewModel chatViewModel) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("AIMessageControl.fxml"));
        loader.setController(this);
        try {
            VBox loadedContent = loader.load();
            getChildren().addAll(loadedContent.getChildren());
            getStyleClass().addAll(loadedContent.getStyleClass());
            setSpacing(loadedContent.getSpacing());
            copyMessageButton.setOnAction(event -> chatViewModel.copyMessage(message));
            markdownView.setMdString(message.getContent());            
        } catch (IOException e) {
            logger.error("Failed to load AIMessageControl FXML", e);
            throw new RuntimeException("Failed to load AIMessageControl FXML", e);
        }
    }
    
    public AIMessageControl() {
        this(new ChatMessageViewModel("Hello I am *AI Assistant*", ChatMessageViewModel.MessageType.AI,List.of()), null);
    }
    
}
