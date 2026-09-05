package dev.local.ai.ui.chat.viewmodel;

import dev.local.ai.core.tools.IToolExecutionGate;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

class ToolCallChatMessageViewModelTest {

    @Test
    void shouldCompleteFutureWithPassedWhenApproved() {
        var viewModel = newToolCall();
        var approval = new CompletableFuture<IToolExecutionGate.GateCheckResult>();

        viewModel.requestApproval(approval);
        assertThat(viewModel.isNeedsApproval()).isTrue();

        viewModel.approve();

        assertThat(viewModel.isNeedsApproval()).isFalse();
        assertThat(approval).isCompleted();
        assertThat(approval.join().result()).isEqualTo(IToolExecutionGate.GateResult.PASSED);
    }

    @Test
    void shouldCompleteFutureWithRejectedWhenRejected() {
        var viewModel = newToolCall();
        var approval = new CompletableFuture<IToolExecutionGate.GateCheckResult>();

        viewModel.requestApproval(approval);
        viewModel.reject();

        assertThat(approval.join().result()).isEqualTo(IToolExecutionGate.GateResult.REJECTED);
        assertThat(approval.join().reason()).contains("User rejected");
        assertThat(viewModel.isNeedsApproval()).isFalse();
    }

    @Test
    void shouldIgnoreSecondDecisionAfterApproval() {
        var viewModel = newToolCall();
        var approval = new CompletableFuture<IToolExecutionGate.GateCheckResult>();

        viewModel.requestApproval(approval);
        viewModel.approve();
        viewModel.reject();

        assertThat(approval.join().result()).isEqualTo(IToolExecutionGate.GateResult.PASSED);
    }

    private static ToolCallChatMessageViewModel newToolCall() {
        return new ToolCallChatMessageViewModel(
                "Tool call: run_command (cmd: ls)",
                MessageTypeView.TOOL_CALL,
                List.of(),
                null,
                "tool-1"
        );
    }
}
