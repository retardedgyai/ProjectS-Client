package io.github.gyai.projects.ui.runtime.component;

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

import java.util.function.DoubleConsumer;

/** Bounded, step-aware slider with deterministic drag cancellation and keyboard control. */
public final class Slider extends GlassComponent {
    public static final int KEY_RIGHT = 262;
    public static final int KEY_LEFT = 263;
    public static final int KEY_DOWN = 264;
    public static final int KEY_UP = 265;
    public static final int KEY_PAGE_UP = 266;
    public static final int KEY_PAGE_DOWN = 267;
    public static final int KEY_HOME = 268;
    public static final int KEY_END = 269;

    private double minimum;
    private double maximum;
    private double step;
    private double value;
    private SliderOrientation orientation;
    private DoubleConsumer onChanged;
    private boolean dragging;
    private double dragStartValue;

    public Slider(String id, UiRect bounds, double minimum, double maximum, double step,
                  double value, SliderOrientation orientation, DoubleConsumer onChanged) {
        super(id, bounds, UiMaterialTier.GLASS_PANEL, Math.min(8, Math.min(bounds.width(), bounds.height()) / 2));
        validateRange(minimum, maximum, step);
        if (orientation == null || onChanged == null) throw new IllegalArgumentException("orientation/onChanged");
        this.minimum = minimum;
        this.maximum = maximum;
        this.step = step;
        this.orientation = orientation;
        this.onChanged = onChanged;
        this.value = snap(clamp(value));
        setFocusable(true);
        setAccessibility(UiAccessibilityMetadata.of(UiAccessibilityRole.SLIDER, id,
                valueString()).withSelected(false));
    }

    public Slider(String id, UiRect bounds, double minimum, double maximum, double step,
                  double value, DoubleConsumer onChanged) {
        this(id, bounds, minimum, maximum, step, value, SliderOrientation.HORIZONTAL, onChanged);
    }

    public Slider(String id, UiRect bounds, double minimum, double maximum, double value) {
        this(id, bounds, minimum, maximum, (maximum - minimum) / 100, value, ignored -> { });
    }

    public Slider(String id, UiRect bounds, double minimum, double maximum, double value,
                  DoubleConsumer onChanged) {
        this(id, bounds, minimum, maximum, (maximum - minimum) / 100, value, onChanged);
    }

    public double minimum() { return minimum; }
    public double maximum() { return maximum; }
    public double step() { return step; }
    public double value() { return value; }
    public double fraction() { return (value - minimum) / (maximum - minimum); }
    public SliderOrientation orientation() { return orientation; }
    public boolean dragging() { return dragging; }
    public DoubleConsumer onChanged() { return onChanged; }

    public Slider setOnChanged(DoubleConsumer next) {
        if (next == null) throw new NullPointerException("onChanged");
        onChanged = next;
        return this;
    }

    public Slider setOrientation(SliderOrientation next) {
        if (next == null) throw new NullPointerException("orientation");
        orientation = next;
        return this;
    }

    public Slider setRange(double nextMinimum, double nextMaximum, double nextStep) {
        validateRange(nextMinimum, nextMaximum, nextStep);
        minimum = nextMinimum;
        maximum = nextMaximum;
        step = nextStep;
        setValue(value);
        return this;
    }

    public Slider setValue(double next) { return setValueInternal(next, true); }

    public Slider setValueSilently(double next) { return setValueInternal(next, false); }

    private Slider setValueInternal(double next, boolean notify) {
        if (!Double.isFinite(next)) throw new IllegalArgumentException("value");
        double normalized = snap(clamp(next));
        if (Double.compare(value, normalized) == 0) return this;
        value = normalized;
        setAccessibility(accessibility().withValue(valueString()));
        if (notify) onChanged.accept(value);
        return this;
    }

    public Slider setBoundsAndValue(double nextMinimum, double nextMaximum, double nextStep,
                                    double nextValue) {
        setRange(nextMinimum, nextMaximum, nextStep);
        return setValue(nextValue);
    }

    public boolean cancelDrag() {
        if (!dragging) return false;
        dragging = false;
        setValue(dragStartValue);
        clearPointerState();
        return true;
    }

    @Override
    public boolean handleEvent(UiEvent event) {
        if (event instanceof UiPointerEvent pointer) {
            return switch (pointer.type()) {
                case MOVE -> pointerMoveAndUpdate(pointer.pointerId(), pointer.position());
                case ENTER -> pointerEnter(pointer.pointerId());
                case LEAVE -> pointerLeave(pointer.pointerId());
                case DOWN -> pointerDownAndStart(pointer.pointerId(), pointer.position());
                case UP -> pointerUpAndCommit(pointer.pointerId(), pointer.position());
                case CANCEL -> pointerCancelAndRestore(pointer.pointerId());
            };
        }
        if (event instanceof UiKeyEvent key) {
            if (!isEffectivelyVisible() || !isEffectivelyEnabled() || !focused()) return false;
            if (key.action() == UiKeyAction.UP) return false;
            if (key.key() == UiKeyEvent.KEY_ESCAPE) return cancelDrag();
            return switch (key.key()) {
                case KEY_LEFT, KEY_DOWN, KEY_PAGE_DOWN -> adjustBy(-keyboardDelta(key.key()));
                case KEY_RIGHT, KEY_UP, KEY_PAGE_UP -> adjustBy(keyboardDelta(key.key()));
                case KEY_HOME -> setValue(minimum) != null;
                case KEY_END -> setValue(maximum) != null;
                default -> false;
            };
        }
        return false;
    }

