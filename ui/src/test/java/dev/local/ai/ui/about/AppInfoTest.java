package dev.local.ai.ui.about;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppInfoTest {

    @Test
    void should_parse_application_properties() throws Exception {
        AppInfo info = AppInfo.parse(stream("""
                name=Desktop LLM Client
                version=0.1.1
                description=Desktop client for multimodal LLMs
                """));

        assertThat(info.name()).isEqualTo("Desktop LLM Client");
        assertThat(info.version()).isEqualTo("0.1.1");
        assertThat(info.description()).isEqualTo("Desktop client for multimodal LLMs");
    }

    @Test
    void should_reject_unresolved_property() {
        ByteArrayInputStream input = stream("""
                name=${project.parent.name}
                version=0.1.1
                description=Desktop client
                """);

        assertThatThrownBy(() -> AppInfo.parse(input))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("name");
    }

    @Test
    void should_reject_missing_property() {
        ByteArrayInputStream input = stream("""
                name=Desktop LLM Client
                version=0.1.1
                """);

        assertThatThrownBy(() -> AppInfo.parse(input))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("description");
    }

    @Test
    void should_load_name_version_and_description_from_the_pom() {
        AppInfo info = AppInfo.load();

        assertThat(info.name()).isEqualTo("Desktop LLM Client");
        assertThat(info.version()).matches("\\d+\\.\\d+\\.\\d+");
        assertThat(info.description()).contains("multimodal LLMs");
        assertThat(info.description()).doesNotContain("${");
    }

    private static ByteArrayInputStream stream(String properties) {
        return new ByteArrayInputStream(properties.getBytes(StandardCharsets.UTF_8));
    }
}
