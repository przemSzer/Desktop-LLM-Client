package dev.local.ai.core.chat.messages;

import dev.local.ai.core.documents.DocumentDescription;
import java.util.Collections;
import java.util.List;

public record Message(String text, List<DocumentDescription> files) {
    public Message(String text) {
        this(text, Collections.emptyList());
    }

    public Message(String text, List<DocumentDescription> files) {
        this.text = text;
        this.files = files;
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
