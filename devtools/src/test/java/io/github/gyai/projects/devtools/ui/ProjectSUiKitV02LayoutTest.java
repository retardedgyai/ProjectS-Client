package io.github.gyai.projects.devtools.ui;

import io.github.gyai.projects.ui.runtime.UiDrawList;
import io.github.gyai.projects.ui.runtime.UiButtonState;
import io.github.gyai.projects.ui.runtime.UiNode;
import io.github.gyai.projects.ui.runtime.UiRect;
import io.github.gyai.projects.ui.runtime.UiRenderCommand;
import io.github.gyai.projects.ui.runtime.UiTheme;
import io.github.gyai.projects.ui.runtime.UiTree;
import io.github.gyai.projects.ui.runtime.component.Button;
import io.github.gyai.projects.ui.runtime.component.Popup;
import io.github.gyai.projects.ui.runtime.component.ScrollArea;
import io.github.gyai.projects.ui.runtime.component.Tooltip;
import io.github.gyai.projects.ui.runtime.component.TooltipPlacement;

/** Non-GUI layout and composition gate for the dev-only Stage 2 gallery. */
public final class ProjectSUiKitV02LayoutTest {
    private static final int[][] SIZES = {
            {640, 360}, {854, 480}, {1280, 720}, {1920, 1080}, {2560, 1440}, {2560, 720}
    };

    public static void main(String[] args) {
        for (int[] size : SIZES) validateLayout(size[0], size[1]);
        for (double scale : new double[]{1, 1.25, 1.5, 2}) {
            for (int[] physical : new int[][]{{1280, 720}, {1920, 1080}, {2560, 1440}, {2560, 720}}) {
                ProjectSUiKitV02Layout logical = ProjectSUiKitV02Layout.atLogical(
                        physical[0], physical[1], scale);
                check(logical.sectionsDoNotOverlap() && logical.sectionsReachable()
                                && logical.controlsInsideTheme() && logical.iconsInsideGallery(),
                        "logical GUI scale layout " + physical[0] + "x" + physical[1] + " @" + scale);
            }
        }
        logicalCompositionGate();
        compositionGate();
        tooltipViewportAfterScrollGate();
        tooltipEdgePlacementGate();
        resizeScrollReflowGate();
        System.out.println("UI_KIT_V02_LAYOUT_PASS: 640x360 854x480 1280x720 1920x1080 2560x1440 wide logical-scale resize-scroll-reflow composition");
    }

    private static void validateLayout(int width, int height) {
        ProjectSUiKitV02Layout layout = ProjectSUiKitV02Layout.at(width, height);
        check(layout.viewport().equals(new UiRect(0, 0, width, height)), "viewport " + width + "x" + height);
        check(layout.titleBounds().right() <= width && layout.titleBounds().bottom() <= height, "title bounds");
        check(layout.sections().size() == 5 && layout.sectionsDoNotOverlap(), "section topology");
        check(layout.controlsInsideTheme(), "theme controls");
        check(layout.iconsInsideGallery() && layout.iconCells().size() == 27, "icon gallery");
        check(layout.contentHeight() >= layout.contentClip().height(), "scroll content height");
        check(layout.sectionsReachable(), "all sections reachable at " + width + "x" + height);
        check(layout.contentHeight() + layout.contentClip().y()
                        >= layout.maxSectionBottomRelativeToContentOrigin() + layout.contentClip().y(),
                "content extent includes section bottom");
    }

