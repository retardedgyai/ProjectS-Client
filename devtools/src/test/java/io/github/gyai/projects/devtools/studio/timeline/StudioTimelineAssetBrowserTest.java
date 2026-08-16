package io.github.gyai.projects.devtools.studio.timeline;

import io.github.gyai.projects.devtools.studio.assets.StudioAssetBrowser;
import io.github.gyai.projects.devtools.studio.assets.StudioAssetBrowserModel;
import io.github.gyai.projects.devtools.studio.assets.StudioAssetBrowserPresentation;
import io.github.gyai.projects.devtools.studio.assets.StudioAssetCard;
import io.github.gyai.projects.devtools.studio.assets.StudioAssetCategory;
import io.github.gyai.projects.ui.runtime.UiDrawList;
import io.github.gyai.projects.ui.runtime.UiButton;
import io.github.gyai.projects.ui.runtime.UiMaterialTier;
import io.github.gyai.projects.ui.runtime.UiNode;
import io.github.gyai.projects.ui.runtime.UiPoint;
import io.github.gyai.projects.ui.runtime.UiRect;
import io.github.gyai.projects.ui.runtime.UiRenderCommand;
import io.github.gyai.projects.ui.runtime.UiTheme;
import io.github.gyai.projects.ui.runtime.UiTree;
import io.github.gyai.projects.ui.runtime.icon.IconCatalog;
import io.github.gyai.projects.ui.runtime.theme.UiAccentPreset;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Focused, non-GUI acceptance checks for the Lane D Studio shell. */
public final class StudioTimelineAssetBrowserTest {
    private static final int[][] SIZES = {
            {640, 360}, {854, 480}, {1280, 720}, {1920, 1080}, {2560, 1440}, {2560, 720}
    };

    public static void main(String[] args) throws Exception {
        playbackStateAndActions();
        timelineLayouts();
        timelineCompositionAndRendering();
        timelineParentOffsetAndHitRouting();
        browserModel();
        browserLayouts();
        browserCompositionAndRendering();
        browserCloseRequestContract();
        sourceGuard();
        System.out.println("STUDIO_TIMELINE_ASSET_BROWSER_PASS: transport timeline asset-popup compact expanded theme icon source-guard");
    }

    private static void playbackStateAndActions() {
        StudioPlaybackModel playback = new StudioPlaybackModel();
        check(playback.state() == StudioPlaybackState.STOPPED && playback.playheadSeconds() == 0,
                "initial stopped state");
        playback.dispatch(StudioPlaybackAction.RESTART);
        check(playback.isPlaying() && playback.playheadSeconds() == 0, "restart starts from zero");
        playback.dispatch(StudioPlaybackAction.PLAY_PAUSE);
        check(playback.isPaused(), "play/pause pauses");
        playback.dispatch(StudioPlaybackAction.PLAY_PAUSE);
        check(playback.isPlaying(), "play/pause resumes");
        playback.dispatch(StudioPlaybackAction.SPEED);
        check(playback.speed() == StudioPlaybackSpeed.DOUBLE, "speed cycles");
        playback.dispatch(StudioPlaybackAction.LOOP);
        check(playback.looping(), "loop toggles on");
        playback.advance(2.5);
        check(playback.playheadSeconds() > 0, "playing advances");
        playback.dispatch(StudioPlaybackAction.STOP);
        check(playback.isStopped() && playback.playheadSeconds() == 0, "stop resets");
        playback.restart().setLooping(false).advance(10);
        check(playback.isStopped() && playback.playheadSeconds() == playback.durationSeconds(),
                "non-loop playback ends stopped at duration");
        playback.restart().setLooping(true).advance(10);
        check(playback.isPlaying() && playback.playheadSeconds() < playback.durationSeconds(),
                "loop playback wraps");
        playback.selectTrack(StudioTimelineTrack.TrackId.FLAME);
        check(playback.selectedTrackId() == StudioTimelineTrack.TrackId.FLAME, "track selection");
        check(playback.playPauseIcon().equals(io.github.gyai.projects.ui.runtime.IconKey.PAUSE),
                "playing pause icon");
        for (io.github.gyai.projects.ui.runtime.IconKey key : List.of(
                io.github.gyai.projects.ui.runtime.IconKey.RESTART,
                io.github.gyai.projects.ui.runtime.IconKey.PLAY,
                io.github.gyai.projects.ui.runtime.IconKey.PAUSE,
                io.github.gyai.projects.ui.runtime.IconKey.STOP,
                io.github.gyai.projects.ui.runtime.IconKey.LOOP,
                io.github.gyai.projects.ui.runtime.IconKey.SEARCH,
                io.github.gyai.projects.ui.runtime.IconKey.CLOSE,
                io.github.gyai.projects.ui.runtime.IconKey.SHAPE,
                io.github.gyai.projects.ui.runtime.IconKey.PARTICLE,
                io.github.gyai.projects.ui.runtime.IconKey.MOTION,
                io.github.gyai.projects.ui.runtime.IconKey.PHASE)) {
            check(IconCatalog.contains(key) && IconCatalog.spec(key) != null, "icon resolution " + key.id());
        }
    }

