package dev.local.ai.core.tools.mcp;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.service.tool.ToolExecutionResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class McpToolExecutorAdapterTest {

    private static final String OWNED_TOOL = "read_file";
    private static final String OTHER_TOOL = "write_file";

    @Mock
    private McpClient mcpClient;

    @Captor
    private ArgumentCaptor<ToolExecutionRequest> requestCaptor;

    @Test
    void shouldExecuteOwnedToolAndReturnSuccessfulResult() {
        // given
        var successResult = ToolExecutionResult.builder()
            .resultText("file contents")
            .isError(false)
            .build();
        given(mcpClient.executeTool(any(ToolExecutionRequest.class))).willReturn(successResult);

        var adapter = new McpToolExecutorAdapter(mcpClient, Set.of(OWNED_TOOL));
        var request = ToolExecutionRequest.builder()
            .id("call-1")
            .name(OWNED_TOOL)
            .arguments("{\"path\":\"/tmp/x\"}")
            .build();

        // when
        Optional<ToolExecutionResultMessage> result = adapter.execute(request);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().toolName()).isEqualTo(OWNED_TOOL);
        assertThat(result.get().id()).isEqualTo("call-1");
        assertThat(result.get().text()).isEqualTo("file contents");
        assertThat(result.get().isError()).isFalse();

        then(mcpClient).should().executeTool(requestCaptor.capture());
        ToolExecutionRequest forwarded = requestCaptor.getValue();
        assertThat(forwarded.name()).isEqualTo(OWNED_TOOL);
        assertThat(forwarded.id()).isEqualTo("call-1");
        assertThat(forwarded.arguments()).isEqualTo("{\"path\":\"/tmp/x\"}");
    }

    @Test
    void shouldReturnEmptyAndNotCallMcpClientWhenToolNotOwned() {
        // given
        var adapter = new McpToolExecutorAdapter(mcpClient, Set.of(OWNED_TOOL));
        var request = ToolExecutionRequest.builder()
            .id("call-2")
            .name(OTHER_TOOL)
            .arguments("{}")
            .build();

        // when
        Optional<ToolExecutionResultMessage> result = adapter.execute(request);

        // then
        assertThat(result).isEmpty();
        then(mcpClient).should(never()).executeTool(any(ToolExecutionRequest.class));
    }

    @Test
    void shouldPropagateMcpServerErrorFlagWithoutThrowing() {
        // given
        var failureFromServer = ToolExecutionResult.builder()
            .resultText("file not found")
            .isError(true)
            .build();
        given(mcpClient.executeTool(any(ToolExecutionRequest.class))).willReturn(failureFromServer);

        var adapter = new McpToolExecutorAdapter(mcpClient, Set.of(OWNED_TOOL));
        var request = ToolExecutionRequest.builder()
            .id("call-3")
            .name(OWNED_TOOL)
            .arguments("{}")
            .build();

        // when
        Optional<ToolExecutionResultMessage> result = adapter.execute(request);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().isError()).isTrue();
        assertThat(result.get().text()).isEqualTo("file not found");
        assertThat(result.get().toolName()).isEqualTo(OWNED_TOOL);
    }

    @Test
    void shouldReturnErrorResultWhenMcpClientThrows() {
        // given
        willThrow(new RuntimeException("connection refused"))
            .given(mcpClient).executeTool(any(ToolExecutionRequest.class));

        var adapter = new McpToolExecutorAdapter(mcpClient, Set.of(OWNED_TOOL));
        var request = ToolExecutionRequest.builder()
            .id("call-4")
            .name(OWNED_TOOL)
            .arguments("{}")
            .build();

        // when
        Optional<ToolExecutionResultMessage> result = adapter.execute(request);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().isError()).isTrue();
        assertThat(result.get().text()).contains("MCP error").contains("connection refused");
        assertThat(result.get().toolName()).isEqualTo(OWNED_TOOL);
        assertThat(result.get().id()).isEqualTo("call-4");
    }
}
