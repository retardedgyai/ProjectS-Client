package io.github.gyai.projects.devtools.studio.tool;

import io.github.gyai.projects.devtools.studio.inspector.StudioInspectorContext;
import io.github.gyai.projects.devtools.studio.inspector.StudioInspectorContextData;
import io.github.gyai.projects.devtools.studio.inspector.StudioInspectorContextState;
import io.github.gyai.projects.devtools.studio.inspector.StudioInspectorModel;
import io.github.gyai.projects.devtools.studio.inspector.StudioInspectorPresentation;
import io.github.gyai.projects.ui.runtime.IconKey;
import io.github.gyai.projects.ui.runtime.UiDrawList;
import io.github.gyai.projects.ui.runtime.UiInputRouter;
import io.github.gyai.projects.ui.runtime.UiModifiers;
import io.github.gyai.projects.ui.runtime.UiMaterialTier;
import io.github.gyai.projects.ui.runtime.UiNode;
import io.github.gyai.projects.ui.runtime.UiPoint;
import io.github.gyai.projects.ui.runtime.UiRect;
import io.github.gyai.projects.ui.runtime.UiRenderCommand;
import io.github.gyai.projects.ui.runtime.UiTheme;
import io.github.gyai.projects.ui.runtime.UiThemeMode;
import io.github.gyai.projects.ui.runtime.UiTree;
import io.github.gyai.projects.ui.runtime.UiPointerEvent;
import io.github.gyai.projects.ui.runtime.component.IconButton;
import io.github.gyai.projects.ui.runtime.component.ScrollArea;
import io.github.gyai.projects.ui.runtime.component.SectionHeader;
import io.github.gyai.projects.ui.runtime.component.Tooltip;
import io.github.gyai.projects.ui.runtime.icon.IconCatalog;
import io.github.gyai.projects.ui.runtime.theme.UiAccentPreset;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/** Focused pure presentation gate for Lane C. */
public final class StudioToolInspectorTest {
    private static final String[] TOOL_ORDER = {
            "SELECT", "MOVE", "ROTATE", "SCALE", "SHAPE", "MOTION", "PHASE", "TRAIL",
            "ADD", "DUPLICATE", "DELETE"
    };

    public static void main(String[] args) {
        toolIdsAndIcons();
        contextCapabilitiesAndStates();
        palettePresentation();
        paletteTooltipOverlayUpdate();
        inspectorModelAndSwitching();
        inspectorSectionsCollapseAndScroll();
        themeAndMaterialSemantics();
        System.out.println("STUDIO_TOOL_INSPECTOR_PASS: IDs context tooltip compact inspector sections scroll themes materials");
    }

    private static void toolIdsAndIcons() {
        check(Arrays.equals(Arrays.stream(StudioToolId.values()).map(Enum::name).toArray(String[]::new),
                TOOL_ORDER), "tool ID order is frozen");
        for (StudioToolId id : StudioToolId.values()) {
            check(IconCatalog.contains(id.iconKey()), "icon resolves for " + id);
            check(IconCatalog.resolve(id.iconKey()).definition().key().equals(id.iconKey()),
                    "icon definition key for " + id);
            check(IconCatalog.spec(id.iconKey()).key().equals(id.iconKey()), "icon spec for " + id);
            check(!id.title().isBlank() && !id.description().isBlank(), "Japanese tooltip copy for " + id);
        }
        check(StudioToolId.SHAPE.iconKey() == IconKey.SHAPE, "shape frozen IconKey");
        check(StudioToolId.DELETE.iconKey() == IconKey.DELETE, "delete frozen IconKey");
    }