    private static void compositionGate() {
        ProjectSUiKitV02 kit = new ProjectSUiKitV02();
        for (int[] size : SIZES) {
            UiNode root = new UiNode("root", new UiRect(0, 0, size[0], size[1]));
            kit.populate(root, size[0], size[1], () -> { });
            UiTree tree = new UiTree(root);
            for (String id : new String[]{
                    "typography", "theme", "materials", "components", "icons", "ui-kit-scroll",
                    "typography-japanese", "typography-english", "typography-mixed", "typography-wrap", "typography-fallback",
                    "theme-light", "theme-dark", "accent-purple", "accent-blue", "accent-cyan",
                    "accent-green", "accent-amber", "accent-rose", "accent-custom",
                    "component-icon-button", "component-toggle", "component-slider", "component-dropdown",
                    "component-tooltip", "component-popup", "component-separator", "component-section-header"}) {
                check(find(root, id) != null, "missing UI Kit node " + id + " at " + size[0] + "x" + size[1]);
            }
            UiButtonState[] states = {UiButtonState.NORMAL, UiButtonState.HOVER, UiButtonState.PRESSED,
                    UiButtonState.FOCUSED, UiButtonState.SELECTED, UiButtonState.DISABLED};
            for (int index = 0; index < states.length; index++) {
                UiNode node = find(root, "component-state-" + index);
                check(node instanceof Button && ((Button) node).state() == states[index],
                        "component state gallery " + states[index]);
            }
            int iconNodes = 0;
            for (UiNode node : flatten(root)) {
                if (node.id().startsWith("icon-")) iconNodes++;
            }
            check(iconNodes == 27, "all required icon gallery nodes at " + size[0] + "x" + size[1]);
            UiNode components = find(root, "components");
            Tooltip tooltip = (Tooltip) find(root, "component-tooltip");
            Popup popup = (Popup) find(root, "component-popup");
            check(tooltip.shownAt(450), "deterministic tooltip preview visible at " + size[0] + "x" + size[1]);
            check(popup.isOpen() && popup.visible() && popup.previewOnly()
                            && !popup.focusable() && !popup.hitTestable(),
                    "gallery popup is visible and non-modal at " + size[0] + "x" + size[1]);
            assertPreviewBounds(components, tooltip, popup, size[0] + "x" + size[1]);
            UiDrawList drawList = new UiDrawList();
            tree.render(drawList, kit.theme());
            check(drawList.clipDepth() == 0, "balanced clips");
            check(drawList.commands().stream().anyMatch(command -> command instanceof UiRenderCommand.RoundedSurface),
                    "glass surfaces rendered");
            check(drawList.commands().stream().anyMatch(command -> command instanceof UiRenderCommand.Text),
                    "typography rendered");

            // Scroll the actual content until the Components gallery is visible, then prove the
            // preview Popup emits its real material command through the normal UiTree renderer.
            ScrollArea scroll = (ScrollArea) find(root, "ui-kit-scroll");
            ProjectSUiKitV02Layout layout = kit.layout(size[0], size[1]);
            double reveal = Math.max(0, layout.section("components").bounds().y()
                    - layout.contentClip().y());
            scroll.setScrollOffset(0, Math.min(scroll.maxScrollY(), reveal));
            UiDrawList visibleDrawList = new UiDrawList();
            new UiTree(root).render(visibleDrawList, kit.theme());
            check(visibleDrawList.commands().stream().anyMatch(command ->
                            command instanceof UiRenderCommand.RoundedSurface surface
                                    && surface.bounds().equals(popup.panelBounds())),
                    "visible Popup preview emits its panel command at " + size[0] + "x" + size[1]);
        }
    }

    private static void logicalCompositionGate() {
        for (double scale : new double[]{1, 1.25, 1.5, 2}) {
            for (int[] physical : new int[][]{{1280, 720}, {1920, 1080}, {2560, 1440}, {2560, 720}}) {
                int width = Math.max(1, (int) Math.round(physical[0] / scale));
                int height = Math.max(1, (int) Math.round(physical[1] / scale));
                ProjectSUiKitV02 kit = new ProjectSUiKitV02();
                UiNode root = new UiNode("root", new UiRect(0, 0, width, height));
                kit.populate(root, width, height, () -> { });
                Tooltip tooltip = (Tooltip) find(root, "component-tooltip");
                Popup popup = (Popup) find(root, "component-popup");
                assertPreviewBounds(find(root, "components"), tooltip, popup,
                        width + "x" + height + " @" + scale);
            }
        }
    }

