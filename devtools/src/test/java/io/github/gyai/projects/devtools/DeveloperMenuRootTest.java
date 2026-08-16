package io.github.gyai.projects.devtools;

import io.github.gyai.projects.ui.runtime.UiButton;
import io.github.gyai.projects.ui.runtime.UiDrawList;
import io.github.gyai.projects.ui.runtime.UiPoint;
import io.github.gyai.projects.ui.runtime.UiRect;
import io.github.gyai.projects.ui.runtime.UiTheme;

import java.util.concurrent.atomic.AtomicInteger;

public final class DeveloperMenuRootTest {
    public static void main(String[] args) {
        AtomicInteger activations = new AtomicInteger();
        DeveloperMenuRoot root = new DeveloperMenuRoot(
                activations::incrementAndGet,
                activations::incrementAndGet,
                activations::incrementAndGet,
                activations::incrementAndGet);
        assert root.children().size() == 4 : "dashboard must expose exactly four supported tools";

        assertLayout(root, 960, 540);
        assertLayout(root, 640, 360);

        root.setConnected(false);
        assert root.children().stream().noneMatch(node -> node.enabled()) : "server tools must lock offline";
        root.setConnected(true);
        assert root.children().stream().allMatch(node -> node.enabled()) : "server tools must unlock online";

        UiButton server = (UiButton) root.children().getFirst();
        UiRect bounds = server.bounds();
        UiPoint center = new UiPoint(bounds.x() + bounds.width() / 2, bounds.y() + bounds.height() / 2);
        assert server.pointerDown(center);
        assert server.pointerUp(center);
        assert activations.get() == 1 : "tool card must activate its action once";

        UiDrawList drawList = new UiDrawList();
        root.render(drawList, UiTheme.dark());
        drawList.assertBalanced();
        assert drawList.commands().size() >= 30 : "dashboard presentation is unexpectedly incomplete";
    }

    private static void assertLayout(DeveloperMenuRoot root, int width, int height) {
        root.layout(width, height);
        UiRect viewport = root.bounds();
        root.children().forEach(node -> {
            UiRect card = node.bounds();
            assert card.width() > 0 && card.height() > 0 : "tool card must have positive bounds";
            assert viewport.contains(new UiPoint(card.x(), card.y())) : "tool card starts outside viewport";
            assert viewport.contains(new UiPoint(card.right() - .01, card.bottom() - .01))
                    : "tool card ends outside viewport";
        });
    }
}