    private static void contextCapabilitiesAndStates() {
        StudioToolPaletteModel none = new StudioToolPaletteModel(StudioToolContext.NONE, StudioToolId.SELECT);
        check(none.entries().size() == TOOL_ORDER.length, "all tool rows exposed in NONE");
        check(none.isEnabled(StudioToolId.SELECT) && none.isActive(StudioToolId.SELECT),
                "select enabled and active without selection");
        check(none.isEnabled(StudioToolId.ADD), "add enabled without selection");
        check(!none.isEnabled(StudioToolId.MOVE) && !none.isEnabled(StudioToolId.DUPLICATE)
                        && !none.isEnabled(StudioToolId.DELETE),
                "editing and destructive tools disabled without selection");

        StudioToolPaletteModel primitive = new StudioToolPaletteModel(
                StudioToolContextState.primitive(), StudioToolId.SHAPE);
        check(primitive.entries().stream().allMatch(StudioToolEntry::enabled),
                "primitive exposes the complete direct-edit palette");
        check(primitive.isActive(StudioToolId.SHAPE), "primitive shape active");

        StudioToolPaletteModel emission = new StudioToolPaletteModel(
                StudioToolContextState.emission(), StudioToolId.MOTION);
        check(emission.isEnabled(StudioToolId.PHASE) && emission.isEnabled(StudioToolId.TRAIL),
                "emission motion tools enabled");
        check(!emission.isEnabled(StudioToolId.SHAPE), "emission shape tool disabled");
        check(emission.isActive(StudioToolId.MOTION), "emission motion active");

        StudioToolPaletteModel handle = new StudioToolPaletteModel(
                StudioToolContextState.handle(), StudioToolId.MOVE);
        check(handle.isEnabled(StudioToolId.MOVE) && handle.isEnabled(StudioToolId.SHAPE),
                "handle direct editing enabled");
        check(!handle.isEnabled(StudioToolId.DUPLICATE) && !handle.isEnabled(StudioToolId.DELETE),
                "handle duplicate/delete disabled");

        StudioToolContextState custom = StudioToolContextState.none()
                .withDuplicate(true).withDelete(true).withDirectEditing(true);
        StudioToolPaletteModel customModel = new StudioToolPaletteModel(custom, StudioToolId.DELETE);
        check(customModel.isEnabled(StudioToolId.DUPLICATE) && customModel.isEnabled(StudioToolId.DELETE),
                "context capabilities can be supplied by host");
        check(customModel.isActive(StudioToolId.DELETE), "custom delete active");
    }

    private static void palettePresentation() {
        AtomicReference<StudioToolId> activated = new AtomicReference<>();
        StudioToolPaletteModel model = new StudioToolPaletteModel(
                StudioToolContextState.primitive(), StudioToolId.SHAPE);
        StudioToolPalettePresentation palette = new StudioToolPalettePresentation(
                "studio-tool-palette", new UiRect(0, 0, 52, 480), model, false, activated::set);
        check(palette.materialTier() == UiMaterialTier.GLASS_THIN, "palette uses GLASS_THIN");
        check(palette.buttons().size() == TOOL_ORDER.length && palette.tooltips().size() == TOOL_ORDER.length,
                "palette exposes one button and tooltip per tool");
        for (StudioToolId id : StudioToolId.values()) {
            IconButton button = palette.button(id);
            Tooltip tooltip = palette.tooltip(id);
            check(button != null && tooltip != null, "palette nodes for " + id);
            check(button.icon().key().equals(id.iconKey()), "palette icon key for " + id);
            check(tooltip.title().equals(id.title()) && tooltip.description().equals(id.description()),
                    "Japanese tooltip content for " + id);
            check(tooltip.materialTier() == UiMaterialTier.GLASS_SOLID, "tooltip solid tier for " + id);
        }
        check(palette.button(StudioToolId.SHAPE).selected()
                        && palette.button(StudioToolId.SHAPE).materialTier() == UiMaterialTier.ACCENT_GLASS,
                "active tool uses ACCENT_GLASS");
        check(palette.button(StudioToolId.MOVE).materialTier() == UiMaterialTier.GLASS_THIN,
                "inactive tool uses GLASS_THIN");

        UiRect moveBounds = palette.button(StudioToolId.MOVE).globalBounds();
        io.github.gyai.projects.ui.runtime.UiPoint moveCenter = new io.github.gyai.projects.ui.runtime.UiPoint(
                moveBounds.x() + moveBounds.width() / 2, moveBounds.y() + moveBounds.height() / 2);
        palette.button(StudioToolId.MOVE).pointerDown(moveCenter);
        palette.button(StudioToolId.MOVE).pointerUp(moveCenter);
        check(activated.get() == StudioToolId.MOVE, "palette activation callback");
        check(palette.model().isActive(StudioToolId.MOVE), "palette active state updates");

        StudioToolPalettePresentation compact = new StudioToolPalettePresentation(
                "studio-tool-palette-compact", new UiRect(0, 0, 40, 360),
                StudioToolContextState.none(), StudioToolId.SELECT, true);
        check(compact.compact() && compact.bounds().width() == 40, "640 compact palette state and width");
        for (StudioToolId id : StudioToolId.values()) {
            IconButton button = compact.button(id);
            check(button.bounds().width() <= 32 && button.bounds().height() <= 32,
                    "compact icon-only button bounds for " + id);
            check(!button.enabled() == (id != StudioToolId.SELECT && id != StudioToolId.ADD),
                    "compact context enabled state for " + id);
        }
        assertBalancedAndHasGlass(palette, UiTheme.light(), "palette light render");
    }