    private static void tooltipViewportAfterScrollGate() {
        for (int[] size : SIZES) {
            ProjectSUiKitV02 kit = new ProjectSUiKitV02();
            UiNode root = new UiNode("tooltip-scroll-root", new UiRect(0, 0, size[0], size[1]));
            kit.populate(root, size[0], size[1], () -> { });
            ProjectSUiKitV02Layout layout = kit.layout(size[0], size[1]);
            ScrollArea scroll = (ScrollArea) find(root, "ui-kit-scroll");
            UiNode components = find(root, "components");
            Tooltip tooltip = (Tooltip) find(root, "component-tooltip");
            check(scroll != null && components != null && tooltip != null,
                    "tooltip scroll fixture nodes " + size[0] + "x" + size[1]);

            double top = scrollPositionForComponents(components, scroll, 0);
            double middle = scrollPositionForComponents(components, scroll, 1);
            double bottom = scrollPositionForComponents(components, scroll, 2);
            double[] positions = {top, middle, bottom, scroll.maxScrollY()};
            for (int index = 0; index < positions.length; index++) {
                scroll.setScrollOffset(0, positions[index]);
                UiDrawList drawList = new UiDrawList();
                new UiTree(root).render(drawList, kit.theme());
                UiRect visibleClip = visibleScrollClip(root, scroll);
                UiRect currentComponent = components.globalBounds();
                UiRect expectedViewport = currentComponent.intersection(visibleClip);
                if (expectedViewport.isEmpty()) expectedViewport = currentComponent.intersection(root.globalBounds());
                check(!expectedViewport.isEmpty(), "components visible for tooltip "
                        + positionName(index) + " at " + size[0] + "x" + size[1]);
                check(tooltip.viewport().equals(expectedViewport),
                        "tooltip viewport follows current global/scroll clip " + positionName(index)
                                + " at " + size[0] + "x" + size[1]);
                UiRect tooltipBounds = tooltip.placementBounds();
                check(contains(visibleClip, tooltipBounds),
                        "post-scroll tooltip stays in visible clip " + positionName(index)
                                + " at " + size[0] + "x" + size[1]);
                check(contains(currentComponent, tooltipBounds),
                        "post-scroll tooltip stays in Components " + positionName(index)
                                + " at " + size[0] + "x" + size[1]);
                assertNoPreviewSiblingIntersection(components, tooltip, size[0] + "x" + size[1]
                        + " " + positionName(index));
                assertTooltipDrawCommands(drawList, tooltipBounds, visibleClip,
                        size[0] + "x" + size[1] + " " + positionName(index));
            }

            if (size[0] == 640 && size[1] == 360) {
                check(Math.abs(layout.section("components").bounds().y() - 666.4) < 1e-9,
                        "640x360 Components canonical y regression pin");
                check(Math.abs(top - 616) < 1e-9,
                        "640x360 reveal scroll offset regression pin");
            }
        }

        for (double scale : new double[]{1, 1.25, 1.5, 2}) {
            for (int[] physical : new int[][]{{1280, 720}, {1920, 1080}, {2560, 1440}, {2560, 720}}) {
                int width = Math.max(1, (int) Math.round(physical[0] / scale));
                int height = Math.max(1, (int) Math.round(physical[1] / scale));
                ProjectSUiKitV02 kit = new ProjectSUiKitV02();
                UiNode root = new UiNode("logical-tooltip-scroll-root", new UiRect(0, 0, width, height));
                kit.populate(root, width, height, () -> { });
                ScrollArea scroll = (ScrollArea) find(root, "ui-kit-scroll");
                UiNode components = find(root, "components");
                Tooltip tooltip = (Tooltip) find(root, "component-tooltip");
                double middle = scrollPositionForComponents(components, scroll, 1);
                scroll.setScrollOffset(0, middle);
                UiDrawList drawList = new UiDrawList();
                new UiTree(root).render(drawList, kit.theme());
                UiRect visibleClip = visibleScrollClip(root, scroll);
                UiRect tooltipBounds = tooltip.placementBounds();
                check(contains(visibleClip, tooltipBounds),
                        "logical-scale tooltip visible at " + physical[0] + "x" + physical[1]
                                + " @" + scale);
                assertTooltipDrawCommands(drawList, tooltipBounds, visibleClip,
                        physical[0] + "x" + physical[1] + " @" + scale);
            }
        }
    }

