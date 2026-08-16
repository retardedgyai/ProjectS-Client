package io.github.gyai.projects.ui.runtime.component;

import io.github.gyai.projects.ui.runtime.TextStyle;
import io.github.gyai.projects.ui.runtime.UiAccessibilityMetadata;
import io.github.gyai.projects.ui.runtime.UiAccessibilityRole;
import io.github.gyai.projects.ui.runtime.UiColor;
import io.github.gyai.projects.ui.runtime.UiColorRole;
import io.github.gyai.projects.ui.runtime.UiDrawList;
import io.github.gyai.projects.ui.runtime.UiEvent;
import io.github.gyai.projects.ui.runtime.UiInputRouter;
import io.github.gyai.projects.ui.runtime.UiKeyAction;
import io.github.gyai.projects.ui.runtime.UiKeyEvent;
import io.github.gyai.projects.ui.runtime.UiMaterialTier;
import io.github.gyai.projects.ui.runtime.UiPoint;
import io.github.gyai.projects.ui.runtime.UiPointerEvent;
import io.github.gyai.projects.ui.runtime.UiRect;
import io.github.gyai.projects.ui.runtime.UiTheme;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** Generic combo box with keyboard navigation and an explicit renderer-neutral popup region. */
public final class Dropdown<T> extends GlassComponent {
    public static final double DEFAULT_ROW_HEIGHT = 24;

    public record Option<T>(T value, String label, boolean enabled) {
        public Option {
            if (label == null || label.isBlank()) throw new IllegalArgumentException("option label");
        }

        public Option(T value, String label) { this(value, label, true); }
    }

    private UiRect collapsedBounds;
    private UiRect interactionBounds;
    private UiPoint contentOffset = UiPoint.zero();
    private List<Option<T>> options;
    private int selectedIndex;
    private int highlightedIndex;
    private boolean open;
    private boolean pointerDownWhenOpen;
    private boolean pointerDownOnHeader;
    private double rowHeight = DEFAULT_ROW_HEIGHT;
    private Consumer<T> onChanged;
    private UiInputRouter boundInput;
    private io.github.gyai.projects.ui.runtime.UiNode restoreFocus;

    public Dropdown(String id, UiRect bounds, List<Option<T>> options, int selectedIndex,
                    Consumer<T> onChanged) {
        super(id, bounds, UiMaterialTier.GLASS_PANEL, 8);
        if (options == null || options.isEmpty() || onChanged == null) {
            throw new IllegalArgumentException("options/onChanged");
        }
        this.collapsedBounds = bounds;
        this.options = List.copyOf(options);
        this.onChanged = onChanged;
        this.selectedIndex = requireEnabledIndex(selectedIndex, false);
        this.highlightedIndex = this.selectedIndex;
        setFocusable(true);
        setAccessibility(UiAccessibilityMetadata.of(UiAccessibilityRole.COMBO_BOX, id,
                selectedLabel()).withSelected(false));
    }

    public Dropdown(String id, UiRect bounds, List<Option<T>> options, int selectedIndex) {
        this(id, bounds, options, selectedIndex, ignored -> { });
    }

    public static <T> Option<T> option(T value, String label) { return new Option<>(value, label); }

    public List<Option<T>> options() { return options; }
    public int selectedIndex() { return selectedIndex; }
    public int highlightedIndex() { return highlightedIndex; }
    public T selectedValue() { return options.get(selectedIndex).value(); }
    public String selectedLabel() { return options.get(selectedIndex).label(); }
    public boolean open() { return open; }
    public double rowHeight() { return rowHeight; }
    public UiRect interactionBounds() { return interactionBounds; }
    public Consumer<T> onChanged() { return onChanged; }
    public io.github.gyai.projects.ui.runtime.UiNode restoreFocusTarget() { return restoreFocus; }

    public boolean open(UiInputRouter input) {
        if (input == null) throw new NullPointerException("input");
        bindInput(input);
        if (interactionBounds == null && parent() == input.tree().root()) {
            setInteractionBounds(input.tree().root().bounds());
        }
        boolean opened = openDropdown();
        if (opened) input.focus().requestFocus(this);
        return opened;
    }

    /** Binds the host router before focus moves so outside/Escape close can restore focus. */
    public void bindInput(UiInputRouter input) {
        if (input == null) throw new NullPointerException("input");
        if (boundInput == null) {
            boundInput = input;
            restoreFocus = input.focus().current().orElse(null);
        }
    }

