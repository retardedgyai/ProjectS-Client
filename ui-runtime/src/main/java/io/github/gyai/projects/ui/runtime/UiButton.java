package io.github.gyai.projects.ui.runtime;

/** Pure button state machine; a Minecraft Widget is deliberately not involved. */
public class UiButton extends UiNode {
    private String label;
    private final Runnable action;
    private UiMaterialTier materialTier = UiMaterialTier.ACCENT_GLASS;
    private double radius = 8;
    private boolean hovered;
    private boolean pointerPressed;
    private int keyboardPressedKey = -1;
    private boolean presentationPressed;
    private boolean selected;
    private long timelineNow;
    private UiButtonState transitionedState = UiButtonState.NORMAL;
    private double transitionFrom;
    private long transitionStartedAt;
    private boolean transitionActive;
    private static final long TRANSITION_DURATION = 180;

    public UiButton(String id, UiRect bounds, String label) { this(id, bounds, label, () -> { }); }

    public UiButton(String id, UiRect bounds, String label, Runnable action) {
        super(id, bounds);
        if (label == null || action == null) throw new IllegalArgumentException("label/action");
        this.label = label;
        this.action = action;
        setFocusable(true);
        setAccessibility(UiAccessibilityMetadata.of(UiAccessibilityRole.BUTTON, label));
    }

    public String label() { return label; }
    public UiButton setLabel(String next) {
        label = next == null ? "" : next;
        setAccessibility(accessibility().withLabel(label));
        return this;
    }
    public UiMaterialTier materialTier() { return materialTier; }
    public UiButton setMaterialTier(UiMaterialTier next) {
        if (next == null) throw new NullPointerException("materialTier");
        materialTier = next;
        return this;
    }

    public UiButton setRadius(double next) {
        if (!Double.isFinite(next) || next < 0) throw new IllegalArgumentException("radius");
        radius = next;
        return this;
    }
    public boolean hovered() { return hovered; }
    public boolean pressed() { return presentationPressed || pointerPressed || keyboardPressedKey >= 0; }

    public UiButton setHovered(boolean next) { hovered = next; return this; }
    /** Presentation-only override used by the UI Kit gallery; input sources remain separate. */
    public UiButton setPressed(boolean next) { presentationPressed = next; return this; }

    public boolean selected() { return selected; }

    public UiButton setSelected(boolean next) {
        selected = next;
        setAccessibility(accessibility().withSelected(next));
        return this;
    }

    public UiButtonState state() {
        if (!isEffectivelyEnabled()) return UiButtonState.DISABLED;
        if (pressed()) return UiButtonState.PRESSED;
        if (selected) return UiButtonState.SELECTED;
        if (hovered) return UiButtonState.HOVER;
        if (focused()) return UiButtonState.FOCUSED;
        return UiButtonState.NORMAL;
    }

    /** Explicit timeline hook for deterministic hover/selection animation sampling. */
    public final void advanceTo(long now) {
        if (now < timelineNow) throw new IllegalArgumentException("timeline cannot move backwards");
        timelineNow = now;
        ensureTransition();
    }

    public final long timelineNow() { return timelineNow; }

    public final double transitionProgress(long now) {
        if (now < timelineNow) throw new IllegalArgumentException("timeline cannot move backwards");
        ensureTransition();
        if (!transitionActive) return 1;
        if (TRANSITION_DURATION == 0 || now >= transitionStartedAt + TRANSITION_DURATION) return 1;
        if (now <= transitionStartedAt) return 0;
        return (double) (now - transitionStartedAt) / TRANSITION_DURATION;
    }

    public final double transitionValue(long now) {
        double linear = transitionProgress(now);
        double eased = linear * linear * (3 - 2 * linear);
        return transitionFrom + (1 - transitionFrom) * eased;
    }

    private void ensureTransition() {
        UiButtonState next = state();
        if (next == transitionedState) return;
        double previous = transitionActive ? transitionValueWithoutRefresh(timelineNow) : 0;
        transitionedState = next;
        transitionFrom = previous;
        transitionStartedAt = timelineNow;
        transitionActive = true;
    }

    private double transitionValueWithoutRefresh(long now) {
        if (now >= transitionStartedAt + TRANSITION_DURATION) return 1;
        if (now <= transitionStartedAt || TRANSITION_DURATION == 0) return transitionFrom;
        double linear = (double) (now - transitionStartedAt) / TRANSITION_DURATION;
        double eased = linear * linear * (3 - 2 * linear);
        return transitionFrom + (1 - transitionFrom) * eased;
    }

