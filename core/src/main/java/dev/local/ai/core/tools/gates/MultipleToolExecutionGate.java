 package dev.local.ai.core.tools.gates;

 import dev.langchain4j.agent.tool.ToolExecutionRequest;
 import dev.local.ai.core.tools.IToolExecutionGate;

 import java.util.HashMap;
 import java.util.Map;

 public class MultipleToolExecutionGate implements IToolExecutionGate {

    private final Map<String, IToolExecutionGate> toolExecutionGates = new HashMap<>();
     private final IToolExecutionGate defaultGate;

     public MultipleToolExecutionGate(IToolExecutionGate defaultToolExecutionGate) {
        this.defaultGate = defaultToolExecutionGate;
    }

    public void addGate(String toolName, IToolExecutionGate toolExecutionGate) {
        toolExecutionGates.put(toolName, toolExecutionGate);
    }

    @Override
    public GateCheckResult beforeToolExecution(ToolExecutionRequest toolExecutionRequest) {
        var internalGate = toolExecutionGates.get(toolExecutionRequest.name());
        if (internalGate == null) {
            return defaultGate.beforeToolExecution(toolExecutionRequest);
        }else{
            return internalGate.beforeToolExecution(toolExecutionRequest);
        }
    }

}
