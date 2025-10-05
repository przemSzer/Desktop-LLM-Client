package dev.local.ai.ui.chat.converters;

import java.util.List;
import java.util.Optional;

import dev.local.ai.core.chat.messages.Message;
import dev.local.ai.ui.chat.viewmodel.ChatMessageViewModel;
import dev.local.ai.ui.chat.viewmodel.ChatMessageViewModel.MessageType;
import dev.local.ai.ui.files.viewmodel.AttachedFileViewModel;
import dev.local.ai.ui.files.viewmodel.FileStatus;

public class MessageConverter {
    public Optional<ChatMessageViewModel> convert(Message message) {
        final var messageType = getMessageType(message);
        if (messageType == null) {
            return Optional.empty();
        }
        final var filesFromMessage = extractFilesFromMessage(message);
        if (messageType == ChatMessageViewModel.MessageType.TOOL) {
            return toToolMessage(message, filesFromMessage);
        }
        return Optional.of(new ChatMessageViewModel(message.text(), messageType, filesFromMessage));
    }

    private Optional<ChatMessageViewModel> toToolMessage(Message message, List<AttachedFileViewModel> filesFromMessage) {
        var text = "";
        if (message.type() == dev.local.ai.core.chat.messages.MessageType.TOOL_CALL) {
            text = "Tool call: " + message.text();
        }else if (message.type() == dev.local.ai.core.chat.messages.MessageType.TOOL_RESULT) {
            text = "Tool result: " + message.text();
        }

        return Optional.of(new ChatMessageViewModel(text, ChatMessageViewModel.MessageType.TOOL, filesFromMessage));
    }

    private MessageType getMessageType(Message message) {
        switch (message.type()) {
            case USER:
                return ChatMessageViewModel.MessageType.USER;
            case AI:
                return ChatMessageViewModel.MessageType.AI;
            case PARTIAL:
                return ChatMessageViewModel.MessageType.PARTIAL;
            case TOOL_CALL:
                return ChatMessageViewModel.MessageType.TOOL;
            case TOOL_RESULT:
                return ChatMessageViewModel.MessageType.TOOL;
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
