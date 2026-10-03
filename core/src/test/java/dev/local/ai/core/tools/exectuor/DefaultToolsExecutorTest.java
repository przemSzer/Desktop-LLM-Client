package dev.local.ai.core.tools.exectuor;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.local.ai.core.tools.ICancellable;
import dev.local.ai.core.tools.ITool;
import dev.local.ai.core.tools.IToolExecutionEventListener;
import dev.local.ai.core.tools.IToolExecutionGate;
import dev.local.ai.core.tools.IToolProvider;
import dev.local.ai.core.tools.ToolDescriptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.withSettings;

@ExtendWith(MockitoExtension.class)
class DefaultToolsExecutorTest {

    @Mock
    private IToolProvider toolProvider;

    @Mock
    private IToolExecutionGate toolExecutionGate;

    @Mock
    private ITool tool;

    @Mock
    private IToolExecutionEventListener listener;

    @Captor
    private ArgumentCaptor<ToolExecutionResultMessage> resultCaptor;

    private DefaultToolsExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new DefaultToolsExecutor(toolProvider, toolExecutionGate);
    }

    @AfterEach
    void tearDown() throws Exception {
        executor.close();
    }

    @Test
    void should_execute_tool_when_gate_passes() {
        var request = createToolRequest("tool-1", "run_command");

        givenToolDescriptor("run_command", tool);
        requestWillPassAGate(request);
        given(tool.execute(request))
                .willReturn(Optional.of(successResult(request, "ok")));

        var results = executor.execute(List.of(request), listener);

        assertThat(results)
                .hasSize(1);
        assertThat(results.getFirst().text())
                .isEqualTo("ok");
        assertThat(results.getFirst().isError())
                .isFalse();
        then(tool)
                .should()
                .execute(request);
        then(listener)
                .should()
                .onToolCallFinished(resultCaptor.capture());
        assertThat(resultCaptor.getValue().text())
                .isEqualTo("ok");
    }

    @Test
    void should_not_invoke_tool_when_gate_rejects() {
        var request = createToolRequest("tool-1", "run_command");
        givenToolDescriptor("run_command", tool);
        given(toolExecutionGate.beforeToolExecution(request))
                .willReturn(IToolExecutionGate.GateCheckResult.rejected("User rejected tool execution"));

        var results = executor.execute(List.of(request), listener);

        assertThat(results)
                .hasSize(1);
        assertThat(results.getFirst().isError())
                .isTrue();
        assertThat(results.getFirst().id())
                .isEqualTo("tool-1");
        assertThat(results.getFirst().toolName())
                .isEqualTo("run_command");
        assertThat(results.getFirst().text())
                .contains("User rejected tool execution");
        then(tool)
                .should(never())
                .execute(any());
        then(listener)
                .should()
                .onToolCallFinished(any());
    }

    @Test
    void should_not_invoke_tool_when_gate_returns_error() {
        var request = createToolRequest("tool-1", "run_command");
        givenToolDescriptor("run_command", tool);
        given(toolExecutionGate.beforeToolExecution(request))
                .willReturn(IToolExecutionGate.GateCheckResult.error("No approval provider found"));

        var results = executor.execute(List.of(request), listener);

        assertThat(results)
                .hasSize(1);
        assertThat(results.getFirst().isError())
                .isTrue();
        assertThat(results.getFirst().id())
                .isEqualTo("tool-1");
        assertThat(results.getFirst().toolName())
                .isEqualTo("run_command");
        assertThat(results.getFirst().text())
                .contains("No approval provider found");
        then(tool)
                .should(never())
                .execute(any());
        then(listener)
                .should()
                .onToolCallFinished(any());
    }

    @Test
    void should_not_invoke_tool_when_gate_cancels() {
        var request = createToolRequest("tool-1", "run_command");
        givenToolDescriptor("run_command", tool);
        given(toolExecutionGate.beforeToolExecution(request))
                .willReturn(IToolExecutionGate.GateCheckResult.cancelled("Chat stopped by user"));

        var results = executor.execute(List.of(request), listener);

        assertThat(results)
                .hasSize(1);
        assertThat(results.getFirst().isError())
                .isTrue();
        assertThat(results.getFirst().id())
                .isEqualTo("tool-1");
        assertThat(results.getFirst().toolName())
                .isEqualTo("run_command");
        assertThat(results.getFirst().text())
                .contains("Chat stopped by user");
        then(tool)
                .should(never())
                .execute(any());
        then(listener)
                .should()
                .onToolCallFinished(any());
    }

    @Test
    void should_notify_listener_when_no_matching_tool_found() {
        var request = createToolRequest("tool-missing", "unknown_tool");
        given(toolProvider.getToolDescriptors())
                .willReturn(List.of());

        var results = executor.execute(List.of(request), listener);

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().isError()).isTrue();
        assertThat(results.getFirst().id()).isEqualTo("tool-missing");
        assertThat(results.getFirst().toolName()).isEqualTo("unknown_tool");
        assertThat(results.getFirst().text()).contains("No matching tool found");
        then(listener)
                .should()
                .onToolCallFinished(resultCaptor.capture());
        assertThat(resultCaptor.getValue().id()).isEqualTo("tool-missing");
        then(toolExecutionGate)
                .should(never())
                .beforeToolExecution(any());
    }

    @Test
    void should_return_error_result_with_id_and_name_when_tool_throws() {
        var request = createToolRequest("tool-1", "run_command");
        givenToolDescriptor("run_command", tool);
        requestWillPassAGate(request);
        given(tool.execute(request))
                .willThrow(new RuntimeException("boom"));

        var results = executor.execute(List.of(request), listener);

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().isError())
                .isTrue();
        assertThat(results.getFirst().id())
                .isEqualTo("tool-1");
        assertThat(results.getFirst().toolName())
                .isEqualTo("run_command");
        assertThat(results.getFirst().text())
                .contains("boom");
        then(listener)
                .should()
                .onToolCallFinished(resultCaptor.capture());
        assertThat(resultCaptor.getValue().id())
                .isEqualTo("tool-1");
        assertThat(resultCaptor.getValue().toolName())
                .isEqualTo("run_command");
    }

    @Test
    void should_return_error_result_when_tool_returns_empty_optional() {
        var request = createToolRequest("tool-1", "run_command");
        givenToolDescriptor("run_command", tool);
        requestWillPassAGate(request);
        given(tool.execute(request))
                .willReturn(Optional.empty());

        var results = executor.execute(List.of(request), listener);

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().isError())
                .isTrue();
        assertThat(results.getFirst().id())
                .isEqualTo("tool-1");
        assertThat(results.getFirst().toolName())
                .isEqualTo("run_command");
        assertThat(results.getFirst().text())
                .contains("Tool returned empty result");
        then(listener)
                .should()
                .onToolCallFinished(resultCaptor.capture());
        assertThat(resultCaptor.getValue().id())
                .isEqualTo("tool-1");
        assertThat(resultCaptor.getValue().text())
                .contains("Tool returned empty result");
    }

    @Test
    void should_recover_when_submitted_future_completes_with_exception() {
        var request = createToolRequest("tool-1", "run_command");
        givenToolDescriptor("run_command", tool);
        requestWillPassAGate(request);
        given(tool.execute(request))
                .willThrow(new AssertionError("future failed"));

        var results = executor.execute(List.of(request), listener);

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().isError())
                .isTrue();
        assertThat(results.getFirst().text())
                .contains("future failed");
        then(listener)
                .should()
                .onToolCallFinished(resultCaptor.capture());
        assertThat(resultCaptor.getValue().isError())
                .isTrue();
        assertThat(resultCaptor.getValue().text())
                .contains("future failed");
    }

    @Test
    void should_return_tool_specifications_from_provider_descriptors() {
        var specA = ToolSpecification.builder().name("tool_a").description("A").build();
        var specB = ToolSpecification.builder().name("tool_b").description("B").build();
        given(toolProvider.getToolDescriptors())
                .willReturn(List.of(
                        new ToolDescriptor("tool_a", "Tool A", specA, tool),
                        new ToolDescriptor("tool_b", "Tool B", specB, tool)
                ));

        var specifications = executor.toolSpecifications();

        assertThat(specifications)
                .containsExactly(specA, specB);
    }

    @Test
    void should_forward_cancel_to_cancellable_gate() throws Exception {
        var cancellableGate = mock(IToolExecutionGate.class, withSettings().extraInterfaces(ICancellable.class));
        try (var cancellableExecutor = new DefaultToolsExecutor(toolProvider, cancellableGate)) {
            cancellableExecutor.cancel();

            then((ICancellable) cancellableGate)
                    .should()
                    .cancel();
        }
    }

    @Test
    void should_ignore_cancel_when_gate_is_not_cancellable() {
        executor.cancel();
        then(toolExecutionGate)
                .shouldHaveNoInteractions();
    }

    @Test
    void should_not_propagate_exception_when_listener_throws() {
        var request = createToolRequest("tool-1", "run_command");
        givenToolDescriptor("run_command", tool);
        requestWillPassAGate(request);
        given(tool.execute(request))
                .willReturn(Optional.of(successResult(request, "ok")));
        willThrow(new RuntimeException("listener failed"))
                .given(listener)
                .onToolCallFinished(any());

        var results = executor.execute(List.of(request), listener);

        assertThat(results)
                .hasSize(1);
        assertThat(results.getFirst().text())
                .isEqualTo("ok");
        assertThat(results.getFirst().isError())
                .isFalse();
        then(listener)
                .should()
                .onToolCallFinished(any());
    }

    @Test
    void should_restore_interrupt_flag_when_waiting_for_tools_is_interrupted() throws Exception {
        var request = createToolRequest("tool-1", "run_command");
        givenToolDescriptor("run_command", tool);
        var gateEntered = new CountDownLatch(1);
        var releaseGate = new CountDownLatch(1);

        willAnswer(invocation -> {
                gateEntered.countDown();
                assertThat(releaseGate.await(5, TimeUnit.SECONDS)).isTrue();
                return IToolExecutionGate.GateCheckResult.cancelled("test cleanup");
            })
                .given(toolExecutionGate)
                .beforeToolExecution(request);

        var waitingThread = Thread.currentThread();
        var interrupter = Thread.ofVirtual().start(() -> {
            try {
                assertThat(gateEntered.await(5, TimeUnit.SECONDS)).isTrue();
                waitingThread.interrupt();
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
            }
        });

        try {
            var results = executor.execute(List.of(request), listener);

            assertThat(Thread.currentThread().isInterrupted()).isTrue();
            assertThat(results)
                    .isEmpty();
            then(listener)
                    .should(never())
                    .onToolCallFinished(any());
        } finally {
            releaseGate.countDown();
            interrupter.join(TimeUnit.SECONDS.toMillis(5));
            Thread.interrupted();
        }
    }

    @Test
    void should_notify_listener_for_both_requests_in_parallel() {
        var requestA = createToolRequest("tool-a", "tool_a");
        var requestB = createToolRequest("tool-b", "tool_b");
        ITool toolA = req -> Optional.of(successResult(req, "a"));
        ITool toolB = req -> Optional.of(successResult(req, "b"));
        given(toolProvider.getToolDescriptors())
                .willReturn(List.of(
                        descriptor("tool_a", toolA),
                        descriptor("tool_b", toolB)
                ));
        requestWillPassAGate(any());

        var results = executor.execute(List.of(requestA, requestB), listener);

        assertThat(results)
                .hasSize(2);
        var resultTexts = results.stream()
                .map(ToolExecutionResultMessage::text).toList();
        assertThat(resultTexts)
                .containsExactlyInAnyOrder("a", "b");
        then(listener)
                .should(times(2))
                .onToolCallFinished(resultCaptor.capture());
        var listenerTexts = resultCaptor.getAllValues().stream().map(ToolExecutionResultMessage::text).toList();
        assertThat(listenerTexts)
                .containsExactlyInAnyOrder("a", "b");
    }

    @Test
    void should_keep_collecting_when_one_request_has_no_matching_tool() {
        var known = createToolRequest("tool-1", "run_command");
        var unknown = createToolRequest("tool-2", "missing_tool");
        givenToolDescriptor("run_command", tool);
        requestWillPassAGate(known);
        given(tool.execute(known))
                .willReturn(Optional.of(successResult(known, "ok")));

        var results = executor.execute(List.of(known, unknown), listener);

        assertThat(results)
                .hasSize(2)
                .anySatisfy(r -> {
                    assertThat(r.id()).isEqualTo("tool-1");
                    assertThat(r.isError()).isFalse();
                })
                .anySatisfy(r -> {
                    assertThat(r.id()).isEqualTo("tool-2");
                    assertThat(r.isError()).isTrue();
                });
        then(listener)
                .should(times(2))
                .onToolCallFinished(any());
    }

    private void requestWillPassAGate(ToolExecutionRequest request) {
        given(toolExecutionGate.beforeToolExecution(request))
                .willReturn(IToolExecutionGate.GateCheckResult.passed());
    }

    private void givenToolDescriptor(String toolId, ITool toolImpl) {
        given(toolProvider.getToolDescriptors())
                .willReturn(List.of(descriptor(toolId, toolImpl)));
    }

    private static ToolDescriptor descriptor(String toolId, ITool toolImpl) {
        var spec = ToolSpecification.builder().name(toolId).description(toolId).build();
        return new ToolDescriptor(toolId, toolId, spec, toolImpl);
    }

    private static ToolExecutionRequest createToolRequest(String id, String name) {
        return ToolExecutionRequest.builder()
                .id(id)
                .name(name)
                .arguments("{}")
                .build();
    }

    private static ToolExecutionResultMessage successResult(ToolExecutionRequest request, String text) {
        return ToolExecutionResultMessage.builder()
                .id(request.id())
                .toolName(request.name())
                .text(text)
                .isError(false)
                .build();
    }
}
