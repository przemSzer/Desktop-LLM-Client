package dev.local.ai.core.tools.mcp.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class McpServersStoreTest {

    @TempDir
    Path tempDir;

    private McpServersStore store;

    @BeforeEach
    void setUp() {
        store = new McpServersStore(tempDir.resolve("mcp-servers.json"));
    }

    @Test
    void should_return_empty_list_when_file_is_missing() {
        assertThat(store.readAll())
                .isEmpty();
        assertThat(store.readStartable())
                .isEmpty();
    }

    @Test
    void should_read_http_and_stdio_servers_in_file_order() throws Exception {
        Files.writeString(store.file(), """
                {
                  "mcpServers": {
                    "tavily": {
                      "url": "http://localhost:8088/mcp",
                      "headers": { "Authorization": "Bearer x" },
                      "timeoutSeconds": 45
                    },
                    "filesystem": {
                      "name": "Filesystem",
                      "command": "npx",
                      "args": ["-y", "@modelcontextprotocol/server-filesystem", "C:/Users/me"],
                      "env": { "API_KEY": "secret" },
                      "workingDirectory": "C:/tmp",
                      "serializeRequests": true
                    }
                  }
                }
                """);

        List<McpServerConfig> configs = store.readAll();

        assertThat(configs)
                .extracting(McpServerConfig::id)
                .containsExactly("tavily", "filesystem");
        assertThat(configs.get(0).transportType())
                .isEqualTo(McpTransportType.HTTP);
        assertThat(configs.get(0).headers())
                .containsEntry("Authorization", "Bearer x");
        assertThat(configs.get(0).resolvedTimeoutSeconds())
                .isEqualTo(45);
        assertThat(configs.get(1).transportType())
                .isEqualTo(McpTransportType.STDIO);
        assertThat(configs.get(1).displayName())
                .isEqualTo("Filesystem");
        assertThat(configs.get(1).commandLine())
                .containsExactly(
                        "npx",
                        "-y",
                        "@modelcontextprotocol/server-filesystem",
                        "C:/Users/me"
                );
        assertThat(configs.get(1).env())
                .containsEntry("API_KEY", "secret");
        assertThat(configs.get(1).workingDirectory())
                .isEqualTo("C:/tmp");
        assertThat(configs.get(1).shouldSerializeRequests())
                .isTrue();
        assertThat(configs.get(0).shouldSerializeRequests())
                .isFalse();
    }

    @Test
    void should_include_disabled_servers_in_readAll_but_not_in_readStartable() throws Exception {
        Files.writeString(store.file(), """
                {
                  "mcpServers": {
                    "tavily": { "url": "http://localhost:8088/mcp" },
                    "filesystem": {
                      "command": "npx",
                      "args": ["-y", "pkg"],
                      "enabled": false
                    }
                  }
                }
                """);

        assertThat(store.readAll())
                .extracting(McpServerConfig::id)
                .containsExactly("tavily", "filesystem");
        assertThat(store.readStartable())
                .extracting(McpServerConfig::id)
                .containsExactly("tavily");
    }

    @Test
    void should_skip_invalid_entries_that_have_both_or_neither_transport() throws Exception {
        Files.writeString(store.file(), """
                {
                  "mcpServers": {
                    "both": {
                      "url": "http://localhost:1/mcp",
                      "command": "npx"
                    },
                    "neither": { "name": "Broken" },
                    "ok": { "url": "http://localhost:2/mcp" }
                  }
                }
                """);

        assertThat(store.readAll())
                .extracting(McpServerConfig::id)
                .containsExactly("ok");
    }

    @Test
    void should_ignore_unknown_json_fields() throws Exception {
        Files.writeString(store.file(), """
                {
                  "mcpServers": {
                    "tavily": {
                      "url": "http://localhost:8088/mcp",
                      "futureField": true
                    }
                  },
                  "extra": 1
                }
                """);

        assertThat(store.readAll())
                .extracting(McpServerConfig::id)
                .containsExactly("tavily");
    }

    @Test
    void should_return_empty_when_file_is_invalid_json() throws Exception {
        Files.writeString(store.file(), "not-json");

        assertThat(store.readAll())
                .isEmpty();
    }

    @Test
    void should_round_trip_servers_through_writeAll() {
        var http = new McpServerConfig(
                "tavily",
                "Tavily",
                true,
                "http://localhost:8088/mcp",
                Map.of("Authorization", "Bearer x"),
                null,
                null,
                null,
                null,
                30,
                null
        );
        var stdio = new McpServerConfig(
                "filesystem",
                "Filesystem",
                false,
                null,
                null,
                "npx",
                List.of("-y", "pkg"),
                Map.of("API_KEY", "secret"),
                "C:/tmp",
                60,
                true
        );

        store.writeAll(List.of(http, stdio));
        List<McpServerConfig> loaded = store.readAll();

        assertThat(loaded)
                .extracting(McpServerConfig::id)
                .containsExactly("tavily", "filesystem");
        assertThat(loaded.get(0).url())
                .isEqualTo("http://localhost:8088/mcp");
        assertThat(loaded.get(1).isEnabled())
                .isFalse();
        assertThat(loaded.get(1).commandLine())
                .containsExactly("npx", "-y", "pkg");
        assertThat(loaded.get(1).shouldSerializeRequests())
                .isTrue();
    }

    @Test
    void should_use_map_key_as_display_name_when_name_omitted() throws Exception {
        Files.writeString(store.file(), """
                {
                  "mcpServers": {
                    "tavily": { "url": "http://localhost:8088/mcp" }
                  }
                }
                """);

        assertThat(store.readAll().getFirst().displayName())
                .isEqualTo("tavily");
    }
}
