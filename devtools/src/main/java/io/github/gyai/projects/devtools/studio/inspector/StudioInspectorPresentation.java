package io.github.gyai.projects.devtools.studio.inspector;

import io.github.gyai.projects.ui.runtime.TextStyle;
import io.github.gyai.projects.ui.runtime.UiColorRole;
import io.github.gyai.projects.ui.runtime.UiDrawList;
import io.github.gyai.projects.ui.runtime.UiInsets;
import io.github.gyai.projects.ui.runtime.UiMaterialTier;
import io.github.gyai.projects.ui.runtime.UiNode;
import io.github.gyai.projects.ui.runtime.UiPoint;
import io.github.gyai.projects.ui.runtime.UiRect;
import io.github.gyai.projects.ui.runtime.UiTheme;
import io.github.gyai.projects.ui.runtime.component.GlassPanel;
import io.github.gyai.projects.ui.runtime.component.ScrollArea;
import io.github.gyai.projects.ui.runtime.component.SectionHeader;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.LinkedHashMap;

/**
 * Glass-panel inspector shell backed by deterministic Stage 3 context data.
 *
 * <p>Placement remains caller-owned: the supplied bounds are used verbatim,
 * and compact drawer visibility is represented by the immutable state rather
 * than hidden layout assumptions.</p>
 */
public final class StudioInspectorPresentation extends GlassPanel {
    private final StudioInspectorModel model;
    private StudioInspectorContextState state;
    private final LinkedHashSet<String> collapsedSections = new LinkedHashSet<>();
    private final LinkedHashMap<String, SectionHeader> headers = new LinkedHashMap<>();
    private ScrollArea scrollArea;
    private boolean rebuilding;

    public StudioInspectorPresentation(String id, UiRect bounds) {
        this(id, bounds, new StudioInspectorModel(),
                StudioInspectorContextState.of(StudioInspectorContext.NO_SELECTION));
    }

    public StudioInspectorPresentation(String id, UiRect bounds,
                                       StudioInspectorContextState state) {
        this(id, bounds, new StudioInspectorModel(), state);
    }

    public StudioInspectorPresentation(String id, UiRect bounds, StudioInspectorModel model,
                                       StudioInspectorContextState state) {
        super(id, bounds, UiMaterialTier.GLASS_PANEL, 12, new UiInsets(12));
        this.model = Objects.requireNonNull(model, "model");
        this.state = Objects.requireNonNull(state, "state");
        rebuild();
    }

    public StudioInspectorModel model() { return model; }

    public StudioInspectorContextState state() { return state; }

    public StudioInspectorContext context() { return state.context(); }

    public boolean compact() { return state.compact(); }

    public boolean isCompact() { return state.compact(); }

    public boolean drawerOpen() { return state.drawerOpen(); }

    public boolean isDrawerOpen() { return state.drawerOpen(); }

    public boolean drawerVisible() { return state.visible(); }

    public ScrollArea scrollArea() { return scrollArea; }

    public Map<String, SectionHeader> sectionHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    public Set<String> collapsedSections() {
        return Collections.unmodifiableSet(collapsedSections);
    }

    public SectionHeader section(String id) { return headers.get(id); }

    public StudioInspectorContextData contextData() { return model.context(state.context()); }

    public List<StudioInspectorSection> sections() { return contextData().sections(); }

    public boolean sectionExpanded(String id) {
        SectionHeader header = headers.get(id);
        return header != null && header.expanded();
    }

    public StudioInspectorPresentation setState(StudioInspectorContextState next) {
        state = Objects.requireNonNull(next, "state");
        rebuild();
        return this;
    }

    public StudioInspectorPresentation setContext(StudioInspectorContext next) {
        return setState(state.withContext(next));
    }

    public StudioInspectorPresentation setCompact(boolean next) {
        return setState(state.withCompact(next));
    }

    public StudioInspectorPresentation setDrawerOpen(boolean next) {
        return setState(state.withDrawerOpen(next));
    }

    public StudioInspectorPresentation setSectionExpanded(String id, boolean expanded) {
        SectionHeader header = headers.get(id);
        if (header == null) throw new IllegalArgumentException("unknown section: " + id);
        header.setExpanded(expanded);
        return this;
    }

