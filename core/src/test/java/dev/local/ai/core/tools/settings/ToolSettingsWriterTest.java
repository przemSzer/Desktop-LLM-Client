package dev.local.ai.core.tools.settings;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class ToolSettingsWriterTest {

    @TempDir
    Path tempDir;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ObjectWriter objectWriter;

    private ToolSettingsReader reader;
    private ToolSettingsWriter writer;

    @BeforeEach
    void setUp() {
        reader = new ToolSettingsReader();
        writer = new ToolSettingsWriter();
    }

    @Test
    void should_round_trip_always_allowed_tools_through_reader() {
        Path file = tempDir.resolve("conv-1").resolve("tool-settings.json");
        ToolSettings original = new ToolSettings(List.of("downloadWebPage", "executeLocalCommand"));

        writer.write(file, original);
        ToolSettings loaded = reader.read(file);

        assertThat(loaded.alwaysAllowedTools())
                .containsExactly("downloadWebPage", "executeLocalCommand");
    }

    @Test
    void should_create_parent_directory_when_missing() {
        Path file = tempDir.resolve("nested").resolve("chat").resolve("tool-settings.json");

        writer.write(file, new ToolSettings(List.of("downloadWebPage")));

        assertThat(Files.exists(file))
                .isTrue();
        assertThat(reader.read(file).alwaysAllowedTools())
                .containsExactly("downloadWebPage");
    }

    @Test
    void should_write_empty_settings_when_settings_are_null() {
        Path file = tempDir.resolve("tool-settings.json");

        writer.write(file, null);

        assertThat(reader.read(file).alwaysAllowedTools())
                .isEmpty();
    }

    @Test
    void should_swallow_any_exception_when_writing_fails() throws IOException {
        Path file = tempDir.resolve("tool-settings.json");
        var settings = new ToolSettings(List.of("downloadWebPage"));
        var settingsWriter = new ToolSettingsWriter(objectMapper);
        given(objectMapper.writerWithDefaultPrettyPrinter())
                .willReturn(objectWriter);
        willThrow(new RuntimeException("boom"))
                .given(objectWriter)
                .writeValue(any(File.class), eq(settings));

        assertThatCode(() -> settingsWriter.write(file, settings))
                .doesNotThrowAnyException();

        assertThat(Files.exists(file))
                .isFalse();
        then(objectWriter)
                .should()
                .writeValue(any(File.class), eq(settings));
    }
}
