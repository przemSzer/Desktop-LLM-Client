package dev.local.ai.ui.about;

/**
 * One open-source library shown in the About dialog.
 *
 * @param copyright attribution text when the license requires a specific notice; otherwise empty
 */
public record OssLibrary(
        String name,
        String version,
        String license,
        String licenseUrl,
        String projectUrl,
        String copyright) {
}
