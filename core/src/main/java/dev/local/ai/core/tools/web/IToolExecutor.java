package dev.local.ai.core.tools.web;

import java.util.Optional;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.ToolExecutionResultMessage;

public interface IToolExecutor {
    Optional<ToolExecutionResultMessage> execute(ToolExecutionRequest toolExecutionRequest);
}
