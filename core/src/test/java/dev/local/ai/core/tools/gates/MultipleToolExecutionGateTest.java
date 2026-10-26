package dev.local.ai.core.tools.gates;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.local.ai.core.tools.ICancellable;
import dev.local.ai.core.tools.IToolExecutionGate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class MultipleToolExecutionGateTest {

    @Mock
    private CancellableGate defaultGate;

    @Mock
    private CancellableGate namedGate;

    @Mock
    private IToolExecutionGate plainGate;

    private MultipleToolExecutionGate gate;

    @BeforeEach
    void setUp() {
        gate = new MultipleToolExecutionGate(defaultGate);
    }

    @Test
    void should_delegate_to_default_gate_when_no_gate_is_registered_for_the_tool() {
        var request = request("run_command");
        var expected = IToolExecutionGate.GateCheckResult.passed();
        given(defaultGate.beforeToolExecution(request))
                .willReturn(expected);

        var result = gate.beforeToolExecution(request);

        assertThat(result)
                .isEqualTo(expected);
        then(defaultGate)
                .should()
                .beforeToolExecution(request);
    }

    @Test
    void should_delegate_to_registered_gate_when_tool_name_matches() {
        var request = request("run_command");
        var expected = IToolExecutionGate.GateCheckResult.rejected("User rejected tool execution");
        gate.addGate("run_command", namedGate);
        given(namedGate.beforeToolExecution(request))
                .willReturn(expected);

        var result = gate.beforeToolExecution(request);

        assertThat(result)
                .isEqualTo(expected);
        then(namedGate)
                .should()
                .beforeToolExecution(request);
        then(defaultGate)
                .should(never())
                .beforeToolExecution(any());
    }

    @Test
    void should_delegate_to_default_gate_when_registered_gate_is_for_another_tool() {
        var request = request("run_command");
        var expected = IToolExecutionGate.GateCheckResult.error("approval failed");
        gate.addGate("download_page", namedGate);
        given(defaultGate.beforeToolExecution(request))
                .willReturn(expected);

        var result = gate.beforeToolExecution(request);

        assertThat(result)
                .isEqualTo(expected);
        then(defaultGate)
                .should()
                .beforeToolExecution(request);
        then(namedGate)
                .should(never())
                .beforeToolExecution(any());
    }

    @Test
    void should_use_latest_registered_gate_when_the_same_tool_name_is_added_again() {
        var request = request("run_command");
        var expected = IToolExecutionGate.GateCheckResult.cancelled("Chat stopped by user");
        gate.addGate("run_command", namedGate);
        gate.addGate("run_command", plainGate);
        given(plainGate.beforeToolExecution(request))
                .willReturn(expected);

        var result = gate.beforeToolExecution(request);

        assertThat(result)
                .isEqualTo(expected);
        then(plainGate)
                .should()
                .beforeToolExecution(request);
        then(namedGate)
                .should(never())
                .beforeToolExecution(any());
    }

    @Test
    void should_cancel_default_gate_when_it_is_cancellable() {
        gate.cancel();

        then(defaultGate)
                .should()
                .cancel();
    }

    @Test
    void should_cancel_registered_cancellable_gates_and_skip_gates_that_are_not_cancellable() {
        gate.addGate("run_command", namedGate);
        gate.addGate("download_page", plainGate);

        gate.cancel();

        then(defaultGate)
                .should()
                .cancel();
        then(namedGate)
                .should()
                .cancel();
        then(plainGate)
                .shouldHaveNoInteractions();
    }

    @Test
    void should_skip_cancel_when_default_gate_is_not_cancellable() {
        var nonCancellableDefault = mock(IToolExecutionGate.class);
        var subject = new MultipleToolExecutionGate(nonCancellableDefault);
        subject.addGate("download_page", plainGate);

        subject.cancel();

        then(nonCancellableDefault)
                .shouldHaveNoInteractions();
        then(plainGate)
                .shouldHaveNoInteractions();
    }

    private static ToolExecutionRequest request(String toolName) {
        return ToolExecutionRequest.builder()
                .id("tool-1")
                .name(toolName)
                .arguments("{}")
                .build();
    }

    private interface CancellableGate extends IToolExecutionGate, ICancellable {
    }
}
