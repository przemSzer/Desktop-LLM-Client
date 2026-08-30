 package dev.local.ai.core.tools.gates;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.local.ai.core.tools.IToolExecutionGate;

public class DefaultToolExecutionGate implements IToolExecutionGate {

    @Override
    public GateCheckResult beforeToolExecution(ToolExecutionRequest toolExecutionRequest) {
        return GateCheckResult.passed();
    }

}
