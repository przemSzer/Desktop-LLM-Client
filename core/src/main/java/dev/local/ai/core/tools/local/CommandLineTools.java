package dev.local.ai.core.tools.local;

import dev.langchain4j.agent.tool.*;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.local.ai.core.tools.ITool;
import dev.local.ai.core.tools.ToolDescriptor;
import dev.local.ai.core.tools.ToolHelper;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

public class CommandLineTools implements ITool {

    private static final Logger logger = LoggerFactory.getLogger(CommandLineTools.class);
    private static final String TOOL_NAME = "executeLocalCommand";
    private static final String PARAMETER_NAME = "command";

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

    ToolSpecification toolSpecifications() {
        String osName = System.getProperty("os.name", "");
        String windowsRecipe = osName.startsWith("Windows")
            ? """

                On Windows, do not call dir, echo, type, cd, copy, or del directly.
                Run those builtins through cmd.exe, for example: cmd /c dir C:\\absolute\\path
                For a pipeline, pass one quoted script to PowerShell, for example: \
                powershell -NoProfile -Command "Get-ChildItem -Path 'C:\\absolute\\path' | Select-Object -First 20 -ExpandProperty Name"
                Quote paths inside that script with single quotes and a single backslash.
                Do not write backslash-escaped quotes. A \\" is not an escaped quote: \
                the backslash is kept and the quote ends the argument.
                """
            : "";
        return ToolSpecification.builder()
            .name(TOOL_NAME)
            .description("""
                Runs one local program and returns its standard output.
                Pass the executable and its arguments as a single string, for example "git status".
                This starts a process directly: pipes, redirects, &&, ||, ;, globs, and shell builtins \
                (dir, echo, type, cd, copy, del) are not available.
                The working directory is the application directory and is not kept between calls. \
                Pass absolute paths.
                On success the result is trimmed stdout. 
                An empty string means exit code 0 and no output. \
                stderr is not included on success.
                A missing program or a non-zero exit is returned as an error. \
                Do not repeat the same command unchanged.
                The operating system is: %s.%s
                """.formatted(osName, windowsRecipe))
            .parameters(
                JsonObjectSchema.builder()
                    .addStringProperty(
                    PARAMETER_NAME,
                    "Executable plus arguments, one string. Example: git status")
                .required(PARAMETER_NAME)
                .build())
            .build();
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
            Map<String, String> args = ToolHelper.getArguments(toolExecutionRequest);

            String cmd = args.get(PARAMETER_NAME);
            if (cmd == null || cmd.isBlank()) {
                return Optional.of(
                    ToolExecutionResultMessage.builder()
                        .isError(true)
                        .id(toolExecutionRequest.id())
                        .toolName(toolExecutionRequest.name())
                        .text("Error: Missing required '" + PARAMETER_NAME + "' parameter")
                        .build()
                );
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
