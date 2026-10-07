package dev.local.ai.core.tools.gates;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.local.ai.core.tools.IToolExecutionGate;
import io.reactivex.rxjava4.subjects.Subject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class WaitForApprovalGateTest {

    private WaitForApprovalGate gate;

    @BeforeEach
    void setUp() {
        gate = new WaitForApprovalGate();
    }

    @AfterEach
    void tearDown() {
        gate.close();
    }

    @Test
    void should_emit_approval_requested_and_complete_when_approved() throws Exception {
        var events = gate.events().test();
        var result = new AtomicReference<IToolExecutionGate.GateCheckResult>();

        var threadWaitingForToolApproval = Thread.startVirtualThread(() -> {
            result.set(gate.beforeToolExecution(request("tool-1")));
        });
        events.awaitCount(1);

        gate.approve("tool-1");
        threadWaitingForToolApproval.join(2_000);

        assertThat(result.get().result())
                .isEqualTo(IToolExecutionGate.GateResult.PASSED);
        events.assertValue(new ToolApprovalEvent.ToolApprovalRequested("tool-1", "tool-1"));
    }

    @Test
    void should_complete_decision_as_rejected() throws Exception {
        var events = gate.events().test();
        var result = new AtomicReference<IToolExecutionGate.GateCheckResult>();
        var waiting = Thread.startVirtualThread(() -> {
            result.set(gate.beforeToolExecution(request("tool-1")));
        });
        events.awaitCount(1);

        gate.reject("tool-1");
        waiting.join(2_000);

        assertThat(result.get().result())
                .isEqualTo(IToolExecutionGate.GateResult.REJECTED);
        assertThat(result.get().reason())
                .contains("User rejected");
    }

    @Test
    void should_ignore_unknown_approval_id() {
        gate.approve("missing");
        gate.reject("missing");
    }

    @Test
    void should_not_expose_the_subject_to_subscribers() {
        assertThat(gate.events()).isNotInstanceOf(Subject.class);
    }

    @Test
    void should_complete_pending_decision_as_cancelled() throws Exception {
        var events = gate.events().test();
        var result = new AtomicReference<IToolExecutionGate.GateCheckResult>();
        var waiting = Thread.startVirtualThread(() -> {
            result.set(gate.beforeToolExecution(request()));
        });
        events.awaitCount(1);

        gate.cancel();
        waiting.join(2_000);

        assertThat(result.get().result())
                .isEqualTo(IToolExecutionGate.GateResult.CANCELLED);
        assertThat(result.get().reason())
                .contains("stopped");
    }

    private static final Logger logger =  LoggerFactory.getLogger(WaitForApprovalGateTest.class);

    @Test
    void should_resolve_concurrent_tool_calls_when_each_is_approved_rejected_or_cancelled() throws InterruptedException {
        var toolRequests = getMultipleToolRequests(10);
        var results = new ConcurrentHashMap<String, IToolExecutionGate.GateCheckResult>();
        var expectedResults = new ConcurrentHashMap<String, IToolExecutionGate.GateCheckResult>();
        var decisions = new CountDownLatch(toolRequests.size());
        var random = new Random();

        var events = gate.events()
                .ofType(ToolApprovalEvent.ToolApprovalRequested.class)
                .map(requested -> decisionFor(requested, random, expectedResults))
                .subscribe(decision ->
                    Thread.startVirtualThread(() -> {
                        pauseBriefly(random);
                        decision.run();
                        logger.info("Executed new decision");
                        decisions.countDown();
                    }
                ));
        try {
            for (var toolRequest : toolRequests) {
                Thread.startVirtualThread(() ->
                        results.put(toolRequest.id(), gate.beforeToolExecution(toolRequest)));
            }

            assertThat(decisions.await(10, TimeUnit.SECONDS))
                    .isTrue();
            gate.cancel();
            await().atMost(5, TimeUnit.SECONDS)
                    .until(() -> results.size() == toolRequests.size());

            assertThat(results)
                    .isEqualTo(expectedResults);
        } finally {
            gate.cancel();
            events.dispose();
        }
    }

    private Runnable decisionFor(
            ToolApprovalEvent.ToolApprovalRequested requested,
            Random random,
            ConcurrentHashMap<String, IToolExecutionGate.GateCheckResult> expectedResults) {
        return switch (random.nextInt(3)) {
            case 0 -> {
                expectedResults.put(requested.approvalId(), IToolExecutionGate.GateCheckResult.passed());
                yield () -> gate.approve(requested.approvalId());
            }
            case 1 -> {
                expectedResults.put(requested.approvalId(), IToolExecutionGate.GateCheckResult.rejected("User rejected tool execution"));
                yield () -> gate.reject(requested.approvalId());
            }
            default -> {
                expectedResults.put(requested.approvalId(), IToolExecutionGate.GateCheckResult.cancelled("Chat stopped by user"));
                yield WaitForApprovalGateTest::leavePendingForCancel;
            }
        };
    }

    private static void leavePendingForCancel() {
    }

    private static void pauseBriefly(Random random) {
        try {
            Thread.sleep(random.nextInt(1));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private List<ToolExecutionRequest> getMultipleToolRequests(int count) {
        var result = new ArrayList<ToolExecutionRequest>(count);
        for (int i = 0; i < count; i++) {
            result.add(request("tool-" + i));
        }
        return result;
    }

    private static ToolExecutionRequest request() {
        return request("tool-1");
    }

    private static ToolExecutionRequest request(String id) {
        return ToolExecutionRequest.builder()
                .id(id)
                .name("run_command")
                .arguments("{}")
                .build();
    }
}
