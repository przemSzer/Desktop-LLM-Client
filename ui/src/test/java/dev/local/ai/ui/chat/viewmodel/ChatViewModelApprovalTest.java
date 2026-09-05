package dev.local.ai.ui.chat.viewmodel;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.memory.ChatMemory;
import dev.local.ai.core.chat.messages.Message;
import dev.local.ai.core.chat.streaming.StreamingChat;
import dev.local.ai.core.events.CoreEventBus;
import dev.local.ai.core.storage.conversations.ConversationStore;
import dev.local.ai.core.tools.IToolExecutionGate;
import dev.local.ai.ui.chat.session.ChatSession;
import dev.local.ai.ui.chat.session.ChatSessionFactory;
import dev.local.ai.ui.commands.CommandManager;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

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
    private CoreEventBus eventBus;

    @Mock(lenient = true)
    private ConversationStore conversationStore;

    private ChatViewModel viewModel;

    @BeforeEach
    void setUp() {
        given(mockMemory.messages()).willReturn(Collections.emptyList());
        given(mockChat.getSystemMessage()).willReturn("");
        given(conversationStore.findSummary(anyString())).willReturn(Optional.empty());
        var session = new ChatSession("conv-test", mockMemory, mockChat, provider -> {});
        viewModel = new ChatViewModel(session, sessionFactory, conversationStore, commandManager, eventBus);
    }

    @Test
    void shouldReuseExistingToolCallMessageAndCompleteOnApprove() throws Exception {
        viewModel.onMessageAdded(Message.toolCall("run_command", Map.of("cmd", "ls"), "tool-1"), "req-1");
        runOnFxThreadAndWait(() -> { });

        var request = ToolExecutionRequest.builder()
                .id("tool-1")
                .name("run_command")
                .arguments("{\"cmd\":\"ls\"}")
                .build();

        var future = viewModel.approvalProvider().askForApproval(request);
        runOnFxThreadAndWait(() -> { });

        assertThat(viewModel.getChatMessages()).hasSize(1);
        var toolCall = (ToolCallChatMessageViewModel) viewModel.getChatMessages().getFirst();
        assertThat(toolCall.isNeedsApproval()).isTrue();

        runOnFxThreadAndWait(toolCall::approve);

        assertThat(future.get(2, TimeUnit.SECONDS).result()).isEqualTo(IToolExecutionGate.GateResult.PASSED);
        assertThat(toolCall.isNeedsApproval()).isFalse();
    }

    @Test
    void shouldCreateToolCallMessageWhenNoneExistsYet() throws Exception {
        var request = ToolExecutionRequest.builder()
                .id("tool-missing")
                .name("download_page")
                .arguments("{\"url\":\"https://example.com\"}")
                .build();

        var future = viewModel.approvalProvider().askForApproval(request);
        runOnFxThreadAndWait(() -> { });

        assertThat(viewModel.getChatMessages()).hasSize(1);
        var toolCall = (ToolCallChatMessageViewModel) viewModel.getChatMessages().getFirst();
        assertThat(toolCall.getId()).isEqualTo("tool-missing");
        assertThat(toolCall.isNeedsApproval()).isTrue();

        runOnFxThreadAndWait(toolCall::reject);

        assertThat(future.get(2, TimeUnit.SECONDS).result()).isEqualTo(IToolExecutionGate.GateResult.REJECTED);
    }

    @Test
    void shouldRejectPendingApprovalOnCancel() throws Exception {
        var request = ToolExecutionRequest.builder()
                .id("tool-cancel")
                .name("run_command")
                .arguments("{}")
                .build();

        var future = viewModel.approvalProvider().askForApproval(request);
        runOnFxThreadAndWait(() -> { });

        viewModel.onCancel();
        runOnFxThreadAndWait(() -> { });

        assertThat(future.get(2, TimeUnit.SECONDS).result()).isEqualTo(IToolExecutionGate.GateResult.REJECTED);
        assertThat(future.get().reason()).contains("Cancelled");
    }
}
