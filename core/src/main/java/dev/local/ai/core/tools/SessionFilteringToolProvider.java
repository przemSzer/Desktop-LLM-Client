package dev.local.ai.core.tools;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Filters tools for one chat session. Enablement is owned by the session,
 * not by a global event.
 */
public class SessionFilteringToolProvider implements IToolProvider {

    private final IToolProvider catalog;
    private final Supplier<Set<String>> enabledToolIds;
    private final Supplier<List<ToolDescriptor>> sessionMcpTools;

    public SessionFilteringToolProvider(IToolProvider catalog,
                                        Supplier<Set<String>> enabledToolIds,
                                        Supplier<List<ToolDescriptor>> sessionMcpTools) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.enabledToolIds = Objects.requireNonNull(enabledToolIds, "enabledToolIds");
        this.sessionMcpTools = Objects.requireNonNull(sessionMcpTools, "sessionMcpTools");
    }

    @Override
    public List<ToolDescriptor> getAllToolDescriptors() {
        List<ToolDescriptor> all = new java.util.ArrayList<>(localTools());
        all.addAll(sessionMcpTools.get());
        return List.copyOf(all);
    }

    @Override
    public List<ToolDescriptor> getToolDescriptors() {
        Set<String> enabled = enabledToolIds.get();
        return getAllToolDescriptors().stream()
                .filter(descriptor -> enabled.contains(descriptor.id()))
                .toList();
    }

    private List<ToolDescriptor> localTools() {
        if (catalog instanceof AppToolsCatalog appCatalog) {
            return appCatalog.localTools();
        }
        return catalog.getAllToolDescriptors().stream()
                .filter(d -> ToolDescriptor.LOCAL_GROUP.equals(d.group()))
                .toList();
    }
}
