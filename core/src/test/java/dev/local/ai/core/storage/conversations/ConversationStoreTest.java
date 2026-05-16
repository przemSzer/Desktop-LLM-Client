package dev.local.ai.core.storage.conversations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConversationStoreTest {

    @TempDir
    private Path tempDir;

    private ConversationStore store;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        store = new ConversationStore(tempDir);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void shouldReturnEmptyListWhenNoConversationsExist() {
        assertThat(store.listConversations()).isEmpty();
    }

    @Test
    void shouldCreateNewConversation() {
        String id = store.createConversation();

        assertThat(id).isNotNull().isNotBlank();
        assertThat(Files.exists(tempDir.resolve(id))).isTrue();
        assertThat(Files.exists(tempDir.resolve(id).resolve("metadata.json"))).isTrue();
    }

    @Test
    void shouldListCreatedConversations() {
        store.createConversation();
        store.createConversation();

        List<ConversationSummary> conversations = store.listConversations();
        assertThat(conversations).hasSize(2);
    }

    @Test
    void shouldReturnLastConversation() throws Exception {
        String first = store.createConversation();
        Thread.sleep(50);
        String second = store.createConversation();

        var last = store.getLastConversation();
        assertThat(last).isPresent();
        assertThat(last.get().id()).isEqualTo(second);
    }

    @Test
    void shouldReturnEmptyWhenNoLastConversation() {
        assertThat(store.getLastConversation()).isEmpty();
    }

    @Test
    void shouldDeleteConversation() {
        String id = store.createConversation();
        assertThat(store.listConversations()).hasSize(1);

        store.deleteConversation(id);

        assertThat(store.listConversations()).isEmpty();
        assertThat(Files.exists(tempDir.resolve(id))).isFalse();
    }

    @Test
    void shouldLoadExistingConversationsOnConstruction() throws IOException {
        String id = "pre-existing";
        Path convDir = tempDir.resolve(id);
        Files.createDirectories(convDir);

        Instant now = Instant.now();
        ConversationMetadata metadata = new ConversationMetadata(id, "Old chat", now, now, null);
        objectMapper.writeValue(convDir.resolve("metadata.json").toFile(), metadata);

        ConversationStore freshStore = new ConversationStore(tempDir);

        List<ConversationSummary> conversations = freshStore.listConversations();
        assertThat(conversations).hasSize(1);
        assertThat(conversations.getFirst().id()).isEqualTo(id);
        assertThat(conversations.getFirst().title()).isEqualTo("Old chat");
    }

    @Test
    void shouldRefreshSummary() {
        String id = store.createConversation();

        Instant now = Instant.now();
        ConversationSummary updated = new ConversationSummary(id, "Updated title", now, now);
        store.refreshSummary(id, updated);

        var summaries = store.listConversations();
        assertThat(summaries).hasSize(1);
        assertThat(summaries.getFirst().title()).isEqualTo("Updated title");
    }

    @Test
    void shouldRefreshCache() throws IOException {
        store.createConversation();
        assertThat(store.listConversations()).hasSize(1);

        String externalId = "external";
        Path extDir = tempDir.resolve(externalId);
        Files.createDirectories(extDir);
        Instant now = Instant.now();
        ConversationMetadata metadata = new ConversationMetadata(externalId, "External", now, now, null);
        objectMapper.writeValue(extDir.resolve("metadata.json").toFile(), metadata);

        store.refreshCache();

        assertThat(store.listConversations()).hasSize(2);
    }

    @Test
    void shouldSortConversationsByUpdatedAtDescending() throws Exception {
        String older = store.createConversation();
        Thread.sleep(50);
        String newer = store.createConversation();

        List<ConversationSummary> conversations = store.listConversations();
        assertThat(conversations.getFirst().id()).isEqualTo(newer);
        assertThat(conversations.get(1).id()).isEqualTo(older);
    }
}
