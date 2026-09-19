package dev.local.ai.core.tools.gates;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.local.ai.core.tools.IToolExecutionGate;

import java.util.concurrent.CompletableFuture;

public interface IApprovalProvider {
    CompletableFuture<IToolExecutionGate.GateCheckResult> askForApproval(ToolExecutionRequest toolExecutionRequest);
}
