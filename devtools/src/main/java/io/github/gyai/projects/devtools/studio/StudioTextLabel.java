package io.github.gyai.projects.devtools.studio;

import io.github.gyai.projects.ui.runtime.TextStyle;
import io.github.gyai.projects.ui.runtime.UiAccessibilityMetadata;
import io.github.gyai.projects.ui.runtime.UiAccessibilityRole;
import io.github.gyai.projects.ui.runtime.UiColorRole;
import io.github.gyai.projects.ui.runtime.UiDrawList;
import io.github.gyai.projects.ui.runtime.UiNode;
import io.github.gyai.projects.ui.runtime.UiPoint;
import io.github.gyai.projects.ui.runtime.UiRect;
import io.github.gyai.projects.ui.runtime.UiTheme;

import java.util.Objects;

/** Small renderer-neutral text node shared by the Studio chrome compositions. */
public final class StudioTextLabel extends UiNode {
    private String value;
    private TextStyle style;

    public StudioTextLabel(String id, UiRect bounds, String value, TextStyle style) {
        super(id, bounds);
        this.value = requireText(value, "value");
        this.style = Objects.requireNonNull(style, "style");
        setHitTestable(false);
        setAccessibility(UiAccessibilityMetadata.of(UiAccessibilityRole.LABEL, value));
    }

    public String value() {
        return value;
    }

    public TextStyle style() {
        return style;
    }

    public StudioTextLabel setValue(String nextValue) {
        value = requireText(nextValue, "value");
        setAccessibility(accessibility().withLabel(value));
        return this;
    }

    public StudioTextLabel setStyle(TextStyle nextStyle) {
        style = Objects.requireNonNull(nextStyle, "style");
        return this;
    }

    @Override
    protected void appendSelf(UiDrawList drawList, UiTheme theme,
                              UiRect globalBounds, UiRect clip) {
        UiColorRole role = style.colorRole();
        drawList.text(new UiPoint(globalBounds.x(), globalBounds.y()), value, style,
                theme.color(role));
    }

    private static String requireText(String text, String name) {
        if (text == null || text.isBlank()) throw new IllegalArgumentException(name);
        return text;
    }
}
