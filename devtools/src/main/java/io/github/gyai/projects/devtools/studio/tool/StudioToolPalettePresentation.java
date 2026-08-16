package io.github.gyai.projects.devtools.studio.tool;

import io.github.gyai.projects.ui.runtime.UiInsets;
import io.github.gyai.projects.ui.runtime.UiMaterialTier;
import io.github.gyai.projects.ui.runtime.UiNode;
import io.github.gyai.projects.ui.runtime.UiRect;
import io.github.gyai.projects.ui.runtime.UiEvent;
import io.github.gyai.projects.ui.runtime.UiPointerEvent;
import io.github.gyai.projects.ui.runtime.component.GlassPanel;
import io.github.gyai.projects.ui.runtime.component.IconButton;
import io.github.gyai.projects.ui.runtime.component.Tooltip;
import io.github.gyai.projects.ui.runtime.component.TooltipPlacement;
import io.github.gyai.projects.ui.runtime.icon.IconCatalog;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Liquid-Glass vertical palette for the Studio shell.
 *
 * <p>All rows remain icon-first in normal mode.  Compact mode only changes the
 * measured rail and control spacing, so the 640px presentation remains a
 * compact icon rail.</p>
 */
public final class StudioToolPalettePresentation extends GlassPanel {
    private final StudioToolPaletteModel model;
    private final Consumer<StudioToolId> onToolActivated;
    private final EnumMap<StudioToolId, IconButton> buttons = new EnumMap<>(StudioToolId.class);
    private final EnumMap<StudioToolId, Tooltip> tooltips = new EnumMap<>(StudioToolId.class);
    private final StudioToolPaletteTooltipOverlay tooltipOverlay;
    private boolean compact;
    private UiRect tooltipViewport = new UiRect(-4096, -4096, 8192, 8192);

    public StudioToolPalettePresentation(String id, UiRect bounds) {
        this(id, bounds, new StudioToolPaletteModel(), false, ignored -> { });
    }

    public StudioToolPalettePresentation(String id, UiRect bounds,
                                         StudioToolContextState contextState,
                                         StudioToolId activeTool, boolean compact) {
        this(id, bounds, new StudioToolPaletteModel(contextState, activeTool), compact,
                ignored -> { });
    }

    public StudioToolPalettePresentation(String id, UiRect bounds, StudioToolPaletteModel model,
                                         boolean compact) {
        this(id, bounds, model, compact, ignored -> { });
    }

    public StudioToolPalettePresentation(String id, UiRect bounds, StudioToolPaletteModel model,
                                         boolean compact, Consumer<StudioToolId> onToolActivated) {
        super(id, bounds, UiMaterialTier.GLASS_THIN, 12, UiInsets.zero());
        this.model = Objects.requireNonNull(model, "model");
        this.compact = compact;
        this.onToolActivated = Objects.requireNonNull(onToolActivated, "onToolActivated");
        this.tooltipOverlay = new StudioToolPaletteTooltipOverlay(id + "-tooltip-overlay")
                .setViewport(tooltipViewport);
        rebuild();
    }

    public StudioToolPaletteModel model() { return model; }

    public List<StudioToolEntry> entries() { return model.entries(); }

    public StudioToolEntry entry(StudioToolId id) {
        return model.entry(id).orElse(null);
    }

    public boolean isEnabled(StudioToolId id) { return model.isEnabled(id); }

    public boolean isActive(StudioToolId id) { return model.isActive(id); }

    public boolean compact() { return compact; }

    public boolean isCompact() { return compact; }

    public Map<StudioToolId, IconButton> buttons() {
        return Collections.unmodifiableMap(buttons);
    }

    public Map<StudioToolId, Tooltip> tooltips() {
        return Collections.unmodifiableMap(tooltips);
    }

    public IconButton button(StudioToolId id) { return buttons.get(id); }

    public Tooltip tooltip(StudioToolId id) { return tooltips.get(id); }

    /**
     * Returns the sibling overlay that Integration should add to the viewport
     * root after adding this palette.  It must not be nested inside the rail.
     */
    public StudioToolPaletteTooltipOverlay tooltipOverlay() { return tooltipOverlay; }

    public StudioToolPalettePresentation setCompact(boolean next) {
        if (compact != next) {
            compact = next;
            rebuild();
        }
        return this;
    }

    public StudioToolPalettePresentation setContext(StudioToolContext context) {
        model.setContext(context);
        rebuild();
        return this;
    }

    public StudioToolPalettePresentation setContextState(StudioToolContextState next) {
        model.setContextState(next);
        rebuild();
        return this;
    }

    public StudioToolPalettePresentation setActiveTool(StudioToolId next) {
        model.setActiveTool(next);
        rebuild();
        return this;
    }

    /** Sets the screen-space clamp used by all Stage 2 tooltip components. */
    public StudioToolPalettePresentation setTooltipViewport(UiRect next) {
        tooltipViewport = Objects.requireNonNull(next, "tooltipViewport");
        tooltipOverlay.setViewport(next);
        return this;
    }

    @Override
    public StudioToolPalettePresentation setBounds(UiRect nextBounds) {
        super.setBounds(nextBounds);
        rebuild();
        return this;
    }

