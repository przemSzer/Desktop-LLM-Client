package dev.local.ai.core.storage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationDirectoryTest {

    private static final String USER_HOME = "user.home";

    @TempDir
    private Path tempDir;

    private String previousUserHome;

    @BeforeEach
    void setUp() {
        previousUserHome = System.getProperty(USER_HOME);
        System.setProperty(USER_HOME, tempDir.toString());
    }

    @AfterEach
    void tearDown() {
        if (previousUserHome == null) {
            System.clearProperty(USER_HOME);
        } else {
            System.setProperty(USER_HOME, previousUserHome);
        }
    }

    @Test
    void resolvesDlcUnderUserHome() {
        Path root = ApplicationDirectory.root();

        assertThat(root).isEqualTo(tempDir.resolve(".dlc"));
        assertThat(ApplicationDirectory.chats()).isEqualTo(root.resolve("chats"));
        assertThat(ApplicationDirectory.logs()).isEqualTo(root.resolve("logs"));
    }
}
