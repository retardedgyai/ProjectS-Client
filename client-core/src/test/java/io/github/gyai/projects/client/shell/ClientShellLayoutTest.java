package io.github.gyai.projects.client.shell;

import io.github.gyai.projects.ui.runtime.UiRect;

import java.util.ArrayList;
import java.util.List;

/** Frozen-size, breakpoint-edge, containment, non-overlap, and focus-order checks. */
public final class ClientShellLayoutTest {
    private static final List<int[]> SIZES = List.of(
            new int[]{1920, 1080}, new int[]{1280, 720}, new int[]{1024, 640},
            new int[]{1121, 720}, new int[]{1120, 720}, new int[]{901, 640},
            new int[]{900, 640}, new int[]{561, 640}, new int[]{560, 640},
            new int[]{320, 240});

    public static void main(String[] args) {
        for (int[] size : SIZES) {
            for (ClientShellPage page : ClientShellPage.values()) {
                for (ClientShellState state : ClientShellState.values()) {
                    ClientShellLayout.Snapshot layout = ClientShellLayout.compute(
                            size[0], size[1], page, state);
                    assert layout.bounds("shell").right() <= size[0] + .01;
                    assert layout.bounds("shell").bottom() <= size[1] + .01;
                    assert layout.bounds("content").right() <= layout.bounds("shell").right() + .01;
                    assert layout.bounds("content").bottom() <= layout.bounds("body").bottom() + .01;
                    assert layout.bounds("content.canvas").right() <= layout.bounds("view").right() + .01;
                    assert layout.bounds("content.canvas").height() >= layout.bounds("view").height() - .01;
                    assert !layout.focusOrder().contains("");
                    assert new ArrayList<>(layout.focusOrder()).stream().distinct().count()
                            == layout.focusOrder().size();
                    for (String focusId : layout.focusOrder()) {
                        assert layout.contains(focusId, focusId);
                        assert layout.bounds(focusId).width() > 0 && layout.bounds(focusId).height() > 0;
                    }
                    assertInteractiveControls(layout);
                    assertNoOverlap(layout, page, state);
                }
            }
        }
        assert ClientShellLayout.breakpoint(1121) == ClientShellLayout.Breakpoint.WIDE;
        assert ClientShellLayout.breakpoint(1120) == ClientShellLayout.Breakpoint.TABLET;
        assert ClientShellLayout.breakpoint(901) == ClientShellLayout.Breakpoint.TABLET;
        assert ClientShellLayout.breakpoint(900) == ClientShellLayout.Breakpoint.COMPACT;
        assert ClientShellLayout.breakpoint(561) == ClientShellLayout.Breakpoint.COMPACT;
        assert ClientShellLayout.breakpoint(560) == ClientShellLayout.Breakpoint.NARROW;
    }

    private static void assertNoOverlap(ClientShellLayout.Snapshot layout,
                                        ClientShellPage page, ClientShellState state) {
        List<String> ids = new ArrayList<>(List.of("content.intro"));
        if (page == ClientShellPage.HOME && state == ClientShellState.HOME) {
            ids.addAll(List.of("home.play", "home.server", "home.journey", "home.sample"));
        } else if (page == ClientShellPage.HOME) {
            ids.addAll(List.of("flow.primary", "flow.snapshot", "flow.note"));
        } else if (page == ClientShellPage.LIBRARY) {
            ids.addAll(List.of("library.horizon", "library.atelier", "library.home"));
        } else {
            ids.addAll(List.of("settings.motion", "settings.privacy", "settings.home"));
        }
        for (String id : ids) assert layout.contains(id, "content.canvas") : id;
        for (int first = 0; first < ids.size(); first++) {
            for (int second = first + 1; second < ids.size(); second++) {
                UiRect a = layout.bounds(ids.get(first));
                UiRect b = layout.bounds(ids.get(second));
                assert a.intersection(b).isEmpty() : ids.get(first) + " overlaps " + ids.get(second);
            }
        }
    }

    private static void assertInteractiveControls(ClientShellLayout.Snapshot layout) {
        List<String> controls = layout.focusOrder();
        for (String id : controls) {
            String parent = id.equals("brand") ? "topbar"
                    : id.startsWith("tab.") ? "tabs"
                    : id.startsWith("action.") ? "actions"
                    : id.startsWith("side.") ? "sidebar"
                    : id.startsWith("toolbar.") ? "toolbar" : "content.canvas";
            assert layout.contains(id, parent) : id + " outside " + parent;
        }
        for (int first = 0; first < controls.size(); first++) {
            for (int second = first + 1; second < controls.size(); second++) {
                String firstId = controls.get(first);
                String secondId = controls.get(second);
                if (!sameControlGroup(firstId, secondId)) continue;
                assert layout.bounds(firstId).intersection(layout.bounds(secondId)).isEmpty()
                        : firstId + " overlaps " + secondId;
            }
        }
    }

    private static boolean sameControlGroup(String first, String second) {
        String firstGroup = first.contains(".") ? first.substring(0, first.indexOf('.')) : first;
        String secondGroup = second.contains(".") ? second.substring(0, second.indexOf('.')) : second;
        return firstGroup.equals(secondGroup) && !firstGroup.equals("side")
                && !firstGroup.equals("tab") && !firstGroup.equals("action");
    }
}
