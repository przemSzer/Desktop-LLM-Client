package dev.local.ai.ui.chat.converters;

import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.NonNull;

import dev.local.ai.core.chat.messages.Message;
import dev.local.ai.ui.chat.viewmodel.ChatMessageViewModel;
import dev.local.ai.ui.chat.viewmodel.MessageTypeView;
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
            return toToolMessage(message, messageType, filesFromMessage);
        }
        return Optional.of(new ChatMessageViewModel(message.text(), messageType, filesFromMessage, message.statistics(), null));
    }

    private Optional<@NonNull ChatMessageViewModel> toToolMessage(Message message, MessageTypeView messageTypeView, List<AttachedFileViewModel> filesFromMessage) {
        var text = "";
        if (message.type() == dev.local.ai.core.chat.messages.MessageType.TOOL_CALL) {
            text = "Tool call: " + message.text();
        }else if (message.type() == dev.local.ai.core.chat.messages.MessageType.TOOL_RESULT) {
            text = "Tool result: " + message.text();
        }

        return Optional.of(new ChatMessageViewModel(text, MessageTypeView.TOOL_RESULT, filesFromMessage, message.statistics(), null));
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
