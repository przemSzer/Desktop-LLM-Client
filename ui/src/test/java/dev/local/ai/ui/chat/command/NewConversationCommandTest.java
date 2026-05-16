package dev.local.ai.ui.chat.command;

import dev.local.ai.core.storage.conversations.ConversationStore;
import dev.local.ai.ui.chat.viewmodel.ChatViewModel;

import javafx.beans.property.SimpleBooleanProperty;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class NewConversationCommandTest {

    @Mock
    private ConversationStore conversationStore;

    @Mock
    private ChatViewModel viewModel;

    private NewConversationCommand command;

    @BeforeEach
    void setUp() {
        given(viewModel.sendingMessageInProgressProperty()).willReturn(new SimpleBooleanProperty(false));
        command = new NewConversationCommand(conversationStore, viewModel);
    }

    @Test
    void shouldCreateConversationAndLoadIt() {
        given(conversationStore.createConversation()).willReturn("new-id");

        command.execute();

        then(conversationStore).should().createConversation();
        then(viewModel).should().loadConversation("new-id");
    }

    @Test
    void shouldNotRunWhileSendingMessage() {
        given(viewModel.sendingMessageInProgressProperty()).willReturn(new SimpleBooleanProperty(true));
        command = new NewConversationCommand(conversationStore, viewModel);

        command.execute();

        then(conversationStore).should(never()).createConversation();
        then(viewModel).should(never()).loadConversation(anyString());
    }
}
