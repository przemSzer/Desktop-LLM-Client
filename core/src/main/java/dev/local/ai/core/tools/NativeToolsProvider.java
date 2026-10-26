package dev.local.ai.core.tools;

import java.util.List;

import dev.local.ai.core.tools.local.CommandLineTools;
import dev.local.ai.core.tools.web.WebPageDownloaderTools;

public class NativeToolsProvider implements IToolProvider {

    private final List<ToolDescriptor> descriptors;

    public NativeToolsProvider() {
        this.descriptors = List.of(
            WebPageDownloaderTools.getInstance().toDescriptor(),
            CommandLineTools.getInstance().toDescriptor()
        );
    }

    @Override
    public List<ToolDescriptor> getToolDescriptors() {
        return descriptors;
    }
}
