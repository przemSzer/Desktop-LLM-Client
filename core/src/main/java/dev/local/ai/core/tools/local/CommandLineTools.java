package dev.local.ai.core.tools.local;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolMemoryId;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.community.code.local.CommandLineTool;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.local.ai.core.tools.IToolExecutor;
import dev.local.ai.core.tools.ToolDescriptor;
import dev.local.ai.core.tools.ToolHelper;

public class CommandLineTools implements IToolExecutor {

    private static final Logger logger = LoggerFactory.getLogger(CommandLineTools.class);
    private static final String TOOL_NAME = "executeLocalCommand";

    private final CommandLineTool commandLineTool;

    public CommandLineTools() {
        this.commandLineTool = new CommandLineTool();
    }

    private String doExecute(String command){
        DefaultExecutor executor = DefaultExecutor.builder().get();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ByteArrayOutputStream errorStream = new ByteArrayOutputStream();

        PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream, errorStream);
        executor.setStreamHandler(streamHandler);

        CommandLine cmdLine = CommandLine.parse(command);
        logger.debug("execute command line: {}", cmdLine);

        try {
            executor.execute(cmdLine);
            return outputStream.toString().trim();
        } catch (IOException e) {
            throw new RuntimeException(errorStream.toString(), e);
        }
    }

    @Tool("Executes a local command. Returns the output of the command or error message in case of failure.")
    public String executeLocalCommand(@P("The command to execute") String command, @ToolMemoryId String toolMemoryId) {
        logger.info("Executing local command: {}, tool memory id: {}", command, toolMemoryId);
        try {
            return commandLineTool.execute(command);
        } catch (Exception e) {
            logger.error("Failed to execute local command: {}, tool memory id: {}", command, toolMemoryId, e);
            var message = e.getMessage();
            if (e.getCause() instanceof Exception cause) {
                message += ", " + cause.getMessage();
            }
            return "Failed to execute local command, the following error occurred: " + message;
        }
    }

    public List<ToolSpecification> toolSpecifications() {
        String osName = System.getProperty("os.name");
        return ToolSpecifications.toolSpecificationsFrom(getInstance()).stream()
            .map(spec -> ToolSpecification.builder()
                .name(spec.name())
                .description(spec.description() + " The operating system is: " + osName)
                .parameters(spec.parameters())
                .build())
            .toList();
    }

    public ToolDescriptor toDescriptor() {
        return new ToolDescriptor(TOOL_NAME, "Execute local commands", toolSpecifications(), this);
    }

    @Override
    public Optional<ToolExecutionResultMessage> execute(ToolExecutionRequest toolExecutionRequest) {
        if (!canExecute(toolExecutionRequest)) {
            return Optional.empty();
        }

        try {
            Map<String, String> args = ToolHelper.getArguments(toolExecutionRequest, "cmd");

            String cmd = args.get("arg0");
            if (cmd == null || cmd.isBlank()) {
                return Optional.of(ToolExecutionResultMessage.from(
                    toolExecutionRequest, "Error: Missing required 'cmd' parameter"));
            }

            logger.info("Executing command: {}", cmd);
            String result = doExecute(cmd);
            return Optional.of(ToolExecutionResultMessage.from(toolExecutionRequest, result));

        } catch (Exception e) {
            logger.error("Command execution failed: {}", toolExecutionRequest.name(), e);
            var errorMessage = e.getMessage();
            if (errorMessage == null || errorMessage.isBlank() && e.getCause() != null){
                errorMessage = e.getCause().getMessage();
            }
            var failedResult = ToolExecutionResultMessage.builder()
                .isError(true)
                .id(toolExecutionRequest.id())
                .toolName(toolExecutionRequest.name())
                .text("Error: " + errorMessage)
                .build();
            return Optional.of(failedResult);
        }
    }

    public boolean canExecute(ToolExecutionRequest toolExecutionRequest) {
        return toolExecutionRequest.name().equals(TOOL_NAME);
    }

    private static class InternalInstanceHolder {
        private static final CommandLineTools INSTANCE = new CommandLineTools();
    }

    public static CommandLineTools getInstance() {
        return InternalInstanceHolder.INSTANCE;
    }
}
