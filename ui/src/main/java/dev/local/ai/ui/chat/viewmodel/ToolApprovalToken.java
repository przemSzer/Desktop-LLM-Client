package dev.local.ai.ui.chat.viewmodel;

public interface ToolApprovalToken {

    enum Decision {
        APPROVED,
        REJECTED
    }

    void setResult(Decision decision);
}
