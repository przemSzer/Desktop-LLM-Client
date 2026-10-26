package dev.local.ai.core.tools.exectuor;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.local.ai.core.tools.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class DefaultToolsExecutor implements IToolExecutor, AutoCloseable, ICancellable {

    private static final Logger logger = LoggerFactory.getLogger(DefaultToolsExecutor.class);
    private final IToolProvider toolProvider;
    private final IToolExecutionGate toolExecutionGate;
    private final ExecutorService executor;

    public DefaultToolsExecutor(IToolProvider toolProvider, IToolExecutionGate toolExecutionGate) {
        this.toolProvider = toolProvider;
        this.toolExecutionGate = toolExecutionGate;
        var toolsThreadFactory = createToolsThreadFactory();
        this.executor = Executors.newThreadPerTaskExecutor(toolsThreadFactory);
    }

    private ThreadFactory createToolsThreadFactory() {
        return Thread.ofVirtual()
                .name("tools-", 0)
                .uncaughtExceptionHandler((th, ex) -> logger.error("Uncaught exception in during tool call", ex))
                .factory();
    }

    @Override
    public List<ToolExecutionResultMessage> execute(List<ToolExecutionRequest> toolExecutionRequests, IToolExecutionEventListener listener) {
        var completionService = prepareExecutor();
        logger.debug("Processing {} requests", toolExecutionRequests.size());
        var results = new ArrayList<ToolExecutionResultMessage>(toolExecutionRequests.size());
        for (var currentToolRequest : toolExecutionRequests) {
            logger.debug("Processing request {}", currentToolRequest);
            var toolForCurrentRequest = getMatchingTool(currentToolRequest);
            if (toolForCurrentRequest != null) {
                completionService.submit(() -> executeToolSafely(currentToolRequest, toolForCurrentRequest));
            } else {
                logger.warn("No matching tool found for {}", currentToolRequest);
                var toolNotFound = toolNotFoundError(currentToolRequest, "No matching tool found for " + currentToolRequest + " or tool disabled");
                results.add(toolNotFound);
                notifyToolCallFinished(listener, toolNotFound);
            }
        }
        waitForToolCallsBeingFinished(completionService,
                results,
                toolExecutionRequests.size(),
                listener
        );
        return results;
    }

    private void waitForToolCallsBeingFinished(CompletionService<ToolExecutionResultMessage> executor, ArrayList<ToolExecutionResultMessage> results, int expectedToolResults, IToolExecutionEventListener listener) {
        try{
            while(results.size() < expectedToolResults) {
                collectNextToolResult(executor, results, listener);
            }
        } catch (InterruptedException _){
            logger.info("Waiting for tools to finish interrupted");
            Thread.currentThread().interrupt();
        }
    }

    private void collectNextToolResult(CompletionService<ToolExecutionResultMessage> executor, ArrayList<ToolExecutionResultMessage> results, IToolExecutionEventListener listener) throws InterruptedException {
        try {
            var finishedCall = executor.take();
            var toolCallResult = finishedCall.get();
            results.add(toolCallResult);
            notifyToolCallFinished(listener, toolCallResult);
        } catch (ExecutionException e) {
            logger.warn("Execution of a tool threw an exception", e);
            var failed = toolExecutionFailedWithoutRequest(e);
            results.add(failed);
            notifyToolCallFinished(listener, failed);
        }
    }

    private CompletionService<ToolExecutionResultMessage> prepareExecutor() {
        return new ExecutorCompletionService<>(
            executor
        );
    }

    private ToolExecutionResultMessage executeToolSafely(ToolExecutionRequest currentRequest, ToolDescriptor toolForCurrentRequest) {
        try {
            return executeToolIncludingGates(currentRequest, toolForCurrentRequest);
        } catch (Exception e) {
            logger.warn("Execution of tool {} failed", currentRequest.name(), e);
            return toolExecutionFailed(currentRequest, e);
        }
    }

    private void notifyToolCallFinished(IToolExecutionEventListener listener, ToolExecutionResultMessage result) {
        try {
            listener.onToolCallFinished(result);
        } catch (Exception e) {
            logger.warn("Tool finished listener failed for tool {}", result.toolName(), e);
        }
    }

    private ToolExecutionResultMessage toolExecutionFailed(ToolExecutionRequest currentRequest, Exception e) {
        return responseBuilderFrom(currentRequest)
                .isError(true)
                .text("Execution of a tool failed: " + failureMessage(e))
                .build();
    }

    private ToolExecutionResultMessage toolExecutionFailedWithoutRequest(ExecutionException e) {
        return ToolExecutionResultMessage.builder()
                .isError(true)
                .text("Execution of a tool failed: " + failureMessage(e.getCause() != null ? e.getCause() : e))
                .build();
    }

    private static String failureMessage(Throwable failure) {
        var message = failure.getMessage();
        if (message == null || message.isBlank()) {
            return failure.getClass().getSimpleName();
        }
        return message;
    }

    private ToolExecutionResultMessage executeToolIncludingGates(ToolExecutionRequest currentRequest, ToolDescriptor toolForCurrentRequest) {
        var beforeToolExecutionResult = toolExecutionGate.beforeToolExecution(currentRequest);
        if (beforeToolExecutionResult.result() != IToolExecutionGate.GateResult.PASSED){
            return beforeToolGateRejected(beforeToolExecutionResult, currentRequest);
        }
        logger.debug("Tool passed 'before execution gate', so executing it");
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

    @Override
    public void close() throws Exception {
        this.executor.close();
    }

    @Override
    public void cancel() {
        if (toolExecutionGate instanceof ICancellable cancellable){
            cancellable.cancel();
        }
    }
}
