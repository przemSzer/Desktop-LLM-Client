package dev.local.ai.ui.chat.command;

import dev.local.ai.core.chat.ILLMChat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ClearChatCommandTest {

    @Mock
    private ILLMChat chat;

    private ClearChatCommand command;

    @BeforeEach
    void setUp() {
        command = new ClearChatCommand(chat);
    }

    @Test
    void shouldEmptyConversationViaEmptyNonSystemMessages() {
        command.execute();

        then(chat).should().emptyNonSystemMessages();
        then(chat).should(never()).clearMemory();
    }
}
