package io.github.gyai.projects.ui.runtime.component;

import io.github.gyai.projects.ui.runtime.UiAccessibilityMetadata;
import io.github.gyai.projects.ui.runtime.UiColor;
import io.github.gyai.projects.ui.runtime.UiColorRole;
import io.github.gyai.projects.ui.runtime.UiDrawList;
import io.github.gyai.projects.ui.runtime.UiEvent;
import io.github.gyai.projects.ui.runtime.UiInsets;
import io.github.gyai.projects.ui.runtime.UiMaterialStyle;
import io.github.gyai.projects.ui.runtime.UiMaterialTier;
import io.github.gyai.projects.ui.runtime.UiNode;
import io.github.gyai.projects.ui.runtime.UiPoint;
import io.github.gyai.projects.ui.runtime.UiPointerEvent;
import io.github.gyai.projects.ui.runtime.UiRect;
import io.github.gyai.projects.ui.runtime.UiTheme;
import io.github.gyai.projects.ui.runtime.UiButtonState;
import io.github.gyai.projects.ui.runtime.TextStyle;

/**
 * Platform-neutral interaction and material base for components that are not buttons. It is
 * intentionally small: UiInputRouter remains the owner of focus and pointer capture.
 */
public abstract class GlassComponent extends UiNode {
    private UiMaterialTier materialTier;
    private double radius;
    private boolean hovered;
    private boolean pointerPressed;
    private int activePointerId = -1;
    private int keyboardPressedKey = -1;
    private boolean presentationPressed;
    private boolean selected;
    private long timelineNow;
    private ComponentState transitionedState = ComponentState.NORMAL;
    private ComponentTransition transition = ComponentTransition.completed(0, 0);

    protected GlassComponent(String id, UiRect bounds, UiMaterialTier materialTier,
                             double radius) {
        super(id, bounds);
        if (materialTier == null || !Double.isFinite(radius) || radius < 0) {
            throw new IllegalArgumentException("material/radius");
        }
        this.materialTier = materialTier;
        this.radius = radius;
    }

    public UiMaterialTier materialTier() { return materialTier; }

    public GlassComponent setMaterialTier(UiMaterialTier next) {
        if (next == null) throw new NullPointerException("materialTier");
        materialTier = next;
        return this;
    }

    public double radius() { return radius; }

    public GlassComponent setRadius(double next) {
        if (!Double.isFinite(next) || next < 0) throw new IllegalArgumentException("radius");
        radius = next;
        return this;
    }

    public boolean hovered() { return hovered; }
    public boolean pressed() { return presentationPressed || pointerPressed || keyboardPressedKey >= 0; }
    public boolean pointerPressed() { return pointerPressed; }
    public int activePointerId() { return activePointerId; }
    public int keyboardPressedKey() { return keyboardPressedKey; }
    public boolean selected() { return selected; }

    public GlassComponent setHovered(boolean next) {
        hovered = next;
        return this;
    }

    /** Presentation-only override used by galleries and deterministic preview fixtures. */
    public GlassComponent setPressed(boolean next) {
        presentationPressed = next;
        return this;
    }

    public GlassComponent setSelected(boolean next) {
        selected = next;
        setAccessibility(accessibility().withSelected(next));
        return this;
    }

    public ComponentState state() {
        if (!isEffectivelyEnabled()) return ComponentState.DISABLED;
        if (pressed()) return ComponentState.PRESSED;
        if (selected) return ComponentState.SELECTED;
        if (hovered) return ComponentState.HOVER;
        if (focused()) return ComponentState.FOCUSED;
        return ComponentState.NORMAL;
    }

    /** Advances the supplied logical UI timeline and records state-transition ownership. */
    public final void advanceTo(long now) {
        if (now < timelineNow) throw new IllegalArgumentException("timeline cannot move backwards");
        timelineNow = now;
        ensureTransition();
    }

    public final long timelineNow() { return timelineNow; }

    public final ComponentTransition transition() {
        ensureTransition();
        return transition;
    }

    public final double transitionProgress(long now) {
        if (now < timelineNow) throw new IllegalArgumentException("timeline cannot move backwards");
        ensureTransition();
        return transition.progress(now);
    }

    public final double transitionValue(long now) {
        if (now < timelineNow) throw new IllegalArgumentException("timeline cannot move backwards");
        ensureTransition();
        return transition.sample(now);
    }

    private void ensureTransition() {
        ComponentState next = state();
        if (next == transitionedState) return;
        double previous = transition.sample(timelineNow);
        transitionedState = next;
        transition = new ComponentTransition(previous, 1, timelineNow, 180);
    }

    @Override
    public boolean handleEvent(UiEvent event) {
        if (event instanceof UiPointerEvent pointer) {
            return switch (pointer.type()) {
                case MOVE -> pointerMove(pointer.pointerId(), pointer.position());
                case ENTER -> pointerEnter(pointer.pointerId());
                case LEAVE -> pointerLeave(pointer.pointerId());
                case DOWN -> pointerDown(pointer.pointerId(), pointer.position());
                case UP -> pointerUp(pointer.pointerId(), pointer.position());
                case CANCEL -> pointerCancel(pointer.pointerId());
            };
        }
        return false;
    }

    public boolean pointerMove(UiPoint position) { return pointerMove(-1, position); }

