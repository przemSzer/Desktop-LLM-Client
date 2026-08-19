package dev.local.ai.core.tools;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.local.ai.core.events.CoreEventBus;

import static org.assertj.core.api.Assertions.assertThat;

class FilterableToolProviderTest {

    private CoreEventBus eventBus;
    private ToolDescriptor descriptorA;
    private ToolDescriptor descriptorB;
    private StubToolProvider delegate;

    @BeforeEach
    void setUp() {
        eventBus = new CoreEventBus();

        var specA = ToolSpecification.builder().name("toolA").description("Tool A").build();
        var specB = ToolSpecification.builder().name("toolB").description("Tool B").build();

        IToolExecutor executorA = request -> Optional.empty();
        IToolExecutor executorB = request -> Optional.empty();

        descriptorA = new ToolDescriptor("toolA", "Tool A", List.of(specA), executorA);
        descriptorB = new ToolDescriptor("toolB", "Tool B", List.of(specB), executorB);

        delegate = new StubToolProvider(List.of(descriptorA, descriptorB));
    }

    @Test
    void noToolsEnabledByDefault() {
        var provider = new FilterableToolProvider(delegate, eventBus);

        assertThat(provider.getAllToolDescriptors()).containsExactly(descriptorA, descriptorB);
        assertThat(provider.getToolDescriptors()).isEmpty();
        assertThat(provider.getToolSpecifications()).isEmpty();
        assertThat(provider.getToolExecutors()).isEmpty();
    }

    @Test
    void filtersDescriptorsAfterSelectionEvent() {
        var provider = new FilterableToolProvider(delegate, eventBus);

        eventBus.publishSync(new ToolsSelectionChangedEvent("test", Set.of("toolA")));

        assertThat(provider.getToolDescriptors()).containsExactly(descriptorA);
        assertThat(provider.getToolSpecifications()).hasSize(1);
        assertThat(provider.getToolExecutors()).containsExactly(descriptorA.executor());
    }

    @Test
    void returnsEmptyWhenAllToolsDisabled() {
        var provider = new FilterableToolProvider(delegate, eventBus);

        eventBus.publishSync(new ToolsSelectionChangedEvent("test", Set.of("toolA")));
        eventBus.publishSync(new ToolsSelectionChangedEvent("test", Set.of()));

        assertThat(provider.getToolDescriptors()).isEmpty();
        assertThat(provider.getToolSpecifications()).isEmpty();
        assertThat(provider.getToolExecutors()).isEmpty();
    }

    @Test
    void reactsToMultipleSelectionEvents() {
        var provider = new FilterableToolProvider(delegate, eventBus);

        eventBus.publishSync(new ToolsSelectionChangedEvent("test", Set.of("toolB")));
        assertThat(provider.getToolDescriptors()).containsExactly(descriptorB);

        eventBus.publishSync(new ToolsSelectionChangedEvent("test", Set.of("toolA", "toolB")));
        assertThat(provider.getToolDescriptors()).containsExactly(descriptorA, descriptorB);
    }

    private static class StubToolProvider implements IToolProvider {
        private final List<ToolDescriptor> descriptors;

        StubToolProvider(List<ToolDescriptor> descriptors) {
            this.descriptors = descriptors;
        }

        @Override
        public List<ToolDescriptor> getToolDescriptors() {
            return descriptors;
        }
    }
}
