package dev.local.ai.core.storage;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class ApplicationDirectory {

    public static final String NAME = ".dlc";

    private ApplicationDirectory() {}

    public static Path root() {
        return Paths.get(System.getProperty("user.home"), NAME);
    }

    public static Path chats() {
        return root().resolve("chats");
    }

    public static Path logs() {
        return root().resolve("logs");
    }
}
