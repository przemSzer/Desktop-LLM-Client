package dev.local.ai.core.tools;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.local.ai.core.events.CoreEventBus;
import dev.local.ai.core.events.EventListener;

/**
 * Decorator around IToolProvider that filters tools based on
 * ToolsSelectionChangedEvent from the event bus.
 *
 * Tools are disabled by default. When the UI toggles tools,
 * this provider dynamically filters getToolDescriptors(),
 * which in turn filters getToolSpecifications() and getToolExecutors()
 * via the default methods in IToolProvider.
 */
public class FilterableToolProvider implements IToolProvider, EventListener<ToolsSelectionChangedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(FilterableToolProvider.class);

    private final IToolProvider delegate;
    private volatile Set<String> enabledToolIds;

    public FilterableToolProvider(IToolProvider delegate, CoreEventBus eventBus) {
        this.delegate = delegate;
        this.enabledToolIds = Set.of();

        eventBus.subscribe(ToolsSelectionChangedEvent.EVENT_TYPE, this);

        logger.info("FilterableToolProvider initialized with tools disabled");
    }

    @Override
    public void onEvent(ToolsSelectionChangedEvent event) {
        this.enabledToolIds = event.getEnabledToolIds();
        logger.info("Tools selection updated. Enabled tools: {}", enabledToolIds);
    }

    @Override
    public List<ToolDescriptor> getAllToolDescriptors() {
        return delegate.getAllToolDescriptors();
    }

    @Override
    public List<ToolDescriptor> getToolDescriptors() {
        return delegate.getToolDescriptors().stream()
            .filter(descriptor -> enabledToolIds.contains(descriptor.id()))
            .toList();
    }
}
