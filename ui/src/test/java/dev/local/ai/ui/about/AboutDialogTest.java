package dev.local.ai.ui.about;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class AboutDialogTest {

    @BeforeAll
    static void startJavaFx() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "JavaFX dialog test needs a display");
        CountDownLatch started = new CountDownLatch(1);
        try {
            Platform.startup(started::countDown);
        } catch (IllegalStateException alreadyStarted) {
            started.countDown();
        }
        assertThat(started.await(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void should_show_general_and_open_source_tabs() throws Exception {
        AppInfo info = new AppInfo(
                "Desktop LLM Client",
                "0.1.1",
                "Desktop client for multimodal LLMs");
        OssLibrary twemoji = new OssLibrary(
                "Twemoji",
                "14.0.2",
                "CC BY 4.0",
                "https://creativecommons.org/licenses/by/4.0/",
                "https://github.com/twitter/twemoji",
                "Copyright (c) Twitter, Inc. and other contributors");

        AtomicReference<TabPane> tabs = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                tabs.set(AboutDialog.load(info, List.of(twemoji)));
            } catch (Throwable thrown) {
                failure.set(thrown);
            } finally {
                done.countDown();
            }
        });

        assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
        if (failure.get() != null) {
            throw new AssertionError("Failed to build About dialog", failure.get());
        }

        TabPane pane = tabs.get();
        assertThat(pane.getTabs()).extracting(Tab::getText).containsExactly("General", "Open source");

        Label title = (Label) pane.getTabs().get(0).getContent().lookup("#about-app-name");
        Label version = (Label) pane.getTabs().get(0).getContent().lookup("#about-app-version");
        Label description = (Label) pane.getTabs().get(0).getContent().lookup("#about-app-description");
        assertThat(title.getText()).isEqualTo("Desktop LLM Client");
        assertThat(version.getText()).isEqualTo("Version 0.1.1");
        assertThat(description.getText()).contains("multimodal LLMs");

        @SuppressWarnings("unchecked")
        TableView<OssLibrary> table = (TableView<OssLibrary>) pane.getTabs().get(1).getContent().lookup("#about-oss-table");
        assertThat(table.getItems()).containsExactly(twemoji);
    }
}
