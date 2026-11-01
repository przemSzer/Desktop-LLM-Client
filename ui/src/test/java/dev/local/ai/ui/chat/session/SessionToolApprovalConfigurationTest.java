package dev.local.ai.ui.chat.session;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.local.ai.core.tools.IToolExecutionGate;
import dev.local.ai.core.tools.gates.MultipleToolExecutionGate;
import dev.local.ai.core.tools.settings.ToolSettings;
import dev.local.ai.core.tools.settings.ToolSettingsReader;
import dev.local.ai.core.tools.settings.ToolSettingsWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class SessionToolApprovalConfigurationTest {

    @TempDir
    Path tempDir;

    @Mock
    private IToolExecutionGate defaultGate;

    private MultipleToolExecutionGate gate;
    private ToolSettingsWriter writer;
    private ToolSettingsReader reader;
    private Path settingsFile;

    @BeforeEach
    void setUp() {
        gate = new MultipleToolExecutionGate(defaultGate);
        writer = new ToolSettingsWriter();
        reader = new ToolSettingsReader();
        settingsFile = tempDir.resolve("tool-settings.json");
    }

    @Test
    void should_register_always_allowed_tools_from_initial_set() {
        var request = request("downloadWebPage");
        var approval = new SessionToolApprovalConfiguration(gate, writer, settingsFile, Set.of("downloadWebPage"));

        var result = gate.beforeToolExecution(request);

        assertThat(result.result())
                .isEqualTo(IToolExecutionGate.GateResult.PASSED);
        assertThat(approval.alwaysAllowedTools())
                .containsExactly("downloadWebPage");
        then(defaultGate)
                .should(never())
                .beforeToolExecution(any());
    }

    @Test
    void should_add_gate_and_persist_when_tool_is_always_allowed() {
        var approval = new SessionToolApprovalConfiguration(gate, writer, settingsFile, Set.of());

        approval.setAlwaysAllowed("downloadWebPage", true);

        assertThat(approval.alwaysAllowedTools())
                .containsExactly("downloadWebPage");
        assertThat(reader.read(settingsFile).alwaysAllowedTools())
                .containsExactly("downloadWebPage");
        assertThat(gate.beforeToolExecution(request("downloadWebPage")).result())
                .isEqualTo(IToolExecutionGate.GateResult.PASSED);
        then(defaultGate)
                .should(never())
                .beforeToolExecution(any());
    }

    @Test
    void should_remove_gate_and_persist_when_always_allow_is_turned_off() {
        var expected = IToolExecutionGate.GateCheckResult.passed();
        given(defaultGate.beforeToolExecution(any()))
                .willReturn(expected);
        var approval = new SessionToolApprovalConfiguration(gate, writer, settingsFile, Set.of("downloadWebPage"));

        approval.setAlwaysAllowed("downloadWebPage", false);

        assertThat(approval.alwaysAllowedTools())
                .isEmpty();
        assertThat(reader.read(settingsFile).alwaysAllowedTools())
                .isEmpty();
        assertThat(gate.beforeToolExecution(request("downloadWebPage")))
                .isEqualTo(expected);
        then(defaultGate)
                .should()
                .beforeToolExecution(any());
    }

    @Test
    void should_not_rewrite_settings_when_value_is_unchanged() {
        var approval = new SessionToolApprovalConfiguration(gate, writer, settingsFile, Set.of("downloadWebPage"));
        writer.write(settingsFile, new ToolSettings(java.util.List.of("marker")));

        approval.setAlwaysAllowed("downloadWebPage", true);

        assertThat(reader.read(settingsFile).alwaysAllowedTools())
                .containsExactly("marker");
    }

    private static ToolExecutionRequest request(String toolName) {
        return ToolExecutionRequest.builder()
                .id("tool-1")
                .name(toolName)
                .arguments("{}")
                .build();
    }
}
