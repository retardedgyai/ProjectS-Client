package io.github.gyai.projects.ui.runtime.component;

import io.github.gyai.projects.ui.runtime.UiAccessibilityMetadata;
import io.github.gyai.projects.ui.runtime.UiAccessibilityRole;
import io.github.gyai.projects.ui.runtime.UiColorRole;
import io.github.gyai.projects.ui.runtime.UiEvent;
import io.github.gyai.projects.ui.runtime.UiFocusManager;
import io.github.gyai.projects.ui.runtime.UiInputRouter;
import io.github.gyai.projects.ui.runtime.UiKeyAction;
import io.github.gyai.projects.ui.runtime.UiKeyEvent;
import io.github.gyai.projects.ui.runtime.UiMaterialTier;
import io.github.gyai.projects.ui.runtime.UiNode;
import io.github.gyai.projects.ui.runtime.UiPoint;
import io.github.gyai.projects.ui.runtime.UiPointerEvent;
import io.github.gyai.projects.ui.runtime.UiRect;
import io.github.gyai.projects.ui.runtime.UiDrawList;
import io.github.gyai.projects.ui.runtime.UiTheme;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Consumer;

/** Modal liquid-glass surface with explicit outside-click, Escape, and focus-restore seams. */
public final class Popup extends GlassComponent {
    private final UiRect panelBounds;
    private UiRect overlayBounds;
    private UiRect panelLocalBounds;
    private boolean open;
    private boolean previewOnly;
    private UiInputRouter input;
    private UiNode restoreFocus;
    private Runnable onClose;
    private final Map<UiNode, UiRect> childPanelBounds = new IdentityHashMap<>();

    public Popup(String id, UiRect bounds) { this(id, bounds, () -> { }); }

    public Popup(String id, UiRect bounds, Runnable onClose) {
        super(id, bounds, UiMaterialTier.GLASS_SOLID, 10);
        if (onClose == null) throw new NullPointerException("onClose");
        panelBounds = bounds;
        panelLocalBounds = new UiRect(0, 0, bounds.width(), bounds.height());
        this.onClose = onClose;
        setFocusable(true);
        setAccessibility(UiAccessibilityMetadata.of(UiAccessibilityRole.DIALOG, id));
        setVisible(false);
    }

    public boolean isOpen() { return open; }
    public boolean previewOnly() { return previewOnly; }
    /** True only while an open Popup is allowed to participate in input routing. */
    public boolean isInputParticipating() { return open && !previewOnly; }
    public UiRect panelBounds() { return panelBoundsGlobal(); }
    public UiRect overlayBounds() { return overlayBounds; }
    public UiNode restoreFocusTarget() { return restoreFocus; }

    public Popup setOnClose(Runnable next) {
        if (next == null) throw new NullPointerException("onClose");
        onClose = next;
        return this;
    }

    /** Optional parent-local hit-test surface. Use the root/client bounds for outside-click routing. */
    public Popup setOverlayBounds(UiRect nextOverlayBounds) {
        if (nextOverlayBounds == null) {
            overlayBounds = null;
            panelLocalBounds = new UiRect(0, 0, panelBounds.width(), panelBounds.height());
            if (open) {
                setBounds(panelBounds);
                restoreChildPanelBounds();
            }
            return this;
        }
        overlayBounds = nextOverlayBounds;
        if (previewOnly) {
            panelLocalBounds = new UiRect(0, 0, panelBounds.width(), panelBounds.height());
            if (open) {
                setBounds(panelBounds);
                restoreChildPanelBounds();
            }
            return this;
        }
        panelLocalBounds = new UiRect(panelBounds.x() - nextOverlayBounds.x(),
                panelBounds.y() - nextOverlayBounds.y(), panelBounds.width(), panelBounds.height());
        if (open) applyOverlayBounds();
        return this;
    }

    public boolean open() { return open(null); }

    public boolean open(UiInputRouter nextInput) {
        if (open) return true;
        previewOnly = false;
        setHitTestable(true);
        setFocusable(true);
        input = nextInput;
        restoreFocus = nextInput == null ? null : nextInput.focus().current().orElse(null);
        if (nextInput != null && overlayBounds == null && parent() == nextInput.tree().root()) {
            setOverlayBounds(nextInput.tree().root().bounds());
        }
        open = true;
        setVisible(true);
        if (overlayBounds != null) applyOverlayBounds();
        if (nextInput != null) {
            nextInput.onModalOpened();
            UiFocusManager focus = nextInput.focus();
            if (focus.setModalBoundary(this)) {
                if (!focus.requestFocus(this)) focus.traverseForward();
            }
        }
        return true;
    }

