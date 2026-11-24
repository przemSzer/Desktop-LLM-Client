package dev.local.ai.core.tools.mcp;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.service.tool.ToolExecutionResult;
import dev.local.ai.core.tools.ToolDescriptor;
import dev.local.ai.core.tools.mcp.config.McpServerConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class McpConnectionRegistryTest {

    @Mock
    private McpClient mcpClient;

    private McpConnectionRegistry registry;
    private AtomicInteger createCount;

    @BeforeEach
    void setUp() {
        createCount = new AtomicInteger();
        McpServerConfig config = httpConfig("tavily");
        Executor direct = Runnable::run;
        registry = new McpConnectionRegistry(
                List.of(config),
                cfg -> {
                    createCount.incrementAndGet();
                    return mcpClient;
                },
                direct
        );
    }

    @AfterEach
    void tearDown() {
        if (registry != null) {
            registry.close();
        }
    }

    @Test
    void should_start_client_once_and_share_across_acquires() throws Exception {
        given(mcpClient.listTools()).willReturn(List.of(spec("search")));

        List<ToolDescriptor> first = registry.acquire("tavily").get(2, TimeUnit.SECONDS);
        List<ToolDescriptor> second = registry.acquire("tavily").get(2, TimeUnit.SECONDS);

        assertThat(createCount.get())
                .isEqualTo(1);
        assertThat(first)
                .extracting(ToolDescriptor::id)
                .containsExactly("tavily-search");
        assertThat(second)
                .extracting(ToolDescriptor::id)
                .containsExactly("tavily-search");
        assertThat(registry.isRunning("tavily"))
                .isTrue();
    }

    @Test
    void should_close_client_only_after_last_release() throws Exception {
        given(mcpClient.listTools()).willReturn(List.of(spec("search")));

        registry.acquire("tavily").get(2, TimeUnit.SECONDS);
        registry.acquire("tavily").get(2, TimeUnit.SECONDS);

        registry.release("tavily");
        then(mcpClient)
                .should(times(0))
                .close();
        assertThat(registry.isRunning("tavily"))
                .isTrue();

        registry.release("tavily");
        then(mcpClient)
                .should()
                .close();
        assertThat(registry.isRunning("tavily"))
                .isFalse();
    }

    @Test
    void should_keep_client_open_while_tool_call_is_in_flight_after_release() throws Exception {
        given(mcpClient.listTools()).willReturn(List.of(spec("search")));
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch releaseGate = new CountDownLatch(1);
        given(mcpClient.executeTool(any())).willAnswer(invocation -> {
            started.countDown();
            assertThat(releaseGate.await(2, TimeUnit.SECONDS))
                    .isTrue();
            return ToolExecutionResult.builder().resultText("ok").build();
        });

        List<ToolDescriptor> tools = registry.acquire("tavily").get(2, TimeUnit.SECONDS);
        ToolDescriptor descriptor = tools.getFirst();

        CompletableFuture<java.util.Optional<ToolExecutionResultMessage>> call =
                CompletableFuture.supplyAsync(() -> descriptor.executor().execute(
                        ToolExecutionRequest.builder()
                                .id("1")
                                .name("tavily-search")
                                .arguments("{}")
                                .build()
                ));

        assertThat(started.await(2, TimeUnit.SECONDS))
                .isTrue();
        registry.release("tavily");
        then(mcpClient)
                .should(times(0))
                .close();

        releaseGate.countDown();
        assertThat(call.get(2, TimeUnit.SECONDS))
                .isPresent();
        then(mcpClient)
                .should()
                .close();
    }

    @Test
    void should_cache_tool_names_after_cacheToolsConnectingIfNeeded_and_close_unused_server() throws Exception {
        given(mcpClient.listTools()).willReturn(List.of(spec("search")));

        List<McpCachedTool> cached = registry.cacheToolsConnectingIfNeeded("tavily").get(2, TimeUnit.SECONDS);

        assertThat(cached)
                .extracting(McpCachedTool::id)
                .containsExactly("tavily-search");
        assertThat(registry.cachedTools("tavily"))
                .isPresent();
        assertThat(registry.isRunning("tavily"))
                .isFalse();
        then(mcpClient)
                .should()
                .close();
    }

    @Test
    void should_skip_failed_server_without_affecting_others() {
        McpServerConfig good = httpConfig("good");
        McpServerConfig bad = httpConfig("bad");
        McpClient goodClient = mcpClient;
        registry.close();
        registry = new McpConnectionRegistry(
                List.of(good, bad),
                cfg -> {
                    if ("bad".equals(cfg.id())) {
                        throw new RuntimeException("boom");
                    }
                    return goodClient;
                },
                Runnable::run
        );
        given(goodClient.listTools()).willReturn(List.of(spec("a")));

        assertThatThrownBy(() -> registry.acquire("bad").join())
                .hasRootCauseMessage("boom");
        assertThat(registry.acquire("good").join())
                .extracting(ToolDescriptor::id)
                .containsExactly("good-a");
    }

    private static McpServerConfig httpConfig(String id) {
        return new McpServerConfig(
                id,
                id,
                true,
                "http://localhost/" + id,
                Map.of(),
                null,
                null,
                Map.of(),
                null,
                30,
                null
        );
    }

    private static ToolSpecification spec(String name) {
        return ToolSpecification.builder()
                .name(name)
                .description(name)
                .build();
    }
}
