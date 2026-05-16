package dev.local.ai.core.storage.conversations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class ConversationStore {

    private static final Logger logger = LoggerFactory.getLogger(ConversationStore.class);
    private static final String METADATA_FILE = "metadata.json";

    private final Path chatsDirectory;
    private final ObjectMapper objectMapper;
    private final Map<String, ConversationSummary> cache = new ConcurrentHashMap<>();

    public ConversationStore() {
        this(Paths.get(System.getProperty("user.home"), ".local-ai", "chats"));
    }

    public ConversationStore(Path chatsDirectory) {
        this.chatsDirectory = chatsDirectory;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        createDirectoryIfNotExists(chatsDirectory);
        loadAllSummaries();
    }

    public List<ConversationSummary> listConversations() {
        return cache.values().stream()
                .sorted(Comparator.comparing(ConversationSummary::updatedAt).reversed())
                .toList();
    }

    public Optional<ConversationSummary> getLastConversation() {
        return cache.values().stream()
                .max(Comparator.comparing(ConversationSummary::updatedAt));
    }

    public String createConversation() {
        String id = UUID.randomUUID().toString();
        Path conversationDir = chatsDirectory.resolve(id);
        createDirectoryIfNotExists(conversationDir);

        Instant now = Instant.now();
        ConversationMetadata metadata = new ConversationMetadata(id, null, now, now, null);

        Path metadataFile = conversationDir.resolve(METADATA_FILE);
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(metadataFile.toFile(), metadata);
            cache.put(id, metadata.toSummary());
            logger.info("Created new conversation: {}", id);
        } catch (IOException e) {
            logger.error("Failed to create conversation metadata for {}: {}", id, e.getMessage());
        }

        return id;
    }

    public void deleteConversation(String id) {
        Path conversationDir = chatsDirectory.resolve(id);
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
            cache.remove(id);
            logger.info("Deleted conversation: {}", id);
        } catch (IOException e) {
            logger.error("Failed to delete conversation {}: {}", id, e.getMessage());
        }
    }

    public void refreshSummary(String id, ConversationSummary summary) {
        cache.put(id, summary);
    }

    public void refreshCache() {
        cache.clear();
        loadAllSummaries();
    }

    private void loadAllSummaries() {
        if (!Files.exists(chatsDirectory)) {
            return;
        }
        try (Stream<Path> dirs = Files.list(chatsDirectory)) {
            dirs.filter(Files::isDirectory)
                    .forEach(this::loadSummaryFromDir);
        } catch (IOException e) {
            logger.error("Failed to scan chats directory {}: {}", chatsDirectory, e.getMessage());
        }
    }

    private void loadSummaryFromDir(Path conversationDir) {
        Path metadataFile = conversationDir.resolve(METADATA_FILE);
        if (!Files.exists(metadataFile)) {
            return;
        }
        try {
            ConversationMetadata metadata = objectMapper.readValue(metadataFile.toFile(), ConversationMetadata.class);
            cache.put(metadata.id(), metadata.toSummary());
        } catch (IOException e) {
            logger.error("Failed to read metadata from {}: {}", metadataFile, e.getMessage());
        }
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
