package dev.local.ai.core.tools;

import dev.langchain4j.data.message.ToolExecutionResultMessage;

public interface IToolExecutionEventListener {

    void onToolCallFinished(ToolExecutionResultMessage gatedToolResult);
}
