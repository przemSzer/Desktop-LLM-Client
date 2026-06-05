package dev.local.ai.ui.sidebar;

public record SidebarSettings(double width, boolean collapsed) {

    public static final double DEFAULT_WIDTH = 280;
    public static final double COLLAPSED_WIDTH = 48;
    public static final double MAX_WIDTH = 500;

    public static SidebarSettings defaults() {
        return new SidebarSettings(DEFAULT_WIDTH, false);
    }
}
