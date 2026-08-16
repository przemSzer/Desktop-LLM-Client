package dev.local.ai.ui.viewmodel;

import dev.langchain4j.memory.ChatMemory;
import dev.local.ai.core.chat.streaming.StreamingChat;
import dev.local.ai.core.events.CoreEventBus;
import dev.local.ai.core.storage.conversations.ConversationStore;
import dev.local.ai.ui.chat.session.ChatSession;
import dev.local.ai.ui.chat.session.ConversationSessionFactory;
import dev.local.ai.ui.chat.viewmodel.ChatViewModel;
import dev.local.ai.ui.commands.CommandManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ChatViewModelTest {

    @Mock(lenient = true)
    private StreamingChat mockChat;

    @Mock(lenient = true)
    private ChatMemory mockMemory;

    @Mock(lenient = true)
    private ConversationSessionFactory sessionFactory;

    @Mock(lenient = true)
    private CommandManager commandManager;

    @Mock(lenient = true)
    private CoreEventBus eventBus;

    @Mock(lenient = true)
    private ConversationStore conversationStore;

    private ChatSession session;
    private ChatViewModel viewModel;

    @BeforeEach
    void setUp() {
        given(mockMemory.messages()).willReturn(Collections.emptyList());
        given(mockChat.getSystemMessage()).willReturn("");
        given(conversationStore.findSummary(anyString())).willReturn(Optional.empty());
        session = new ChatSession("conv-test", mockMemory, mockChat);
        viewModel = new ChatViewModel(session, sessionFactory, conversationStore, commandManager, eventBus);
    }

    @Test
    void testInitialization() {
        assertNotNull(viewModel);
        assertEquals("", viewModel.getInputMessage());
        assertEquals(0, viewModel.getChatMessages().size());
        assertEquals("conv-test", viewModel.getCurrentConversationId());
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

        assertEquals(0, viewModel.getChatMessages().size());
    }

    @Test
    void testGetMessageCount() {
        given(mockChat.getMessageCount()).willReturn(5);
        assertEquals(5, viewModel.getMessageCount());
    }
}
