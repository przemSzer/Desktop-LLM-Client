package dev.local.ai.ui.chat.viewmodel;

import dev.local.ai.core.chat.messages.Statistics;
import dev.local.ai.ui.files.viewmodel.AttachedFileViewModel;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.util.List;

public class ToolCallChatMessageViewModel extends ChatMessageViewModel {

    private final BooleanProperty needsApproval = new SimpleBooleanProperty(false);
    private ToolApprovalToken pendingApproval;

    public ToolCallChatMessageViewModel(String content, MessageTypeView type, List<AttachedFileViewModel> attachedFiles, Statistics statistics, String id) {
        super(content, type, attachedFiles, statistics, id);
    }

    public boolean isNeedsApproval() {
        return needsApproval.get();
    }

    public BooleanProperty needsApprovalProperty() {
        return needsApproval;
    }

    public void requestApproval(ToolApprovalToken approval) {
        this.pendingApproval = approval;
        this.needsApproval.set(true);
    }

    public void approve() {
        completePending(ToolApprovalToken.Decision.APPROVED);
    }

    public void reject() {
        completePending(ToolApprovalToken.Decision.REJECTED);
    }

    public void clearApproval() {
        pendingApproval = null;
        needsApproval.set(false);
    }

    public boolean hasPendingApproval() {
        return pendingApproval != null && needsApproval.get();
    }

    private void completePending(ToolApprovalToken.Decision decision) {
        var approval = pendingApproval;
        if (approval == null) {
            return;
        }
        pendingApproval = null;
        needsApproval.set(false);
        approval.setResult(decision);
    }
}
