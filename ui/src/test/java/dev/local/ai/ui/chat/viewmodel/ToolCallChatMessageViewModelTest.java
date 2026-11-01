package dev.local.ai.ui.chat.viewmodel;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class ToolCallChatMessageViewModelTest {

    @Test
    void should_report_approved_when_approved() {
        var viewModel = newToolCall();
        var decision = new AtomicReference<ToolApprovalToken.Decision>();

        viewModel.requestApproval(decision::set);
        assertThat(viewModel.isNeedsApproval()).isTrue();

        viewModel.approve();

        assertThat(viewModel.isNeedsApproval()).isFalse();
        assertThat(decision.get()).isEqualTo(ToolApprovalToken.Decision.APPROVED);
    }

    @Test
    void should_report_rejected_when_rejected() {
        var viewModel = newToolCall();
        var decision = new AtomicReference<ToolApprovalToken.Decision>();

        viewModel.requestApproval(decision::set);
        viewModel.reject();

        assertThat(decision.get()).isEqualTo(ToolApprovalToken.Decision.REJECTED);
        assertThat(viewModel.isNeedsApproval()).isFalse();
    }

    @Test
    void should_ignore_second_decision_after_approval() {
        var viewModel = newToolCall();
        var decision = new AtomicReference<ToolApprovalToken.Decision>();

        viewModel.requestApproval(decision::set);
        viewModel.approve();
        viewModel.reject();

        assertThat(decision.get()).isEqualTo(ToolApprovalToken.Decision.APPROVED);
    }

    @Test
    void should_hide_approval_without_a_decision_when_cleared() {
        var viewModel = newToolCall();
        var decision = new AtomicReference<ToolApprovalToken.Decision>();

        viewModel.requestApproval(decision::set);
        viewModel.clearApproval();

        assertThat(viewModel.isNeedsApproval()).isFalse();
        assertThat(decision.get()).isNull();
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
