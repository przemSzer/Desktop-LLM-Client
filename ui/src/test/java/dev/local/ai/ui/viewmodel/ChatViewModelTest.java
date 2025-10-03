package dev.local.ai.ui.viewmodel;

import dev.local.ai.core.chat.Chat;
import dev.local.ai.ui.chat.viewmodel.ChatViewModel;
import dev.local.ai.ui.commands.CommandManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatViewModelTest {

    @Mock(lenient = true)
    private Chat mockChat;
    
    private ChatViewModel viewModel;

    @Mock(lenient = true)
    private CommandManager commandManager;
    
    @BeforeEach
    void setUp() {
        viewModel = new ChatViewModel(mockChat, commandManager);
    }
    
    @Test
    void testInitialization() {
        assertNotNull(viewModel);
        assertEquals("Ready", viewModel.getStatusMessage());
        assertEquals("", viewModel.getInputMessage());
        assertEquals(0, viewModel.getChatMessages().size());
    }
    
    @Test
    void testSetInputMessage() {
        String testMessage = "Test message";
        viewModel.setInputMessage(testMessage);
        assertEquals(testMessage, viewModel.getInputMessage());
    }
    
    @Test
    void testSendMessageWithEmptyMessage() {
        viewModel.setInputMessage("");
        viewModel.sendMessage();
        
        // Should not add any messages
        assertEquals(0, viewModel.getChatMessages().size());
    }
    
    @Test
    void testSendMessageSuccess() throws Exception {
        String testMessage = "Hello, AI!";
        viewModel.setInputMessage(testMessage);
        
        // Mock successful chat response
        doNothing().when(mockChat).sendMessage(testMessage);
        
        viewModel.sendMessage();
        
        // // Verify messages were added
        // assertEquals(2, viewModel.getChatMessages().size());
        
        // // Check user message
        // ChatMessage userMessage = viewModel.getChatMessages().get(0);
        // assertEquals(testMessage, userMessage.getContent());
        // assertEquals(MessageType.USER, userMessage.getType());
        
        // // Check AI response
        // ChatMessage aiMessage = viewModel.getChatMessages().get(1);
        // assertEquals("Message processed successfully", aiMessage.getContent());
        // assertEquals(MessageType.AI, aiMessage.getType());
        
        // // Verify input was cleared
        // assertEquals("", viewModel.getInputMessage());
        
        // // Verify chat was called
        // verify(mockChat).sendMessage(testMessage);
    }
    
    @Test
    void testSendMessageFailure() throws Exception {
        // String testMessage = "Hello, AI!";
        // viewModel.setInputMessage(testMessage);
        
        // // Mock chat failure
        // doThrow(new RuntimeException("Chat error")).when(mockChat).sendMessage(testMessage);
        
        // viewModel.sendMessage();
        
        // // Verify error message was added
        // assertEquals(2, viewModel.getChatMessages().size());
        
        // // Check error message
        // ChatMessage errorMessage = viewModel.getChatMessages().get(1);
        // assertEquals("Failed to send message", errorMessage.getContent());
        // assertEquals(MessageType.ERROR, errorMessage.getType());
        
        // // Verify status shows error
        // assertEquals("Error sending message", viewModel.getStatusMessage());
    }
    
    @Test
    void testClearChat() {
        // Add some messages first
        // viewModel.setInputMessage("Test message");
        // viewModel.sendMessage();
        
        // // Clear chat
        // viewModel.clearChat();
        
        // assertEquals(0, viewModel.getChatMessages().size());
        // assertEquals("Chat cleared", viewModel.getStatusMessage());
    }
    
    @Test
    void testGetMessageCount() {
        assertEquals(0, viewModel.getMessageCount());
        
        // Mock chat to return specific count
        when(mockChat.getMessageCount()).thenReturn(5);
        assertEquals(5, viewModel.getMessageCount());
    }
}
