package dev.local.ai.core.tools.gates;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.local.ai.core.tools.IToolExecutionGate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class WaitForApprovalGateTest {

    @Mock
    private IApprovalProvider approvalProvider;

    @Test
    void shouldReturnErrorWhenProviderIsMissing() {
        var gate = new WaitForApprovalGate();
        var request = request();

        var result = gate.beforeToolExecution(request);

        assertThat(result.result()).isEqualTo(IToolExecutionGate.GateResult.ERROR);
        assertThat(result.reason()).contains("No approval provider");
    }

    @Test
    void shouldReturnProviderDecision() {
        var gate = new WaitForApprovalGate();
        gate.setApprovalProvider(approvalProvider);
        var request = request();
        given(approvalProvider.askForApproval(request))
                .willReturn(CompletableFuture.completedFuture(IToolExecutionGate.GateCheckResult.passed()));

        var result = gate.beforeToolExecution(request);

        assertThat(result.result()).isEqualTo(IToolExecutionGate.GateResult.PASSED);
    }

    @Test
    void shouldReturnErrorWhenProviderReturnsNullFuture() {
        var gate = new WaitForApprovalGate();
        gate.setApprovalProvider(approvalProvider);
        var request = request();
        given(approvalProvider.askForApproval(request)).willReturn(null);

        var result = gate.beforeToolExecution(request);

        assertThat(result.result()).isEqualTo(IToolExecutionGate.GateResult.ERROR);
    }

    private static ToolExecutionRequest request() {
        return ToolExecutionRequest.builder()
                .id("tool-1")
                .name("run_command")
                .arguments("{}")
                .build();
    }
}