    /** Synchronizes keyboard Escape dismissal with the router's centralized Escape handling. */
    public boolean syncWithInput() {
        if (!open || boundInput == null) return false;
        if (boundInput.focus().current().orElse(null) != this) {
            closeDropdown();
            return true;
        }
        return false;
    }

    @Override
    public Dropdown<T> setBounds(UiRect nextBounds) {
        super.setBounds(nextBounds);
        if (!open) collapsedBounds = nextBounds;
        return this;
    }

    @Override
    public ComponentState state() {
        if (!isEffectivelyEnabled()) return ComponentState.DISABLED;
        if (pressed()) return ComponentState.PRESSED;
        if (open) return ComponentState.SELECTED;
        if (hovered()) return ComponentState.HOVER;
        if (focused()) return ComponentState.FOCUSED;
        return ComponentState.NORMAL;
    }

    public Dropdown<T> setOnChanged(Consumer<T> next) {
        if (next == null) throw new NullPointerException("onChanged");
        onChanged = next;
        return this;
    }

    public Dropdown<T> setRowHeight(double next) {
        if (!Double.isFinite(next) || next < 16) throw new IllegalArgumentException("rowHeight");
        rowHeight = next;
        if (open) expandBounds();
        return this;
    }

    /** Optional parent-local overlay hit surface so outside clicks reach this dropdown node. */
    public Dropdown<T> setInteractionBounds(UiRect next) {
        interactionBounds = next;
        contentOffset = !open || next == null ? UiPoint.zero()
                : new UiPoint(collapsedBounds.x() - next.x(), collapsedBounds.y() - next.y());
        if (open) expandBounds();
        return this;
    }

    public Dropdown<T> setOptions(List<Option<T>> nextOptions, int nextSelectedIndex) {
        if (nextOptions == null || nextOptions.isEmpty()) throw new IllegalArgumentException("options");
        options = List.copyOf(nextOptions);
        selectedIndex = requireEnabledIndex(nextSelectedIndex, false);
        highlightedIndex = selectedIndex;
        updateAccessibility();
        if (open) expandBounds();
        return this;
    }

    public boolean openDropdown() {
        if (!isEffectivelyVisible() || !isEffectivelyEnabled()) return false;
        if (open) return true;
        open = true;
        highlightedIndex = selectedIndex;
        contentOffset = interactionBounds == null ? UiPoint.zero()
                : new UiPoint(collapsedBounds.x() - interactionBounds.x(),
                        collapsedBounds.y() - interactionBounds.y());
        expandBounds();
        return true;
    }

    public boolean closeDropdown() {
        if (!open) return false;
        UiInputRouter nextInput = boundInput;
        io.github.gyai.projects.ui.runtime.UiNode nextRestore = restoreFocus;
        open = false;
        setBounds(collapsedBounds);
        contentOffset = interactionBounds == null ? UiPoint.zero()
                : new UiPoint(collapsedBounds.x() - interactionBounds.x(),
                        collapsedBounds.y() - interactionBounds.y());
        highlightedIndex = selectedIndex;
        boundInput = null;
        restoreFocus = null;
        if (nextInput != null) {
            nextInput.focus().clearFocus();
            if (nextRestore != null && nextRestore.isEffectivelyInteractive(nextInput.tree().root())
                    && nextRestore.focusable()) nextInput.focus().requestFocus(nextRestore);
        }
        return true;
    }

    public boolean toggleDropdown() { return open ? closeDropdown() : openDropdown(); }

    public boolean selectIndex(int index) {
        if (index < 0 || index >= options.size() || !options.get(index).enabled()) return false;
        boolean changed = selectedIndex != index;
        selectedIndex = index;
        highlightedIndex = index;
        updateAccessibility();
        if (changed) onChanged.accept(options.get(index).value());
        closeDropdown();
        return true;
    }

    public boolean moveHighlight(int delta) {
        if (options.isEmpty()) return false;
        int index = highlightedIndex;
        int direction = Integer.compare(delta, 0);
        int remaining = Math.abs(delta);
        while (remaining-- > 0) {
            int next = index;
            for (int tries = 0; tries < options.size(); tries++) {
                next = Math.floorMod(next + direction, options.size());
                if (options.get(next).enabled()) break;
            }
            if (next == index || !options.get(next).enabled()) break;
            index = next;
        }
        boolean changed = highlightedIndex != index;
        highlightedIndex = index;
        return changed;
    }

