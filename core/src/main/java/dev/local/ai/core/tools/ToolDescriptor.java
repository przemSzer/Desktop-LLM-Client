package dev.local.ai.core.tools;

import java.util.List;

import dev.langchain4j.agent.tool.ToolSpecification;

public record ToolDescriptor(
    String id,
    String displayName,
    List<ToolSpecification> specifications,
    IToolExecutor executor
) {}
