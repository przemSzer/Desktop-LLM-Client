package dev.local.ai.ui.theme;

import atlantafx.base.theme.CupertinoDark;
import atlantafx.base.theme.CupertinoLight;
import atlantafx.base.theme.Dracula;
import atlantafx.base.theme.NordDark;
import atlantafx.base.theme.NordLight;
import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import dev.local.ai.core.storage.SettingsStorage;
import javafx.application.Application;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public final class ThemeManager {

    public enum Theme {
        PRIMER_LIGHT("Primer Light", new PrimerLight(), false),
        PRIMER_DARK("Primer Dark", new PrimerDark(), true),
        NORD_LIGHT("Nord Light", new NordLight(), false),
        NORD_DARK("Nord Dark", new NordDark(), true),
        CUPERTINO_LIGHT("Cupertino Light", new CupertinoLight(), false),
        CUPERTINO_DARK("Cupertino Dark", new CupertinoDark(), true),
        DRACULA("Dracula", new Dracula(), true);

        private final String displayName;
        private final atlantafx.base.theme.Theme style;
        private final boolean dark;

        Theme(String displayName, atlantafx.base.theme.Theme style, boolean dark) {
            this.displayName = displayName;
            this.style = style;
            this.dark = dark;
        }

        public String displayName() {
            return displayName;
        }

        public boolean isDark() {
            return dark;
        }

        String userAgentStylesheet() {
            return style.getUserAgentStylesheet();
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(ThemeManager.class);
    private static final String SETTINGS_KEY = "ui.theme";
    private static final Theme DEFAULT_THEME = Theme.NORD_DARK;

    private final SettingsStorage settingsStorage;
    private final ObjectProperty<Theme> currentTheme;
    private Consumer<Boolean> darkModeConsumer = isDark -> {};

    public ThemeManager(SettingsStorage settingsStorage) {
        this.settingsStorage = settingsStorage;
        this.currentTheme = new SimpleObjectProperty<>(resolveSavedTheme());
    }

    public ReadOnlyObjectProperty<Theme> currentThemeProperty() {
        return currentTheme;
    }

    public Theme getCurrentTheme() {
        return currentTheme.get();
    }

    public void applyUserAgentStylesheet() {
        Application.setUserAgentStylesheet(currentTheme.get().userAgentStylesheet());
    }

    public void apply(Theme theme) {
        if (theme == null || theme == currentTheme.get()) {
            return;
        }
        Application.setUserAgentStylesheet(theme.userAgentStylesheet());
        currentTheme.set(theme);
        darkModeConsumer.accept(theme.isDark());
        settingsStorage.save(SETTINGS_KEY, theme.name());
        logger.debug("Applied theme {}", theme);
    }

    public void setDarkModeConsumer(Consumer<Boolean> darkModeConsumer) {
        this.darkModeConsumer = darkModeConsumer != null ? darkModeConsumer : isDark -> {};
        this.darkModeConsumer.accept(currentTheme.get().isDark());
    }

    private Theme resolveSavedTheme() {
        return settingsStorage.read(SETTINGS_KEY, String.class)
                .map(this::parseTheme)
                .orElse(DEFAULT_THEME);
    }

    private Theme parseTheme(String name) {
        try {
            return Theme.valueOf(name);
        } catch (IllegalArgumentException e) {
            logger.warn("Unknown saved theme '{}', falling back to {}", name, DEFAULT_THEME);
            return DEFAULT_THEME;
        }
    }
}