    private static void tooltipEdgePlacementGate() {
        UiRect viewport = new UiRect(0, 0, 180, 120);
        UiNode root = new UiNode("tooltip-edge-root", viewport);
        Tooltip bottom = new Tooltip("tooltip-bottom-edge", new UiRect(130, 86, 28, 20),
                "Bottom", "Edge clamp", "B")
                .setViewport(viewport).setPlacement(TooltipPlacement.BOTTOM);
        bottom.hoverAt(0).update(450);
        Tooltip right = new Tooltip("tooltip-right-edge", new UiRect(154, 38, 20, 20),
                "Right", "Edge clamp", "R")
                .setViewport(viewport).setPlacement(TooltipPlacement.RIGHT);
        right.hoverAt(0).update(450);
        root.addChild(bottom);
        root.addChild(right);
        UiDrawList drawList = new UiDrawList();
        new UiTree(root).render(drawList, UiTheme.light());

        UiRect bottomBounds = bottom.placementBounds();
        check(bottomBounds.y() < bottom.globalBounds().y(), "bottom-edge placement flips above anchor");
        check(contains(viewport, bottomBounds), "bottom-edge Tooltip rectangle clamps inside viewport");
        UiRect rightBounds = right.placementBounds();
        check(rightBounds.x() < right.globalBounds().x(), "right-edge placement flips left of anchor");
        check(contains(viewport, rightBounds), "right-edge Tooltip rectangle clamps inside viewport");
        assertTooltipDrawCommands(drawList, bottomBounds, viewport, "bottom-edge Tooltip commands");
        assertTooltipDrawCommands(drawList, rightBounds, viewport, "right-edge Tooltip commands");
    }

    private static void resizeScrollReflowGate() {
        ProjectSUiKitV02 kit = new ProjectSUiKitV02();
        UiNode root = new UiNode("root", new UiRect(0, 0, 854, 480));
        kit.populate(root, 854, 480, () -> { });
        ScrollArea initial = (ScrollArea) find(root, "ui-kit-scroll");
        initial.scrollBy(0, 10_000);
        int[][] resizes = {{1280, 720}, {2560, 720}, {1920, 1080}, {640, 360}};
        for (int[] size : resizes) {
            kit.applyLayout(root, size[0], size[1]);
            ProjectSUiKitV02Layout layout = kit.layout(size[0], size[1]);
            ScrollArea scroll = (ScrollArea) find(root, "ui-kit-scroll");
            check(scroll.contentHeight() >= layout.contentHeight(), "canonical content height after resize");
            check(Math.abs(scroll.viewportBounds().width() - layout.contentClip().width()) < 1e-9,
                    "scroll viewport after resize");
            Tooltip tooltip = (Tooltip) find(root, "component-tooltip");
            Popup popup = (Popup) find(root, "component-popup");
            check(tooltip.viewport().equals(layout.section("components").bounds()),
                    "tooltip viewport after resize");
            check(popup.overlayBounds().equals(layout.viewport()),
                    "popup overlay after resize");
            assertPreviewBounds(find(root, "components"), tooltip, popup,
                    size[0] + "x" + size[1] + " after resize");
            scroll.setScrollOffset(0, scroll.maxScrollY());
            for (ProjectSUiKitV02Layout.Section section : layout.sections()) {
                UiNode sectionNode = find(root, section.id());
                check(sectionNode != null, "section after resize: " + section.id());
                UiRect visible = sectionNode.globalBounds();
                UiRect canonical = visible.offset(0, scroll.scrollY());
                check(Math.abs(canonical.y() - section.bounds().y()) < 1e-9
                                && Math.abs(canonical.bottom() - section.bounds().bottom()) < 1e-9,
                        "descendant reflow after resize: " + section.id());
            }
            UiDrawList drawList = new UiDrawList();
            new UiTree(root).render(drawList, kit.theme());
            check(drawList.clipDepth() == 0, "balanced resize render clips");
        }
    }

    private static java.util.List<UiNode> flatten(UiNode node) {
        java.util.ArrayList<UiNode> result = new java.util.ArrayList<>();
        result.add(node);
        for (UiNode child : node.children()) result.addAll(flatten(child));
        return result;
    }

    private static UiNode find(UiNode node, String id) {
        if (node == null) return null;
        if (node.id().equals(id)) return node;
        for (UiNode child : node.children()) {
            UiNode found = find(child, id);
            if (found != null) return found;
        }
        return null;
    }

