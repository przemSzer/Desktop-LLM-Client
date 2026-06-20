package dev.local.ai.logging;

import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

public final class LoggingHelper {
    private static final String PID_PROPERTY = "app.pid";
    private static final String STDOUT_LOGGER = "system.out";
    private static final String STDERR_LOGGER = "system.err";

    private LoggingHelper() {}

    public static void setPIDProperty() {
        System.setProperty(PID_PROPERTY, Long.toString(ProcessHandle.current().pid()));
    }

    public static void bridgeJulToSlf4j() {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }
    public static void redirectStandardStreams() {
        var stdoutLogger = LoggerFactory.getLogger(STDOUT_LOGGER);
        var stderrLogger = LoggerFactory.getLogger(STDERR_LOGGER);
        System.setOut(LoggingStreams.asPrintStream(stdoutLogger::info));
        System.setErr(LoggingStreams.asPrintStream(stderrLogger::warn));
    }

}
