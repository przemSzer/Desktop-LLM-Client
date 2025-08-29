package dev.local.ai.ui.chat.controller;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.local.ai.core.chat.DefaultChats;
import dev.local.ai.ui.chat.controls.MessageCell;
import dev.local.ai.ui.chat.model.ChatMessage;
import dev.local.ai.ui.chat.viewmodel.ChatViewModel;
import dev.local.ai.ui.commands.CommandManager;

/**
 * Controller for the Chat UI following MVVM pattern.
 * Handles only UI events and delegates business logic to ViewModel.
 */
public class ChatController {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    
    // UI Components
    @FXML
    private TextArea systemMessageTextArea;

    @FXML
    private TextArea messageInput;
    
    @FXML
    private Button sendButton;

    @FXML
    private ListView<ChatMessage> chatListView;
    
    @FXML
    private Label statusLabel;

    @FXML
    private Button clearChatButton;
    
    // ViewModel
    private ChatViewModel chatViewModel;
    
    @FXML
    public void initialize() {
        logger.debug("Initializing ChatController");
        
        // Create ViewModel with the Chat model
        chatViewModel = new ChatViewModel(DefaultChats.openAIGPT4oMiniStreaming(), new CommandManager());
        
        // Set up data binding
        setupDataBinding();
        
        // Set up event handlers
        setupEventHandlers();
        
        chatListView.setCellFactory(lv -> new MessageCell());
        
        logger.debug("ChatController initialized.");
    }
    
    private void setupDataBinding() {
        systemMessageTextArea.textProperty().bindBidirectional(chatViewModel.systemMessageProperty());

        chatListView.setItems(chatViewModel.getChatMessages());
        chatViewModel.getChatMessages().addListener((ListChangeListener<ChatMessage>) change -> {
            if (change.next() && change.wasAdded()) {
                // Use Platform.runLater to ensure UI is updated first
                Platform.runLater(() -> {
                    int lastIndex = chatListView.getItems().size() - 1;
                    if (lastIndex >= 0) {
                        chatListView.scrollTo(lastIndex);                        
                    }
                });
            }
        });
        
        messageInput.textProperty().bindBidirectional(chatViewModel.inputMessageProperty());
        
        statusLabel.textProperty().bind(chatViewModel.statusMessageProperty());
        
        logger.debug("Data binding setup completed");
    }
    
    private void setupEventHandlers() {
        // Send button click handler
        sendButton.setOnAction(event -> {
            logger.debug("Send button clicked");
            chatViewModel.sendMessage();
        });
        
        // Enter key handler for message input
        messageInput.setOnKeyPressed(event -> {
            if (event.getCode().toString().equals("ENTER") && event.isControlDown()) {
                event.consume(); // Prevent new line
                chatViewModel.sendMessage();
            }
        });

        clearChatButton.setOnAction(event -> {
            chatViewModel.clearChat();
        });
        
        logger.debug("Event handlers setup completed");
    }
    
    // Public method to access ViewModel if needed
    public ChatViewModel getChatViewModel() {
        return chatViewModel;
    }
} 