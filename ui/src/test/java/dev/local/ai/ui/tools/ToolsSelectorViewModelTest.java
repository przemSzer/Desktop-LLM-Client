package dev.local.ai.ui.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.local.ai.core.events.CoreEventBus;
import dev.local.ai.core.events.Event;
import dev.local.ai.core.events.EventListener;
import dev.local.ai.core.tools.FilterableToolProvider;
import dev.local.ai.core.tools.IToolProvider;
import dev.local.ai.core.tools.ToolDescriptor;
import dev.local.ai.core.tools.ToolsSelectionChangedEvent;

import static org.junit.jupiter.api.Assertions.*;

class ToolsSelectorViewModelTest {

    private StubToolProvider toolProvider;
    private SyncEventBus eventBus;
    private EventCaptor eventCaptor;

    @BeforeEach
    void setUp() {
        var specDownload = ToolSpecification.builder()
            .name("downloadWebPage").description("Downloads a web page").build();
        var specCommand = ToolSpecification.builder()
            .name("executeLocalCommand").description("Executes a local command").build();

        var descriptors = List.of(
            new ToolDescriptor("downloadWebPage", "Download web page",
                List.of(specDownload), request -> Optional.empty()),
            new ToolDescriptor("executeLocalCommand", "Execute local commands",
                List.of(specCommand), request -> Optional.empty())
        );

        toolProvider = new StubToolProvider(descriptors);
        eventBus = new SyncEventBus();
        eventCaptor = new EventCaptor();
        eventBus.subscribe(ToolsSelectionChangedEvent.EVENT_TYPE, eventCaptor);
    }

    @Test
    void loadsToolsFromProvider() {
        var viewModel = new ToolsSelectorViewModel(toolProvider, eventBus);

        assertEquals(2, viewModel.getTools().size());
        assertEquals("downloadWebPage", viewModel.getTools().get(0).getToolId());
        assertEquals("Download web page", viewModel.getTools().get(0).getDisplayName());
        assertEquals("executeLocalCommand", viewModel.getTools().get(1).getToolId());
        assertEquals("Execute local commands", viewModel.getTools().get(1).getDisplayName());
    }

    @Test
    void listsToolsWhenFilterableProviderHasNoneEnabled() {
        var filterable = new FilterableToolProvider(toolProvider, eventBus);

        var viewModel = new ToolsSelectorViewModel(filterable, eventBus);

        assertEquals(2, viewModel.getTools().size());
        assertTrue(viewModel.getTools().stream().noneMatch(ToolItemViewModel::isEnabled));
    }

    @Test
    void allToolsDisabledByDefault() {
        var viewModel = new ToolsSelectorViewModel(toolProvider, eventBus);

        assertTrue(viewModel.getTools().stream().noneMatch(ToolItemViewModel::isEnabled));
    }

    @Test
    void publishesEventWhenToolIsEnabled() {
        var viewModel = new ToolsSelectorViewModel(toolProvider, eventBus);

        viewModel.getTools().get(0).setEnabled(true);

        assertEquals(1, eventCaptor.captured.size());
        assertEquals(Set.of("downloadWebPage"), eventCaptor.captured.get(0).getEnabledToolIds());
    }

    @Test
    void publishesEventWhenToolIsDisabled() {
        var viewModel = new ToolsSelectorViewModel(toolProvider, eventBus);

        viewModel.getTools().get(0).setEnabled(true);
        viewModel.getTools().get(0).setEnabled(false);

        assertEquals(2, eventCaptor.captured.size());
        assertTrue(eventCaptor.lastEvent().getEnabledToolIds().isEmpty());
    }

    @Test
    void publishesAllIdsWhenAllToolsEnabled() {
        var viewModel = new ToolsSelectorViewModel(toolProvider, eventBus);

        viewModel.getTools().get(0).setEnabled(true);
        viewModel.getTools().get(1).setEnabled(true);

        assertEquals(
            Set.of("downloadWebPage", "executeLocalCommand"),
            eventCaptor.lastEvent().getEnabledToolIds()
        );
    }

    @Test
    void usesDisplayNameFromDescriptor() {
        var spec = ToolSpecification.builder()
            .name("someNewTool").description("A new tool").build();
        var descriptor = new ToolDescriptor("someNewTool", "My Custom Tool Name",
            List.of(spec), request -> Optional.empty());
        var provider = new StubToolProvider(List.of(descriptor));

        var viewModel = new ToolsSelectorViewModel(provider, eventBus);

        assertEquals("My Custom Tool Name", viewModel.getTools().get(0).getDisplayName());
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

    private static class EventCaptor implements EventListener<ToolsSelectionChangedEvent> {
        final List<ToolsSelectionChangedEvent> captured = new ArrayList<>();

        @Override
        public void onEvent(ToolsSelectionChangedEvent event) {
            captured.add(event);
        }

        ToolsSelectionChangedEvent lastEvent() {
            return captured.get(captured.size() - 1);
        }
    }

    private static class SyncEventBus extends CoreEventBus {
        @Override
        public void publish(Event event) {
            publishSync(event);
        }
    }
}
