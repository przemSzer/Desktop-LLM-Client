package dev.local.ai.core.tools;

import java.util.Optional;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.ToolExecutionResultMessage;

public interface ITool {
    Optional<ToolExecutionResultMessage> execute(ToolExecutionRequest toolExecutionRequest);
}