    private static void paletteTooltipOverlayUpdate() {
        UiRect viewport = new UiRect(0, 0, 480, 600);
        UiNode root = new UiNode("studio-tooltip-root", viewport).setClipToBounds(true);
        StudioToolPalettePresentation palette = new StudioToolPalettePresentation(
                "studio-tool-palette-overlay", new UiRect(8, 40, 52, 480),
                StudioToolContextState.primitive(), StudioToolId.SELECT, false);
        root.addChild(palette);
        StudioToolPaletteTooltipOverlay overlay = palette.tooltipOverlay();
        root.addChild(overlay);
        // Rebuild after attachment so tooltip anchors are expressed in root screen space.
        palette.setBounds(new UiRect(8, 40, 52, 480));
        palette.setTooltipViewport(viewport);

        UiTree tree = new UiTree(root);
        UiInputRouter input = new UiInputRouter(tree);
        IconButton button = palette.button(StudioToolId.SELECT);
        Tooltip tooltip = palette.tooltip(StudioToolId.SELECT);
        UiRect anchor = button.globalBounds();
        UiPoint center = new UiPoint(anchor.x() + anchor.width() / 2,
                anchor.y() + anchor.height() / 2);

        tree.update(0);
        check(input.dispatch(UiPointerEvent.move(0, center, UiModifiers.none())),
                "actual input update reaches palette hit area");
        check(tooltip.tooltipHovered(), "actual pointer traversal starts tooltip hover");
        tree.update(Tooltip.DEFAULT_DELAY_MS - 1);
        check(!tooltip.shown(), "tooltip remains delayed before 450ms");
        tree.update(Tooltip.DEFAULT_DELAY_MS);
        check(tooltip.shown(), "actual UiTree update advances tooltip after 450ms");

        UiRect popup = tooltip.placementBounds();
        check(popup.x() >= palette.globalBounds().right(),
                "RIGHT tooltip is outside clipped palette rail");
        check(contains(viewport, popup), "tooltip draw bounds remain inside viewport");
        check(tooltip.parent() == overlay && overlay.parent() == root,
                "tooltip is hosted by the root-level overlay contract");
        check(!palette.children().contains(tooltip), "tooltip is not nested in clipped palette");

        UiDrawList drawList = new UiDrawList();
        tree.render(drawList, UiTheme.light());
        check(drawList.commands().stream().anyMatch(command -> command instanceof UiRenderCommand.RoundedSurface surface
                        && surface.tier() == UiMaterialTier.GLASS_SOLID
                        && surface.bounds().equals(popup)),
                "visible tooltip emits GLASS_SOLID at actual popup bounds");
        check(drawList.clipDepth() == 0, "overlay render clips are balanced");
    }

    private static void inspectorModelAndSwitching() {
        StudioInspectorModel model = new StudioInspectorModel();
        check(model.contexts().size() == 4, "all inspector contexts present");
        StudioInspectorContextData primitive = model.context(StudioInspectorContext.PRIMITIVE);
        check(primitive.title().equals("螺旋") && primitive.technical().equals("SPIRAL"),
                "primitive demo title and technical identity");
        check(primitive.sections().stream().map(section -> section.title()).toList()
                        .equals(List.of("見た目", "形状", "時間", "動き")),
                "primitive section order and Japanese titles");

        StudioInspectorPresentation inspector = new StudioInspectorPresentation(
                "studio-inspector", new UiRect(0, 0, 340, 360), model,
                StudioInspectorContextState.of(StudioInspectorContext.PRIMITIVE));
        check(inspector.materialTier() == UiMaterialTier.GLASS_PANEL, "inspector uses GLASS_PANEL");
        check(inspector.context() == StudioInspectorContext.PRIMITIVE
                        && inspector.sectionHeaders().size() == 4,
                "primitive inspector composition");
        check(inspector.scrollArea() != null && inspector.scrollArea().materialTier() == UiMaterialTier.GLASS_PANEL,
                "inspector uses clipped GLASS_PANEL ScrollArea");

        inspector.setContext(StudioInspectorContext.EMISSION);
        check(inspector.context() == StudioInspectorContext.EMISSION
                        && inspector.title().equals("放出") && inspector.subtitle().equals("EMISSION"),
                "inspector context switching");
        inspector.setContext(StudioInspectorContext.HANDLE);
        check(inspector.context() == StudioInspectorContext.HANDLE
                        && inspector.section("motion") != null,
                "handle inspector context switching");
        inspector.setContext(StudioInspectorContext.NO_SELECTION);
        check(inspector.title().equals("選択なし") && inspector.subtitle().equals("NO_SELECTION"),
                "no-selection inspector context switching");
    }

