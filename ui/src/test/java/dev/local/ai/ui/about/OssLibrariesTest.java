package dev.local.ai.ui.about;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OssLibrariesTest {

    @Test
    void should_skip_comments_and_blank_lines() throws Exception {
        List<OssLibrary> libraries = OssLibraries.parse(stream("""
                # comment

                Twemoji|14.0.2|CC BY 4.0|https://creativecommons.org/licenses/by/4.0/|https://github.com/twitter/twemoji|Copyright (c) Twitter, Inc. and other contributors
                """));

        assertThat(libraries).containsExactly(new OssLibrary(
                "Twemoji",
                "14.0.2",
                "CC BY 4.0",
                "https://creativecommons.org/licenses/by/4.0/",
                "https://github.com/twitter/twemoji",
                "Copyright (c) Twitter, Inc. and other contributors"));
    }

    @Test
    void should_reject_a_line_with_the_wrong_field_count() {
        ByteArrayInputStream input = stream("Only|three|fields\n");

        assertThatThrownBy(() -> OssLibraries.parse(input))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("line 1");
    }

    @Test
    void should_reject_an_unresolved_version() {
        ByteArrayInputStream input = stream(
                "Twemoji|${twemoji.version}|CC BY 4.0|https://example|https://example|\n");

        assertThatThrownBy(() -> OssLibraries.parse(input))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("version");
    }

    @Test
    void should_load_libraries_that_require_attribution() {
        List<OssLibrary> libraries = OssLibraries.load();

        assertThat(libraries).extracting(OssLibrary::name).containsExactly(
                "LangChain4j",
                "LangChain4j OpenAI",
                "LangChain4j Ollama",
                "LangChain4j Google AI Gemini",
                "LangChain4j Anthropic",
                "LangChain4j MCP",
                "LangChain4j Apache Tika",
                "LangChain4j Jsoup",
                "Apache Commons Exec",
                "Jackson JSR-310",
                "Typesafe Config",
                "Logback",
                "SLF4J",
                "RxJava",
                "OpenJFX",
                "Flexmark",
                "AtlantaFX",
                "ControlsFX",
                "Ikonli",
                "JSpecify",
                "Twemoji",
                "KaTeX",
                "Mermaid");
        assertThat(libraries).allSatisfy(library -> {
            assertThat(library.version()).doesNotContain("${").isNotBlank();
            assertThat(library.license()).isNotBlank();
            assertThat(library.licenseUrl()).startsWith("https://");
            assertThat(library.projectUrl()).startsWith("https://");
        });

        OssLibrary twemoji = libraries.stream()
                .filter(library -> library.name().equals("Twemoji"))
                .findFirst()
                .orElseThrow();
        assertThat(twemoji.license()).isEqualTo("CC BY 4.0");
        assertThat(twemoji.copyright()).isEqualTo("Copyright (c) Twitter, Inc. and other contributors");

        OssLibrary katex = libraries.stream()
                .filter(library -> library.name().equals("KaTeX"))
                .findFirst()
                .orElseThrow();
        assertThat(katex.license()).isEqualTo("MIT License");
        assertThat(katex.copyright()).contains("Khan Academy");

        OssLibrary mermaid = libraries.stream()
                .filter(library -> library.name().equals("Mermaid"))
                .findFirst()
                .orElseThrow();
        assertThat(mermaid.license()).isEqualTo("MIT License");
        assertThat(mermaid.copyright()).contains("Knut Sveidqvist");
    }

    private static ByteArrayInputStream stream(String text) {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }
}
