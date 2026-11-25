package dev.local.ai.ui.about;

import dev.local.ai.ui.sidebar.SidebarView;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kordamp.ikonli.javafx.FontIcon;

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

    @Test
    void should_use_the_same_icon_as_the_button_that_opens_it() throws Exception {
        AppInfo info = new AppInfo("Desktop LLM Client", "0.1.1", "Desktop client for multimodal LLMs");

        AtomicReference<String> buttonLiteral = new AtomicReference<>();
        AtomicReference<int[]> windowPixels = new AtomicReference<>();
        AtomicReference<int[]> buttonPixels = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                FontIcon graphic = sidebarAboutIcon();
                buttonLiteral.set(graphic.getIconLiteral());
                Stage about = AboutDialog.createStage(null, info, List.of());
                Image windowIcon = about.getIcons().stream()
                        .filter(image -> image.getWidth() >= 32)
                        .findFirst()
                        .orElseThrow();
                Image buttonGlyph = AboutIcons.image(graphic.getIconCode(), 32);
                windowPixels.set(pixels(windowIcon));
                buttonPixels.set(pixels(buttonGlyph));
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

        assertThat(buttonLiteral.get()).isEqualTo(AboutIcons.ICON.getDescription());
        assertThat(opaquePixelCount(windowPixels.get())).isPositive();
        assertThat(windowPixels.get()).containsExactly(buttonPixels.get());
    }

    private static FontIcon sidebarAboutIcon() {
        SidebarView sidebar = new SidebarView();
        Button settings = null;
        for (Node node : sidebar.lookupAll(".button")) {
            if (node instanceof Button button
                    && button.getGraphic() instanceof FontIcon icon
                    && "fth-settings".equals(icon.getIconLiteral())) {
                settings = button;
                break;
            }
        }
        assertThat(settings).isNotNull();
        HBox row = (HBox) settings.getParent();
        Button about = (Button) row.getChildren().get(row.getChildren().indexOf(settings) + 1);
        assertThat(about.getGraphic()).isInstanceOf(FontIcon.class);
        return (FontIcon) about.getGraphic();
    }

    private static int[] pixels(Image image) {
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        int[] argb = new int[width * height];
        image.getPixelReader().getPixels(
                0, 0, width, height, PixelFormat.getIntArgbInstance(), argb, 0, width);
        return argb;
    }

    private static int opaquePixelCount(int[] argb) {
        int count = 0;
        for (int pixel : argb) {
            if ((pixel >>> 24) != 0) {
                count++;
            }
        }
        return count;
    }
}
