package dev.local.ai.core.tools;

import java.util.List;

import dev.local.ai.core.tools.local.CommandLineTools;
import dev.local.ai.core.tools.web.WebPageDownloaderTools;

public class ToolsProvider implements IToolProvider {

    private final List<ToolDescriptor> descriptors;

    public ToolsProvider() {
        this.descriptors = List.of(
            WebPageDownloaderTools.getInstance().toDescriptor(),
            CommandLineTools.getInstance().toDescriptor()
        );
    }

    private static class InternalInstanceHolder {
        private static final ToolsProvider INSTANCE = new ToolsProvider();
    }

    public static IToolProvider getInstance() {
        return InternalInstanceHolder.INSTANCE;
    }

    @Override
    public List<ToolDescriptor> getToolDescriptors() {
        return descriptors;
    }
}
