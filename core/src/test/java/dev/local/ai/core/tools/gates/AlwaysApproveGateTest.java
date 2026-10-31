package dev.local.ai.core.tools.gates;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.local.ai.core.tools.IToolExecutionGate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AlwaysApproveGateTest {

    @Test
    void should_return_passed_for_any_tool_request() {
        var request = ToolExecutionRequest.builder()
                .id("tool-1")
                .name("downloadWebPage")
                .arguments("{}")
                .build();

        var result = AlwaysApproveGate.INSTANCE.beforeToolExecution(request);

        assertThat(result.result())
                .isEqualTo(IToolExecutionGate.GateResult.PASSED);
        assertThat(result.reason())
                .isEmpty();
    }
}
