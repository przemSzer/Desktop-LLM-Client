package dev.local.ai.core.tools;

import dev.langchain4j.agent.tool.ToolSpecification;

public record ToolDescriptor(
    String id,
    String displayName,
    ToolSpecification specification,
    ITool executor
) {}
