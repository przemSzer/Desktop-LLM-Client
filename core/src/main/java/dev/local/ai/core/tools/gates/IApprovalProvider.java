package dev.local.ai.core.tools.gates;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.local.ai.core.tools.IToolExecutionGate;

import java.util.concurrent.Future;

public interface IApprovalProvider {
    Future<IToolExecutionGate.GateCheckResult> askForApproval(ToolExecutionRequest toolExecutionRequest);
}