    private static void inspectorSectionsCollapseAndScroll() {
        StudioInspectorPresentation inspector = new StudioInspectorPresentation(
                "studio-inspector-sections", new UiRect(0, 0, 300, 260),
                StudioInspectorContextState.of(StudioInspectorContext.PRIMITIVE));
        ScrollArea scroll = inspector.scrollArea();
        check(scroll.maxScrollY() > 0, "inspector content is scrollable");
        scroll.scrollBy(0, 10000);
        check(scroll.scrollY() == scroll.maxScrollY(), "inspector scroll clamps to content");
        SectionHeader shape = inspector.section("shape");
        check(shape != null && shape.expanded(), "shape section initially expanded");
        inspector.setSectionExpanded("shape", false);
        check(!inspector.section("shape").expanded(), "shape section collapses");
        check(inspector.section("shape").children().stream().noneMatch(UiNode::visible),
                "collapsed section values hidden");
        check(inspector.collapsedSections().contains("shape"), "collapsed state is exposed");
        check(inspector.scrollArea().maxScrollY() < scroll.maxScrollY(),
                "collapse reflows scroll content");
        inspector.setSectionExpanded("shape", true);
        check(inspector.section("shape").expanded() && inspector.section("shape").children()
                        .stream().allMatch(UiNode::visible), "section expands and restores values");

        StudioInspectorPresentation compactClosed = new StudioInspectorPresentation(
                "studio-inspector-compact", new UiRect(0, 0, 300, 240),
                StudioInspectorContextState.compact(StudioInspectorContext.PRIMITIVE, false));
        check(compactClosed.compact() && !compactClosed.drawerVisible() && compactClosed.scrollArea() == null,
                "compact inspector closed drawer is hidden");
        compactClosed.setDrawerOpen(true);
        check(compactClosed.drawerVisible() && compactClosed.scrollArea() != null,
                "compact inspector opens as drawer");

        assertBalancedAndHasGlass(inspector, UiTheme.dark(UiAccentPreset.CYAN), "inspector dark render");
    }

    private static void themeAndMaterialSemantics() {
        StudioToolPalettePresentation palette = new StudioToolPalettePresentation(
                "studio-theme-palette", new UiRect(0, 0, 52, 480),
                StudioToolContextState.primitive(), StudioToolId.MOVE, false);
        UiDrawList light = new UiDrawList();
        new UiTree(palette).render(light, UiTheme.light(UiAccentPreset.PURPLE));
        UiDrawList dark = new UiDrawList();
        new UiTree(palette).render(dark, UiTheme.of(UiThemeMode.DARK, UiAccentPreset.CYAN));
        check(!light.commands().equals(dark.commands()), "Light/Dark presentation changes render tokens");
        UiRenderCommand.RoundedSurface active = findSurface(light, UiMaterialTier.ACCENT_GLASS);
        check(active != null, "accent surface emitted for active tool");
        check(findSurface(light, UiMaterialTier.GLASS_THIN) != null, "thin surface emitted for palette");
        check(light.commands().stream().anyMatch(command -> command instanceof UiRenderCommand.StatefulIcon),
                "IconButton uses Stage 2 icon command");

        StudioInspectorPresentation inspector = new StudioInspectorPresentation(
                "studio-theme-inspector", new UiRect(0, 0, 340, 480),
                StudioInspectorContextState.of(StudioInspectorContext.PRIMITIVE));
        UiDrawList accent = new UiDrawList();
        new UiTree(inspector).render(accent, UiTheme.of(UiThemeMode.LIGHT, UiAccentPreset.ROSE));
        check(findSurface(accent, UiMaterialTier.GLASS_PANEL) != null, "panel tier emitted for inspector");
        check(findSurface(accent, UiMaterialTier.GLASS_THIN) != null, "section header tier emitted for inspector");
        check(accent.commands().stream().anyMatch(command -> command instanceof UiRenderCommand.Text text
                        && text.value().equals("螺旋")), "custom typography title emitted");
        check(accent.commands().stream().anyMatch(command -> command instanceof UiRenderCommand.Text text
                        && text.value().equals("SPIRAL") && text.style().family()
                        == io.github.gyai.projects.ui.runtime.UiFontFamilyRole.TECHNICAL_MONO),
                "technical typography emitted for identity");
    }

    private static void assertBalancedAndHasGlass(UiNode node, UiTheme theme, String message) {
        UiDrawList drawList = new UiDrawList();
        new UiTree(node).render(drawList, theme);
        check(drawList.clipDepth() == 0, message + " balanced clips");
        check(drawList.commands().stream().anyMatch(command -> command instanceof UiRenderCommand.RoundedSurface),
                message + " rounded glass");
    }

    private static UiRenderCommand.RoundedSurface findSurface(UiDrawList drawList, UiMaterialTier tier) {
        return drawList.commands().stream()
                .filter(command -> command instanceof UiRenderCommand.RoundedSurface surface
                        && surface.tier() == tier)
                .map(command -> (UiRenderCommand.RoundedSurface) command)
                .findFirst().orElse(null);
    }

    private static boolean contains(UiRect outer, UiRect inner) {
        return inner.x() >= outer.x() && inner.y() >= outer.y()
                && inner.right() <= outer.right() && inner.bottom() <= outer.bottom();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
