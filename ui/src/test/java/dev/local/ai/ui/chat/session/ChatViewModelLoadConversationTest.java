package dev.local.ai.ui.chat.session;

import dev.langchain4j.memory.ChatMemory;
import dev.local.ai.core.chat.streaming.StreamingChat;
import dev.local.ai.core.events.CoreEventBus;
import dev.local.ai.core.storage.conversations.ConversationStore;
import dev.local.ai.ui.chat.viewmodel.ChatViewModel;
import dev.local.ai.ui.commands.CommandManager;

import javafx.application.Platform;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ChatViewModelLoadConversationTest {

    private static final AtomicBoolean PLATFORM_STARTED = new AtomicBoolean(false);

    @BeforeAll
    static void initJavaFx() throws InterruptedException {
        if (PLATFORM_STARTED.compareAndSet(false, true)) {
            CountDownLatch latch = new CountDownLatch(1);
            try {
                Platform.startup(latch::countDown);
            } catch (IllegalStateException _) {
                latch.countDown();
            }
            assertTrue(latch.await(5, TimeUnit.SECONDS),
                    "JavaFX toolkit failed to start within 5 seconds");
        }
    }

    private static void runOnFxThreadAndWait(Runnable r) throws InterruptedException {
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                r.run();
            } finally {
                done.countDown();
            }
        });
        assertTrue(done.await(5, TimeUnit.SECONDS), "FX action did not complete in 5s");
    }

    @Mock(lenient = true)
    private StreamingChat initialChat;

    @Mock(lenient = true)
    private ChatMemory initialMemory;

    @Mock(lenient = true)
    private StreamingChat newChat;

    @Mock(lenient = true)
    private ChatMemory newMemory;

    @Mock(lenient = true)
    private ChatSessionFactory sessionFactory;

    @Mock(lenient = true)
    private CommandManager commandManager;

    @Mock(lenient = true)
    private CoreEventBus eventBus;

    @Mock(lenient = true)
    private ConversationStore conversationStore;

    private ChatSession initialSession;
    private ChatSession newSession;
    private ChatViewModel viewModel;

    @BeforeEach
    void setUp() {
        given(initialMemory.messages()).willReturn(Collections.emptyList());
        given(newMemory.messages()).willReturn(Collections.emptyList());
        given(initialChat.getSystemMessage()).willReturn("");
        given(newChat.getSystemMessage()).willReturn("");

        initialSession = new ChatSession("conv-A", initialMemory, initialChat, provider -> {});
        newSession = new ChatSession("conv-B", newMemory, newChat, provider -> {});

        given(sessionFactory.openConversation("conv-B")).willReturn(newSession);
        given(conversationStore.findSummary(anyString())).willReturn(Optional.empty());

        viewModel = new ChatViewModel(initialSession, sessionFactory, conversationStore, commandManager, eventBus);
    }

    @Test
    void shouldOpenNewSessionViaFactoryWhenSwitching() throws InterruptedException {
        viewModel.loadConversation("conv-B");
        runOnFxThreadAndWait(() -> { /* drain any queued runLater */ });

        then(sessionFactory).should().openConversation("conv-B");
    }

    @Test
    void shouldCloseTheReplacedSession() throws InterruptedException {
        viewModel.loadConversation("conv-B");
        runOnFxThreadAndWait(() -> { });

        then(initialChat).should().close();
    }

    @Test
    void shouldAttachCallbackToNewChat() throws InterruptedException {
        viewModel.loadConversation("conv-B");
        runOnFxThreadAndWait(() -> { });

        then(newChat).should().setCallback(viewModel);
    }

    @Test
    void shouldUpdateCurrentConversationId() throws InterruptedException {
        viewModel.loadConversation("conv-B");
        runOnFxThreadAndWait(() -> { });

        assertEquals("conv-B", viewModel.getCurrentConversationId());
    }

    @Test
    void shouldNotSwitchWhenTargetEqualsCurrent() {
        viewModel.loadConversation("conv-A");

        then(sessionFactory).should(never()).openConversation("conv-A");
        then(initialChat).should(never()).close();
        assertSame("conv-A", viewModel.getCurrentConversationId());
    }

    @Test
    void shouldRefuseToSwitchWhileMessageInProgress() {
        viewModel.sendingMessageInProgressProperty().set(true);

        viewModel.loadConversation("conv-B");

        then(sessionFactory).should(never()).openConversation("conv-B");
        then(initialChat).should(never()).close();
        assertEquals("conv-A", viewModel.getCurrentConversationId());
    }
}
