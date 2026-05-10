package dev.local.ai.core.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JsonSettingsStorageTest {

    @TempDir
    private Path tempDir;

    private JsonSettingsStorage storage;

    @BeforeEach
    void setUp() {
        storage = new JsonSettingsStorage(tempDir, "settings.json");
    }

    @Test
    void shouldSaveAndReadStringValue() {
        storage.save("theme", "dark");

        Optional<String> result = storage.read("theme", String.class);

        assertThat(result).hasValue("dark");
    }

    @Test
    void shouldReturnEmptyWhenKeyDoesNotExist() {
        Optional<String> result = storage.read("nonexistent", String.class);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldOverwriteExistingValue() {
        storage.save("theme", "light");
        storage.save("theme", "dark");

        Optional<String> result = storage.read("theme", String.class);

        assertThat(result).hasValue("dark");
    }

    @Test
    void shouldSaveAndReadComplexObject() {
        TestSettings settings = new TestSettings("dark", 14, true);

        storage.save("editor", settings);

        Optional<TestSettings> result = storage.read("editor", TestSettings.class);

        assertThat(result).isPresent();
        assertThat(result.get().theme()).isEqualTo("dark");
        assertThat(result.get().fontSize()).isEqualTo(14);
        assertThat(result.get().autoSave()).isTrue();
    }

    @Test
    void shouldStoreMultipleKeysIndependently() {
        storage.save("key1", "value1");
        storage.save("key2", "value2");

        assertThat(storage.read("key1", String.class)).hasValue("value1");
        assertThat(storage.read("key2", String.class)).hasValue("value2");
    }

    @Test
    void shouldPersistAcrossInstances() {
        storage.save("persistent", "data");

        JsonSettingsStorage newInstance = new JsonSettingsStorage(tempDir, "settings.json");

        assertThat(newInstance.read("persistent", String.class)).hasValue("data");
    }

    record TestSettings(String theme, int fontSize, boolean autoSave) {}
}
