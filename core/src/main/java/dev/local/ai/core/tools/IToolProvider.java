package dev.local.ai.core.tools;

import java.util.List;

import dev.langchain4j.agent.tool.ToolSpecification;

public interface IToolProvider {

    List<ToolDescriptor> getToolDescriptors();

    default List<ToolDescriptor> getAllToolDescriptors() {
        return getToolDescriptors();
    }

    default List<ToolSpecification> getToolSpecifications() {
        return getToolDescriptors().stream()
            .map(ToolDescriptor::specification)
            .toList();
    }

    default List<ITool> getToolExecutors() {
        return getToolDescriptors().stream()
            .map(ToolDescriptor::executor)
            .toList();
    }
}
