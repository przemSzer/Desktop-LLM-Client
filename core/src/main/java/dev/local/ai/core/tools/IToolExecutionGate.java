package dev.local.ai.core.tools;

import dev.langchain4j.agent.tool.ToolExecutionRequest;

public interface IToolExecutionGate {
    enum GateResult {
        PASSED,
        REJECTED,
        ERROR
    }

    record GateCheckResult(GateResult result, String reason){
        public static GateCheckResult passed(){
            return new  GateCheckResult(GateResult.PASSED, "");
        }

        public static GateCheckResult rejected(String reason){
            return new  GateCheckResult(GateResult.REJECTED, reason);
        }

        public static GateCheckResult error(String reason){
            return new  GateCheckResult(GateResult.ERROR, reason);
        }
    }

    GateCheckResult beforeToolExecution(ToolExecutionRequest toolExecutionRequest);
}
