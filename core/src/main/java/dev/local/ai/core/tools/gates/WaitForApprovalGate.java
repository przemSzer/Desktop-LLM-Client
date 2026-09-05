package dev.local.ai.core.tools.gates;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.local.ai.core.tools.IToolExecutionGate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

public class WaitForApprovalGate implements IToolExecutionGate {

    private static final Logger logger = LoggerFactory.getLogger(WaitForApprovalGate.class.getName());

    private IApprovalProvider approvalProvider;

    public void setApprovalProvider(IApprovalProvider approvalProvider) {
        this.approvalProvider = approvalProvider;
    }

    @Override
    public GateCheckResult beforeToolExecution(ToolExecutionRequest toolExecutionRequest) {
        if (approvalProvider == null){
            return GateCheckResult.error("No approval provider found");
        }
        var approvalChallenge = approvalProvider.askForApproval(toolExecutionRequest);
        if (approvalChallenge == null) {
            return GateCheckResult.error("Approval provider returned no result");
        }
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
        }
    }
}
