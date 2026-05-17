package dev.local.ai.ui.chat.conversations;

import dev.local.ai.core.storage.conversations.ConversationStore;
import dev.local.ai.core.storage.conversations.ConversationSummary;
import dev.local.ai.ui.chat.viewmodel.ChatViewModel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ConversationsViewModelTest {

    @TempDir
    private Path tempDir;

    @Mock(lenient = true)
    private ChatViewModel chatViewModel;

    private ConversationStore store;
    private ConversationsViewModel viewModel;

    @BeforeEach
    void setUp() {
        store = new ConversationStore(tempDir);
        viewModel = new ConversationsViewModel(store, chatViewModel);
    }

    @Test
    void openSelectedInvokesLoadConversationForChosenRow() {
        String id = store.createConversation();
        ConversationSummary sel = store.listConversations().getFirst();
        viewModel.setSelectedConversation(sel);

        viewModel.openSelected();

        then(chatViewModel).should().loadConversation(id);
    }

    @Test
    void deletingCurrentConversationSwitchesToFallbackConversation() throws Exception {
        String idA = store.createConversation();
        Thread.sleep(50);
        String idB = store.createConversation();
        given(chatViewModel.getCurrentConversationId()).willReturn(idA);
        viewModel.setSelectedConversation(store.findSummary(idA).orElseThrow());

        viewModel.deleteSelected();

        then(chatViewModel).should().loadConversation(idB);
        assertTrue(store.findSummary(idA).isEmpty());
    }

    @Test
    void deletingAnotherConversationDoesNotInvokeLoadConversation() {
        String idA = store.createConversation();
        String idB = store.createConversation();
        given(chatViewModel.getCurrentConversationId()).willReturn(idA);
        viewModel.setSelectedConversation(store.findSummary(idB).orElseThrow());

        viewModel.deleteSelected();

        then(chatViewModel).should(never()).loadConversation(anyString());
        assertTrue(store.findSummary(idB).isEmpty());
        Optional<ConversationSummary> remaining = store.findSummary(idA);
        assertTrue(remaining.isPresent());
        assertEquals(idA, remaining.get().id());
    }
}