    public boolean handleOutsidePointer(UiPoint position) {
        if (!open) return false;
        if (!headerBoundsGlobal().contains(position) && !popupBoundsGlobal().contains(position)) {
            closeDropdown();
            return true;
        }
        return false;
    }

    public UiRect headerBoundsGlobal() {
        UiRect global = globalBounds();
        return new UiRect(global.x() + contentOffset.x(), global.y() + contentOffset.y(),
                collapsedBounds.width(), collapsedBounds.height());
    }

    public UiRect popupBoundsGlobal() {
        UiRect global = globalBounds();
        return new UiRect(global.x() + contentOffset.x(),
                global.y() + contentOffset.y() + collapsedBounds.height(), collapsedBounds.width(),
                open ? options.size() * rowHeight : 0);
    }

    public UiRect optionBoundsGlobal(int index) {
        if (index < 0 || index >= options.size()) throw new IndexOutOfBoundsException("index");
        UiRect popup = popupBoundsGlobal();
        return new UiRect(popup.x(), popup.y() + index * rowHeight, popup.width(), rowHeight);
    }

    @Override
    public boolean handleEvent(UiEvent event) {
        if (event instanceof UiPointerEvent pointer) {
            return switch (pointer.type()) {
                case MOVE -> pointerMoveAndHighlight(pointer.pointerId(), pointer.position());
                case ENTER -> pointerEnter(pointer.pointerId());
                case LEAVE -> pointerLeave(pointer.pointerId());
                case DOWN -> pointerDownDropdown(pointer.pointerId(), pointer.position());
                case UP -> pointerUpDropdown(pointer.pointerId(), pointer.position());
                case CANCEL -> pointerCancelDropdown(pointer.pointerId());
            };
        }
        if (event instanceof UiKeyEvent key) {
            if (!isEffectivelyVisible() || !isEffectivelyEnabled() || !focused()) return false;
            if (key.action() == UiKeyAction.UP) return false;
            return switch (key.key()) {
                case UiKeyEvent.KEY_ESCAPE -> closeDropdown();
                case UiKeyEvent.KEY_ENTER, UiKeyEvent.KEY_SPACE -> open
                        ? selectIndex(highlightedIndex) : openDropdown();
                case Slider.KEY_DOWN, Slider.KEY_RIGHT -> {
                    if (!open) openDropdown();
                    yield moveHighlight(1);
                }
                case Slider.KEY_UP, Slider.KEY_LEFT -> {
                    if (!open) openDropdown();
                    yield moveHighlight(-1);
                }
                case Slider.KEY_HOME -> {
                    if (!open) openDropdown();
                    highlightedIndex = firstEnabled();
                    yield true;
                }
                case Slider.KEY_END -> {
                    if (!open) openDropdown();
                    highlightedIndex = lastEnabled();
                    yield true;
                }
                default -> false;
            };
        }
        return false;
    }

    private boolean pointerDownDropdown(int pointerId, UiPoint position) {
        if (!isEffectivelyVisible() || !isEffectivelyEnabled()) return false;
        if (!globalBounds().contains(position)) return false;
        if (open && !headerBoundsGlobal().contains(position) && !popupBoundsGlobal().contains(position)) {
            closeDropdown();
            return true;
        }
        pointerDownWhenOpen = open;
        pointerDownOnHeader = headerBoundsGlobal().contains(position);
        pointerDown(pointerId, position);
        if (!open) return openDropdown();
        int option = optionAt(position);
        if (option >= 0 && options.get(option).enabled()) highlightedIndex = option;
        return true;
    }

    private boolean pointerMoveAndHighlight(int pointerId, UiPoint position) {
        boolean handled = pointerMove(pointerId, position);
        if (open) {
            int option = optionAt(position);
            if (option >= 0 && options.get(option).enabled()) highlightedIndex = option;
        }
        return handled || open;
    }