    /**
     * Explicit update seam for hosts that keep the palette outside their main
     * tree clock.  Normal Studio hosts should attach {@link #tooltipOverlay()}
     * to the root and call the normal UiTree update contract.
     */
    public StudioToolPalettePresentation updateTooltips(long now) {
        for (StudioToolButtonHost host : hosts()) host.advanceTooltipClock(now);
        tooltipOverlay.update(now);
        return this;
    }

    private void rebuild() {
        for (UiNode child : children()) removeChild(child);
        buttons.clear();
        tooltips.clear();
        tooltipOverlay.clearTooltips();

        double width = bounds().width();
        double height = bounds().height();
        double horizontalPadding = compact ? 4 : 8;
        double verticalPadding = compact ? 5 : 8;
        double buttonSize = compact ? 30 : 36;
        double available = Math.max(0, height - verticalPadding * 2);
        double gap = Math.max(2, Math.min(compact ? 5 : 7,
                (available - buttonSize * StudioToolId.values().length)
                        / Math.max(1, StudioToolId.values().length - 1)));
        if (available < buttonSize * StudioToolId.values().length) {
            buttonSize = Math.max(20, (available - 2 * (StudioToolId.values().length - 1))
                    / StudioToolId.values().length);
            gap = 2;
        }
        double x = Math.max(0, (width - buttonSize) / 2);
        double y = verticalPadding;
        for (StudioToolEntry entry : model.entries()) {
            StudioToolId id = entry.id();
            UiRect rowBounds = new UiRect(x, y, Math.min(buttonSize, Math.max(1, width - horizontalPadding * 2)),
                    buttonSize);
            IconButton button = new IconButton("studio-tool-" + id.name().toLowerCase(),
                    new UiRect(0, 0, rowBounds.width(), rowBounds.height()),
                    IconCatalog.spec(entry.iconKey()), entry.title(), () -> activate(id));
            button.setMaterialTier(entry.active() ? UiMaterialTier.ACCENT_GLASS : UiMaterialTier.GLASS_THIN);
            button.setSelected(entry.active());
            button.setEnabled(entry.enabled());
            StudioToolButtonHost host = new StudioToolButtonHost(
                    "studio-tool-hit-area-" + id.name().toLowerCase(), rowBounds, button);
            host.setEnabled(entry.enabled());
            addChild(host);
            buttons.put(id, button);

            Tooltip tooltip = new Tooltip("studio-tool-tooltip-" + id.name().toLowerCase(), rowBounds,
                    entry.tooltipTitle(), entry.tooltipDescription())
                    .setPlacement(TooltipPlacement.RIGHT)
                    .setViewport(tooltipViewport);
            host.setTooltip(tooltip);
            tooltipOverlay.addTooltip(id, tooltip, button.globalBounds());
            tooltips.put(id, tooltip);
            y += buttonSize + gap;
        }
    }

    private List<StudioToolButtonHost> hosts() {
        return children().stream()
                .filter(StudioToolButtonHost.class::isInstance)
                .map(StudioToolButtonHost.class::cast)
                .toList();
    }

    private void activate(StudioToolId id) {
        if (!model.isEnabled(id)) return;
        model.setActiveTool(id);
        rebuild();
        onToolActivated.accept(id);
    }
}

/** Hit-area bridge that keeps IconButton semantics while syncing its Tooltip from real input. */
final class StudioToolButtonHost extends UiNode {
    private final IconButton button;
    private Tooltip tooltip;

    StudioToolButtonHost(String id, UiRect bounds, IconButton button) {
        super(id, bounds);
        this.button = Objects.requireNonNull(button, "button");
        setFocusable(true);
        button.setHitTestable(false);
        addChild(button);
    }

    void setTooltip(Tooltip next) { tooltip = Objects.requireNonNull(next, "tooltip"); }

    void advanceTooltipClock(long now) {
        button.advanceTo(now);
        synchronizeTooltip(now);
    }

    @Override
    public UiNode setFocused(boolean next) {
        super.setFocused(next);
        button.setFocused(next);
        return this;
    }

    @Override
    public boolean handleEvent(UiEvent event) {
        boolean handled = button.handleEvent(event);
        if (event instanceof UiPointerEvent pointer) {
            switch (pointer.type()) {
                case ENTER -> tooltip.hoverAt(button.timelineNow());
                case LEAVE, CANCEL -> tooltip.leaveAt(button.timelineNow());
                case MOVE, DOWN, UP -> synchronizeTooltip(button.timelineNow());
            }
        }
        return handled;
    }

    @Override
    public UiNode setVisible(boolean next) {
        super.setVisible(next);
        if (!next && tooltip != null) tooltip.leaveAt(button.timelineNow());
        return this;
    }

    @Override
    public UiNode setEnabled(boolean next) {
        super.setEnabled(next);
        button.setEnabled(next);
        if (!next && tooltip != null) tooltip.leaveAt(button.timelineNow());
        return this;
    }

    private void synchronizeTooltip(long now) {
        if (button.hovered()) {
            if (!tooltip.tooltipHovered()) tooltip.hoverAt(now);
        } else if (tooltip.tooltipHovered()) {
            tooltip.leaveAt(now);
        }
    }
}
