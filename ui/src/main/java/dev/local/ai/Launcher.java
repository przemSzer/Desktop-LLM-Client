package dev.local.ai;

import dev.local.ai.logging.LoggingHelper;

public class Launcher {

    static void main(String[] args) {
        LoggingHelper.setPIDProperty();
        LoggingHelper.bridgeJulToSlf4j();
        LoggingHelper.redirectStandardStreams();
        GlobalExceptionHandler.install();

        MainApplication.launch(MainApplication.class, args);
    }
}
