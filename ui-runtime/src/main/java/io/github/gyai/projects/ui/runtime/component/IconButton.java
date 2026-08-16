package io.github.gyai.projects.ui.runtime.component;

import io.github.gyai.projects.ui.runtime.IconSpec;
import io.github.gyai.projects.ui.runtime.UiButton;
import io.github.gyai.projects.ui.runtime.UiButtonState;
import io.github.gyai.projects.ui.runtime.UiColor;
import io.github.gyai.projects.ui.runtime.UiColorRole;
import io.github.gyai.projects.ui.runtime.UiDrawList;
import io.github.gyai.projects.ui.runtime.UiMaterialTier;
import io.github.gyai.projects.ui.runtime.UiPoint;
import io.github.gyai.projects.ui.runtime.UiRect;
import io.github.gyai.projects.ui.runtime.UiTheme;
import io.github.gyai.projects.ui.runtime.icon.IconState;
import io.github.gyai.projects.ui.runtime.icon.IconStyle;

/** Icon-first button; icon identity stays renderer-neutral through IconSpec. */
public final class IconButton extends UiButton {
    private IconSpec icon;

    public IconButton(String id, UiRect bounds, IconSpec icon, String accessibleLabel,
                      Runnable action) {
        super(id, bounds, requireLabel(accessibleLabel), action);
        setIcon(icon);
    }

    public IconButton(String id, UiRect bounds, IconSpec icon, String accessibleLabel) {
        this(id, bounds, icon, accessibleLabel, () -> { });
    }

    public IconButton(String id, UiRect bounds, IconSpec icon, Runnable action) {
        this(id, bounds, icon, icon == null ? "Icon button" : icon.key().id(), action);
    }

    public IconSpec icon() { return icon; }

    public IconButton setIcon(IconSpec next) {
        if (next == null) throw new NullPointerException("icon");
        icon = next;
        return this;
    }

    @Override
    public IconButton setMaterialTier(UiMaterialTier next) {
        super.setMaterialTier(next);
        return this;
    }

    @Override
    public IconButton setRadius(double next) {
        super.setRadius(next);
        return this;
    }

    @Override
    protected void appendSelf(UiDrawList drawList, UiTheme theme, UiRect globalBounds, UiRect clip) {
        UiButtonState state = state();
        drawButtonFrame(drawList, theme, globalBounds, state);
        double size = Math.max(8, Math.min(globalBounds.width(), globalBounds.height()) - 10);
        UiRect iconBounds = new UiRect(
                globalBounds.x() + (globalBounds.width() - size) / 2,
                globalBounds.y() + (globalBounds.height() - size) / 2,
                size, size);
        IconState iconState = switch (state) {
            case DISABLED -> IconState.DISABLED;
            case PRESSED -> IconState.PRESSED;
            case SELECTED -> IconState.SELECTED;
            case HOVER -> IconState.HOVERED;
            case FOCUSED -> IconState.FOCUSED;
            case NORMAL -> IconState.NORMAL;
        };
        IconStyle style = IconStyle.resolve(icon, iconState, theme);
        drawList.icon(iconBounds, icon, style.resolvedTint(), iconState);
    }

    private static String requireLabel(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("accessibleLabel");
        return value;
    }
}