    /**
     * Opens a gallery-only preview without an overlay, modal boundary, focus capture, or input routing.
     * The configured overlay bounds remain available for deterministic layout/reload checks.
     */
    public boolean openPreview() {
        if (open) return true;
        previewOnly = true;
        input = null;
        restoreFocus = null;
        open = true;
        setHitTestable(false);
        setFocusable(false);
        panelLocalBounds = new UiRect(0, 0, panelBounds.width(), panelBounds.height());
        setBounds(panelBounds);
        restoreChildPanelBounds();
        setVisible(true);
        return true;
    }

    public boolean close() {
        if (!open) return false;
        UiInputRouter nextInput = input;
        UiNode nextRestore = restoreFocus;
        open = false;
        if (nextInput != null) {
            UiFocusManager focus = nextInput.focus();
            focus.clearModalBoundary();
            focus.clearFocus();
            setVisible(false);
            if (nextRestore != null && nextRestore.isEffectivelyInteractive(focus.root())
                    && nextRestore.focusable()) {
                focus.requestFocus(nextRestore);
            }
        } else {
            setVisible(false);
        }
        input = null;
        restoreFocus = nextRestore;
        previewOnly = false;
        setHitTestable(true);
        setFocusable(true);
        if (overlayBounds != null) {
            setBounds(panelBounds);
            restoreChildPanelBounds();
        }
        onClose.run();
        return true;
    }

    /** Synchronizes with global router Escape/modal cancellation, which is intentionally centralized. */
    public boolean syncWithInput() {
        if (!isInputParticipating() || input == null) return false;
        if (input.focus().modalBoundary().orElse(null) != this
                || input.focus().current().isEmpty()) return close();
        return false;
    }

    public boolean handleOutsidePointer(UiPoint position) {
        if (!isInputParticipating() || panelBoundsGlobal().contains(position)) return false;
        return close();
    }

    @Override
    public Popup addChild(UiNode child) {
        super.addChild(child);
        childPanelBounds.put(child, child.bounds());
        if (open && overlayBounds != null) {
            child.setBounds(child.bounds().offset(panelLocalBounds.x(), panelLocalBounds.y()));
        }
        return this;
    }

    @Override
    public boolean removeChild(UiNode child) {
        boolean removed = super.removeChild(child);
        if (removed) childPanelBounds.remove(child);
        return removed;
    }

    @Override
    public boolean handleEvent(UiEvent event) {
        if (!open || previewOnly) return false;
        if (event instanceof UiKeyEvent key && key.action() == UiKeyAction.DOWN
                && key.key() == UiKeyEvent.KEY_ESCAPE) return close();
        if (event instanceof UiPointerEvent pointer) {
            return switch (pointer.type()) {
                case MOVE -> pointerMove(pointer.pointerId(), pointer.position());
                case ENTER -> pointerEnter(pointer.pointerId());
                case LEAVE -> pointerLeave(pointer.pointerId());
                case DOWN -> {
                    if (!panelBoundsGlobal().contains(pointer.position())) yield close();
                    yield pointerDown(pointer.pointerId(), pointer.position());
                }
                case UP -> pointerUp(pointer.pointerId(), pointer.position());
                case CANCEL -> pointerCancel(pointer.pointerId());
            };
        }
        return false;
    }

    @Override
    protected void appendSelf(UiDrawList drawList, UiTheme theme, UiRect globalBounds, UiRect clip) {
        if (!open) return;
        if (overlayBounds != null && !previewOnly) {
            drawList.fillRect(globalBounds, theme.color(UiColorRole.SURFACE).withAlpha(56));
        }
        drawMaterial(drawList, theme, panelBoundsGlobal(), state());
    }

    private UiRect panelBoundsGlobal() {
        UiRect global = globalBounds();
        return new UiRect(global.x() + panelLocalBounds.x(), global.y() + panelLocalBounds.y(),
                panelLocalBounds.width(), panelLocalBounds.height());
    }

    private void applyOverlayBounds() {
        setBounds(overlayBounds);
        for (Map.Entry<UiNode, UiRect> entry : childPanelBounds.entrySet()) {
            entry.getKey().setBounds(entry.getValue().offset(panelLocalBounds.x(), panelLocalBounds.y()));
        }
    }

    private void restoreChildPanelBounds() {
        for (Map.Entry<UiNode, UiRect> entry : childPanelBounds.entrySet()) {
            entry.getKey().setBounds(entry.getValue());
        }
    }
}
