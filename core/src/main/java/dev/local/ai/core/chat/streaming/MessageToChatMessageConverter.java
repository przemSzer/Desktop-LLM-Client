package dev.local.ai.core.chat.streaming;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import dev.langchain4j.data.image.Image;
import dev.langchain4j.data.message.*;
import dev.langchain4j.data.message.ImageContent.DetailLevel;
import dev.local.ai.core.chat.messages.Message;
import dev.local.ai.core.chat.messages.MessageType;
import dev.local.ai.core.documents.DocumentDescription;
import org.apache.tika.mime.MediaType;

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
            if (buffer.isEmpty()){
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
            StringBuilder textContent = new StringBuilder();
            textContent.append(message.text());
            for (var file : message.files()) {
                if (file.type().getType().equals("image")) {
                    contents.add(new ImageContent(file.text(), file.type().toString(), DetailLevel.HIGH));
                }
                else {
                    textContent.append(createTextContentFromFile(file));                  
                }
            }
            contents.add(new TextContent(textContent.toString()));
            return new UserMessage(contents);
        }
    }

    private StringBuilder getTextFromMessageAndFiles(Message message) {
        var buffer = new StringBuilder(message.text());
        for (var file : message.files()) {
            var fileContent = createTextContentFromFile(file);
            buffer.append(fileContent);
        }
        return buffer;
    }

    private String createTextContentFromFile(DocumentDescription file) {            
        var buffer = new StringBuilder();
        buffer.append("\n");
        buffer.append("<file name=\"").append(file.title()).append("\" type=\"").append(file.type().toString()).append("\">\n");
        buffer.append(file.text()).append("\n");
        buffer.append("</file>");                
        buffer.append("\n");
        return buffer.toString();
    }

    public static Message toCoreMessage(ChatMessage chatMessage) {
        if (chatMessage instanceof UserMessage userMsg) {
            if (userMsg.hasSingleText()) {
                return new Message(userMsg.singleText(), List.of(), MessageType.USER);
            }else{
                StringBuilder builder = new StringBuilder();
                userMsg.contents()
                        .stream()
                        .filter(c -> c.type() == ContentType.TEXT)
                        .forEach(builder::append);
                //TODO: restore images and other file types
                var images = userMsg.contents()
                        .stream()
                        .filter(c -> c.type() == ContentType.IMAGE)
                        .map(c->(ImageContent)c)
                        .map(ic -> ic.image())
                        .map(MessageToChatMessageConverter::imageToDocument)
                        .filter(Objects::nonNull)
                        .toList();
                return new Message(builder.toString(), images, MessageType.USER);
            }
        }
        if (chatMessage instanceof AiMessage aiMsg) {
            return Message.ai(aiMsg.text(), null);
        }
        if (chatMessage instanceof ToolExecutionResultMessage toolResult) {
            return Message.toolResult(toolResult.text(), List.of());
        }
        if (chatMessage instanceof SystemMessage) {
            return null;
        }
        return null;
    }

    private static DocumentDescription imageToDocument(Image image){
        var spitedType = image.mimeType().split("\\\\");
        if (spitedType.length == 0 || spitedType.length == 1){
            return null;
        }
        MediaType mediaType = new MediaType(spitedType[0], spitedType[1]);
        return new DocumentDescription(image.revisedPrompt(),mediaType,image.base64Data(), null);
    }
}
