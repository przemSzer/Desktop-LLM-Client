package dev.local.ai.core.storage.conversations;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JsonFileChatMemoryStoreTest {

    @TempDir
    private Path tempDir;

    private JsonFileChatMemoryStore store;

    @BeforeEach
    void setUp() {
        store = new JsonFileChatMemoryStore(tempDir);
    }

    @Test
    void shouldReturnEmptyListWhenNoMessagesExist() {
        List<ChatMessage> messages = store.getMessages("nonexistent");

        assertThat(messages).isEmpty();
    }

    @Test
    void shouldSaveAndLoadMessages() {
        String conversationId = "test-conv-1";
        List<ChatMessage> messages = List.of(
                UserMessage.from("Hello"),
                AiMessage.from("Hi there!")
        );

        store.updateMessages(conversationId, messages);

        List<ChatMessage> loaded = store.getMessages(conversationId);
        assertThat(loaded).hasSize(2);
        assertThat(((UserMessage) loaded.get(0)).singleText()).isEqualTo("Hello");
        assertThat(((AiMessage) loaded.get(1)).text()).isEqualTo("Hi there!");
    }

    @Test
    void shouldCreateConversationDirectory() {
        String conversationId = "test-conv-2";
        store.updateMessages(conversationId, List.of(UserMessage.from("test")));

        assertThat(Files.exists(tempDir.resolve(conversationId))).isTrue();
        assertThat(Files.exists(tempDir.resolve(conversationId).resolve("messages.json"))).isTrue();
    }

    @Test
    void shouldCreateMetadataOnFirstWrite() {
        String conversationId = "test-conv-3";
        store.updateMessages(conversationId, List.of(UserMessage.from("How do I use Docker?")));

        Path metadataFile = tempDir.resolve(conversationId).resolve("metadata.json");
        assertThat(Files.exists(metadataFile)).isTrue();
    }

    @Test
    void shouldGenerateTitleFromFirstUserMessage() {
        String conversationId = "test-conv-4";
        ConversationStore conversationStore = new ConversationStore(tempDir);
        store.setConversationStore(conversationStore);

        store.updateMessages(conversationId, List.of(
                UserMessage.from("How do I configure Postgres in Docker?"),
                AiMessage.from("You can use the official image...")
        ));

        List<ConversationSummary> summaries = conversationStore.listConversations();
        assertThat(summaries).hasSize(1);
        assertThat(summaries.getFirst().title()).isEqualTo("How do I configure Postgres in Docker?");
    }

    @Test
    void shouldTruncateLongTitles() {
        String conversationId = "test-conv-5";
        String longMessage = "A".repeat(100);

        store.updateMessages(conversationId, List.of(UserMessage.from(longMessage)));

        JsonFileChatMemoryStore freshStore = new JsonFileChatMemoryStore(tempDir);
        ConversationStore conversationStore = new ConversationStore(tempDir);

        List<ConversationSummary> summaries = conversationStore.listConversations();
        ConversationSummary summary = summaries.stream()
                .filter(s -> s.id().equals(conversationId))
                .findFirst().orElseThrow();
        assertThat(summary.title()).hasSize(63); // 60 chars + "..."
        assertThat(summary.title()).endsWith("...");
    }

    @Test
    void shouldDeleteConversationDirectory() {
        String conversationId = "test-conv-6";
        store.updateMessages(conversationId, List.of(UserMessage.from("test")));

        assertThat(Files.exists(tempDir.resolve(conversationId))).isTrue();

        store.deleteMessages(conversationId);

        assertThat(Files.exists(tempDir.resolve(conversationId))).isFalse();
    }

    @Test
    void shouldOverwriteMessagesOnUpdate() {
        String conversationId = "test-conv-7";
        store.updateMessages(conversationId, List.of(UserMessage.from("first")));
        store.updateMessages(conversationId, List.of(
                UserMessage.from("first"),
                AiMessage.from("response"),
                UserMessage.from("second")
        ));

        List<ChatMessage> loaded = store.getMessages(conversationId);
        assertThat(loaded).hasSize(3);
    }

    @Test
    void shouldUpdateMetadataTimestampOnSubsequentWrites() throws Exception {
        String conversationId = "test-conv-8";
        store.updateMessages(conversationId, List.of(UserMessage.from("first")));

        Thread.sleep(50);

        store.updateMessages(conversationId, List.of(
                UserMessage.from("first"),
                AiMessage.from("response")
        ));

        ConversationStore conversationStore = new ConversationStore(tempDir);
        List<ConversationSummary> summaries = conversationStore.listConversations();
        ConversationSummary summary = summaries.stream()
                .filter(s -> s.id().equals(conversationId))
                .findFirst().orElseThrow();

        assertThat(summary.updatedAt()).isAfter(summary.createdAt());
    }
}
