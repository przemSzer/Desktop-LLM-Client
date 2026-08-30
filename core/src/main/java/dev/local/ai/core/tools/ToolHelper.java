package dev.local.ai.core.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class ToolHelper {

    private static final Logger logger = LoggerFactory.getLogger(ToolHelper.class);

    private ToolHelper() {}

    public static class ToolHelperException extends RuntimeException {
        public ToolHelperException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static Map<String, String> getArguments(ToolExecutionRequest toolExecutionRequest) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            return objectMapper.readValue(
                toolExecutionRequest.arguments(), 
                new TypeReference<Map<String, String>>() {}
            );
        } catch (JsonProcessingException e) {
            throw new ToolHelperException("Can not extract arguments from tool execution request: " + toolExecutionRequest.arguments(), e);
        }
    }

    public static Map<String, String> getArgumentsIgnoringError(ToolExecutionRequest toolExecutionRequest) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            return objectMapper.readValue(
                toolExecutionRequest.arguments(), 
                new TypeReference<Map<String, String>>() {}
            );
        } catch (JsonProcessingException e) {
            logger.debug("Can not extract arguments from tool execution request: " + toolExecutionRequest.arguments(), e);
            return Map.of("arg0", toolExecutionRequest.arguments());
        }
    }
}
