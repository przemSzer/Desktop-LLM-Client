package dev.local.ai.ui.tools;

import dev.local.ai.core.tools.AppToolsCatalog;
import dev.local.ai.core.tools.mcp.McpConnectionRegistry;
import dev.local.ai.core.tools.mcp.config.McpServerConfig;
import dev.local.ai.ui.chat.session.SessionTestSupport;
import dev.local.ai.ui.chat.session.SessionToolApprovalConfiguration;
import dev.local.ai.ui.chat.session.SessionToolSelection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class ToolsSelectorViewModelTest {

    @TempDir
    Path tempDir;

    private AppToolsCatalog catalog;
    private SessionToolSelection selection;
    private ToolsSelectorViewModel viewModel;

    @BeforeEach
    void setUp() {
        catalog = new AppToolsCatalog(new McpConnectionRegistry(
                List.of(),
                config -> {
                    throw new UnsupportedOperationException("no MCP");
                }
        ));
        SessionToolApprovalConfiguration approval = SessionTestSupport.emptyToolApproval();
        selection = new SessionToolSelection(
                catalog,
                new dev.local.ai.core.tools.settings.ToolSettingsWriter(),
                tempDir.resolve("tool-settings.json"),
                approval,
                new LinkedHashSet<>()
        );
        viewModel = new ToolsSelectorViewModel(catalog);
        viewModel.bindSession(selection);
    }

    @Test
    void should_load_local_group_with_local_tools() {
        assertThat(viewModel.getGroups())
                .isNotEmpty();
        ToolGroupViewModel local = viewModel.getGroups().getFirst();
        assertThat(local.getDisplayName())
                .isEqualTo("Local");
        assertThat(local.getTools())
                .extracting(ToolItemViewModel::getToolId)
                .contains("downloadWebPage", "executeLocalCommand");
    }

    @Test
    void should_enable_local_tool_on_session() {
        ToolItemViewModel tool = viewModel.getGroups().getFirst().getTools().getFirst();

        tool.setEnabled(true);

        assertThat(selection.enabledToolIds())
                .contains(tool.getToolId());
    }

    @Test
    void should_refresh_always_allowed_into_approval_icons() {
        ToolItemViewModel tool = viewModel.getGroups().getFirst().getTools().getFirst();

        viewModel.refreshAlwaysAllowed(java.util.Set.of(tool.getToolId()));

        assertThat(tool.isRequiresApproval())
                .isFalse();
    }

    @Test
    void should_notify_when_requires_approval_toggled() {
        AtomicBoolean called = new AtomicBoolean();
        viewModel.setOnAlwaysAllowChanged((id, always) -> called.set(true));
        ToolItemViewModel tool = viewModel.getGroups().getFirst().getTools().getFirst();

        tool.setRequiresApproval(false);

        assertThat(called.get())
                .isTrue();
    }

    @Test
    void should_add_mcp_server_group_from_catalog() {
        var server = new McpServerConfig(
                "tavily",
                "Tavily",
                true,
                "http://localhost:8088/mcp",
                Map.of(),
                null,
                null,
                Map.of(),
                null,
                30
        );
        var withMcp = new AppToolsCatalog(new McpConnectionRegistry(
                List.of(server),
                config -> {
                    throw new UnsupportedOperationException("no connect in this test");
                }
        ));
        var vm = new ToolsSelectorViewModel(withMcp);

        assertThat(vm.getGroups())
                .extracting(ToolGroupViewModel::getDisplayName)
                .contains("Local", "Tavily");
    }
}
