package dev.local.ai.core.tools;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.langchain4j.agent.tool.ToolExecutionRequest;

public class ToolHelper {

    private static final Logger logger = LoggerFactory.getLogger(ToolHelper.class);

    private ToolHelper() {}

    public static class ToolHelperException extends RuntimeException {
        public ToolHelperException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static Map<String, String> getArguments(ToolExecutionRequest toolExecutionRequest, String... argumentNames) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            Map<String, String> args = objectMapper.readValue(
                toolExecutionRequest.arguments(), 
                new TypeReference<Map<String, String>>() {}
            );
            return args;
        } catch (JsonProcessingException e) {
            throw new ToolHelperException("Can not extract arguments from tool execution request: " + toolExecutionRequest.arguments(), e);
        }
    }

    public static Map<String, String> getArgumentsIgnoringError(ToolExecutionRequest toolExecutionRequest, String... argumentNames) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            Map<String, String> args = objectMapper.readValue(
                toolExecutionRequest.arguments(), 
                new TypeReference<Map<String, String>>() {}
            );
            return args;
        } catch (JsonProcessingException e) {
            logger.debug("Can not extract arguments from tool execution request: " + toolExecutionRequest.arguments(), e);
            return Map.of("arg0", toolExecutionRequest.arguments());
        }
    }
}
