package dev.local.ai.core.tools.local;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommandLineToolsTest {

    private final CommandLineTools tools = CommandLineTools.getInstance();

    @Test
    void should_describe_command_parameter_and_current_operating_system() {
        ToolSpecification spec = tools.toDescriptor().specification();
        JsonObjectSchema parameters = spec.parameters();

        assertThat(tools.toDescriptor().id())
                .isEqualTo("executeLocalCommand");
        assertThat(spec.name())
                .isEqualTo("executeLocalCommand");
        assertThat(spec.description())
                .contains("Runs one local program")
                .contains(System.getProperty("os.name"));
        assertThat(parameters.required())
                .containsExactly("command");
        assertThat(stringProperty(parameters, "command").description())
                .contains("git status");
    }

    @Test
    void should_include_windows_recipe_only_on_windows() {
        String description = tools.toDescriptor().specification().description();

        if (isWindows()) {
            assertThat(description)
                    .contains("cmd /c dir")
                    .contains("powershell -NoProfile -Command");
            return;
        }

        assertThat(description)
                .doesNotContain("On Windows");
    }

    @Test
    void should_return_empty_when_tool_name_does_not_match() {
        var result = tools.execute(request("otherTool", "{\"command\":\"whoami\"}"));

        assertThat(result)
                .isEmpty();
    }

    @Test
    void should_report_missing_command_when_argument_is_absent() {
        ToolExecutionResultMessage result = tools.execute(request("executeLocalCommand", "{}"))
                .orElseThrow();

        assertThat(result.isError())
                .isTrue();
        assertThat(result.text())
                .isEqualTo("Error: Missing required 'command' parameter");
    }

    @Test
    void should_report_missing_command_when_argument_is_blank() {
        ToolExecutionResultMessage result = tools.execute(request("executeLocalCommand", "{\"command\":\"   \"}"))
                .orElseThrow();

        assertThat(result.isError())
                .isTrue();
        assertThat(result.text())
                .isEqualTo("Error: Missing required 'command' parameter");
    }

    @Test
    void should_return_stdout_when_program_exits_zero() {
        ToolExecutionResultMessage result = tools.execute(request("executeLocalCommand", commandJson(echoCommand())))
                .orElseThrow();

        assertThat(result.isError())
                .isNotEqualTo(Boolean.TRUE);
        assertThat(result.text())
                .isEqualTo("hello-from-tool");
    }

    @Test
    void should_return_error_when_program_is_missing() {
        ToolExecutionResultMessage result = tools.execute(
                request("executeLocalCommand", commandJson("this-program-does-not-exist-xyz")))
                .orElseThrow();

        assertThat(result.isError())
                .isTrue();
        assertThat(result.text())
                .contains("Cannot run program")
                .contains("this-program-does-not-exist-xyz");
    }

    private static ToolExecutionRequest request(String name, String arguments) {
        return ToolExecutionRequest.builder()
                .id("tool-1")
                .name(name)
                .arguments(arguments)
                .build();
    }

    private static String commandJson(String command) {
        return "{\"command\":\"" + command + "\"}";
    }

    private static String echoCommand() {
        if (isWindows()) {
            return "cmd /c echo hello-from-tool";
        }
        return "echo hello-from-tool";
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").startsWith("Windows");
    }

    private static JsonStringSchema stringProperty(JsonObjectSchema parameters, String name) {
        return (JsonStringSchema) parameters.properties().get(name);
    }
}
