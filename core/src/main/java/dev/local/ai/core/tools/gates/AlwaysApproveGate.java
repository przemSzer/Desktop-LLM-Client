package dev.local.ai.core.tools.gates;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.local.ai.core.tools.IToolExecutionGate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AlwaysApproveGate implements IToolExecutionGate {

    private static final Logger LOGGGER = LoggerFactory.getLogger(AlwaysApproveGate.class);

    public static final AlwaysApproveGate INSTANCE = new AlwaysApproveGate();

    private AlwaysApproveGate() {
    }

    @Override
    public GateCheckResult beforeToolExecution(ToolExecutionRequest toolExecutionRequest) {
        LOGGGER.debug("Auto approve for tool call: {}", toolExecutionRequest.id());
        return GateCheckResult.passed();
    }
}
