package dev.local.ai.core.tools;

import dev.langchain4j.agent.tool.ToolSpecification;

public record ToolDescriptor(
    String id,
    String displayName,
    String group,
    ToolSpecification specification,
    ITool executor
) {

    public static final String LOCAL_GROUP = "Local";

    public ToolDescriptor {
        if (group == null || group.isBlank()) {
            group = LOCAL_GROUP;
        }
    }

    public ToolDescriptor(String id, String displayName, ToolSpecification specification, ITool executor) {
        this(id, displayName, LOCAL_GROUP, specification, executor);
    }
}