    private static void timelineLayouts() {
        StudioPlaybackModel playback = new StudioPlaybackModel();
        for (int[] size : SIZES) {
            UiRect bounds = new UiRect(0, size[1] - 168, size[0], 168);
            StudioTimelinePresentation.Layout expanded = StudioTimelinePresentation.layout(
                    bounds, StudioTimelineMode.EXPANDED, playback);
            check(expanded.mode().isExpanded() && expanded.bodyVisible(), "expanded timeline body " + size[0]);
            check(expanded.controls().size() == 5 && expanded.lanes().size() == 2,
                    "expanded transport/tracks " + size[0]);
            check(expanded.materialTier() == UiMaterialTier.GLASS_PANEL
                            && expanded.lanes().stream().anyMatch(lane -> lane.materialTier() == UiMaterialTier.ACCENT_GLASS),
                    "timeline panel and selected lane material " + size[0]);
            check(expanded.containsAllChrome(), "expanded containment " + size[0]);
            check(expanded.playheadFraction() >= 0 && expanded.playheadFraction() <= 1,
                    "playhead range " + size[0]);

            StudioTimelinePresentation.Layout compact = StudioTimelinePresentation.layout(
                    new UiRect(0, Math.max(0, size[1] - 36), size[0], 36), StudioTimelineMode.COMPACT, playback);
            check(compact.controls().size() == 4 && !compact.bodyVisible() && compact.lanes().isEmpty(),
                    "compact transport-only " + size[0]);
            check(compact.controls().stream().noneMatch(control -> control.action() == StudioPlaybackAction.SPEED),
                    "compact hides speed " + size[0]);
            check(compact.containsAllChrome(), "compact containment " + size[0]);
        }
        StudioTimelinePresentation.Layout sharedCompact = StudioTimelinePresentation.layout(
                new UiRect(0, 0, 640, 48), io.github.gyai.projects.devtools.studio.layout.StudioTimelineMode.COMPACT,
                playback);
        check(sharedCompact.controls().size() == 4 && !sharedCompact.bodyVisible(),
                "shared shell mode adapter");
        StudioTimeline hidden = new StudioTimeline(new UiRect(0, 0, 640, 168),
                io.github.gyai.projects.devtools.studio.layout.StudioTimelineMode.HIDDEN);
        check(!hidden.visible() && hidden.presentation().controls().isEmpty(), "shared hidden mode adapter");
    }

    private static void timelineCompositionAndRendering() {
        StudioTimeline timeline = new StudioTimeline(new UiRect(0, 192, 640, 168), StudioTimelineMode.EXPANDED);
        check(timeline.presentation().bounds().equals(new UiRect(0, 192, 640, 168)),
                "timeline preserves supplied bounds");
        check(timeline.presentation().containsAllChrome() && timeline.children().size() >= 8,
                "expanded timeline composition");
        timeline.dispatch(StudioPlaybackAction.PLAY_PAUSE);
        check(timeline.playback().isPlaying()
                        && timeline.control(StudioPlaybackAction.PLAY_PAUSE) != null,
                "timeline control action");
        timeline.setMode(StudioTimelineMode.COMPACT);
        check(timeline.children().size() == 4 && !timeline.bodyVisible(), "compact composition hides body");
        timeline.setMode(StudioTimelineMode.EXPANDED).selectTrack(StudioTimelineTrack.TrackId.FLAME);
        UiNode root = new UiNode("timeline-test-root", new UiRect(0, 0, 640, 360));
        root.addChild(timeline);
        UiDrawList drawList = new UiDrawList();
        new UiTree(root).render(drawList, UiTheme.light());
        check(drawList.commands().stream().anyMatch(command -> command instanceof UiRenderCommand.RoundedSurface
                        && ((UiRenderCommand.RoundedSurface) command).tier() == UiMaterialTier.GLASS_PANEL),
                "timeline glass panel render");
        check(drawList.commands().stream().anyMatch(command -> command instanceof UiRenderCommand.Icon
                        || command instanceof UiRenderCommand.StatefulIcon),
                "timeline icon render");
        check(drawList.commands().stream().anyMatch(command -> command instanceof UiRenderCommand.Text
                        && ((UiRenderCommand.Text) command).style().family()
                        == io.github.gyai.projects.ui.runtime.UiFontFamilyRole.TECHNICAL_MONO),
                "timeline technical typography render");
        for (io.github.gyai.projects.ui.runtime.UiTheme theme : List.of(
                UiTheme.light(), UiTheme.dark(), UiTheme.light(UiAccentPreset.CYAN))) {
            UiDrawList themed = new UiDrawList();
            new UiTree(root).render(themed, theme);
            check(!themed.commands().isEmpty(), "timeline theme render");
        }
    }

