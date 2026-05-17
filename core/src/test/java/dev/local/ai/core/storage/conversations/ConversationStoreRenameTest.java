package dev.local.ai.core.storage.conversations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ConversationStoreRenameTest {

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
    void renameUpdatesMetadataFileCacheAndSummariesList() throws Exception {
        String older = store.createConversation();
        Thread.sleep(50);
        String newer = store.createConversation();

        store.rename(older, "Renamed title");

        ConversationMetadata fromDisk = objectMapper.readValue(
                tempDir.resolve(older).resolve("metadata.json").toFile(),
                ConversationMetadata.class);
        assertThat(fromDisk.title()).isEqualTo("Renamed title");

        assertThat(store.findSummary(older)).hasValueSatisfying(s -> assertThat(s.title()).isEqualTo("Renamed title"));

        assertThat(store.getConversationSummaries().getFirst().id()).isEqualTo(older);
        assertThat(store.getConversationSummaries()).extracting(ConversationSummary::id).containsExactlyInAnyOrder(older, newer);
    }

    @Test
    void blankRenameClearsTitle() throws Exception {
        String id = store.createConversation();
        store.rename(id, "Visible");

        store.rename(id, "   ");

        ConversationMetadata fromDisk = objectMapper.readValue(
                tempDir.resolve(id).resolve("metadata.json").toFile(),
                ConversationMetadata.class);
        assertThat(fromDisk.title()).isNull();
    }

    @Test
    void listenerIsNotifiedWhenRenameChangesOrder() throws Exception {
        String first = store.createConversation();
        Thread.sleep(50);
        store.createConversation();

        AtomicInteger notificationsAfterRegister = new AtomicInteger();
        store.addConversationSummariesListener(s -> notificationsAfterRegister.incrementAndGet());

        int afterRegister = notificationsAfterRegister.get();

        store.rename(first, "Lift me");

        assertThat(store.getConversationSummaries()).hasSize(2);
        assertThat(notificationsAfterRegister.get()).isGreaterThan(afterRegister);
        assertThat(store.getConversationSummaries().getFirst().id()).isEqualTo(first);
    }
}
