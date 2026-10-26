package dev.local.ai.ui.chat.viewmodel;

import dev.langchain4j.memory.ChatMemory;
import dev.local.ai.core.chat.streaming.ChatEvent;
import dev.local.ai.core.chat.streaming.StreamingChat;
import dev.local.ai.core.storage.conversations.ConversationStore;
import dev.local.ai.core.tools.gates.ToolApprovalEvent;
import dev.local.ai.core.tools.gates.WaitForApprovalGate;
import dev.local.ai.ui.chat.session.ChatSession;
import dev.local.ai.ui.chat.session.ChatSessionFactory;
import dev.local.ai.ui.commands.CommandManager;
import io.reactivex.rxjava4.subjects.PublishSubject;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ChatViewModelApprovalTest {

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
    private StreamingChat mockChat;

    @Mock(lenient = true)
    private ChatMemory mockMemory;

    @Mock(lenient = true)
    private ChatSessionFactory sessionFactory;

    @Mock(lenient = true)
    private CommandManager commandManager;

    @Mock(lenient = true)
    private ConversationStore conversationStore;

    @Mock(lenient = true)
    private WaitForApprovalGate approval;

    private final PublishSubject<ChatEvent> events = PublishSubject.create();
    private final PublishSubject<ToolApprovalEvent> approvalEvents = PublishSubject.create();

    private ChatViewModel viewModel;

    @BeforeEach
    void setUp() {
        given(mockMemory.messages()).willReturn(Collections.emptyList());
        given(mockChat.getSystemMessage()).willReturn("");
        given(mockChat.events()).willReturn(events);
        given(approval.events()).willReturn(approvalEvents);
        given(conversationStore.findSummary(anyString())).willReturn(Optional.empty());
        var session = new ChatSession("conv-test", mockMemory, mockChat, approval);
        viewModel = new ChatViewModel(session, sessionFactory, conversationStore, commandManager);
    }

    @Test
    void shouldShowApprovalOnTheToolBubbleAndApprove() throws Exception {
        events.onNext(new ChatEvent.ToolCallRequested("req-1", "tool-1", "run_command", "cmd: ls"));
        runOnFxThreadAndWait(() -> { });
        approvalEvents.onNext(new ToolApprovalEvent.ToolApprovalRequested("tool-1", "tool-1"));
        runOnFxThreadAndWait(() -> { });

        assertThat(viewModel.getChatMessages()).hasSize(1);
        var toolCall = (ToolCallChatMessageViewModel) viewModel.getChatMessages().getFirst();
        assertThat(toolCall.isNeedsApproval()).isTrue();

        runOnFxThreadAndWait(toolCall::approve);

        then(approval).should().approve("tool-1");
        assertThat(toolCall.isNeedsApproval()).isFalse();
    }

    @Test
    void shouldIgnoreApprovalWhenToolCallMessageIsMissing() throws Exception {
        approvalEvents.onNext(new ToolApprovalEvent.ToolApprovalRequested("tool-missing", "tool-missing"));
        runOnFxThreadAndWait(() -> { });

        assertThat(viewModel.getChatMessages()).isEmpty();
        then(approval).should(never()).approve(anyString());
        then(approval).should(never()).reject(anyString());
    }

    @Test
    void shouldNotAttachApprovalToADifferentToolCall() throws Exception {
        events.onNext(new ChatEvent.ToolCallRequested("req-1", "tool-1", "run_command", "cmd: ls"));
        runOnFxThreadAndWait(() -> { });
        approvalEvents.onNext(new ToolApprovalEvent.ToolApprovalRequested("tool-other", "tool-other"));
        runOnFxThreadAndWait(() -> { });

        var existing = (ToolCallChatMessageViewModel) viewModel.getChatMessages().getFirst();
        assertThat(existing.isNeedsApproval()).isFalse();
        then(approval).should(never()).approve(anyString());
    }

    @Test
    void shouldHideApprovalButtonsWhenTurnIsCancelled() throws Exception {
        events.onNext(new ChatEvent.ToolCallRequested("req-1", "tool-cancel", "run_command", ""));
        runOnFxThreadAndWait(() -> { });
        approvalEvents.onNext(new ToolApprovalEvent.ToolApprovalRequested("tool-cancel", "tool-cancel"));
        runOnFxThreadAndWait(() -> { });

        events.onNext(new ChatEvent.TurnCancelled("req-1"));
        runOnFxThreadAndWait(() -> { });

        var toolCall = (ToolCallChatMessageViewModel) viewModel.getChatMessages().getFirst();
        assertThat(toolCall.isNeedsApproval()).isFalse();
        then(approval).should(never()).reject(anyString());
        then(approval).should(never()).approve(anyString());
    }
}
