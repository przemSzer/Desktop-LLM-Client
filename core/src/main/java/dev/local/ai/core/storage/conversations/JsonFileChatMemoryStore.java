package dev.local.ai.core.storage.conversations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

public class JsonFileChatMemoryStore implements ChatMemoryStore {

    private static final Logger logger = LoggerFactory.getLogger(JsonFileChatMemoryStore.class);
    private static final String MESSAGES_FILE = "messages.json";
    private static final String METADATA_FILE = "metadata.json";
    private static final int MAX_TITLE_LENGTH = 60;

    private final Path chatsDirectory;
    private final ObjectMapper objectMapper;
    private ConversationStore conversationStore;

    public JsonFileChatMemoryStore(Path chatsDirectory) {
        this.chatsDirectory = chatsDirectory;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        createDirectoryIfNotExists(chatsDirectory);
    }

    public void setConversationStore(ConversationStore conversationStore) {
        this.conversationStore = conversationStore;
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        Path messagesFile = resolveMessagesFile(memoryId);
        if (!Files.exists(messagesFile)) {
            logger.debug("No messages file for conversation {}", memoryId);
            return List.of();
        }
        try {
            String json = Files.readString(messagesFile);
            List<ChatMessage> messages = ChatMessageDeserializer.messagesFromJson(json);
            logger.debug("Loaded {} messages for conversation {}", messages.size(), memoryId);
            return messages;
        } catch (IOException e) {
            logger.error("Failed to load messages for conversation {}: {}", memoryId, e.getMessage());
            return List.of();
        }
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        Path conversationDir = chatsDirectory.resolve(memoryId.toString());
        createDirectoryIfNotExists(conversationDir);

        Path messagesFile = conversationDir.resolve(MESSAGES_FILE);
        try {
            String json = ChatMessageSerializer.messagesToJson(messages);
            Files.writeString(messagesFile, json);
            logger.debug("Saved {} messages for conversation {}", messages.size(), memoryId);
        } catch (IOException e) {
            logger.error("Failed to save messages for conversation {}: {}", memoryId, e.getMessage());
            return;
        }

        updateMetadata(memoryId.toString(), messages, conversationDir);
    }

    @Override
    public void deleteMessages(Object memoryId) {
        Path conversationDir = chatsDirectory.resolve(memoryId.toString());
        if (!Files.exists(conversationDir)) {
            return;
        }
        try {
            try (var paths = Files.walk(conversationDir)) {
                paths.sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                logger.error("Failed to delete {}: {}", path, e.getMessage());
                            }
                        });
            }
            logger.info("Deleted conversation directory {}", conversationDir);
        } catch (IOException e) {
            logger.error("Failed to delete conversation {}: {}", memoryId, e.getMessage());
        }
    }

    private void updateMetadata(String conversationId, List<ChatMessage> messages, Path conversationDir) {
        Path metadataFile = conversationDir.resolve(METADATA_FILE);
        try {
            ConversationMetadata existing = loadMetadata(metadataFile);
            Instant now = Instant.now();

            ConversationMetadata updated;
            if (existing == null) {
                String title = generateTitle(messages);
                updated = new ConversationMetadata(conversationId, title, now, now, null);
            } else {
                updated = existing.withUpdatedAt(now);
                if (existing.title() == null || existing.title().isBlank()) {
                    updated = updated.withTitle(generateTitle(messages));
                }
            }

            objectMapper.writerWithDefaultPrettyPrinter().writeValue(metadataFile.toFile(), updated);

            if (conversationStore != null) {
                conversationStore.refreshSummary(conversationId, updated.toSummary());
            }
        } catch (IOException e) {
            logger.error("Failed to update metadata for conversation {}: {}", conversationId, e.getMessage());
        }
    }

    private ConversationMetadata loadMetadata(Path metadataFile) {
        if (!Files.exists(metadataFile)) {
            return null;
        }
        try {
            return objectMapper.readValue(metadataFile.toFile(), ConversationMetadata.class);
        } catch (IOException e) {
            logger.error("Failed to read metadata from {}: {}", metadataFile, e.getMessage());
            return null;
        }
    }

    private String generateTitle(List<ChatMessage> messages) {
        return messages.stream()
                .filter(UserMessage.class::isInstance)
                .map(UserMessage.class::cast)
                .map(msg -> msg.singleText())
                .findFirst()
                .map(text -> text.length() > MAX_TITLE_LENGTH
                        ? text.substring(0, MAX_TITLE_LENGTH) + "..."
                        : text)
                .orElse("New conversation");
    }

    private Path resolveMessagesFile(Object memoryId) {
        return chatsDirectory.resolve(memoryId.toString()).resolve(MESSAGES_FILE);
    }

    private void createDirectoryIfNotExists(Path directory) {
        try {
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
            }
        } catch (IOException e) {
            logger.error("Failed to create directory {}: {}", directory, e.getMessage());
        }
    }
}
