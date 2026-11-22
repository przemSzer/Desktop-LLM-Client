package dev.local.ai.core.tools.mcp;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.service.tool.ToolExecutionResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class SerializingMcpClientTest {

    @Mock
    private McpClient delegate;

    @Test
    void should_serialize_overlapping_executeTool_invocations() throws Exception {
        var maxConcurrent = new AtomicInteger();
        var inFlight = new AtomicInteger();
        var firstInside = new CountDownLatch(1);
        var allowFirstToFinish = new CountDownLatch(1);

        given(delegate.executeTool(any(ToolExecutionRequest.class))).willAnswer(invocation -> {
            int current = inFlight.incrementAndGet();
            maxConcurrent.accumulateAndGet(current, Math::max);
            firstInside.countDown();
            assertThat(allowFirstToFinish.await(3, TimeUnit.SECONDS)).isTrue();
            inFlight.decrementAndGet();
            return ToolExecutionResult.builder().resultText("ok").build();
        });

        var serializing = new SerializingMcpClient(delegate);
        var request = ToolExecutionRequest.builder()
                .id("1")
                .name("readFile")
                .arguments("{}")
                .build();

        var finished = new CountDownLatch(2);
        Thread t1 = Thread.ofVirtual().start(() -> {
            try {
                serializing.executeTool(request);
            } finally {
                finished.countDown();
            }
        });
        assertThat(firstInside.await(2, TimeUnit.SECONDS)).isTrue();

        Thread t2 = Thread.ofVirtual().start(() -> {
            try {
                serializing.executeTool(request);
            } finally {
                finished.countDown();
            }
        });
        Thread.sleep(100);
        assertThat(maxConcurrent.get()).isEqualTo(1);
        assertThat(inFlight.get()).isEqualTo(1);

        allowFirstToFinish.countDown();
        assertThat(finished.await(3, TimeUnit.SECONDS)).isTrue();
        assertThat(maxConcurrent.get()).isEqualTo(1);

        then(delegate).should(times(2)).executeTool(any(ToolExecutionRequest.class));
        t1.join(1000);
        t2.join(1000);
    }
}
