package dev.local.ai.ui.connection;

import dev.local.ai.ui.utils.MainStageProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ManageConnectionsDialogTest {

    @Test
    void should_reject_null_controller_factory() {
        assertThatThrownBy(() -> new ManageConnectionsDialog(null, new MainStageProvider()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Controller factory cannot be null");
    }

    @Test
    void should_reject_null_main_stage_provider() {
        assertThatThrownBy(() -> new ManageConnectionsDialog(type -> null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Main stage provider cannot be null");
    }
}