    private static void assertPreviewBounds(UiNode components, Tooltip tooltip, Popup popup, String size) {
        check(components != null, "components panel for preview bounds " + size);
        check(tooltip.shownAt(450), "tooltip preview timing at " + size);
        check(popup.isOpen() && popup.visible() && popup.previewOnly()
                        && !popup.focusable() && !popup.hitTestable(),
                "popup preview remains non-modal at " + size);
        UiRect section = components.globalBounds();
        UiRect tooltipBounds = tooltip.placementBounds();
        UiRect popupBounds = popup.panelBounds();
        check(contains(section, tooltipBounds), "tooltip preview stays inside Components section " + size);
        check(contains(section, popupBounds), "popup preview stays inside Components section " + size);
        for (UiNode sibling : components.children()) {
            if (sibling != tooltip) {
                check(tooltipBounds.intersection(sibling.globalBounds()).isEmpty(),
                        "tooltip preview overlaps " + sibling.id() + " at " + size);
            }
            if (sibling != popup) {
                check(popupBounds.intersection(sibling.globalBounds()).isEmpty(),
                        "popup preview overlaps " + sibling.id() + " at " + size);
            }
        }
    }

    private static void assertNoPreviewSiblingIntersection(UiNode components, Tooltip tooltip, String size) {
        UiRect tooltipBounds = tooltip.placementBounds();
        for (UiNode sibling : components.children()) {
            if (sibling != tooltip) {
                check(tooltipBounds.intersection(sibling.globalBounds()).isEmpty(),
                        "tooltip full rectangle overlaps " + sibling.id() + " at " + size);
            }
        }
    }

    private static void assertTooltipDrawCommands(UiDrawList drawList, UiRect tooltipBounds,
                                                   UiRect visibleClip, String context) {
        java.util.List<UiRenderCommand> commands = drawList.commands();
        int surfaceIndex = -1;
        for (int index = 0; index < commands.size(); index++) {
            UiRenderCommand command = commands.get(index);
            if (command instanceof UiRenderCommand.RoundedSurface surface
                    && surface.bounds().equals(tooltipBounds)) {
                surfaceIndex = index;
                break;
            }
        }
        check(surfaceIndex >= 1, "Tooltip material command emitted " + context);
        check(commands.get(surfaceIndex - 1) instanceof UiRenderCommand.Shadow shadow
                        && contains(visibleClip, shadow.bounds()),
                "Tooltip shadow command stays in visible clip " + context);
        check(contains(visibleClip, tooltipBounds), "Tooltip rounded surface stays in visible clip " + context);
        check(surfaceIndex + 2 < commands.size()
                        && commands.get(surfaceIndex + 1) instanceof UiRenderCommand.Gradient gradient
                        && gradient.bounds().equals(tooltipBounds)
                        && commands.get(surfaceIndex + 2) instanceof UiRenderCommand.Border border
                        && border.bounds().equals(tooltipBounds),
                "Tooltip material command bounds are real placement bounds " + context);
        int textCount = 0;
        for (int index = surfaceIndex + 3; index < commands.size(); index++) {
            UiRenderCommand command = commands.get(index);
            if (!(command instanceof UiRenderCommand.Text text)) break;
            check(text.origin().x() >= tooltipBounds.x()
                            && text.origin().x() < tooltipBounds.right()
                            && text.origin().y() >= tooltipBounds.y()
                            && text.origin().y() < tooltipBounds.bottom(),
                    "Tooltip text command stays in placement bounds " + context);
            textCount++;
        }
        check(textCount == 3, "Tooltip emits title/description/shortcut draw commands " + context);
    }

    private static double scrollPositionForComponents(UiNode components, ScrollArea scroll, int position) {
        UiRect component = components.globalBounds().offset(0, scroll.scrollY());
        UiRect clip = scroll.globalBounds();
        double target = switch (position) {
            case 0 -> component.y() - clip.y();
            case 1 -> component.y() + (component.height() - clip.height()) / 2 - clip.y();
            case 2 -> component.bottom() - clip.bottom();
            default -> throw new IllegalArgumentException("position");
        };
        return Math.clamp(target, 0, scroll.maxScrollY());
    }

    private static UiRect visibleScrollClip(UiNode root, ScrollArea scroll) {
        return root.globalBounds().intersection(scroll.globalBounds());
    }

    private static String positionName(int position) {
        return switch (position) {
            case 0 -> "top";
            case 1 -> "middle";
            case 2 -> "bottom";
            case 3 -> "max";
            default -> "unknown";
        };
    }

    private static boolean contains(UiRect outer, UiRect inner) {
        return inner.x() >= outer.x() && inner.y() >= outer.y()
                && inner.right() <= outer.right() && inner.bottom() <= outer.bottom();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
