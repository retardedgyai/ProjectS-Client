package io.github.gyai.projects.client.ui.icon;

import io.github.gyai.projects.client.ui.theme.ProjectSThemeTokens;

/** Pure Theme-token to icon-color resolution. */
public final class ProjectSIconTint {
    private ProjectSIconTint() { }

    public static int resolve(
            ProjectSIconColorRole role,
            ProjectSIconState state,
            ProjectSThemeTokens tokens
    ) {
        if (state == ProjectSIconState.DISABLED
                || role == ProjectSIconColorRole.DISABLED) return tokens.textDisabled();
        if (state == ProjectSIconState.SELECTED) return tokens.accentPrimary();
        if (state == ProjectSIconState.FOCUSED) return tokens.accentPrimaryHover();
        if (state == ProjectSIconState.HOVERED
                && (role == ProjectSIconColorRole.DEFAULT
                || role == ProjectSIconColorRole.SECONDARY)) {
            return tokens.textPrimary();
        }
        return switch (role) {
            case DEFAULT, SECONDARY -> tokens.textSecondary();
            case PRIMARY -> tokens.textPrimary();
            case MUTED -> tokens.textMuted();
            case DISABLED -> tokens.textDisabled();
            case SUCCESS -> tokens.success();
            case WARNING -> tokens.warning();
            case DANGER -> tokens.danger();
            case INFO -> tokens.info();
        };
    }
}
