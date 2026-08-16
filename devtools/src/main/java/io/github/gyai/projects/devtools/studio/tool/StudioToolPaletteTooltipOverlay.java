package io.github.gyai.projects.devtools.studio.tool;

import io.github.gyai.projects.ui.runtime.UiLayer;
import io.github.gyai.projects.ui.runtime.UiNode;
import io.github.gyai.projects.ui.runtime.UiRect;
import io.github.gyai.projects.ui.runtime.component.Tooltip;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Hostable tooltip layer for a Studio palette.
 *
 * <p>This is intentionally a transparent, zero-area sibling node.  Integration
 * attaches it to the same viewport root as the palette, sets its viewport, and
 * therefore lets Tooltip commands render outside the clipped rail while still
 * inheriting the root viewport clip.  The empty bounds also keep this layer
 * pass-through for input; its Tooltip children are non-hit-testable.</p>
 */
public final class StudioToolPaletteTooltipOverlay extends UiNode {
    private final EnumMap<StudioToolId, Tooltip> tooltips = new EnumMap<>(StudioToolId.class);
    private UiRect viewport = new UiRect(0, 0, 1920, 1080);

    public StudioToolPaletteTooltipOverlay(String id) {
        super(id, UiRect.empty());
        setLayer(UiLayer.OVERLAY);
    }

    public UiRect viewport() { return viewport; }

    public Map<StudioToolId, Tooltip> tooltips() {
        return Collections.unmodifiableMap(tooltips);
    }

    public Tooltip tooltip(StudioToolId id) { return tooltips.get(id); }

    public StudioToolPaletteTooltipOverlay setViewport(UiRect next) {
        viewport = Objects.requireNonNull(next, "viewport");
        for (Tooltip tooltip : tooltips.values()) tooltip.setViewport(next);
        return this;
    }

    /** Replaces the overlay contents while preserving the overlay host node. */
    public StudioToolPaletteTooltipOverlay clearTooltips() {
        for (UiNode child : children()) removeChild(child);
        tooltips.clear();
        return this;
    }

    /**
     * Adds a screen-space anchor.  The overlay must be hosted as a sibling of
     * the palette under a root whose origin matches these coordinates.
     */
    public StudioToolPaletteTooltipOverlay addTooltip(StudioToolId id, Tooltip tooltip,
                                                       UiRect screenSpaceAnchor) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(tooltip, "tooltip");
        Objects.requireNonNull(screenSpaceAnchor, "screenSpaceAnchor");
        if (tooltips.containsKey(id)) throw new IllegalArgumentException("duplicate tooltip: " + id);
        tooltip.setBounds(screenSpaceAnchor);
        tooltip.setViewport(viewport);
        tooltip.setHitTestable(false);
        tooltip.setLayer(UiLayer.OVERLAY);
        addChild(tooltip);
        tooltips.put(id, tooltip);
        return this;
    }

    /** Explicit host-time contract; UiTree.update also advances attached Tooltip children. */
    @Override
    public StudioToolPaletteTooltipOverlay update(long now) {
        super.update(now);
        return this;
    }
}
