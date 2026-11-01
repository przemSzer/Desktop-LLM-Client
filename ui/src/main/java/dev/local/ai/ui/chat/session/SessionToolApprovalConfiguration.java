package dev.local.ai.ui.chat.session;

import dev.local.ai.core.tools.gates.AlwaysApproveGate;
import dev.local.ai.core.tools.gates.MultipleToolExecutionGate;
import dev.local.ai.core.tools.settings.ToolSettings;
import dev.local.ai.core.tools.settings.ToolSettingsWriter;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

public final class SessionToolApprovalConfiguration {

    private final MultipleToolExecutionGate gate;
    private final ToolSettingsWriter writer;
    private final Path settingsFile;
    private final Set<String> alwaysAllowedTools;

    public SessionToolApprovalConfiguration(MultipleToolExecutionGate gate,
                                            ToolSettingsWriter writer,
                                            Path settingsFile,
                                            Set<String> alwaysAllowedTools) {
        this.gate = gate;
        this.writer = writer;
        this.settingsFile = settingsFile;
        this.alwaysAllowedTools = new LinkedHashSet<>(alwaysAllowedTools);
        for (String toolId : this.alwaysAllowedTools) {
            gate.addGate(toolId, AlwaysApproveGate.INSTANCE);
        }
    }

    public Set<String> alwaysAllowedTools() {
        return Set.copyOf(alwaysAllowedTools);
    }

    public void setAlwaysAllowed(String toolId, boolean alwaysAllowed) {
        if (toolId == null || toolId.isBlank()) {
            return;
        }
        boolean changed;
        if (alwaysAllowed) {
            changed = alwaysAllowedTools.add(toolId);
            if (changed) {
                gate.addGate(toolId, AlwaysApproveGate.INSTANCE);
            }
        } else {
            changed = alwaysAllowedTools.remove(toolId);
            if (changed) {
                gate.removeGate(toolId);
            }
        }
        if (changed) {
            writer.write(settingsFile, new ToolSettings(alwaysAllowedTools.stream().toList()));
        }
    }
}
