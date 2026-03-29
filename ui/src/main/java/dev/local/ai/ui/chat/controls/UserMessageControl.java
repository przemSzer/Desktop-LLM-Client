package dev.local.ai.ui.chat.controls;

import dev.local.ai.ui.chat.viewmodel.ChatMessageViewModel;
import dev.local.ai.ui.chat.viewmodel.ChatViewModel;
import dev.local.ai.ui.files.viewmodel.AttachedFileViewModel;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserMessageControl extends VBox {
    
    @FXML
    private Label messageType;
    
    @FXML
    private SelectableText content;

    @FXML
    private Button copyMessageButton;

    @FXML
    private ListView<AttachedFileViewModel> attachmentList;

    private final Logger logger = LoggerFactory.getLogger(UserMessageControl.class);
    
    public UserMessageControl(ChatMessageViewModel message, ChatViewModel chatViewModel) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("UserMessageControl.fxml"));
        loader.setController(this);
        try {
            VBox loadedContent = loader.load();
            getChildren().addAll(loadedContent.getChildren());
            getStyleClass().addAll(loadedContent.getStyleClass());
            setSpacing(loadedContent.getSpacing());
            
            if (message != null) {
                content.setText(message.getContent());
                if (message.getAttachedFiles().isEmpty()) {
                    this.getChildren().remove(attachmentList);
                }else{
                    configureFiles(message);
                }
            }
            
            copyMessageButton.setOnAction(event -> chatViewModel.copyMessage(message));
        } catch (IOException e) {
            logger.error("Failed to load UserMessageControl FXML", e);
        }
    }

    private void configureFiles(ChatMessageViewModel message) {
        attachmentList.itemsProperty()
            .bind(message.attachedFilesProperty().map(FXCollections::observableArrayList));
        attachmentList.setCellFactory(listView -> new javafx.scene.control.ListCell<AttachedFileViewModel>() {
            @Override
            protected void updateItem(AttachedFileViewModel item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(null);
                if (empty || item == null) {
                    setText(null);
                }else{
                    setText(item.getDescription().title());
                }
            }
        });
        attachmentList
            .visibleProperty()
            .bind(message.attachedFilesProperty().map(list -> !list.isEmpty()));
    }
}
