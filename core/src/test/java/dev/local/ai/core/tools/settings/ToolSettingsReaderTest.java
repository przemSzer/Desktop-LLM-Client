package dev.local.ai.core.tools.settings;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class ToolSettingsReaderTest {

    @TempDir
    Path tempDir;

    private ToolSettingsReader reader;

    @BeforeEach
    void setUp() {
        reader = new ToolSettingsReader();
    }

    @Test
    void should_return_empty_settings_when_file_is_missing() {
        Path missing = tempDir.resolve("tool-settings.json");

        ToolSettings settings = reader.read(missing);

        assertThat(settings.alwaysAllowedTools())
                .isEmpty();
    }

    @Test
    void should_return_empty_settings_when_file_is_null() {
        ToolSettings settings = reader.read(null);

        assertThat(settings.alwaysAllowedTools())
                .isEmpty();
    }

    @Test
    void should_return_always_allowed_tools_from_file() throws Exception {
        Path file = tempDir.resolve("tool-settings.json");
        Files.writeString(file, """
                {
                  "alwaysAllowedTools": ["downloadWebPage", "local-mcp-tavilysearch"]
                }
                """);

        ToolSettings settings = reader.read(file);

        assertThat(settings.alwaysAllowedTools())
                .containsExactly("downloadWebPage", "local-mcp-tavilysearch");
    }

    @Test
    void should_ignore_unknown_json_fields() throws Exception {
        Path file = tempDir.resolve("tool-settings.json");
        Files.writeString(file, """
                {
                  "alwaysAllowedTools": ["downloadWebPage"],
                  "futureField": true
                }
                """);

        ToolSettings settings = reader.read(file);

        assertThat(settings.alwaysAllowedTools())
                .containsExactly("downloadWebPage");
    }

    @Test
    void should_treat_missing_always_allowed_list_as_empty() throws Exception {
        Path file = tempDir.resolve("tool-settings.json");
        Files.writeString(file, "{}");

        ToolSettings settings = reader.read(file);

        assertThat(settings.alwaysAllowedTools())
                .isEmpty();
    }

    @Test
    void should_return_empty_settings_when_file_is_invalid_json() throws Exception {
        Path file = tempDir.resolve("tool-settings.json");
        Files.writeString(file, "not-json");

        ToolSettings settings = reader.read(file);

        assertThat(settings.alwaysAllowedTools())
                .isEmpty();
    }

    @Mock
    ObjectMapper objectMapper;

    @Test
    void should_return_empty_settings_on_any_exception() throws IOException {
        Path file = tempDir.resolve("tool-settings.json");
        Files.writeString(file, "not-json");
        var settingsReader = new ToolSettingsReader(objectMapper);
        var exception = new RuntimeException();
        willThrow(exception)
                .given(objectMapper)
                .readValue(any(File.class), eq(ToolSettings.class));

        ToolSettings settings = settingsReader.read(file);

        assertThat(settings.alwaysAllowedTools())
                .isEmpty();
    }

    @Test
    void should_return_empty_settings_when_mapper_returns_null() throws IOException {
        Path file = tempDir.resolve("tool-settings.json");
        Files.writeString(file, "not-json");
        var settingsReader = new ToolSettingsReader(objectMapper);
        given(objectMapper.readValue(any(File.class), eq(ToolSettings.class)))
                .willReturn(null);

        ToolSettings settings = settingsReader.read(file);

        assertThat(settings.alwaysAllowedTools())
                .isEmpty();
    }


}
