package dev.local.ai.ui.chat.converters;

import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.NonNull;

import dev.local.ai.core.chat.messages.Message;
import dev.local.ai.ui.chat.viewmodel.ChatMessageViewModel;
import dev.local.ai.ui.chat.viewmodel.MessageTypeView;
import dev.local.ai.ui.chat.viewmodel.ToolCallChatMessageViewModel;
import dev.local.ai.ui.files.viewmodel.AttachedFileViewModel;
import dev.local.ai.ui.files.viewmodel.FileStatus;

public class MessageConverter {
    public Optional<@NonNull ChatMessageViewModel> convert(Message message) {
        final var messageType = getMessageType(message);
        if (messageType == null) {
            return Optional.empty();
        }
        final var filesFromMessage = extractFilesFromMessage(message);
        if (messageType == MessageTypeView.TOOL_RESULT || messageType == MessageTypeView.TOOL_CALL) {
            return toToolMessage(message, filesFromMessage);
        }
        return Optional.of(new ChatMessageViewModel(message.text(), messageType, filesFromMessage, message.statistics(), null));
    }

    private Optional<@NonNull ChatMessageViewModel> toToolMessage(Message message, List<AttachedFileViewModel> filesFromMessage) {
        if (message.type() == dev.local.ai.core.chat.messages.MessageType.TOOL_CALL) {
            return Optional.of(new ToolCallChatMessageViewModel(
                    "Tool call: " + message.text(),
                    MessageTypeView.TOOL_CALL,
                    filesFromMessage,
                    message.statistics(),
                    message.id()
            ));
        }
        return Optional.of(new ChatMessageViewModel(
                "Tool result: " + message.text(),
                MessageTypeView.TOOL_RESULT,
                filesFromMessage,
                message.statistics(),
                message.id()
        ));
    }

    private MessageTypeView getMessageType(Message message) {
        switch (message.type()) {
            case USER:
                return MessageTypeView.USER;
            case AI:
                return MessageTypeView.AI;
            case PARTIAL:
                return MessageTypeView.PARTIAL_AI;
            case TOOL_CALL:
                return MessageTypeView.TOOL_CALL;
            case TOOL_RESULT:
                return MessageTypeView.TOOL_RESULT;
            default:
                return null;
        }
    }

    private List<AttachedFileViewModel> extractFilesFromMessage(Message message) {
        return message.files()
            .stream()
            .map(fileDesc -> new AttachedFileViewModel(fileDesc, FileStatus.VALID))
            .toList();
    }
}