    private boolean pointerDownAndStart(int pointerId, UiPoint position) {
        if (!pointerDown(pointerId, position)) return false;
        dragging = true;
        dragStartValue = value;
        updateFromPointer(position);
        return true;
    }

    private boolean pointerMoveAndUpdate(int pointerId, UiPoint position) {
        if (dragging && (activePointerId() < 0 || activePointerId() == pointerId)) {
            updateFromPointer(position);
            return true;
        }
        return pointerMove(pointerId, position);
    }

    private boolean pointerUpAndCommit(int pointerId, UiPoint position) {
        if (!dragging) return pointerUp(pointerId, position);
        if (activePointerId() >= 0 && activePointerId() != pointerId) return false;
        updateFromPointer(position);
        dragging = false;
        return pointerUp(pointerId, position);
    }

    private boolean pointerCancelAndRestore(int pointerId) {
        if (activePointerId() >= 0 && activePointerId() != pointerId) return false;
        return cancelDrag() || pointerCancel(pointerId);
    }

    private boolean adjustBy(double delta) {
        double before = value;
        setValue(value + delta);
        return Double.compare(before, value) != 0 || delta != 0;
    }

    private double keyboardDelta(int key) {
        return (key == KEY_PAGE_UP || key == KEY_PAGE_DOWN)
                ? Math.max(step, (maximum - minimum) / 10) : step;
    }

    private void updateFromPointer(UiPoint position) {
        UiRect bounds = globalBounds();
        double fraction;
        if (orientation == SliderOrientation.HORIZONTAL) {
            fraction = (position.x() - bounds.x()) / Math.max(1, bounds.width());
        } else {
            fraction = 1 - (position.y() - bounds.y()) / Math.max(1, bounds.height());
        }
        setValue(minimum + Math.clamp(fraction, 0, 1) * (maximum - minimum));
    }

    private String valueString() { return Double.toString(value); }

    private double clamp(double next) { return Math.clamp(next, minimum, maximum); }

    private double snap(double next) {
        double snapped = minimum + Math.round((next - minimum) / step) * step;
        return clamp(snapped);
    }

    @Override
    protected void cancelInteractionSources() {
        if (dragging) {
            dragging = false;
            setValue(dragStartValue);
        }
        super.cancelInteractionSources();
    }

    @Override
    protected void appendSelf(UiDrawList drawList, UiTheme theme, UiRect globalBounds, UiRect clip) {
        drawMaterial(drawList, theme, globalBounds, state());
        UiColor trackColor = theme.color(UiColorRole.BORDER).withAlpha(210);
        UiColor fillColor = state() == ComponentState.DISABLED
                ? theme.color(UiColorRole.DISABLED) : theme.color(UiColorRole.ACCENT);
        if (orientation == SliderOrientation.HORIZONTAL) {
            double y = globalBounds.y() + globalBounds.height() / 2;
            double trackHeight = Math.min(6, Math.max(3, globalBounds.height() / 4));
            UiRect track = new UiRect(globalBounds.x() + 8, y - trackHeight / 2,
                    Math.max(0, globalBounds.width() - 16), trackHeight);
            drawList.roundedSurface(track, trackHeight / 2, trackColor, UiMaterialTier.GLASS_SOLID);
            drawList.roundedSurface(new UiRect(track.x(), track.y(), track.width() * fraction(), track.height()),
                    trackHeight / 2, fillColor, UiMaterialTier.ACCENT_GLASS);
            double knob = Math.max(8, Math.min(16, globalBounds.height() - 6));
            double knobX = track.x() + (track.width() - knob) * fraction();
            drawList.roundedSurface(new UiRect(knobX, y - knob / 2, knob, knob), knob / 2,
                    state() == ComponentState.DISABLED ? theme.color(UiColorRole.DISABLED)
                            : theme.color(UiColorRole.TEXT_PRIMARY), UiMaterialTier.GLASS_SOLID);
        } else {
            double x = globalBounds.x() + globalBounds.width() / 2;
            double trackWidth = Math.min(6, Math.max(3, globalBounds.width() / 4));
            UiRect track = new UiRect(x - trackWidth / 2, globalBounds.y() + 8,
                    trackWidth, Math.max(0, globalBounds.height() - 16));
            drawList.roundedSurface(track, trackWidth / 2, trackColor, UiMaterialTier.GLASS_SOLID);
            double filled = track.height() * fraction();
            drawList.roundedSurface(new UiRect(track.x(), track.bottom() - filled, track.width(), filled),
                    trackWidth / 2, fillColor, UiMaterialTier.ACCENT_GLASS);
            double knob = Math.max(8, Math.min(16, globalBounds.width() - 6));
            double knobY = track.bottom() - (track.height() - knob) * fraction() - knob;
            drawList.roundedSurface(new UiRect(x - knob / 2, knobY, knob, knob), knob / 2,
                    state() == ComponentState.DISABLED ? theme.color(UiColorRole.DISABLED)
                            : theme.color(UiColorRole.TEXT_PRIMARY), UiMaterialTier.GLASS_SOLID);
        }
    }

    private static void validateRange(double minimum, double maximum, double step) {
        if (!Double.isFinite(minimum) || !Double.isFinite(maximum) || maximum <= minimum
                || !Double.isFinite(step) || step <= 0) throw new IllegalArgumentException("range/step");
    }
}
