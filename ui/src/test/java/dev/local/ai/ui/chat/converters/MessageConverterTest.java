package dev.local.ai.ui.chat.converters;

import dev.local.ai.core.chat.messages.Message;
import dev.local.ai.ui.chat.viewmodel.MessageTypeView;
import dev.local.ai.ui.chat.viewmodel.ToolCallChatMessageViewModel;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MessageConverterTest {

    private final MessageConverter converter = new MessageConverter();

    @Test
    void shouldConvertToolCallToToolCallViewModelWithRequestId() {
        var message = Message.toolCall("run_command", Map.of("cmd", "ls"), "tool-42");

        var converted = converter.convert(message);

        assertThat(converted).isPresent();
        assertThat(converted.get()).isInstanceOf(ToolCallChatMessageViewModel.class);
        assertThat(converted.get().getType()).isEqualTo(MessageTypeView.TOOL_CALL);
        assertThat(converted.get().getId()).isEqualTo("tool-42");
        assertThat(converted.get().getContent()).contains("run_command").contains("cmd");
        assertThat(((ToolCallChatMessageViewModel) converted.get()).isNeedsApproval()).isFalse();
    }

    @Test
    void shouldConvertToolResultToRegularToolResultMessage() {
        var message = Message.toolResult("done", java.util.List.of());

        var converted = converter.convert(message);

        assertThat(converted).isPresent();
        assertThat(converted.get()).isNotInstanceOf(ToolCallChatMessageViewModel.class);
        assertThat(converted.get().getType()).isEqualTo(MessageTypeView.TOOL_RESULT);
        assertThat(converted.get().getContent()).contains("Tool result:");
    }
}
