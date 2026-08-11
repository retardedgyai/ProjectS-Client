package io.github.gyai.projects.ui.runtime.component;

import io.github.gyai.projects.ui.runtime.TextStyle;
import io.github.gyai.projects.ui.runtime.UiAccessibilityMetadata;
import io.github.gyai.projects.ui.runtime.UiAccessibilityRole;
import io.github.gyai.projects.ui.runtime.UiColor;
import io.github.gyai.projects.ui.runtime.UiColorRole;
import io.github.gyai.projects.ui.runtime.UiDrawList;
import io.github.gyai.projects.ui.runtime.UiEvent;
import io.github.gyai.projects.ui.runtime.UiKeyAction;
import io.github.gyai.projects.ui.runtime.UiKeyEvent;
import io.github.gyai.projects.ui.runtime.UiMaterialTier;
import io.github.gyai.projects.ui.runtime.UiPoint;
import io.github.gyai.projects.ui.runtime.UiPointerEvent;
import io.github.gyai.projects.ui.runtime.UiRect;
import io.github.gyai.projects.ui.runtime.UiTheme;

import java.util.function.Consumer;

/** Software-style boolean control with pointer, keyboard, focus, and accessibility semantics. */
public final class Toggle extends GlassComponent {
    private String label;
    private Consumer<Boolean> onChanged;

    public Toggle(String id, UiRect bounds, String label, boolean selected,
                  Consumer<Boolean> onChanged) {
        super(id, bounds, UiMaterialTier.ACCENT_GLASS, Math.min(10, bounds.height() / 2));
        if (label == null || onChanged == null) throw new IllegalArgumentException("label/onChanged");
        this.label = label;
        this.onChanged = onChanged;
        setFocusable(true);
        setSelectedSilently(selected);
        setAccessibility(UiAccessibilityMetadata.of(UiAccessibilityRole.TOGGLE, label,
                Boolean.toString(selected)).withSelected(selected));
    }

    public Toggle(String id, UiRect bounds, String label, boolean selected) {
        this(id, bounds, label, selected, ignored -> { });
    }

    public Toggle(String id, UiRect bounds, String label) {
        this(id, bounds, label, false);
    }

    public String label() { return label; }
    public boolean value() { return selected(); }
    public Consumer<Boolean> onChanged() { return onChanged; }

    public Toggle setLabel(String next) {
        label = next == null ? "" : next;
        setAccessibility(accessibility().withLabel(label));
        return this;
    }

    public Toggle setOnChanged(Consumer<Boolean> next) {
        if (next == null) throw new NullPointerException("onChanged");
        onChanged = next;
        return this;
    }

    public Toggle setSelectedSilently(boolean next) {
        super.setSelected(next);
        setAccessibility(accessibility().withValue(Boolean.toString(next)).withSelected(next));
        return this;
    }

    @Override
    public Toggle setSelected(boolean next) {
        return setValue(next);
    }

    public Toggle setValue(boolean next) {
        boolean changed = selected() != next;
        setSelectedSilently(next);
        if (changed) onChanged.accept(next);
        return this;
    }

    public boolean toggle() {
        if (!isEffectivelyVisible() || !isEffectivelyEnabled()) return false;
        setValue(!selected());
        return true;
    }

    @Override
    public boolean handleEvent(UiEvent event) {
        if (event instanceof UiPointerEvent pointer) {
            return switch (pointer.type()) {
                case MOVE -> pointerMove(pointer.pointerId(), pointer.position());
                case ENTER -> pointerEnter(pointer.pointerId());
                case LEAVE -> pointerLeave(pointer.pointerId());
                case DOWN -> pointerDown(pointer.pointerId(), pointer.position());
                case UP -> pointerUpAndToggle(pointer.pointerId(), pointer.position());
                case CANCEL -> pointerCancel(pointer.pointerId());
            };
        }
        if (event instanceof UiKeyEvent key) {
            if (key.key() != UiKeyEvent.KEY_ENTER && key.key() != UiKeyEvent.KEY_SPACE) return false;
            if (key.action() == UiKeyAction.DOWN) return keyDown(key.key());
            boolean activate = keyUp(key.key());
            if (activate) toggle();
            return activate;
        }
        return false;
    }

    private boolean pointerUpAndToggle(int pointerId, UiPoint position) {
        boolean activate = pointerPressed() && isEffectivelyEnabled()
                && globalBounds().contains(position);
        boolean handled = pointerUp(pointerId, position);
        if (activate) toggle();
        return handled;
    }

    @Override
    protected void appendSelf(UiDrawList drawList, UiTheme theme, UiRect globalBounds, UiRect clip) {
        drawMaterial(drawList, theme, globalBounds, state());
        double trackHeight = Math.max(14, Math.min(20, globalBounds.height() - 8));
        double trackWidth = Math.max(28, trackHeight * 1.75);
        UiRect track = new UiRect(globalBounds.x() + 8,
                globalBounds.y() + (globalBounds.height() - trackHeight) / 2,
                trackWidth, trackHeight);
        UiColor trackColor = selected()
                ? theme.color(UiColorRole.ACCENT)
                : theme.color(UiColorRole.BORDER).withAlpha(210);
        if (state() == ComponentState.DISABLED) trackColor = theme.color(UiColorRole.DISABLED);
        drawList.roundedSurface(track, trackHeight / 2, trackColor, UiMaterialTier.GLASS_SOLID);
        double knobSize = Math.max(10, trackHeight - 4);
        double knobX = selected() ? track.right() - knobSize - 2 : track.x() + 2;
        UiColor knobColor = selected()
                ? theme.color(UiColorRole.TEXT_PRIMARY)
                : theme.color(UiColorRole.TEXT_SECONDARY);
        if (state() == ComponentState.DISABLED) knobColor = theme.color(UiColorRole.DISABLED);
        drawList.roundedSurface(new UiRect(knobX, track.y() + 2, knobSize, knobSize), knobSize / 2,
                knobColor, UiMaterialTier.GLASS_SOLID);
        drawText(drawList, theme,
                new UiPoint(track.right() + 8,
                        globalBounds.y() + Math.max(1, (globalBounds.height() - 14) / 2)),
                label, TextStyle.body());
    }
}
