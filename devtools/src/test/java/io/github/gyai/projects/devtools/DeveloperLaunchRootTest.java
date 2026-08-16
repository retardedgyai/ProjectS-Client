package io.github.gyai.projects.devtools;

import io.github.gyai.projects.ui.runtime.UiDrawList;
import io.github.gyai.projects.ui.runtime.UiPoint;
import io.github.gyai.projects.ui.runtime.UiPointerEvent;
import io.github.gyai.projects.ui.runtime.UiModifiers;
import io.github.gyai.projects.ui.runtime.UiTheme;

public final class DeveloperLaunchRootTest {
    public static void main(String[] args) {
        DeveloperLaunchRoot root = new DeveloperLaunchRoot();
        root.layout(1280, 720);
        root.setEnvironment("Developer", "26.1.2");
        root.setServerStatus(DeveloperLaunchRoot.ServerStatus.READY);

        assert root.serverStatus() == DeveloperLaunchRoot.ServerStatus.READY;
        assert root.children().size() == 3;
        root.children().forEach(child -> {
            assert child.bounds().width() > 0;
            assert child.bounds().height() > 0;
            assert root.bounds().contains(center(child.bounds()));
        });

        UiDrawList drawList = new UiDrawList();
        root.render(drawList, UiTheme.dark());
        assert !drawList.commands().isEmpty();

        int[] actions = new int[3];
        root.bindActions(() -> actions[0]++, () -> actions[1]++, () -> actions[2]++);
        var connect = root.children().getFirst();
        UiPoint connectCenter = center(connect.bounds());
        connect.handleEvent(UiPointerEvent.down(0, connectCenter, 0, UiModifiers.none()));
        connect.handleEvent(UiPointerEvent.up(0, connectCenter, 0, UiModifiers.none()));
        assert actions[0] == 1;

        root.layout(640, 360);
        root.children().forEach(child -> {
            assert root.bounds().contains(center(child.bounds()));
        });
        for (int first = 0; first < root.children().size(); first++) {
            for (int second = first + 1; second < root.children().size(); second++) {
                assert root.children().get(first).bounds()
                        .intersection(root.children().get(second).bounds()).isEmpty();
            }
        }
        System.out.println("DEVELOPER_LAUNCH_ROOT_PASS: wide compact status actions");
    }

    private static UiPoint center(io.github.gyai.projects.ui.runtime.UiRect bounds) {
        return new UiPoint(bounds.x() + bounds.width() / 2,
                bounds.y() + bounds.height() / 2);
    }
}