    @Override
    public StudioInspectorPresentation setBounds(UiRect nextBounds) {
        super.setBounds(nextBounds);
        rebuild();
        return this;
    }

    private void rebuild() {
        if (rebuilding) return;
        rebuilding = true;
        try {
            double previousScrollY = scrollArea == null ? 0 : scrollArea.scrollY();
            for (UiNode child : children()) removeChild(child);
            headers.clear();
            scrollArea = null;

            StudioInspectorContextData data = model.context(state.context());
            setTitle(data.title());
            setSubtitle(data.technical());
            setVisible(state.visible());
            if (!state.visible()) return;

            double width = Math.max(1, bounds().width() - 16);
            double height = Math.max(1, bounds().height() - 58);
            double scrollTop = Math.min(48, Math.max(32, bounds().height() - 1));
            UiRect scrollBounds = new UiRect(8, scrollTop, width, height);
            double contentY = 8;
            double sectionGap = 10;
            double rowHeight = 24;
            for (StudioInspectorSection section : data.sections()) {
                boolean expanded = !collapsedSections.contains(section.id());
                double sectionHeight = 28 + (expanded ? section.values().size() * rowHeight + 8 : 0);
                contentY += sectionHeight + sectionGap;
            }
            double contentHeight = Math.max(scrollBounds.height(), contentY);
            scrollArea = new ScrollArea("studio-inspector-scroll", scrollBounds,
                    scrollBounds.width(), contentHeight)
                    .setDrawSurface(false)
                    .setWheelStep(24);
            addChild(scrollArea);

            contentY = 8;
            double rowWidth = Math.max(1, scrollBounds.width() - 16);
            for (StudioInspectorSection section : data.sections()) {
                boolean expanded = !collapsedSections.contains(section.id());
                double sectionHeight = 28 + (expanded ? section.values().size() * rowHeight + 8 : 0);
                SectionHeader header = new SectionHeader("studio-inspector-section-" + section.id(),
                        new UiRect(8, contentY, rowWidth, 28), section.title(), true,
                        next -> sectionExpanded(section.id(), next));
                header.setExpanded(expanded);
                for (int index = 0; index < section.values().size(); index++) {
                    StudioInspectorValue value = section.values().get(index);
                    header.addChild(new InspectorValueRow(
                            "studio-inspector-value-" + section.id() + "-" + index,
                            new UiRect(10, 30 + index * rowHeight, Math.max(1, rowWidth - 20), 20),
                            value));
                }
                scrollArea.addChild(header);
                headers.put(section.id(), header);
                contentY += sectionHeight + sectionGap;
            }
            scrollArea.setScrollOffset(0, previousScrollY);
        } finally {
            rebuilding = false;
        }
    }

    private void sectionExpanded(String id, boolean expanded) {
        if (rebuilding) return;
        if (expanded) collapsedSections.remove(id);
        else collapsedSections.add(id);
        rebuild();
    }
}

/** Small renderer-neutral row used only for inspector demo values. */
final class InspectorValueRow extends UiNode {
    private final StudioInspectorValue value;

    InspectorValueRow(String id, UiRect bounds, StudioInspectorValue value) {
        super(id, bounds);
        this.value = Objects.requireNonNull(value, "value");
        setHitTestable(false);
    }

    public StudioInspectorValue value() { return value; }

    @Override
    protected void appendSelf(UiDrawList drawList, UiTheme theme, UiRect globalBounds, UiRect clip) {
        drawList.text(new UiPoint(globalBounds.x(), globalBounds.y()), value.label(), TextStyle.secondary(),
                theme.color(UiColorRole.TEXT_SECONDARY));
        double valueX = globalBounds.x() + Math.max(88, globalBounds.width() * .52);
        TextStyle style = value.technical()
                ? TextStyle.technical().withColorRole(UiColorRole.ACCENT)
                : TextStyle.body();
        drawList.text(new UiPoint(Math.min(valueX, globalBounds.right() - 1), globalBounds.y()),
                value.value(), style, theme.color(style.colorRole()));
    }
}
