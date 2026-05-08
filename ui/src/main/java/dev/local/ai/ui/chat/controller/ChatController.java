package dev.local.ai.ui.chat.controller;

import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.local.ai.core.chat.DefaultChats;
import dev.local.ai.core.tools.FilterableToolProvider;
import dev.local.ai.ui.chat.controls.ChatWebView;
import dev.local.ai.ui.chat.viewmodel.ChatMessageViewModel;
import dev.local.ai.ui.chat.viewmodel.ChatMessageViewModel.MessageType;
import dev.local.ai.ui.chat.viewmodel.ChatViewModel;
import dev.local.ai.ui.commands.CommandManagerProvider;
import dev.local.ai.ui.files.controls.FileAttachmentControl;
import dev.local.ai.ui.tools.ToolsSelectorView;

/**
 * Controller for the Chat UI following MVVM pattern.
 * Handles only UI events and delegates business logic to ViewModel.
 */
public class ChatController {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    
    @FXML
    private TextArea systemMessageTextArea;

    @FXML
    private TextArea messageInput;
    
    @FXML
    private Button sendButton;

    @FXML
    private Button stopButton;
    
    @FXML
    private ProgressIndicator sendingMessageProgress;
    
    @FXML
    private ChatWebView chatWebView;
    
    @FXML
    private Label statusLabel;

    @FXML
    private Button clearChatButton;
    
    @FXML
    private FileAttachmentControl fileAttachmentControl;

    @FXML
    private FileAttachmentControl systemMessageFileAttachments;

    @FXML
    private ToolsSelectorView toolsSelectorView;

    private ChatViewModel chatViewModel;
    
    @FXML
    public void initialize() {
        try {
            logger.debug("Initializing ChatController");
            
            chatViewModel = new ChatViewModel(DefaultChats.defaultChat(), CommandManagerProvider.get());
            toolsSelectorView.init(FilterableToolProvider.getInstance());
            
            setupDataBinding();
            setupEventHandlers();
            
            logger.debug("ChatController initialized.");
        } catch (Exception e) {
            logger.error("Error initializing ChatController", e);         
        }
    }
    
    private void setupDataBinding() {        
        systemMessageTextArea.textProperty().bindBidirectional(chatViewModel.systemMessageProperty());
        systemMessageFileAttachments.attachedFilesProperty().bind(chatViewModel.systemMessageAttachedFilesProperty());

        chatViewModel.getChatMessages().addListener((ListChangeListener<ChatMessageViewModel>) change -> {
            while (change.next()) {
                if (change.wasReplaced()) {
                    for (int i = change.getFrom(); i < change.getTo(); i++) {
                        ChatMessageViewModel msg = change.getList().get(i);
                        if (msg == null) continue;
                        chatWebView.removePartialMessage();
                        chatWebView.addMessage(msg);
                    }
                } else if (change.wasAdded()) {
                    for (ChatMessageViewModel msg : change.getAddedSubList()) {
                        if (msg == null) continue;
                        if (msg.getType() == MessageType.PARTIAL) {
                            chatWebView.setPartialMessage(msg.getContent());
                            msg.contentProperty().addListener((obs, oldVal, newVal) ->
                                    chatWebView.setPartialMessage(newVal));
                        } else {
                            chatWebView.addMessage(msg);
                        }
                    }
                }
                if (change.wasRemoved() && change.getList().isEmpty()) {
                    chatWebView.clearMessages();
                }
            }
        });
        
        messageInput.textProperty().bindBidirectional(chatViewModel.inputMessageProperty());
        statusLabel.textProperty().bind(chatViewModel.statusMessageProperty());
        fileAttachmentControl.attachedFilesProperty()
            .bind(chatViewModel.attachedFilesProperty());
        sendButton.visibleProperty().bind(chatViewModel.sendingMessageInProgressProperty().not());
        stopButton.visibleProperty().bind(chatViewModel.sendingMessageInProgressProperty());
        sendingMessageProgress.visibleProperty().bind(chatViewModel.sendingMessageInProgressProperty());
        logger.debug("Data binding setup completed");
    }
    
    private void setupEventHandlers() {
        sendButton.setOnAction(event -> {
            logger.debug("Send button clicked");
            chatViewModel.sendMessage();
        });
        
        stopButton.setOnAction(event -> {
            logger.debug("Stop button clicked");
            chatViewModel.stopMessage();
        });

        messageInput.setOnKeyPressed(event -> {
            if (event.getCode().toString().equals("ENTER") && event.isControlDown()) {
                event.consume();
                chatViewModel.sendMessage();
            }
        });

        clearChatButton.setOnAction(event -> {
            chatViewModel.clearChat();
        });
        
        logger.debug("Event handlers setup completed");
    }
    
    public ChatViewModel getChatViewModel() {
        return chatViewModel;
    }
} 