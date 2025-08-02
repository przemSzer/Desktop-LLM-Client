package com.example.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChatController {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    
    @FXML
    private TextArea messageInput;
    
    @FXML
    private Button sendButton;

    @FXML
    private ListView<String> chatListView;

    @FXML
    public void initialize() {
        logger.info("Initializing ChatController");                
    }
    
    @FXML
    private void handleSendMessage() {
        String message = messageInput.getText().trim();
        if (!message.isEmpty()) {
            messageInput.clear();
            logger.info("Message sent: {}", message);
        }
    }
    
        
} 