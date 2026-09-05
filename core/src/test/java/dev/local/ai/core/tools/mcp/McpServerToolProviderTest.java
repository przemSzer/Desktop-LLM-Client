// package dev.local.ai.core.tools.mcp;

// import java.util.List;
// import java.util.Optional;

// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.ArgumentCaptor;
// import org.mockito.Captor;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;

// import dev.langchain4j.agent.tool.ToolExecutionRequest;
// import dev.langchain4j.agent.tool.ToolSpecification;
// import dev.langchain4j.data.message.ToolExecutionResultMessage;
// import dev.langchain4j.mcp.client.McpClient;
// import dev.langchain4j.service.tool.ToolExecutionResult;
// import dev.local.ai.core.tools.ToolDescriptor;

// import static org.assertj.core.api.Assertions.assertThat;
// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.BDDMockito.given;
// import static org.mockito.BDDMockito.then;
// import static org.mockito.Mockito.times;

// @ExtendWith(MockitoExtension.class)
// class McpServerToolProviderTest {

//     private static final String SERVER_ID = "fs-server";
//     private static final String SERVER_DISPLAY_NAME = "Filesystem";

//     @Mock
//     private McpClient mcpClient;

//     @Captor
//     private ArgumentCaptor<ToolExecutionRequest> requestCaptor;

//     @Test
//     void shouldExposeOneDescriptorPerRemoteToolWithPrefixedIds() {
//         // given
//         ToolSpecification readSpec = specOf("read_file");
//         ToolSpecification writeSpec = specOf("write_file");
//         given(mcpClient.listTools()).willReturn(List.of(readSpec, writeSpec));

//         var provider = new McpServerToolProvider(SERVER_ID, SERVER_DISPLAY_NAME, mcpClient);

//         // when
//         List<ToolDescriptor> descriptors = provider.getToolDescriptors();

//         // then
//         assertThat(descriptors).hasSize(2);
//         assertThat(descriptors).extracting(ToolDescriptor::id)
//             .containsExactly("fs-server/read_file", "fs-server/write_file");
//         assertThat(descriptors).extracting(ToolDescriptor::displayName)
//             .containsExactly("Filesystem: read_file", "Filesystem: write_file");
//         assertThat(descriptors.get(0).specification()).containsExactly(readSpec);
//         assertThat(descriptors.get(1).specification()).containsExactly(writeSpec);
//     }

//     @Test
//     void shouldShareSingleExecutorAcrossAllDescriptorsOfTheSameServer() {
//         // given
//         given(mcpClient.listTools()).willReturn(List.of(specOf("a"), specOf("b")));

//         var provider = new McpServerToolProvider(SERVER_ID, SERVER_DISPLAY_NAME, mcpClient);

//         // when
//         List<ToolDescriptor> descriptors = provider.getToolDescriptors();

//         // then
//         assertThat(descriptors).hasSize(2);
//         assertThat(descriptors.get(0).executor())
//             .as("All descriptors of one server must reuse the same executor instance")
//             .isSameAs(descriptors.get(1).executor());
//     }

//     @Test
//     void shouldReturnEmptyDescriptorListWhenServerExposesNoTools() {
//         // given
//         given(mcpClient.listTools()).willReturn(List.of());

//         var provider = new McpServerToolProvider(SERVER_ID, SERVER_DISPLAY_NAME, mcpClient);

//         // when
//         List<ToolDescriptor> descriptors = provider.getToolDescriptors();

//         // then
//         assertThat(descriptors).isEmpty();
//     }

//     @Test
//     void shouldCacheListToolsCallAcrossInvocations() {
//         // given
//         given(mcpClient.listTools()).willReturn(List.of(specOf("a"), specOf("b")));

//         var provider = new McpServerToolProvider(SERVER_ID, SERVER_DISPLAY_NAME, mcpClient);

//         // when
//         provider.getToolDescriptors();
//         provider.getToolDescriptors();
//         provider.getToolDescriptors();

//         // then
//         then(mcpClient).should(times(1)).listTools();
//     }

//     @Test
//     void executorOfBuiltDescriptorShouldRouteCallsToMcpClient() {
//         // given
//         given(mcpClient.listTools()).willReturn(List.of(specOf("read_file")));
//         var executionResult = ToolExecutionResult.builder()
//             .resultText("contents")
//             .isError(false)
//             .build();
//         given(mcpClient.executeTool(any(ToolExecutionRequest.class))).willReturn(executionResult);

//         var provider = new McpServerToolProvider(SERVER_ID, SERVER_DISPLAY_NAME, mcpClient);
//         ToolDescriptor descriptor = provider.getToolDescriptors().get(0);

//         var request = ToolExecutionRequest.builder()
//             .id("call-1")
//             .name("read_file")
//             .arguments("{}")
//             .build();

//         // when
//         Optional<ToolExecutionResultMessage> result = descriptor.executor().execute(request);

//         // then
//         assertThat(result).isPresent();
//         assertThat(result.get().text()).isEqualTo("contents");
//         then(mcpClient).should().executeTool(requestCaptor.capture());
//         assertThat(requestCaptor.getValue().name()).isEqualTo("read_file");
//     }

//     @Test
//     void shouldCloseUnderlyingMcpClientOnClose() throws Exception {
//         // given
//         var provider = new McpServerToolProvider(SERVER_ID, SERVER_DISPLAY_NAME, mcpClient);

//         // when
//         provider.close();

//         // then
//         then(mcpClient).should().close();
//     }

//     private static ToolSpecification specOf(String name) {
//         return ToolSpecification.builder().name(name).description(name + " desc").build();
//     }
// }