    private static void timelineParentOffsetAndHitRouting() {
        UiNode root = new UiNode("offset-root", new UiRect(0, 0, 1280, 720));
        UiNode parent = new UiNode("offset-parent", new UiRect(120, 40, 960, 600));
        root.addChild(parent);

        StudioPlaybackModel standardPlayback = new StudioPlaybackModel();
        StudioTimeline standard = new StudioTimeline("offset-standard", new UiRect(48, 96, 640, 168),
                StudioTimelineMode.EXPANDED, standardPlayback);
        parent.addChild(standard);
        UiRect standardLocal = new UiRect(0, 0, standard.bounds().width(), standard.bounds().height());
        for (UiNode child : standard.children()) {
            check(contains(standardLocal, child.bounds()), "standard child stays parent-local: " + child.id());
            check(contains(standard.globalBounds(), child.globalBounds()),
                    "standard child stays inside global timeline: " + child.id());
            check(contains(parent.globalBounds(), child.globalBounds()),
                    "standard child stays inside parent clip: " + child.id());
        }
        UiNode standardPlayPause = standard.control(StudioPlaybackAction.PLAY_PAUSE);
        activateAt(root, standardPlayPause);
        check(standardPlayback.isPlaying(), "standard play/pause hit fires at offset origin");

        StudioPlaybackModel compactPlayback = new StudioPlaybackModel();
        StudioTimeline compact = new StudioTimeline("offset-compact", new UiRect(48, 420, 640, 36),
                StudioTimelineMode.COMPACT, compactPlayback);
        parent.addChild(compact);
        UiRect compactLocal = new UiRect(0, 0, compact.bounds().width(), compact.bounds().height());
        check(compact.children().size() == 4, "640x360 transport-only child count at offset origin");
        for (UiNode child : compact.children()) {
            check(contains(compactLocal, child.bounds()), "compact child stays parent-local: " + child.id());
            check(contains(compact.globalBounds(), child.globalBounds()),
                    "compact child stays inside global timeline: " + child.id());
            check(contains(parent.globalBounds(), child.globalBounds()),
                    "compact child stays inside parent clip: " + child.id());
        }
        UiNode compactRestart = compact.control(StudioPlaybackAction.RESTART);
        activateAt(root, compactRestart);
        check(compactPlayback.isPlaying(), "compact restart hit fires at offset origin");
    }

    private static void browserModel() {
        StudioAssetBrowserModel model = new StudioAssetBrowserModel();
        check(!model.isOpen() && model.cards().size() == 6 && model.categories().size() == 3,
                "browser closed demo model");
        check(model.filteredCards().size() == 2 && model.category() == StudioAssetCategory.SHAPE,
                "shape filter");
        model.open().selectCategory(StudioAssetCategory.PARTICLE);
        check(model.isOpen() && model.filteredCards().size() == 2, "particle category");
        model.setSearchFocused(true).setSearchQuery("flame");
        check(model.searchChrome().active() && model.filteredCards().size() == 1,
                "search chrome/filter");
        check(model.selectCard("particle-flame") && model.selectedCard().orElseThrow().id().equals("particle-flame"),
                "card selection");
        check(!model.selectCard("shape-spiral"), "hidden card cannot be selected");
        model.selectCategory(StudioAssetCategory.MOTION).clearSearch();
        check(model.filteredCards().size() == 2 && model.selectedCard().isEmpty(),
                "category transition clears stale selection");
        model.close();
        check(!model.isOpen() && !model.searchFocused(), "browser close state");
    }

    private static void browserLayouts() {
        StudioAssetBrowserModel model = new StudioAssetBrowserModel().open();
        for (int[] size : SIZES) {
            double popupWidth = Math.max(260, Math.min(size[0] - 24, 420));
            double popupHeight = Math.max(220, Math.min(size[1] - 24, 420));
            UiRect popup = new UiRect(Math.max(0, size[0] - popupWidth - 12), 12, popupWidth, popupHeight);
            StudioAssetBrowserPresentation.Layout layout = StudioAssetBrowserPresentation.layout(popup, model);
            check(layout.popup() && layout.materialTier() == UiMaterialTier.GLASS_SOLID,
                    "solid popup material " + size[0]);
            check(layout.popupBounds().equals(popup), "supplied popup bounds " + size[0]);
            check(layout.categories().size() == 3 && layout.cells().size() == 2,
                    "category/grid count " + size[0]);
            check(layout.containsAllChrome(), "popup containment " + size[0]);
            check(layout.searchField().iconKey().equals(io.github.gyai.projects.ui.runtime.IconKey.SEARCH),
                    "search icon " + size[0]);
            check(layout.closeButton().right() <= popup.right()
                            && layout.closeButton().bottom() <= popup.bottom(),
                    "close containment " + size[0]);
        }
    }

