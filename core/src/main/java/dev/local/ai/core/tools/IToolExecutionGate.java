package dev.local.ai.core.tools;

import dev.langchain4j.agent.tool.ToolExecutionRequest;

public interface IToolExecutionGate {
    enum GateResult {
        PASSED,
        REJECTED
    }

    record GateCheckResult(GateResult result, String reason){
        public static GateCheckResult passed(){
            return new  GateCheckResult(GateResult.PASSED, "");
        }
    }

    GateCheckResult beforeToolExecution(ToolExecutionRequest toolExecutionRequest);
}
