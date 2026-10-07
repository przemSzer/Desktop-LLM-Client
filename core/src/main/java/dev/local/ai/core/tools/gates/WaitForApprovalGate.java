package dev.local.ai.core.tools.gates;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.local.ai.core.tools.ICancellable;
import dev.local.ai.core.tools.IToolExecutionGate;
import io.reactivex.rxjava4.core.Observable;
import io.reactivex.rxjava4.subjects.PublishSubject;
import io.reactivex.rxjava4.subjects.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

public class WaitForApprovalGate implements IToolExecutionGate, ICancellable, AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(WaitForApprovalGate.class.getName());

    private final Subject<ToolApprovalEvent> events = PublishSubject.<ToolApprovalEvent>create().toSerialized();
    private final ConcurrentHashMap<String, CompletableFuture<GateCheckResult>> pendingDecisions =
            new ConcurrentHashMap<>();
    private final Set<CompletableFuture<GateCheckResult>> pendingApprovals =
            ConcurrentHashMap.newKeySet();

    public Observable<ToolApprovalEvent> events() {
        return events.hide();
    }

    public void approve(String approvalId) {
        completeDecision(approvalId, GateCheckResult.passed());
    }

    public void reject(String approvalId) {
        completeDecision(approvalId, GateCheckResult.rejected("User rejected tool execution"));
    }

    @Override
    public GateCheckResult beforeToolExecution(ToolExecutionRequest toolExecutionRequest) {
        var approvalChallenge = new CompletableFuture<GateCheckResult>();
        var approvalId = approvalIdOf(toolExecutionRequest);
        pendingDecisions.put(approvalId, approvalChallenge);
        pendingApprovals.add(approvalChallenge);
        events.onNext(new ToolApprovalEvent.ToolApprovalRequested(approvalId, toolExecutionRequest.id()));
        try {
            return approvalChallenge.get();
        } catch (InterruptedException e) {
            logger.error("Interrupted while waiting for approval", e);
            Thread.currentThread().interrupt();
            return GateCheckResult.error("Interrupted while waiting for approval");
        } catch (CancellationException _) {
            logger.info("Approval cancelled for tool {}", toolExecutionRequest.name());
            return GateCheckResult.rejected("Approval cancelled");
        } catch (ExecutionException e) {
            logger.error("Error while waiting for approval", e);
            return GateCheckResult.error("Error while waiting for approval");
        } finally {
            pendingApprovals.remove(approvalChallenge);
            pendingDecisions.remove(approvalId, approvalChallenge);
        }
    }

    @Override
    public void cancel() {
        for (var future : pendingApprovals) {
            future.complete(GateCheckResult.cancelled("Chat stopped by user"));
        }
        pendingDecisions.clear();
    }

    @Override
    public void close() {
        pendingDecisions.clear();
        events.onComplete();
    }

    private void completeDecision(String approvalId, GateCheckResult result) {
        if (approvalId == null) {
            return;
        }
        var pending = pendingDecisions.remove(approvalId);
        if (pending == null) {
            logger.warn("No pending approval for {}", approvalId);
            return;
        }
        pending.complete(result);
    }

    private static String approvalIdOf(ToolExecutionRequest request) {
        var id = request.id();
        if (id == null || id.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return id;
    }
}
