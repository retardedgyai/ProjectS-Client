package io.github.gyai.projects.minecraft.adapter.studio.viewport;

import io.github.gyai.projects.ui.runtime.UiPoint;
import io.github.gyai.projects.ui.runtime.UiRect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Deterministic logical hit policy for the Studio shell.
 *
 * <p>The integration screen supplies chrome bounds after layout.  This model
 * never asks Minecraft to hit-test widgets and never falls back to the
 * viewport for a point owned by a panel or overlay.</p>
 */
public final class StudioViewportInputModel {
    private StudioViewportRegion viewport;
    private List<UiRect> uiPanels = List.of();
    private UiRect popupBounds;
    private UiRect modalBounds;
    private UiRect drawerBounds;
    private boolean popupOpen;
    private boolean modalOpen;
    private boolean drawerOpen;

    public StudioViewportInputModel(StudioViewportRegion viewport) {
        this.viewport = Objects.requireNonNull(viewport, "viewport");
    }

    public StudioViewportInputModel(UiRect viewportBounds) {
        this(new StudioViewportRegion(viewportBounds));
    }

    public StudioViewportRegion viewport() {
        return viewport;
    }

    public StudioViewportInputModel setViewport(StudioViewportRegion nextViewport) {
        viewport = Objects.requireNonNull(nextViewport, "viewport");
        return this;
    }

    public StudioViewportInputModel setViewportBounds(UiRect nextBounds) {
        return setViewport(new StudioViewportRegion(nextBounds));
    }

    public List<UiRect> uiPanels() {
        return uiPanels;
    }

    public StudioViewportInputModel setUiPanels(Collection<UiRect> panels) {
        if (panels == null) throw new NullPointerException("panels");
        ArrayList<UiRect> next = new ArrayList<>(panels.size());
        for (UiRect panel : panels) next.add(Objects.requireNonNull(panel, "panel"));
        uiPanels = List.copyOf(next);
        return this;
    }

    public StudioViewportInputModel addUiPanel(UiRect panel) {
        Objects.requireNonNull(panel, "panel");
        ArrayList<UiRect> next = new ArrayList<>(uiPanels);
        next.add(panel);
        uiPanels = List.copyOf(next);
        return this;
    }

    public StudioViewportInputModel clearUiPanels() {
        uiPanels = List.of();
        return this;
    }

    public UiRect popupBounds() {
        return popupBounds;
    }

    public StudioViewportInputModel setPopupBounds(UiRect bounds) {
        popupBounds = bounds;
        return this;
    }

    public UiRect modalBounds() {
        return modalBounds;
    }

    public StudioViewportInputModel setModalBounds(UiRect bounds) {
        modalBounds = bounds;
        return this;
    }

    public UiRect drawerBounds() {
        return drawerBounds;
    }

    public StudioViewportInputModel setDrawerBounds(UiRect bounds) {
        drawerBounds = bounds;
        return this;
    }

    public boolean popupOpen() {
        return popupOpen;
    }

    public StudioViewportInputModel setPopupOpen(boolean open) {
        popupOpen = open;
        return this;
    }

    public boolean modalOpen() {
        return modalOpen;
    }

    public StudioViewportInputModel setModalOpen(boolean open) {
        modalOpen = open;
        return this;
    }

    public boolean drawerOpen() {
        return drawerOpen;
    }

    public StudioViewportInputModel setDrawerOpen(boolean open) {
        drawerOpen = open;
        return this;
    }

    /**
     * Modal and popup state is intentionally modal even outside its rectangle:
     * an outside pointer is an overlay-dismiss/focus-trap event, never a world
     * interaction.  Panel bounds are checked before the viewport.
     */
    public StudioViewportInputTarget targetAt(UiPoint point) {
        if (point == null) return StudioViewportInputTarget.NONE;
        if (modalOpen) return StudioViewportInputTarget.MODAL;
        if (popupOpen) return StudioViewportInputTarget.POPUP;
        if (drawerOpen && drawerBounds != null && drawerBounds.contains(point)) {
            return StudioViewportInputTarget.UI;
        }
        for (UiRect panel : uiPanels) {
            if (panel.contains(point)) return StudioViewportInputTarget.UI;
        }
        return viewport.contains(point)
                ? StudioViewportInputTarget.VIEWPORT
                : StudioViewportInputTarget.NONE;
    }

    public boolean isInsideViewport(UiPoint point) {
        return viewport.contains(point);
    }

    public boolean isInsideUi(UiPoint point) {
        return targetAt(point) == StudioViewportInputTarget.UI;
    }

    public boolean isInsidePopup(UiPoint point) {
        return popupBounds != null && popupBounds.contains(point);
    }

    public boolean isInsideModal(UiPoint point) {
        return modalBounds != null && modalBounds.contains(point);
    }
}
