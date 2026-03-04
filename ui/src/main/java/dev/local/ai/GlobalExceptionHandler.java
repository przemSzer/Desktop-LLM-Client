package dev.local.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public static void install() {
        Thread.setDefaultUncaughtExceptionHandler((t,ex)->{
            logger.error("Uncaught exception occurred in thread: {}", t.getName(), ex);
        });        
    }

}
