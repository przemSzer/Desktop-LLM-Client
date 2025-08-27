package dev.local.ai.ui.chat.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.local.ai.core.chat.DefaultChats;
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
    private TextArea messageInput;
    
    @FXML
    private Button sendButton;

    @FXML
    private ListView<ChatMessage> chatListView;
    
    @FXML
    private Label statusLabel;
    
    // ViewModel
    private ChatViewModel chatViewModel;
    
    @FXML
    public void initialize() {
        logger.info("Initializing ChatController");
        
        // Create ViewModel with the Chat model
        chatViewModel = new ChatViewModel(DefaultChats.openAIGPT4oMini(), new CommandManager());
        
        // Set up data binding
        setupDataBinding();
        
        // Set up event handlers
        setupEventHandlers();
        
        // Set up custom cell factory for better message display
        setupCellFactory();
        
        logger.info("ChatController initialized with MVVM pattern");
    }
    
    private void setupDataBinding() {
        // Bind chat messages to ListView
        chatListView.setItems(chatViewModel.getChatMessages());
        
        // Bind input message to TextArea (two-way binding)
        messageInput.textProperty().bindBidirectional(chatViewModel.inputMessageProperty());
        
        // Bind status message to status label
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
        
        logger.debug("Event handlers setup completed");
    }
    
    private void setupCellFactory() {
        chatListView.setCellFactory(new Callback<ListView<ChatMessage>, ListCell<ChatMessage>>() {
            @Override
            public ListCell<ChatMessage> call(ListView<ChatMessage> param) {
                return new ListCell<ChatMessage>() {
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
                                case SYSTEM:
                                    setStyle("-fx-text-fill: gray; -fx-font-style: italic;");
                                    break;
                                default:
                                    setStyle("");
                            }
                        }
                    }
                };
            }
        });
        
        logger.debug("Custom cell factory setup completed");
    }
    
    // Public method to access ViewModel if needed
    public ChatViewModel getChatViewModel() {
        return chatViewModel;
    }
} 