    private static void browserCompositionAndRendering() {
        StudioAssetBrowser browser = new StudioAssetBrowser(new UiRect(196, 24, 420, 420));
        check(!browser.isOpen() && !browser.visible(), "browser starts closed");
        check(browser.presentation().popupBounds().equals(new UiRect(196, 24, 420, 420)),
                "browser preserves supplied popup bounds");
        browser.open().selectCategory(StudioAssetCategory.PARTICLE).setSearchQuery("flame");
        check(browser.isOpen() && browser.visible() && browser.presentation().cells().size() == 1,
                "browser opens and filters");
        check(browser.presentation().containsAllChrome(), "browser composition containment");
        check(browser.selectCard("particle-flame"), "browser card action");
        UiNode root = new UiNode("browser-test-root", new UiRect(0, 0, 640, 480));
        root.addChild(browser);
        UiDrawList drawList = new UiDrawList();
        new UiTree(root).render(drawList, UiTheme.dark());
        check(drawList.commands().stream().anyMatch(command -> command instanceof UiRenderCommand.RoundedSurface surface
                        && surface.tier() == UiMaterialTier.GLASS_SOLID), "browser solid render");
        check(drawList.commands().stream().anyMatch(command -> command instanceof UiRenderCommand.Icon
                        || command instanceof UiRenderCommand.StatefulIcon),
                "browser icon render");
        browser.close();
        check(!browser.isOpen() && !browser.visible() && browser.children().isEmpty(), "browser closes cleanly");
    }

    private static void browserCloseRequestContract() {
        StudioAssetBrowser browser = new StudioAssetBrowser(new UiRect(200, 80, 360, 320));
        browser.open();
        int[] requests = {0};
        boolean[] workspaceOpen = {true};
        browser.setOnCloseRequest(() -> {
            requests[0]++;
            workspaceOpen[0] = false;
            browser.close();
        });
        UiNode root = new UiNode("close-request-root", new UiRect(0, 0, 800, 520));
        root.addChild(browser);
        UiNode closeButton = browser.children().stream()
                .filter(child -> child.id().equals("studio-asset-browser-close"))
                .findFirst().orElseThrow();
        activateAt(root, closeButton);
        check(requests[0] == 1 && !workspaceOpen[0], "close callback fires once from close affordance");
        check(!browser.isOpen() && !browser.visible() && browser.children().isEmpty(),
                "authoritative close leaves integrated-ready state");
        check(!browser.requestClose() && requests[0] == 1, "closed browser does not repeat callback");

        browser.open();
        browser.close();
        check(requests[0] == 1, "component-local close does not invoke host callback");
        browser.setOnCloseRequest(null).open();
        check(browser.requestClose() && !browser.isOpen() && !browser.visible(),
                "null callback restores component-local close");
    }

    private static void sourceGuard() throws Exception {
        List<Path> roots = List.of(
                Path.of("devtools/src/main/java/io/github/gyai/projects/devtools/studio/timeline"),
                Path.of("devtools/src/main/java/io/github/gyai/projects/devtools/studio/assets"));
        for (Path root : roots) try (var paths = Files.walk(root)) {
            for (Path path : paths.filter(value -> value.toString().endsWith(".java")).toList()) {
                String source = Files.readString(path);
                check(!source.contains("net.minecraft") && !source.contains("Widget")
                                && !source.contains("SkillVfxTimeline"),
                        "Studio source boundary " + path);
            }
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void activateAt(UiNode root, UiNode expected) {
        UiRect bounds = expected.globalBounds();
        UiPoint point = new UiPoint(bounds.x() + bounds.width() / 2,
                bounds.y() + bounds.height() / 2);
        UiNode hit = root.hitTest(point).orElseThrow();
        check(hit == expected, "hit-test resolves expected control: " + expected.id());
        check(hit instanceof UiButton, "hit-test control is button: " + expected.id());
        UiButton button = (UiButton) hit;
        check(button.pointerDown(point) && button.pointerUp(point), "button activation: " + expected.id());
    }

    private static boolean contains(UiRect outer, UiRect inner) {
        return inner.x() >= outer.x() && inner.y() >= outer.y()
                && inner.right() <= outer.right() && inner.bottom() <= outer.bottom();
    }
}
