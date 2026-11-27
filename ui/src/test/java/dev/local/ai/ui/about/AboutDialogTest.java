package dev.local.ai.ui.about;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class AboutDialogTest {

    @BeforeAll
    static void startJavaFx() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "JavaFX dialog test needs a display");
        Platform.setImplicitExit(false);
        CountDownLatch started = new CountDownLatch(1);
        try {
            Platform.startup(started::countDown);
        } catch (IllegalStateException _) {
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

        AtomicReference<Parent> root = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                root.set(AboutDialog.load(info, List.of(twemoji)));
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

        Parent dialog = root.get();
        TabPane pane = (TabPane) dialog.lookup("#about-tabs");
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

        Button ok = (Button) dialog.lookup("#about-ok");
        assertThat(ok.getText()).isEqualTo("OK");
        assertThat(ok.isCancelButton()).isTrue();
        assertThat(ok.isDefaultButton()).isTrue();
    }

    @Test
    void should_close_from_ok_and_escape_and_keep_library_rows_compact() throws Exception {
        AppInfo info = new AppInfo("Desktop LLM Client", "0.1.1", "Desktop client");
        OssLibrary plain = library("LangChain4j", "");
        OssLibrary twemoji = library("Twemoji", "Copyright (c) Twitter, Inc. and other contributors");
        OssLibrary katex = library("KaTeX", "Copyright (c) 2013-2020 Khan Academy and other contributors");
        OssLibrary mermaid = library("Mermaid", "Copyright (c) 2014 - 2022 Knut Sveidqvist");

        AtomicReference<Map<String, Double>> heights = new AtomicReference<>();
        AtomicReference<Boolean> iconVisible = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch shown = new CountDownLatch(1);
        CountDownLatch closed = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                Stage stage = AboutDialog.createStage(null, info, List.of(plain, twemoji, katex, mermaid));
                iconVisible.set(stage.getIcons().stream().anyMatch(AboutDialogTest::hasVisiblePixels));
                stage.show();
                TabPane tabs = (TabPane) stage.getScene().lookup("#about-tabs");
                tabs.getSelectionModel().select(1);
                Platform.runLater(() -> {
                    try {
                        @SuppressWarnings("unchecked")
                        TableView<OssLibrary> table = (TableView<OssLibrary>) stage.getScene().lookup("#about-oss-table");
                        heights.set(rowHeights(table));
                        stage.getScene().getRoot().fireEvent(new KeyEvent(
                                KeyEvent.KEY_PRESSED, "", "", KeyCode.ESCAPE, false, false, false, false));
                        assertThat(stage.isShowing()).isFalse();

                        Stage again = AboutDialog.createStage(null, info, List.of(plain));
                        again.show();
                        Button ok = (Button) again.getScene().lookup("#about-ok");
                        ok.fire();
                        assertThat(again.isShowing()).isFalse();
                    } catch (Throwable thrown) {
                        failure.set(thrown);
                    } finally {
                        closed.countDown();
                    }
                });
            } catch (Throwable thrown) {
                failure.set(thrown);
                closed.countDown();
            } finally {
                shown.countDown();
            }
        });

        assertThat(shown.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(closed.await(10, TimeUnit.SECONDS)).isTrue();
        if (failure.get() != null) {
            throw new AssertionError("Failed to exercise About dialog", failure.get());
        }

        assertThat(iconVisible.get()).isTrue();
        Map<String, Double> rowHeights = heights.get();
        double plainHeight = rowHeights.get("LangChain4j");
        assertThat(plainHeight).isGreaterThan(0);
        assertThat(rowHeights.get("Twemoji")).isLessThan(plainHeight * 2.5);
        assertThat(rowHeights.get("KaTeX")).isLessThan(plainHeight * 2.5);
        assertThat(rowHeights.get("Mermaid")).isLessThan(plainHeight * 2.5);
    }

    private static OssLibrary library(String name, String copyright) {
        return new OssLibrary(name, "1.0.0", "MIT", "https://example.test/license", "https://example.test", copyright);
    }

    private static Map<String, Double> rowHeights(TableView<OssLibrary> table) {
        Map<String, Double> heights = new HashMap<>();
        for (Node node : table.lookupAll(".table-row-cell")) {
            if (node instanceof TableRow<?> row && row.getItem() instanceof OssLibrary library && row.getHeight() > 0) {
                heights.put(library.name(), row.getHeight());
            }
        }
        return heights;
    }

    private static boolean hasVisiblePixels(Image image) {
        if (image == null || image.getPixelReader() == null) {
            return false;
        }
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (image.getPixelReader().getColor(x, y).getOpacity() > 0) {
                    return true;
                }
            }
        }
        return false;
    }
}