    @Override
    public boolean handleEvent(UiEvent event) {
        if (event instanceof UiPointerEvent pointer) {
            return switch (pointer.type()) {
                case MOVE -> pointerMove(pointer.position());
                case ENTER -> pointerEnter();
                case LEAVE -> pointerLeave();
                case DOWN -> pointerDown(pointer.position());
                case UP -> pointerUp(pointer.position());
                case CANCEL -> pointerCancel();
            };
        }
        if (event instanceof UiKeyEvent key) {
            if (!isEffectivelyVisible() || !isEffectivelyEnabled() || !focused()) return false;
            if (key.key() != UiKeyEvent.KEY_ENTER && key.key() != UiKeyEvent.KEY_SPACE) return false;
            if (key.action() == UiKeyAction.DOWN) {
                if (keyboardPressedKey < 0) keyboardPressedKey = key.key();
                return keyboardPressedKey == key.key();
            }
            boolean activate = keyboardPressedKey == key.key()
                    && focused() && isEffectivelyVisible() && isEffectivelyEnabled();
            if (keyboardPressedKey == key.key()) keyboardPressedKey = -1;
            if (activate) { activate(); return true; }
        }
        return false;
    }

    public boolean pointerMove(UiPoint position) {
        if (!isEffectivelyVisible() || !isEffectivelyEnabled()) return false;
        hovered = globalBounds().contains(position);
        return true;
    }

    public boolean pointerEnter() {
        if (!isEffectivelyVisible() || !isEffectivelyEnabled()) return false;
        hovered = true;
        return true;
    }

    public boolean pointerLeave() {
        hovered = false;
        return true;
    }

    public boolean pointerDown(UiPoint position) {
        if (!isEffectivelyVisible() || !isEffectivelyEnabled() || !globalBounds().contains(position)) return false;
        hovered = true;
        pointerPressed = true;
        return true;
    }

    public boolean pointerUp(UiPoint position) {
        if (!isEffectivelyVisible() || !isEffectivelyEnabled()) { pointerPressed = false; return false; }
        boolean activate = pointerPressed && globalBounds().contains(position);
        pointerPressed = false;
        hovered = globalBounds().contains(position);
        if (activate) activate();
        return true;
    }

    public boolean pointerCancel() {
        pointerPressed = false;
        hovered = false;
        return true;
    }

    @Override
    public UiButton setVisible(boolean next) {
        super.setVisible(next);
        if (!next) cancelInteractionSources();
        return this;
    }

    @Override
    public UiButton setEnabled(boolean next) {
        super.setEnabled(next);
        if (!next) cancelInteractionSources();
        return this;
    }

    @Override
    public UiButton setFocused(boolean next) {
        super.setFocused(next);
        if (!next) cancelInteractionSources();
        return this;
    }

    @Override
    protected void onAncestorInteractionInvalidated() {
        cancelInteractionSources();
    }

    private void cancelInteractionSources() {
        pointerPressed = false;
        keyboardPressedKey = -1;
        presentationPressed = false;
        hovered = false;
    }

    @Override
    protected void appendSelf(UiDrawList drawList, UiTheme theme, UiRect globalBounds, UiRect clip) {
        drawButtonFrame(drawList, theme, globalBounds, state());
        drawButtonLabel(drawList, theme, globalBounds, label);
    }

    /** Hook for component subclasses such as IconButton; the action remains private by design. */
    protected final void activate() { action.run(); }

    protected final UiMaterialStyle resolvedStyle(UiTheme theme) {
        return UiMaterialStyle.resolve(materialTier, theme);
    }

    protected final void drawButtonFrame(UiDrawList drawList, UiTheme theme,
                                         UiRect globalBounds, UiButtonState state) {
        UiMaterialStyle style = UiMaterialStyle.resolve(materialTier, theme);
        UiColor fill = switch (state) {
            case DISABLED -> theme.color(UiColorRole.DISABLED).withAlpha(style.fill().alpha());
            case PRESSED -> style.gradientBottom();
            case HOVER -> style.gradientTop();
            case NORMAL, FOCUSED, SELECTED -> style.fill();
        };
        UiColor border = state == UiButtonState.FOCUSED || state == UiButtonState.SELECTED
                ? theme.color(UiColorRole.ACCENT).withAlpha(235) : style.border();
        drawList.shadow(globalBounds.offset(0, style.shadowSpread()), style.shadowSpread(), style.shadow());
        drawList.roundedSurface(globalBounds, radius, fill, materialTier);
        drawList.gradient(globalBounds, radius, style.gradientTop(), style.gradientBottom());
        drawList.border(globalBounds, radius, style.borderWidth(), border);
        if (state == UiButtonState.FOCUSED) {
            drawList.border(globalBounds.inset(new UiInsets(1.5)), Math.max(0, radius - 1.5), 1,
                    theme.color(UiColorRole.ACCENT).withAlpha(160));
        }
    }

    protected final void drawButtonLabel(UiDrawList drawList, UiTheme theme,
                                         UiRect globalBounds, String text) {
        UiColor textColor = state() == UiButtonState.DISABLED
                ? theme.color(UiColorRole.DISABLED) : theme.color(UiColorRole.TEXT_PRIMARY);
        drawList.text(new UiPoint(globalBounds.x() + 8,
                        globalBounds.y() + Math.max(1, (globalBounds.height() - 14) / 2)),
                text == null ? "" : text, TextStyle.body(), textColor);
    }
}
