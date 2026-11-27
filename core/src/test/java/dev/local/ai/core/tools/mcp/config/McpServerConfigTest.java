package dev.local.ai.core.tools.mcp.config;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class McpServerConfigTest {

    @Test
    void should_infer_http_from_url() {
        var config = new McpServerConfig(
                "tavily", null, null, "http://localhost/mcp",
                Map.of(), null, null, Map.of(), null, null, null
        );

        assertThat(config.transportType())
                .isEqualTo(McpTransportType.HTTP);
        assertThat(config.isValid())
                .isTrue();
        assertThat(config.isEnabled())
                .isTrue();
        assertThat(config.resolvedTimeoutSeconds())
                .isEqualTo(McpServerConfig.DEFAULT_TIMEOUT_SECONDS);
        assertThat(config.shouldSerializeRequests())
                .isFalse();
    }

    @Test
    void should_infer_stdio_from_command() {
        var config = new McpServerConfig(
                "fs", "Filesystem", true, null, Map.of(),
                "npx", List.of("-y", "pkg"), Map.of("K", "V"), "C:/tmp", 90, true
        );

        assertThat(config.transportType())
                .isEqualTo(McpTransportType.STDIO);
        assertThat(config.commandLine())
                .containsExactly("npx", "-y", "pkg");
        assertThat(config.resolvedTimeoutSeconds())
                .isEqualTo(90);
        assertThat(config.shouldSerializeRequests())
                .isTrue();
    }

    @Test
    void should_be_invalid_when_both_or_neither_transport_fields_set() {
        var both = new McpServerConfig(
                "both", null, null, "http://x", Map.of(),
                "npx", List.of(), Map.of(), null, null, null
        );
        var neither = new McpServerConfig(
                "neither", null, null, null, Map.of(),
                null, List.of(), Map.of(), null, null, null
        );

        assertThat(both.isValid())
                .isFalse();
        assertThat(neither.isValid())
                .isFalse();
    }

    @Test
    void should_treat_enabled_false_as_disabled() {
        var config = new McpServerConfig(
                "tavily", null, false, "http://localhost/mcp",
                Map.of(), null, null, Map.of(), null, null, null
        );

        assertThat(config.isEnabled())
                .isFalse();
    }

    @Test
    void should_treat_serializeRequests_false_as_off() {
        var config = new McpServerConfig(
                "fs", null, null, null, Map.of(),
                "npx", List.of(), Map.of(), null, null, false
        );

        assertThat(config.shouldSerializeRequests())
                .isFalse();
    }
}
