package dev.local.ai.ui.chat.session;

import dev.local.ai.core.tools.AppToolsCatalog;
import dev.local.ai.core.tools.gates.MultipleToolExecutionGate;
import dev.local.ai.core.tools.gates.WaitForApprovalGate;
import dev.local.ai.core.tools.mcp.McpConnectionRegistry;
import dev.local.ai.core.tools.settings.ToolSettingsWriter;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class SessionTestSupport {

    private SessionTestSupport() {}

    public static SessionToolApprovalConfiguration emptyToolApproval() {
        return new SessionToolApprovalConfiguration(
                new MultipleToolExecutionGate(new WaitForApprovalGate()),
                new ToolSettingsWriter(),
                Path.of("unused-tool-settings.json"),
                Set.of()
        );
    }

    public static SessionToolSelection emptyToolSelection(SessionToolApprovalConfiguration approval) {
        var registry = new McpConnectionRegistry(
                List.of(),
                config -> {
                    throw new UnsupportedOperationException("no MCP in unit test");
                }
        );
        var catalog = new AppToolsCatalog(registry);
        return new SessionToolSelection(
                catalog,
                new ToolSettingsWriter(),
                Path.of("unused-tool-settings.json"),
                approval,
                new LinkedHashSet<>()
        );
    }
}
