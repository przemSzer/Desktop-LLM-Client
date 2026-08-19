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
            .flatMap(d -> d.specifications().stream())
            .toList();
    }

    default List<IToolExecutor> getToolExecutors() {
        return getToolDescriptors().stream()
            .map(ToolDescriptor::executor)
            .toList();
    }
}
