package io.github.gyai.projects.devtools.ui;

import io.github.gyai.projects.ui.runtime.UiDrawList;
import io.github.gyai.projects.ui.runtime.UiNode;
import io.github.gyai.projects.ui.runtime.UiPoint;
import io.github.gyai.projects.ui.runtime.UiRect;
import io.github.gyai.projects.ui.runtime.UiRenderCommand;
import io.github.gyai.projects.ui.runtime.UiSurface;
import io.github.gyai.projects.ui.runtime.UiTree;

import java.util.ArrayList;
import java.util.List;

/** Real presentation-tree layout, clipping, hit-test and render-stack reference matrix. */
public final class ProjectSUiKitLayoutTest {
    public static void main(String[] args) {
        for (int[] size : new int[][]{{640, 360}, {854, 480}, {1920, 1080}, {2560, 720}}) {
            ProjectSUiKitLayout layout = ProjectSUiKitLayout.at(size[0], size[1]);
            assert layout.width() == size[0] && layout.height() == size[1];
            assert layout.materialCards().size() == 4;
            assert layout.buttonCards().size() == 5;
            assert layout.hasNonNegativeBounds() : size[0] + "x" + size[1];
            assert positive(layout.titleBounds()) : "title must be positive at " + size[0] + "x" + size[1];
            assert layout.controlsReachable() : "theme controls must stay reachable at " + size[0] + "x" + size[1];
            assert within(layout.titleBounds(), layout.viewport()) : "title must stay in viewport";
            assert !overlap(layout.materialCards()) : "material overlap at " + size[0] + "x" + size[1];
            assert !overlap(layout.buttonCards()) : "button overlap at " + size[0] + "x" + size[1];
            assert !overlap(layout.titleBounds(), layout.themeControls()) : "title/theme overlap at " + size[0] + "x" + size[1];
            assert !overlap(layout.titleBounds(), layout.accentControls()) : "title/accent overlap at " + size[0] + "x" + size[1];
            verifyPresentationTree(layout);
        }
        System.out.println("UI_KIT_LAYOUT_PASS: 640x360 854x480 1920x1080 wide-aspect tree-clip-hit-render-overlap");
    }

    private static void verifyPresentationTree(ProjectSUiKitLayout layout) {
        UiNode root = new UiNode("layout-test-root", layout.viewport());
        ProjectSUiKitPilot pilot = new ProjectSUiKitPilot();
        pilot.populate(root, layout.width(), layout.height(), () -> { });
        UiTree tree = new UiTree(root);
        UiNode background = find(root, "material-background");
        check(background instanceof UiSurface && background.globalBounds().equals(layout.viewport()),
                "pilot has one full-screen material surface");
        check(countFullScreenSurfaces(root, layout.viewport()) == 1,
                "pilot tree must not duplicate the full-screen material surface");
        UiNode content = find(root, "content-viewport");
        check(content != null && content.clipToBounds(), "presentation tree owns content clip");
        check(content.globalBounds().equals(layout.contentClip()), "content clip is applied in global tree coordinates");

        List<UiNode> gallery = new ArrayList<>();
        gallery.add(findRequired(root, "title"));
        for (int index = 0; index < layout.materialCards().size(); index++) {
            gallery.add(findRequired(root, "material-" + index));
            verifyClippedNode(tree, gallery.get(gallery.size() - 1), layout.materialCards().get(index), layout.contentClip());
        }
        for (int index = 0; index < layout.buttonCards().size(); index++) {
            gallery.add(findRequired(root, "button-" + index));
            verifyClippedNode(tree, gallery.get(gallery.size() - 1), layout.buttonCards().get(index), layout.contentClip());
        }
        for (String id : new String[]{"theme-light", "theme-dark", "accent-purple", "accent-blue"}) {
            gallery.add(findRequired(root, id));
        }
        assertNoSiblingOverlap(root);

        double midY = layout.contentClip().y() + Math.max(1, layout.contentClip().height() / 2);
        assertNoKitHit(tree, new UiPoint(layout.contentClip().x() - .5, midY));
        assertNoKitHit(tree, new UiPoint(layout.contentClip().right() + .5, midY));
        double midX = layout.contentClip().x() + Math.max(1, layout.contentClip().width() / 2);
        assertNoKitHit(tree, new UiPoint(midX, layout.contentClip().y() - .5));
        assertNoKitHit(tree, new UiPoint(midX, layout.contentClip().bottom() + .5));

        UiDrawList drawList = new UiDrawList();
        tree.render(drawList, pilot.theme());
        int depth = 0;
        int pushes = 0;
        int pops = 0;
        boolean sawContentPush = false;
        for (UiRenderCommand command : drawList.commands()) {
            if (command instanceof UiRenderCommand.PushClip push) {
                depth++;
                pushes++;
                if (push.clip().equals(layout.contentClip())) {
                    sawContentPush = true;
                    check(depth >= 2, "content clip is nested under the root clip");
                }
            } else if (command instanceof UiRenderCommand.PopClip) {
                check(depth > 0, "clip pop cannot underflow");
                depth--;
                pops++;
            }
        }
        check(sawContentPush && pushes == pops && depth == 0,
                "presentation render commands have balanced nested push/pop clips");
    }

