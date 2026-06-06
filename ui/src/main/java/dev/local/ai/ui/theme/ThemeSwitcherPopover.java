package dev.local.ai.ui.theme;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import org.controlsfx.control.PopOver;

import java.net.URL;

public final class ThemeSwitcherPopover {

    private final ThemeManager themeManager;
    private final PopOver popover;

    public ThemeSwitcherPopover(ThemeManager themeManager) {
        this.themeManager = themeManager;
        this.popover = build();
    }

    public void toggle(javafx.scene.Node owner) {
        if (popover.isShowing()) {
            popover.hide();
        } else {
            popover.show(owner);
        }
    }

    private PopOver build() {
        Label heading = new Label("Theme");
        heading.getStyleClass().add("theme-popover-heading");

        ToggleGroup group = new ToggleGroup();
        VBox content = new VBox(6, heading);
        content.setPadding(new Insets(12));
        content.setPrefWidth(200);
        content.getStyleClass().add("system-popover-content");

        for (ThemeManager.Theme theme : ThemeManager.Theme.values()) {
            RadioButton option = new RadioButton(theme.displayName());
            option.setToggleGroup(group);
            option.setUserData(theme);
            option.setSelected(theme == themeManager.getCurrentTheme());
            option.setOnAction(e -> themeManager.apply(theme));
            content.getChildren().add(option);
        }

        themeManager.currentThemeProperty().addListener((obs, oldTheme, newTheme) ->
                group.getToggles().stream()
                        .filter(toggle -> toggle.getUserData() == newTheme)
                        .findFirst()
                        .ifPresent(toggle -> toggle.setSelected(true)));

        PopOver created = new PopOver(content);
        created.setArrowLocation(PopOver.ArrowLocation.BOTTOM_LEFT);
        created.setDetachable(false);
        created.setHeaderAlwaysVisible(false);

        URL stylesheet = getClass().getResource("/css/styles.css");
        if (stylesheet != null) {
            String css = stylesheet.toExternalForm();
            created.setOnShown(shown -> {
                if (content.getScene() != null && content.getScene().getRoot() != null
                        && !content.getScene().getRoot().getStylesheets().contains(css)) {
                    content.getScene().getRoot().getStylesheets().add(css);
                }
            });
        }
        return created;
    }
}
