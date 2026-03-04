package dev.local.ai.core.chat.streaming;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.local.ai.core.chat.messages.Message;
import dev.local.ai.core.documents.DocumentDescription;

public class MessageToChatMessageConverter {

    public Optional<ChatMessage> convert(Message message) {
        switch (message.type()) {
            case USER:
                return Optional.ofNullable(toUserMessage(message));
            case SYSTEM:
                return Optional.ofNullable(toSystemMessage(message));
            default:
                return Optional.empty();
        }
        
    }

    private SystemMessage toSystemMessage(Message message) {        
        if (message.files().isEmpty()){
            if (message.text().isEmpty()){
                return null;
            }
            return new SystemMessage(message.text());
        }else{
            var buffer = getTextFromMessageAndFiles(message);
            if (buffer.toString().isEmpty()){
                return null;
            }
            return new SystemMessage(buffer.toString());
        }
    }

    private UserMessage toUserMessage(Message message) {
        if (message.files().isEmpty()){
            return new UserMessage(message.text());
        }else{            
            List<Content> contents = new ArrayList<>(message.files().size() + 1);
            contents.add(new TextContent(message.text()));
            for (var file : message.files()) {
                if (file.type().getType().toString().equals("image")) {
                    contents.add(new ImageContent(file.text(), file.type().getType()));
                }
                else {
                    contents.add(new TextContent(createMessageWithFile(file)));
                }
            }
            return new UserMessage(contents);
        }
    }

    private StringBuilder getTextFromMessageAndFiles(Message message) {
        var buffer = new StringBuilder(message.text());
        for (var file : message.files()) {
            var fileContent = createMessageWithFile(file);
            buffer.append(fileContent);
        }
        return buffer;
    }

    private String createMessageWithFile(DocumentDescription file) {            
        var buffer = new StringBuilder();
        buffer.append("\n");
        buffer.append("<file name=\"").append(file.title()).append("\" type=\"").append(file.type().toString()).append("\">\n");
        buffer.append(file.text()).append("\n");
        buffer.append("</file>");                
        buffer.append("\n");
        return buffer.toString();
    }


}
