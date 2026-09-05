package dev.local.ai.core.tools.exectuor;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.local.ai.core.tools.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class DefaultToolsExecutor implements IToolExecutor {

    private static final Logger logger = LoggerFactory.getLogger(DefaultToolsExecutor.class);
    private final IToolProvider toolProvider;
    private final IToolExecutionGate toolExecutionGates;

    public DefaultToolsExecutor(IToolProvider toolProvider, IToolExecutionGate toolExecutionGate) {
        this.toolProvider = toolProvider;
        this.toolExecutionGates = toolExecutionGate;
    }

    @Override
    public List<ToolExecutionResultMessage> execute(List<ToolExecutionRequest> toolExecutionRequests) {
        logger.debug("Processing {} requests", toolExecutionRequests.size());
        var results = new ArrayList<ToolExecutionResultMessage>();
        for (var currentToolRequest : toolExecutionRequests) {
            logger.debug("Processing request {}", currentToolRequest);
            var toolForCurrentRequest = getMatchingTool(currentToolRequest);
            if (toolForCurrentRequest != null) {
                var gatedToolResult = executeToolIncludingGates(currentToolRequest, toolForCurrentRequest);
                results.add(gatedToolResult);
            } else {
                logger.warn("No matching tool found for {}", currentToolRequest);
                results.add(toolNotFoundError(currentToolRequest, "No matching tool found for " + currentToolRequest));
            }
        }
        return results;
    }

    private ToolExecutionResultMessage executeToolIncludingGates(ToolExecutionRequest currentRequest, ToolDescriptor toolForCurrentRequest) {
        var beforeToolExecutionResult = toolExecutionGates.beforeToolExecution(currentRequest);
        if (beforeToolExecutionResult.result() == IToolExecutionGate.GateResult.REJECTED) {
            return beforeToolGateRejected(beforeToolExecutionResult, currentRequest);
        } else if (beforeToolExecutionResult.result() == IToolExecutionGate.GateResult.ERROR) {
            //TODO: what to do on error?
            return beforeToolGateRejected(beforeToolExecutionResult, currentRequest);
        }
        logger.debug("Tool passed before execution gate, so executing it");
        return toolForCurrentRequest.executor()
                .execute(currentRequest)
                .orElseGet(() -> {
                    logger.debug("Tool {} returned empty result", toolForCurrentRequest);
                    return toolNotFoundError(currentRequest, "Tool returned empty result for " + currentRequest);
                        }
                );
    }

    private ToolExecutionResultMessage beforeToolGateRejected(IToolExecutionGate.GateCheckResult beforeToolExecutionResult, ToolExecutionRequest currentRequest) {
        ToolExecutionResultMessage.Builder responseBuilder = responseBuilderFrom(currentRequest);
        return responseBuilder
                        .isError(true)
                        .text("Tool not accepted for execution because " + beforeToolExecutionResult.reason())
                        .build();
    }

    private ToolExecutionResultMessage.Builder responseBuilderFrom(ToolExecutionRequest currentRequest) {
        return ToolExecutionResultMessage
                .builder()
                .toolName(currentRequest.name())
                .id(currentRequest.id());
    }

    private ToolExecutionResultMessage toolNotFoundError(ToolExecutionRequest currentRequest, String textForLLM) {
        return ToolExecutionResultMessage.builder()
                .id(currentRequest.id())
                .toolName(currentRequest.name())
                .isError(true)
                .text(textForLLM)
                .build();
    }

    @Override
    public List<ToolSpecification> toolSpecifications() {
        return toolProvider.getToolDescriptors()
                .stream()
                .map(ToolDescriptor::specification)
                .toList();
    }

    private ToolDescriptor getMatchingTool(ToolExecutionRequest currentRequest) {
        var found = toolProvider.getToolDescriptors()
                .stream()
                .filter(t -> t.id().equals(currentRequest.name()))
                .findFirst();
        var foundTool = found.orElse(null);
        logger.debug("Found the following tool {} for {}",foundTool, currentRequest.name());
        return foundTool;
    }
}