    public boolean pointerMove(int pointerId, UiPoint position) {
        if (!isEffectivelyVisible() || !isEffectivelyEnabled()) return false;
        if (pointerPressed && activePointerId >= 0 && pointerId >= 0 && activePointerId != pointerId) {
            return false;
        }
        hovered = globalBounds().contains(position);
        return true;
    }

    public boolean pointerEnter() { return pointerEnter(-1); }

    public boolean pointerEnter(int pointerId) {
        if (!isEffectivelyVisible() || !isEffectivelyEnabled()) return false;
        hovered = true;
        return true;
    }

    public boolean pointerLeave() { return pointerLeave(-1); }

    public boolean pointerLeave(int pointerId) {
        hovered = false;
        return true;
    }

    public boolean pointerDown(UiPoint position) { return pointerDown(-1, position); }

    public boolean pointerDown(int pointerId, UiPoint position) {
        if (!isEffectivelyVisible() || !isEffectivelyEnabled()
                || !globalBounds().contains(position)) return false;
        if (pointerPressed && activePointerId >= 0 && pointerId >= 0 && activePointerId != pointerId) {
            return false;
        }
        hovered = true;
        pointerPressed = true;
        activePointerId = pointerId;
        return true;
    }

    public boolean pointerUp(UiPoint position) { return pointerUp(-1, position); }

    public boolean pointerUp(int pointerId, UiPoint position) {
        if (activePointerId >= 0 && pointerId >= 0 && pointerId != activePointerId) return false;
        if (!isEffectivelyVisible() || !isEffectivelyEnabled()) {
            clearPointerState();
            return false;
        }
        boolean inside = globalBounds().contains(position);
        clearPointerState();
        hovered = inside;
        return true;
    }

    public boolean pointerCancel() { return pointerCancel(-1); }

    public boolean pointerCancel(int pointerId) {
        if (activePointerId >= 0 && pointerId >= 0 && pointerId != activePointerId) return false;
        clearPointerState();
        hovered = false;
        return true;
    }

    protected final boolean keyDown(int key) {
        if (!isEffectivelyVisible() || !isEffectivelyEnabled() || !focused()) return false;
        if (key != io.github.gyai.projects.ui.runtime.UiKeyEvent.KEY_ENTER
                && key != io.github.gyai.projects.ui.runtime.UiKeyEvent.KEY_SPACE) return false;
        if (keyboardPressedKey < 0) keyboardPressedKey = key;
        return keyboardPressedKey == key;
    }

    protected final boolean keyUp(int key) {
        if (keyboardPressedKey != key) return false;
        keyboardPressedKey = -1;
        return true;
    }

    protected final void clearPointerState() {
        pointerPressed = false;
        activePointerId = -1;
    }

    protected final void clearKeyboardState() { keyboardPressedKey = -1; }

    protected final UiMaterialStyle resolvedStyle(UiTheme theme) {
        return UiMaterialStyle.resolve(materialTier, theme);
    }

    protected final void drawMaterial(UiDrawList drawList, UiTheme theme,
                                      UiRect globalBounds, ComponentState componentState) {
        UiMaterialStyle style = resolvedStyle(theme);
        UiColor fill = switch (componentState) {
            case DISABLED -> theme.color(UiColorRole.DISABLED).withAlpha(style.fill().alpha());
            case PRESSED -> style.gradientBottom();
            case HOVER -> style.gradientTop();
            case NORMAL, FOCUSED, SELECTED -> style.fill();
        };
        UiColor border = componentState == ComponentState.FOCUSED
                || componentState == ComponentState.SELECTED
                ? theme.color(UiColorRole.ACCENT).withAlpha(235) : style.border();
        drawList.shadow(globalBounds.offset(0, style.shadowSpread()), style.shadowSpread(), style.shadow());
        drawList.roundedSurface(globalBounds, radius, fill, materialTier);
        drawList.gradient(globalBounds, radius, style.gradientTop(), style.gradientBottom());
        drawList.border(globalBounds, radius, style.borderWidth(), border);
        if (componentState == ComponentState.FOCUSED) {
            drawList.border(globalBounds.inset(new UiInsets(1.5)), Math.max(0, radius - 1.5), 1,
                    theme.color(UiColorRole.ACCENT).withAlpha(160));
        }
    }

    protected final void drawText(UiDrawList drawList, UiTheme theme, UiPoint origin,
                                  String value, TextStyle style) {
        UiColor color = state() == ComponentState.DISABLED
                ? theme.color(UiColorRole.DISABLED) : theme.color(style.colorRole());
        drawList.text(origin, value == null ? "" : value, style, color);
    }

    @Override
    public GlassComponent setVisible(boolean next) {
        super.setVisible(next);
        if (!next) cancelInteractionSources();
        return this;
    }

    @Override
    public GlassComponent setEnabled(boolean next) {
        super.setEnabled(next);
        if (!next) cancelInteractionSources();
        return this;
    }

    @Override
    public GlassComponent setFocused(boolean next) {
        super.setFocused(next);
        if (!next) {
            clearKeyboardState();
            if (!pointerPressed) hovered = false;
        }
        return this;
    }

    @Override
    protected void onAncestorInteractionInvalidated() { cancelInteractionSources(); }

    protected void cancelInteractionSources() {
        clearPointerState();
        clearKeyboardState();
        presentationPressed = false;
        hovered = false;
    }
}
