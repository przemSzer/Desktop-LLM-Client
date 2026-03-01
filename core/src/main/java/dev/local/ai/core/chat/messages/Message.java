package dev.local.ai.core.chat.messages;

import dev.local.ai.core.documents.DocumentDescription;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record Message(String text, List<DocumentDescription> files, MessageType type) {
    public Message(String text) {
        this(text, Collections.emptyList(), MessageType.USER);
    }

    public Message(String text, List<DocumentDescription> files) {
        this(text, files, MessageType.USER);
    }

    public static Message toolCall(String toolName, Map<String, String> arguments) {
        var argumentsString = arguments.entrySet().stream()
            .map(entry -> entry.getKey() + ": " + entry.getValue())
            .collect(Collectors.joining(", "));
        return new Message(toolName + " (" + argumentsString + ")", List.of(), MessageType.TOOL_CALL);
    }

    public static Message ai(String text, List<DocumentDescription> files) {
        return new Message(text, files, MessageType.AI);
    }

    public static Message toolResult(String text, List<DocumentDescription> files) {
        return new Message(text, files, MessageType.TOOL_RESULT);
    }

    @Override
    public final String toString() {
        StringBuffer buffer = new StringBuffer();
        if (text != null && !text.isEmpty()) {
            if (text.length() > 100) {
                buffer.append("text: ");
                buffer.append(text.substring(0, 100) + "...");
                buffer.append("...");
            }else{
                buffer.append("text: ");
                buffer.append(text);
            }
        }
        if (files.isEmpty()) {
            buffer.append("[NO FILES]");
        }else{
            buffer.append("file count: ");
            buffer.append(files.size());
            files.stream()
                .map(DocumentDescription::title)
                .map(t -> "[" + t + "],").forEach(buffer::append);            
        }
        
        return buffer.toString();
    }
}
