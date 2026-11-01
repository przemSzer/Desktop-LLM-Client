package dev.local.ai.core.tools.gates;

public sealed interface ToolApprovalEvent permits ToolApprovalEvent.ToolApprovalRequested {

    record ToolApprovalRequested(String approvalId, String toolRequestId) implements ToolApprovalEvent {
    }
}
