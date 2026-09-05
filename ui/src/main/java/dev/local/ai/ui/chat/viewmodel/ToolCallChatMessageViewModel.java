package dev.local.ai.ui.chat.viewmodel;

import dev.local.ai.core.chat.messages.Statistics;
import dev.local.ai.core.tools.IToolExecutionGate;
import dev.local.ai.ui.files.viewmodel.AttachedFileViewModel;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ToolCallChatMessageViewModel extends ChatMessageViewModel {

    private final BooleanProperty needsApproval = new SimpleBooleanProperty(false);
    private CompletableFuture<IToolExecutionGate.GateCheckResult> pendingApproval;

    public ToolCallChatMessageViewModel(String content, MessageTypeView type, List<AttachedFileViewModel> attachedFiles, Statistics statistics, String id) {
        super(content, type, attachedFiles, statistics, id);
    }

    public boolean isNeedsApproval() {
        return needsApproval.get();
    }

    public BooleanProperty needsApprovalProperty() {
        return needsApproval;
    }

    public void requestApproval(CompletableFuture<IToolExecutionGate.GateCheckResult> approval) {
        this.pendingApproval = approval;
        this.needsApproval.set(true);
    }

    public void approve() {
        completePending(IToolExecutionGate.GateCheckResult.passed());
    }

    public void reject() {
        completePending(IToolExecutionGate.GateCheckResult.rejected("User rejected tool execution"));
    }

    public void rejectIfPending(String reason) {
        completePending(IToolExecutionGate.GateCheckResult.rejected(reason));
    }

    public boolean hasPendingApproval() {
        return pendingApproval != null && !pendingApproval.isDone();
    }

    private void completePending(IToolExecutionGate.GateCheckResult result) {
        var approval = pendingApproval;
        if (approval == null || approval.isDone()) {
            return;
        }
        pendingApproval = null;
        needsApproval.set(false);
        approval.complete(result);
    }
}
