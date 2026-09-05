package dev.local.ai.core.tools;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.ToolExecutionResultMessage;

import java.util.List;

public interface IToolExecutor {
    List<ToolExecutionResultMessage> execute(List<ToolExecutionRequest> toolExecutionRequests);
    List<ToolSpecification> toolSpecifications();
}