    private static void assertNoSiblingOverlap(UiNode root) {
        List<UiNode> visible = new ArrayList<>();
        collectVisible(root, visible);
        for (int left = 0; left < visible.size(); left++) {
            for (int right = left + 1; right < visible.size(); right++) {
                UiNode first = visible.get(left);
                UiNode second = visible.get(right);
                if (first.isDescendantOf(second) || second.isDescendantOf(first)) continue;
                check(first.globalBounds().intersection(second.globalBounds()).isEmpty(),
                        "unintended visible sibling overlap: " + first.id() + " / " + second.id());
            }
        }
    }

    private static void collectVisible(UiNode node, List<UiNode> result) {
        if (!node.isEffectivelyVisible()) return;
        result.add(node);
        for (UiNode child : node.children()) collectVisible(child, result);
    }

    private static int countFullScreenSurfaces(UiNode node, UiRect viewport) {
        int count = node instanceof UiSurface && node.globalBounds().equals(viewport) ? 1 : 0;
        for (UiNode child : node.children()) count += countFullScreenSurfaces(child, viewport);
        return count;
    }

    private static void verifyClippedNode(UiTree tree, UiNode node, UiRect expectedGlobal, UiRect clip) {
        UiRect actual = node.globalBounds();
        check(actual.equals(expectedGlobal), node.id() + " global layout");
        check(actual.width() > 0 && actual.height() > 0, node.id() + " positive bounds");
        UiRect clipped = actual.intersection(clip);
        check(clipped.width() >= 0 && clipped.height() >= 0, node.id() + " non-negative real intersection");
        check(clipped.x() == Math.max(actual.x(), clip.x())
                        && clipped.y() == Math.max(actual.y(), clip.y())
                        && clipped.right() == Math.min(actual.right(), clip.right())
                        && clipped.bottom() == Math.min(actual.bottom(), clip.bottom()),
                node.id() + " intersection follows clip edges");
        check(!clipped.isEmpty(), node.id() + " remains reachable inside content clip");
        UiPoint center = new UiPoint(clipped.x() + clipped.width() / 2,
                clipped.y() + clipped.height() / 2);
        UiNode hit = tree.hitTest(center).orElse(null);
        check(hit == node, node.id() + " reachable by clipped hit test; actual="
                + (hit == null ? "none" : hit.id()) + " center=" + center);

        UiPoint outside = outsidePoint(actual, clip);
        if (outside != null) assertNoKitHit(tree, outside);
    }

    private static UiPoint outsidePoint(UiRect bounds, UiRect clip) {
        UiPoint[] candidates = {
                new UiPoint(bounds.x() + .5, bounds.y() + bounds.height() / 2),
                new UiPoint(bounds.right() - .5, bounds.y() + bounds.height() / 2),
                new UiPoint(bounds.x() + bounds.width() / 2, bounds.y() + .5),
                new UiPoint(bounds.x() + bounds.width() / 2, bounds.bottom() - .5)
        };
        for (UiPoint candidate : candidates) {
            if (bounds.contains(candidate) && !clip.contains(candidate)) return candidate;
        }
        return null;
    }

    private static void assertNoKitHit(UiTree tree, UiPoint point) {
        UiNode hit = tree.hitTest(point).orElse(null);
        check(hit == null || !isGalleryNode(hit.id()),
                "clip-outside point reached a card/button: " + (hit == null ? "none" : hit.id()) + " at " + point);
    }

    private static boolean isGalleryNode(String id) {
        return id.matches("(?:material|button)-\\d+");
    }

    private static UiNode findRequired(UiNode root, String id) {
        UiNode found = find(root, id);
        check(found != null, "missing presentation node " + id);
        return found;
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

    private static boolean positive(UiRect rect) { return rect.width() > 0 && rect.height() > 0; }

    private static boolean within(UiRect rect, UiRect viewport) {
        return rect.x() >= viewport.x() && rect.y() >= viewport.y()
                && rect.right() <= viewport.right() && rect.bottom() <= viewport.bottom();
    }

    private static boolean overlap(UiRect first, List<UiRect> others) {
        return others.stream().anyMatch(other -> !first.intersection(other).isEmpty());
    }

    private static boolean overlap(List<UiRect> rects) {
        for (int left = 0; left < rects.size(); left++) {
            for (int right = left + 1; right < rects.size(); right++) {
                if (!rects.get(left).intersection(rects.get(right)).isEmpty()) return true;
            }
        }
        return false;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