    private boolean pointerUpDropdown(int pointerId, UiPoint position) {
        if (activePointerId() >= 0 && activePointerId() != pointerId) return false;
        int option = open ? optionAt(position) : -1;
        boolean inside = globalBounds().contains(position);
        boolean handled = pointerUp(pointerId, position);
        if (open && option >= 0 && options.get(option).enabled()) return selectIndex(option);
        if (open && pointerDownWhenOpen && pointerDownOnHeader && headerBoundsGlobal().contains(position)) {
            closeDropdown();
            return true;
        }
        if (!inside && open) closeDropdown();
        pointerDownWhenOpen = false;
        pointerDownOnHeader = false;
        return handled || option >= 0;
    }

    private boolean pointerCancelDropdown(int pointerId) {
        boolean wasOpen = open;
        boolean handled = pointerCancel(pointerId);
        if (wasOpen) closeDropdown();
        return handled || wasOpen;
    }

    private int optionAt(UiPoint position) {
        UiRect popup = popupBoundsGlobal();
        if (!popup.contains(position)) return -1;
        int index = (int) Math.floor((position.y() - popup.y()) / rowHeight);
        return index >= 0 && index < options.size() ? index : -1;
    }

    private void expandBounds() {
        if (interactionBounds != null) {
            setBounds(new UiRect(interactionBounds.x(), interactionBounds.y(), interactionBounds.width(),
                    interactionBounds.height()));
        } else {
            setBounds(new UiRect(collapsedBounds.x(), collapsedBounds.y(), collapsedBounds.width(),
                    collapsedBounds.height() + options.size() * rowHeight));
        }
    }

    private int requireEnabledIndex(int requested, boolean allowFirstFallback) {
        if (requested >= 0 && requested < options.size() && options.get(requested).enabled()) return requested;
        if (allowFirstFallback) return firstEnabled();
        int first = firstEnabled();
        if (first < 0) throw new IllegalArgumentException("at least one option must be enabled");
        return first;
    }

    private int firstEnabled() {
        for (int index = 0; index < options.size(); index++) {
            if (options.get(index).enabled()) return index;
        }
        return -1;
    }

    private int lastEnabled() {
        for (int index = options.size() - 1; index >= 0; index--) {
            if (options.get(index).enabled()) return index;
        }
        return -1;
    }

    private void updateAccessibility() {
        setAccessibility(accessibility().withValue(selectedLabel()));
    }

    @Override
    protected void appendSelf(UiDrawList drawList, UiTheme theme, UiRect globalBounds, UiRect clip) {
        UiRect header = headerBoundsGlobal();
        drawMaterial(drawList, theme, header, state());
        drawText(drawList, theme, new UiPoint(header.x() + 9,
                        header.y() + Math.max(1, (header.height() - 14) / 2)),
                selectedLabel(), TextStyle.body());
        UiColor caret = state() == ComponentState.DISABLED
                ? theme.color(UiColorRole.DISABLED) : theme.color(UiColorRole.TEXT_SECONDARY);
        double centerX = header.right() - 14;
        double centerY = header.y() + header.height() / 2;
        drawList.fillRect(new UiRect(centerX - 4, centerY - (open ? 1 : 3), 8, 1), caret);
        drawList.fillRect(new UiRect(centerX - 2, centerY + (open ? 1 : 1), 4, 1), caret);
        if (!open) return;
        UiRect popup = popupBoundsGlobal();
        UiMaterialTier tier = UiMaterialTier.GLASS_SOLID;
        drawList.roundedSurface(popup, 8, theme.color(UiColorRole.SURFACE).withAlpha(238), tier);
        drawList.border(popup, 8, 1, theme.color(UiColorRole.BORDER).withAlpha(200));
        for (int index = 0; index < options.size(); index++) {
            UiRect row = optionBoundsGlobal(index).inset(new io.github.gyai.projects.ui.runtime.UiInsets(1));
            Option<T> option = options.get(index);
            if (index == highlightedIndex) {
                drawList.roundedSurface(row, 6, theme.color(UiColorRole.ACCENT).withAlpha(75),
                        UiMaterialTier.ACCENT_GLASS);
            }
            UiColor color = option.enabled()
                    ? theme.color(UiColorRole.TEXT_PRIMARY) : theme.color(UiColorRole.DISABLED);
            drawList.text(new UiPoint(row.x() + 8, row.y() + Math.max(1, (row.height() - 14) / 2)),
                    option.label(), TextStyle.body(), color);
        }
    }
}
