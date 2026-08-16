package io.github.gyai.projects.client;

import io.github.gyai.projects.client.shell.ClientShellAction;
import io.github.gyai.projects.client.shell.ClientShellAccessibility;
import io.github.gyai.projects.client.shell.ClientShellDataSnapshot;
import io.github.gyai.projects.client.shell.ClientShellLayout;
import io.github.gyai.projects.client.shell.ClientShellModel;
import io.github.gyai.projects.client.shell.ClientShellPage;
import io.github.gyai.projects.client.shell.ClientShellPalette;
import io.github.gyai.projects.client.shell.ClientShellProfile;
import io.github.gyai.projects.client.shell.ClientShellState;
import io.github.gyai.projects.client.shell.ClientShellSnapshot;
import io.github.gyai.projects.minecraft.adapter.MinecraftUiRuntimeResources;
import io.github.gyai.projects.ui.runtime.IconKey;
import io.github.gyai.projects.ui.runtime.IconSpec;
import io.github.gyai.projects.ui.runtime.TextStyle;
import io.github.gyai.projects.ui.runtime.UiAccessibilityMetadata;
import io.github.gyai.projects.ui.runtime.UiAccessibilityRole;
import io.github.gyai.projects.ui.runtime.UiButton;
import io.github.gyai.projects.ui.runtime.UiButtonState;
import io.github.gyai.projects.ui.runtime.UiColor;
import io.github.gyai.projects.ui.runtime.UiColorRole;
import io.github.gyai.projects.ui.runtime.UiDrawList;
import io.github.gyai.projects.ui.runtime.UiEvent;
import io.github.gyai.projects.ui.runtime.UiFontFamilyRole;
import io.github.gyai.projects.ui.runtime.UiFontWeight;
import io.github.gyai.projects.ui.runtime.UiNode;
import io.github.gyai.projects.ui.runtime.UiPoint;
import io.github.gyai.projects.ui.runtime.UiRect;
import io.github.gyai.projects.ui.runtime.UiScrollEvent;
import io.github.gyai.projects.ui.runtime.UiTheme;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Renderer-neutral ProjectS Client Shell tree. Minecraft only owns the host screen; every visual
 * command below is emitted through UiDrawList and every control is a pure UiButton.
 */
final class ClientShellRoot extends UiNode {
    private static final TextStyle EYEBROW = style(UiFontWeight.BOLD, 10, 13);
    private static final TextStyle TITLE = style(UiFontWeight.SEMIBOLD, 32, 36);
    private static final TextStyle CARD_TITLE = style(UiFontWeight.SEMIBOLD, 17, 21);
    private static final TextStyle BODY = style(UiFontWeight.NORMAL, 12, 17);
    private static final TextStyle BODY_STRONG = style(UiFontWeight.SEMIBOLD, 12, 17);
    private static final TextStyle SMALL = style(UiFontWeight.NORMAL, 10, 14);
    private static final TextStyle SMALL_STRONG = style(UiFontWeight.SEMIBOLD, 10, 14);
    private static final TextStyle BUTTON = style(UiFontWeight.SEMIBOLD, 11, 15);
    private static final TextStyle AVATAR = style(UiFontWeight.BOLD, 15, 18);

    private final ClientShellModel model;
    private final boolean constructionGate;
    private ClientShellLayout.Snapshot layout;
    private ShellContentNode content;
    private int laidOutWidth = 1;
    private int laidOutHeight = 1;
    private Runnable focusReset = () -> { };
    private Supplier<String> focusCapture = () -> null;
    private Consumer<String> focusRestore = ignored -> { };
    private Runnable accessibilityChanged = () -> { };

    ClientShellRoot(ClientShellModel model, boolean constructionGate) {
        super("client-shell", new UiRect(0, 0, 1, 1));
        this.model = Objects.requireNonNull(model, "model");
        this.constructionGate = constructionGate;
        setClipToBounds(true);
        setAccessibility(UiAccessibilityMetadata.of(
                UiAccessibilityRole.SURFACE, "ProjectS Client Hub"));
        setVisible(constructionGate);
    }

    ClientShellModel model() { return model; }

    ClientShellLayout.Snapshot layoutSnapshot() { return layout; }

    void bindFocusReset(Runnable nextFocusReset) {
        focusReset = nextFocusReset == null ? () -> { } : nextFocusReset;
    }

    void bindFocusRestoration(Supplier<String> capture, Consumer<String> restore) {
        focusCapture = capture == null ? () -> null : capture;
        focusRestore = restore == null ? ignored -> { } : restore;
    }

    void bindAccessibilityChanged(Runnable listener) {
        accessibilityChanged = listener == null ? () -> { } : listener;
    }

    void layout(int width, int height) {
        laidOutWidth = Math.max(1, width);
        laidOutHeight = Math.max(1, height);
        if (!constructionGate) return;
        refreshLayout();
        setBounds(new UiRect(0, 0, laidOutWidth, laidOutHeight));
        rebuildTree();
    }

    void dispatch(ClientShellAction action) {
        if (!constructionGate || action == null) return;
        String previousFocus = focusCapture.get();
        model.dispatch(action);
        refreshLayout();
        rebuildTree();
        restoreFocus(previousFocus);
        accessibilityChanged.run();
    }

    void tick(long nowMillis) {
        if (!constructionGate) return;
        String previousFocus = focusCapture.get();
        ClientShellSnapshot previous = model.state();
        model.tick(nowMillis);
        if (!previous.equals(model.state())) {
            refreshLayout();
            rebuildTree();
            restoreFocus(previousFocus);
            accessibilityChanged.run();
        }
    }

    private void restoreFocus(String previousFocus) {
        if (previousFocus != null) focusRestore.accept(previousFocus);
        if (focusCapture.get() == null) focusReset.run();
    }

    private void refreshLayout() {
        layout = ClientShellLayout.compute(laidOutWidth, laidOutHeight,
                model.state().page(), model.state().state());
    }

    void cycleProfile() {
        ClientShellDataSnapshot data = model.data();
        List<ClientShellProfile> profiles = data.profiles();
        int current = 0;
        for (int index = 0; index < profiles.size(); index++) {
            if (profiles.get(index).id().equals(model.state().profileId())) {
                current = index;
                break;
            }
        }
        dispatch(ClientShellAction.selectProfile(profiles.get((current + 1) % profiles.size()).id()));
    }

    void rebuildTree() {
        if (!constructionGate || layout == null) return;
        for (UiNode child : children()) removeChild(child);
        addChromeControls();
        content = new ShellContentNode(this, layout.bounds("view"), layout.contentHeight());
        addChild(content);
        content.buildControls();
        ClientShellAccessibility narration = ClientShellAccessibility.from(model.state(), model.data(),
                layout.breakpoint());
        setAccessibility(UiAccessibilityMetadata.of(
                UiAccessibilityRole.SURFACE, narration.label(), narration.value()));
    }

    private void addChromeControls() {
        UiRect brand = layout.bounds("brand");
        addChild(new ShellButton("brand", brand, "ProjectS", IconKey.BRAND,
                ShellButton.Kind.BRAND, () -> dispatch(ClientShellAction.navigateHome()),
                model.state().page() == ClientShellPage.HOME, "ProjectS Client Hub home"));

        UiRect tabs = layout.bounds("tabs");
        double tabWidth = Math.max(1, tabs.width() / 3);
        addChild(new ShellButton("tab.home", new UiRect(tabs.x(), tabs.y(), tabWidth, tabs.height()),
                "Home", IconKey.HOME, ShellButton.Kind.NAV,
                () -> dispatch(ClientShellAction.navigateHome()),
                model.state().page() == ClientShellPage.HOME, "Home"));
        addChild(new ShellButton("tab.library", new UiRect(tabs.x() + tabWidth, tabs.y(), tabWidth, tabs.height()),
                "Library", IconKey.LIBRARY, ShellButton.Kind.NAV,
                () -> dispatch(ClientShellAction.navigateLibrary()),
                model.state().page() == ClientShellPage.LIBRARY, "Library"));
        addChild(new ShellButton("tab.settings", new UiRect(tabs.x() + tabWidth * 2, tabs.y(),
                tabs.width() - tabWidth * 2, tabs.height()),
                "Settings", IconKey.SLIDERS, ShellButton.Kind.NAV,
                () -> dispatch(ClientShellAction.navigateSettings()),
                model.state().page() == ClientShellPage.SETTINGS, "Settings"));

        UiRect actions = layout.bounds("actions");
        boolean compactActions = layout.breakpoint() == ClientShellLayout.Breakpoint.COMPACT
                || layout.breakpoint() == ClientShellLayout.Breakpoint.NARROW;
        double actionX = actions.right() - (compactActions ? 72 : 76);
        double actionHeight = Math.min(34, actions.height());
        addChild(new ShellButton("action.settings", new UiRect(actionX, actions.y(), 34, actionHeight), "",
                IconKey.SLIDERS, ShellButton.Kind.ICON,
                () -> dispatch(ClientShellAction.openAccount()), false, "Open settings"));
        addChild(new ShellButton("action.account", new UiRect(actions.right() - 34, actions.y(), 34, actionHeight), "",
                null, ShellButton.Kind.ACCOUNT,
                () -> dispatch(ClientShellAction.openAccount()), false, "Open sample account settings"));

        UiRect sidebar = layout.bounds("sidebar");
        boolean sidebarVisible = sidebar.width() > 0 && sidebar.height() > 0;
        double sideX = sidebar.x() + ClientShellPalette.spacing(12);
        double sideW = Math.max(0, sidebar.width() - ClientShellPalette.spacing(24));
        addChild(sideButton("side.home", sideX, sidebar.y() + 45, sideW, "Hub home", IconKey.HOME,
                model.state().page() == ClientShellPage.HOME, ClientShellAction.navigateHome(), sidebarVisible));
        addChild(sideButton("side.library", sideX, sidebar.y() + 93, sideW, "Profiles", IconKey.LIBRARY,
                model.state().page() == ClientShellPage.LIBRARY, ClientShellAction.navigateLibrary(), sidebarVisible));
        addChild(sideButton("side.settings", sideX, sidebar.bottom() - 55, sideW, "Preferences", IconKey.SLIDERS,
                model.state().page() == ClientShellPage.SETTINGS, ClientShellAction.navigateSettings(), sidebarVisible));

        UiRect review = layout.bounds("toolbar.review");
        addChild(new ShellButton("toolbar.review", review,
                "Review mode: " + stateLabel(model.state().state()), IconKey.EYE,
                ShellButton.Kind.SELECTOR, this::cycleReviewState, false, "Choose a prototype state to review"));
    }

    private ShellButton sideButton(String id, double x, double y, double width, String label,
                                   IconKey icon, boolean selected, ClientShellAction action,
                                   boolean visible) {
        ShellButton button = new ShellButton(id, new UiRect(x, y, width, 40), label, icon,
                ShellButton.Kind.SIDE, () -> dispatch(action), selected, label);
        button.setVisible(visible);
        return button;
    }

    private void cycleReviewState() {
        ClientShellState[] states = ClientShellState.values();
        int next = (model.state().state().ordinal() + 1) % states.length;
        dispatch(ClientShellAction.review(states[next]));
    }

    @Override
    protected void appendSelf(UiDrawList drawList, UiTheme theme, UiRect globalBounds, UiRect clip) {
        if (!visualsReady()) {
            for (UiNode child : children()) child.setVisible(false);
            return;
        }
        for (UiNode child : children()) {
            boolean sidebarControl = child.id().startsWith("side.");
            child.setVisible(!sidebarControl || layout.bounds("sidebar").width() > 0);
        }
        if (layout == null) return;
        drawBackgroundLayer(drawList);
        UiRect shell = layout.bounds("shell");
        drawList.shadow(shell.offset(0, 4), ClientShellPalette.spacing(12), ClientShellPalette.SHADOW);
        drawList.roundedSurface(shell, ClientShellPalette.radius(30), ClientShellPalette.CONTAINER,
                io.github.gyai.projects.ui.runtime.UiMaterialTier.GLASS_PANEL);
        drawList.border(shell, ClientShellPalette.radius(30), 1, ClientShellPalette.subtle(ClientShellPalette.OUTLINE));
        drawChrome(drawList);
    }

    /**
     * The shell background is a separate flat/translucent layer. It intentionally does not emit a
     * masked UiRenderCommand.Gradient; the renderer profile can replace this layer without making
     * surfaces depend on an unadvertised gradient capability.
     */
    private void drawBackgroundLayer(UiDrawList drawList) {
        drawList.fillRect(new UiRect(0, 0, laidOutWidth, laidOutHeight), ClientShellPalette.PAGE);
        drawList.roundedSurface(new UiRect(-laidOutWidth * .18, -laidOutHeight * .28,
                        laidOutWidth * 1.36, laidOutHeight * .86),
                ClientShellPalette.radius(laidOutHeight * .42),
                ClientShellPalette.PRIMARY.withAlpha(10),
                io.github.gyai.projects.ui.runtime.UiMaterialTier.GLASS_THIN);
        drawList.roundedSurface(new UiRect(laidOutWidth * .32, laidOutHeight * .42,
                        laidOutWidth * .82, laidOutHeight * .72),
                ClientShellPalette.radius(laidOutHeight * .36),
                ClientShellPalette.HIGHEST.withAlpha(20),
                io.github.gyai.projects.ui.runtime.UiMaterialTier.GLASS_THIN);
    }

    private void drawChrome(UiDrawList drawList) {
        UiRect topbar = layout.bounds("topbar");
        UiRect body = layout.bounds("body");
        UiRect sidebar = layout.bounds("sidebar");
        UiRect contentBounds = layout.bounds("content");
        UiRect toolbar = layout.bounds("toolbar");
        UiRect footer = layout.bounds("footer");
        drawList.fillRect(topbar, ClientShellPalette.container(ClientShellPalette.HIGHEST));
        if (layout.breakpoint() != ClientShellLayout.Breakpoint.NARROW) {
            UiRect actions = layout.bounds("actions");
            drawList.roundedSurface(new UiRect(actions.x(), actions.y() + 6, 112, 26), 999,
                    ClientShellPalette.HIGHEST.withAlpha(150),
                    io.github.gyai.projects.ui.runtime.UiMaterialTier.GLASS_THIN);
            dot(drawList, actions.x() + 10, actions.y() + 16, ClientShellPalette.MUTED);
            text(drawList, actions.x() + 20, actions.y() + 12, "Offline concept", SMALL, ClientShellPalette.MUTED);
        }
        drawList.fillRect(body, ClientShellPalette.CONTAINER.withAlpha(150));
        if (sidebar.width() > 0) {
            drawList.fillRect(sidebar, ClientShellPalette.PAGE.withAlpha(120));
            drawList.border(new UiRect(sidebar.right() - 1, sidebar.y(), 1, sidebar.height()),
                    0, 1, ClientShellPalette.subtle(ClientShellPalette.OUTLINE));
            drawSidebarLabels(drawList, sidebar);
        }
        drawList.fillRect(contentBounds, ClientShellPalette.CONTAINER.withAlpha(115));
        drawList.border(toolbar, 0, 1, ClientShellPalette.subtle(ClientShellPalette.OUTLINE));
        UiRect brand = layout.bounds("brand");
        text(drawList, brand.x() + 47, brand.y() + 31, "CLIENT HUB · SAMPLE DATA", SMALL,
                ClientShellPalette.MUTED);
        drawToolbar(drawList);
        drawFooter(drawList, footer);
    }

    private void drawSidebarLabels(UiDrawList drawList, UiRect sidebar) {
        double x = sidebar.x() + ClientShellPalette.spacing(23);
        text(drawList, x, sidebar.y() + 23, "WORKSPACE", EYEBROW, ClientShellPalette.MUTED);
        text(drawList, x, sidebar.y() + 157, "SAMPLE SESSION", EYEBROW, ClientShellPalette.MUTED);
        UiRect session = new UiRect(x, sidebar.y() + 174,
                Math.max(0, sidebar.width() - ClientShellPalette.spacing(40)), 48);
        drawList.roundedSurface(session, ClientShellPalette.radius(12), ClientShellPalette.RAISED,
                io.github.gyai.projects.ui.runtime.UiMaterialTier.GLASS_THIN);
        icon(drawList, session.x() + 10, session.y() + 14, 20, IconKey.SERVER, ClientShellPalette.PRIMARY);
        text(drawList, session.x() + 38, session.y() + 14, model.data().sessionName(), SMALL_STRONG,
                ClientShellPalette.TEXT);
        text(drawList, session.x() + 38, session.y() + 30, model.data().sessionStatus(), SMALL,
                ClientShellPalette.MUTED);
        dot(drawList, session.right() - 13, session.y() + 18, ClientShellPalette.SUCCESS);
        text(drawList, x, sidebar.bottom() - 34,
                "Every value shown here is local sample data.", SMALL, ClientShellPalette.MUTED);
    }

    private void drawToolbar(UiDrawList drawList) {
        if (layout.breakpoint() == ClientShellLayout.Breakpoint.NARROW) return;
        UiRect breadcrumb = layout.bounds("toolbar.breadcrumb");
        String current = switch (model.state().page()) {
            case HOME -> stateLabel(model.state().state());
            case LIBRARY -> "Profiles";
            case SETTINGS -> "Preferences";
        };
        text(drawList, breadcrumb.x(), breadcrumb.y() + 24, "ProjectS", SMALL, ClientShellPalette.MUTED);
        icon(drawList, breadcrumb.x() + 52, breadcrumb.y() + 19, 13, IconKey.CHEVRON_RIGHT,
                ClientShellPalette.OUTLINE);
        text(drawList, breadcrumb.x() + 72, breadcrumb.y() + 24, current, SMALL_STRONG,
                ClientShellPalette.TEXT);
    }

    private void drawFooter(UiDrawList drawList, UiRect footer) {
        drawList.border(footer, 0, 1, ClientShellPalette.subtle(ClientShellPalette.OUTLINE));
        if (layout.breakpoint() == ClientShellLayout.Breakpoint.NARROW) {
            dot(drawList, footer.x() + 20, footer.y() + 16, ClientShellPalette.PRIMARY);
            text(drawList, footer.x() + 33, footer.y() + 12,
                    model.data().footerStatus(), SMALL, ClientShellPalette.MUTED);
            text(drawList, footer.x() + 20, footer.y() + 34,
                    "ESC  Back to hub", SMALL_STRONG, ClientShellPalette.MUTED);
        } else {
            dot(drawList, footer.x() + 22, footer.y() + footer.height() / 2 - 3, ClientShellPalette.PRIMARY);
            text(drawList, footer.x() + 35, footer.y() + footer.height() / 2 - 7,
                    model.data().footerStatus(), SMALL, ClientShellPalette.MUTED);
            text(drawList, footer.right() - 94, footer.y() + footer.height() / 2 - 7,
                    "ESC  Back to hub", SMALL_STRONG, ClientShellPalette.MUTED);
        }
    }

    void drawContent(UiDrawList drawList, double scrollY) {
        if (layout == null || !visualsReady()) return;
        ClientShellDataSnapshot data = model.data();
        double yOffset = -scrollY;
        UiRect intro = shifted("content.intro", yOffset);
        ClientShellPage page = model.state().page();
        ClientShellState state = model.state().state();
        if (page == ClientShellPage.LIBRARY) {
            drawLibrary(drawList, data, intro, yOffset);
        } else if (page == ClientShellPage.SETTINGS) {
            drawSettings(drawList, intro, yOffset);
        } else if (state == ClientShellState.HOME) {
            drawHome(drawList, data, intro, yOffset);
        } else {
            drawFlow(drawList, data, state, intro, yOffset);
        }
    }

    private void drawHome(UiDrawList drawList, ClientShellDataSnapshot data,
                          UiRect intro, double yOffset) {
        ClientShellProfile profile = data.profile(model.state().profileId());
        drawIntro(drawList, intro, IconKey.SPARKLE, "CLIENT HUB / SAMPLE DATA",
                "Choose a world.", "Stay in control.",
                "A quiet starting point for your ProjectS client sessions. Select a sample profile, then review the complete launch story without leaving this offline shell.");
        boolean compact = isCompact();
        double statusWidth = Math.min(190, intro.width());
        double statusX = compact ? intro.x() : intro.right() - statusWidth;
        double statusY = intro.y() + 8;
        drawStatusSurface(drawList, statusX, statusY, statusWidth, 40,
                "Hub is ready", "Local concept state - no live connection", ClientShellPalette.SUCCESS);

        UiRect play = shifted("home.play", yOffset);
        card(drawList, play, ClientShellPalette.RAISED);
        text(drawList, play.x() + 22, play.y() + 25, "SELECTED PROFILE", EYEBROW, ClientShellPalette.MUTED);
        pill(drawList, play.right() - 96, play.y() + 13, 78, 24, "Mock profile", ClientShellPalette.PRIMARY);
        avatar(drawList, play.x() + 24, play.y() + 57, profile, 52);
        text(drawList, play.x() + 91, play.y() + 73, profile.name(), CARD_TITLE, ClientShellPalette.TEXT);
        text(drawList, play.x() + 91, play.y() + 96, profile.description(), BODY, ClientShellPalette.MUTED);
        text(drawList, play.x() + 91, play.y() + 116, "Account: " + data.accountName() + " · local preview", SMALL,
                ClientShellPalette.MUTED);
        boolean compactPlay = layout.breakpoint() == ClientShellLayout.Breakpoint.COMPACT
                || layout.breakpoint() == ClientShellLayout.Breakpoint.NARROW;
        drawMetaGrid(drawList, play.x() + 22, play.y() + (compactPlay ? 130 : 140), play.width() - 44,
                List.of("Server", profile.server(), "Version", profile.version(), "Session", "Ready · mock"),
                3, compactPlay ? 38 : 62);

        UiRect server = shifted("home.server", yOffset);
        card(drawList, server, ClientShellPalette.CONTAINER);
        text(drawList, server.x() + 20, server.y() + 25, "SERVER PROFILES", EYEBROW, ClientShellPalette.MUTED);
        text(drawList, server.x() + 20, server.y() + 52, "Pick a profile", CARD_TITLE, ClientShellPalette.TEXT);
        icon(drawList, server.right() - 42, server.y() + 22, 20, IconKey.SERVER, ClientShellPalette.PRIMARY);
        text(drawList, server.x() + 20, server.y() + 81, "ACTIVE SAMPLE PROFILE", EYEBROW, ClientShellPalette.MUTED);
        UiRect profileControl = new UiRect(server.x() + 20, server.y() + 91,
                Math.max(0, server.width() - 40), 38);
        drawList.roundedSurface(profileControl, ClientShellPalette.radius(11), ClientShellPalette.HIGHEST,
                io.github.gyai.projects.ui.runtime.UiMaterialTier.GLASS_SOLID);
        text(drawList, profileControl.x() + 12, profileControl.y() + 13, profile.name() + " · mock profile",
                SMALL_STRONG, ClientShellPalette.TEXT);
        icon(drawList, profileControl.right() - 24, profileControl.y() + 12, 14, IconKey.CHEVRON_DOWN,
                ClientShellPalette.MUTED);
        detailRow(drawList, server.x() + 20, server.y() + 151, server.width() - 40,
                "Profile status", "Ready", ClientShellPalette.SUCCESS);
        detailRow(drawList, server.x() + 20, server.y() + 175, server.width() - 40,
                "Address", profile.server(), ClientShellPalette.TEXT);
        detailRow(drawList, server.x() + 20, server.y() + 199, server.width() - 40,
                "Response", profile.latency(), ClientShellPalette.TEXT);
        pill(drawList, server.x() + 20, server.bottom() - 39, 70, 23, profile.mode(), ClientShellPalette.SUCCESS);
        pill(drawList, server.x() + 98, server.bottom() - 39,
                Math.max(90, server.width() - 118), 23, profile.detail(), ClientShellPalette.PRIMARY);
        icon(drawList, server.x() + 20, server.bottom() - 70, 14, IconKey.SHIELD,
                ClientShellPalette.PRIMARY);
        text(drawList, server.x() + 41, server.bottom() - 68,
                "Sample values are safe to inspect offline.", SMALL, ClientShellPalette.MUTED);

        UiRect journey = shifted("home.journey", yOffset);
        card(drawList, journey, ClientShellPalette.RAISED);
        icon(drawList, journey.right() - 42, journey.y() + 20, 19, IconKey.LAYERS,
                ClientShellPalette.PRIMARY);
        text(drawList, journey.x() + 20, journey.y() + 24, "THE PRODUCT STORY", EYEBROW, ClientShellPalette.MUTED);
        text(drawList, journey.x() + 20, journey.y() + 48, "One continuous journey", CARD_TITLE, ClientShellPalette.TEXT);
        text(drawList, journey.x() + 20, journey.y() + 68, "Use the review control to inspect each stop.", SMALL, ClientShellPalette.MUTED);
        drawJourney(drawList, journey.x() + 22, journey.y() + 105, journey.width() - 44);

        UiRect sample = shifted("home.sample", yOffset);
        card(drawList, sample, ClientShellPalette.CONTAINER);
        icon(drawList, sample.x() + 20, sample.y() + 20, 19, IconKey.EYE, ClientShellPalette.PRIMARY);
        text(drawList, sample.x() + 48, sample.y() + 16, "QUIET BY DESIGN", EYEBROW, ClientShellPalette.MUTED);
        text(drawList, sample.x() + 48, sample.y() + 44, "What this screen shows", CARD_TITLE, ClientShellPalette.TEXT);
        listItem(drawList, sample.x() + 20, sample.y() + 68, IconKey.SLIDERS,
                "Reviewable states", "Every transition is available from the selector.");
        listItem(drawList, sample.x() + 20, sample.y() + 108, IconKey.SHIELD,
                "Offline-safe", "Account, server, and version values are mock data.");
    }

    private void drawLibrary(UiDrawList drawList, ClientShellDataSnapshot data,
                             UiRect intro, double yOffset) {
        drawIntro(drawList, intro, IconKey.LIBRARY, "PROFILE LIBRARY / SAMPLE DATA",
                "Profiles with", "room to breathe.",
                "A deliberately small library surface for the visual prototype. The hub story remains the primary path.");
        double statusWidth = Math.min(136, intro.width());
        drawStatusSurface(drawList, isCompact() ? intro.x() : intro.right() - statusWidth,
                intro.y() + 8, statusWidth, 40,
                data.profiles().size() + " mock profiles", "Sample data", ClientShellPalette.PRIMARY);
        UiRect horizon = shifted("library.horizon", yOffset);
        UiRect atelier = shifted("library.atelier", yOffset);
        drawProfileLibraryCard(drawList, horizon, data.profiles().getFirst());
        drawProfileLibraryCard(drawList, atelier,
                data.profiles().size() > 1 ? data.profiles().get(1) : data.profiles().getFirst());
    }

    private void drawSettings(UiDrawList drawList, UiRect intro, double yOffset) {
        drawIntro(drawList, intro, IconKey.SLIDERS, "PREFERENCES / PROTOTYPE ONLY",
                "The shell stays", "quiet.",
                "These cards document the visual contract of the concept. They do not change a native client or persist settings.");
        double statusWidth = Math.min(104, intro.width());
        drawStatusSurface(drawList, isCompact() ? intro.x() : intro.right() - statusWidth,
                intro.y() + 8, statusWidth, 40,
                "Local only", "No persistence", ClientShellPalette.MUTED);
        UiRect motion = shifted("settings.motion", yOffset);
        card(drawList, motion, ClientShellPalette.RAISED);
        icon(drawList, motion.x() + 20, motion.y() + 22, 20, IconKey.EYE, ClientShellPalette.PRIMARY);
        text(drawList, motion.x() + 50, motion.y() + 32, "Motion", CARD_TITLE, ClientShellPalette.TEXT);
        wrapped(drawList, motion.x() + 20, motion.y() + 72,
                "Transitions use a restrained 200ms rhythm and follow the operating system reduced-motion preference.",
                BODY, ClientShellPalette.MUTED, motion.width() - 40);
        statusLine(drawList, motion.x() + 20, motion.bottom() - 30,
                "Follows system preference", ClientShellPalette.SUCCESS);

        UiRect privacy = shifted("settings.privacy", yOffset);
        card(drawList, privacy, ClientShellPalette.CONTAINER);
        icon(drawList, privacy.x() + 20, privacy.y() + 22, 20, IconKey.SHIELD, ClientShellPalette.PRIMARY);
        text(drawList, privacy.x() + 50, privacy.y() + 32, "Privacy boundary", CARD_TITLE, ClientShellPalette.TEXT);
        wrapped(drawList, privacy.x() + 20, privacy.y() + 72,
                "There is no network, account, filesystem, telemetry, or persistence path in this standalone screen.",
                BODY, ClientShellPalette.MUTED, privacy.width() - 40);
        statusLine(drawList, privacy.x() + 20, privacy.bottom() - 30,
                "Offline concept", ClientShellPalette.SUCCESS);
    }

    private void drawFlow(UiDrawList drawList, ClientShellDataSnapshot data,
                          ClientShellState state, UiRect intro, double yOffset) {
        ClientShellProfile profile = data.profile(model.state().profileId());
        switch (state) {
            case LAUNCHING -> drawIntro(drawList, intro, IconKey.LOADER,
                    "LAUNCH STORY / SAMPLE FLOW", "Previewing the", "client shell.",
                    "The sample profile is prepared locally, then the prototype hands the story to the client connecting state.");
            case CONNECTING -> drawIntro(drawList, intro, IconKey.MONITOR,
                    "CLIENT HANDOFF / SAMPLE FLOW", "The client preview is", "finding its place.",
                    "A clear connecting surface gives the player confidence before the connected landing preview appears.");
            case CONNECTED -> drawIntro(drawList, intro, IconKey.CHECK,
                    "SESSION READY / SAMPLE FLOW", "The preview is", "settled.",
                    "A connected landing preview that feels settled, legible, and ready for the next local action.");
            case RECOVERABLE_ERROR -> drawIntro(drawList, intro, IconKey.WARNING,
                    "RECOVERABLE STATE / SAMPLE FLOW", "A small detour.", "You can try again.",
                    "The error state explains what happened without trapping the user. Retry, open the client preview, or return to the hub.");
            case HOME -> throw new IllegalStateException("home flow state");
        }
        UiColor flowStatus = state == ClientShellState.RECOVERABLE_ERROR ? ClientShellPalette.ERROR
                : state == ClientShellState.CONNECTED ? ClientShellPalette.SUCCESS : ClientShellPalette.PRIMARY;
        double statusWidth = Math.min(150, intro.width());
        drawStatusSurface(drawList, isCompact() ? intro.x() : intro.right() - statusWidth,
                intro.y() + 8, statusWidth, 40,
                stateLabel(state), model.state().reviewMode() ? "Manual review" : "Sample flow", flowStatus);
        UiRect primary = shifted("flow.primary", yOffset);
        UiRect snapshot = shifted("flow.snapshot", yOffset);
        UiRect note = shifted("flow.note", yOffset);
        if (state == ClientShellState.LAUNCHING) drawLaunching(drawList, primary, profile);
        else if (state == ClientShellState.CONNECTING) drawConnecting(drawList, primary, profile);
        else if (state == ClientShellState.CONNECTED) drawConnected(drawList, primary, profile);
        else drawError(drawList, primary, profile);
        drawSnapshot(drawList, snapshot, data, profile, state);
        drawFlowNote(drawList, note, state);
    }

    private void drawLaunching(UiDrawList drawList, UiRect card, ClientShellProfile profile) {
        card(drawList, card, ClientShellPalette.RAISED);
        icon(drawList, card.right() - 44, card.y() + 22, 20, IconKey.SPARKLE, ClientShellPalette.PRIMARY);
        text(drawList, card.x() + 22, card.y() + 24, "LAUNCH-STAGE STEPPER", EYEBROW, ClientShellPalette.MUTED);
        text(drawList, card.x() + 22, card.y() + 51, "Preparing " + profile.name() + " · sample", CARD_TITLE,
                ClientShellPalette.TEXT);
        text(drawList, card.x() + 22, card.y() + 73,
                model.state().reviewMode() ? "Reviewing this stage manually" : "Moving through the preview",
                SMALL, ClientShellPalette.MUTED);
        text(drawList, card.x() + 22, card.y() + 89,
                "Sample account · no native process is started.", SMALL, ClientShellPalette.MUTED);
        String[] titles = {"Prepare selected profile", "Show client shell preview", "Pass to connection preview"};
        String[] details = {"Read the sample profile and version pairing", "Show the local client handoff surface", "Move into the connected landing state"};
        for (int index = 0; index < titles.length; index++) {
            double rowY = card.y() + 103 + index * 48;
            boolean done = model.state().launchStep() > index + 1;
            boolean current = model.state().launchStep() == index + 1;
            drawList.roundedSurface(new UiRect(card.x() + 22, rowY, 30, 30), 15,
                    done ? ClientShellPalette.PRIMARY : current ? ClientShellPalette.PRIMARY.withAlpha(46)
                            : ClientShellPalette.HIGHEST,
                    io.github.gyai.projects.ui.runtime.UiMaterialTier.GLASS_SOLID);
            if (done) icon(drawList, card.x() + 29, rowY + 7, 16, IconKey.CHECK, ClientShellPalette.ON_PRIMARY);
            else text(drawList, card.x() + 29, rowY + 8, "0" + (index + 1), SMALL_STRONG,
                    current ? ClientShellPalette.PRIMARY : ClientShellPalette.MUTED);
            text(drawList, card.x() + 66, rowY + 4, titles[index], BODY_STRONG, ClientShellPalette.TEXT);
            text(drawList, card.x() + 66, rowY + 21, details[index], SMALL, ClientShellPalette.MUTED);
            text(drawList, card.right() - 52, rowY + 10, done ? "Done" : current ? "Current" : "Next",
                    SMALL, current ? ClientShellPalette.PRIMARY : ClientShellPalette.MUTED);
        }
        double progress = Math.max(14, Math.min(96, model.state().launchStep() * 33.0));
        text(drawList, card.x() + 22, card.bottom() - 75, "PROTOTYPE PROGRESS", EYEBROW, ClientShellPalette.MUTED);
        text(drawList, card.right() - 58, card.bottom() - 75, ((int) progress) + "%", SMALL_STRONG,
                ClientShellPalette.PRIMARY);
        drawList.roundedSurface(new UiRect(card.x() + 22, card.bottom() - 58,
                Math.max(0, card.width() - 44), 8), 4, ClientShellPalette.HIGHEST,
                io.github.gyai.projects.ui.runtime.UiMaterialTier.GLASS_SOLID);
        drawList.roundedSurface(new UiRect(card.x() + 22, card.bottom() - 58,
                Math.max(0, (card.width() - 44) * progress / 100), 8), 4, ClientShellPalette.PRIMARY,
                io.github.gyai.projects.ui.runtime.UiMaterialTier.GLASS_SOLID);
    }

    private void drawConnecting(UiDrawList drawList, UiRect card, ClientShellProfile profile) {
        card(drawList, card, ClientShellPalette.RAISED);
        icon(drawList, card.right() - 44, card.y() + 22, 20, IconKey.MONITOR, ClientShellPalette.PRIMARY);
        text(drawList, card.x() + 22, card.y() + 24, "CLIENT CONNECTING", EYEBROW, ClientShellPalette.MUTED);
        text(drawList, card.x() + 22, card.y() + 51, "Connection preview · " + profile.name() + " · sample",
                CARD_TITLE, ClientShellPalette.TEXT);
        String reviewing = model.state().reviewMode() ? "Manual review stage" : "Local connection preview";
        text(drawList, card.x() + 22, card.y() + 73, reviewing + " · sample status only.", SMALL,
                ClientShellPalette.MUTED);
        boolean narrow = card.width() < 520;
        double centerX = card.x() + card.width() / 2;
        double centerY = card.y() + (narrow ? 112 : 145);
        drawList.border(new UiRect(centerX - 46, centerY - 46, 92, 92), 46, 1,
                ClientShellPalette.PRIMARY.withAlpha(90));
        drawList.border(new UiRect(centerX - 31, centerY - 31, 62, 62), 31,
                1, ClientShellPalette.PRIMARY.withAlpha(130));
        drawList.roundedSurface(new UiRect(centerX - 22, centerY - 22, 44, 44), 22,
                ClientShellPalette.PRIMARY.withAlpha(36), io.github.gyai.projects.ui.runtime.UiMaterialTier.GLASS_SOLID);
        icon(drawList, centerX - 12, centerY - 12, 24, IconKey.MONITOR, ClientShellPalette.PRIMARY);
        double stepsY = card.y() + (narrow ? 170 : 210);
        boolean openingDone = model.state().connectionStep() >= 3;
        boolean landingDone = model.state().state() == ClientShellState.CONNECTED;
        connectionStep(drawList, card.x() + 22, stepsY, "Profile is ready", true, false);
        connectionStep(drawList, card.x() + 22, stepsY + 32, "Opening client preview", openingDone, !openingDone);
        connectionStep(drawList, card.x() + 22, stepsY + 64, "Waiting for connected landing", landingDone,
                !landingDone && openingDone);
    }

    private void drawConnected(UiDrawList drawList, UiRect card, ClientShellProfile profile) {
        card(drawList, card, ClientShellPalette.RAISED);
        icon(drawList, card.right() - 44, card.y() + 22, 20, IconKey.CHECK, ClientShellPalette.SUCCESS);
        text(drawList, card.x() + 22, card.y() + 24, "CONNECTED LANDING PREVIEW", EYEBROW, ClientShellPalette.MUTED);
        text(drawList, card.x() + 22, card.y() + 51, "Connected preview · " + profile.name() + " · sample",
                CARD_TITLE, ClientShellPalette.TEXT);
        text(drawList, card.x() + 22, card.y() + 73, "Preview ready · all values below are sample data.", SMALL,
                ClientShellPalette.MUTED);
        drawList.roundedSurface(new UiRect(card.x() + 22, card.y() + 98,
                Math.max(0, card.width() - 44), 50), ClientShellPalette.radius(12),
                ClientShellPalette.PRIMARY.withAlpha(32), io.github.gyai.projects.ui.runtime.UiMaterialTier.GLASS_SOLID);
        icon(drawList, card.x() + 35, card.y() + 112, 23, IconKey.CHECK, ClientShellPalette.SUCCESS);
        text(drawList, card.x() + 68, card.y() + 117, "Preview looks settled.", BODY_STRONG, ClientShellPalette.TEXT);
        text(drawList, card.x() + 68, card.y() + 135, "Client preview is ready for the next visual pass.", SMALL,
                ClientShellPalette.MUTED);
        boolean compact = layout.breakpoint() == ClientShellLayout.Breakpoint.COMPACT
                || layout.breakpoint() == ClientShellLayout.Breakpoint.NARROW;
        drawMetaGrid(drawList, card.x() + 22, card.y() + 166, card.width() - 44,
                List.of("Server", profile.server(), "Version", profile.version(),
                        "Account", "sample_user · mock", "Status",
                        model.state().clientPreviewOpen() ? "Preview open · mock" : "Connected · mock"),
                2, compact ? 40 : 58);
    }

    private void drawError(UiDrawList drawList, UiRect card, ClientShellProfile profile) {
        card(drawList, card, ClientShellPalette.RAISED);
        icon(drawList, card.right() - 44, card.y() + 22, 20, IconKey.WARNING, ClientShellPalette.ERROR);
        text(drawList, card.x() + 22, card.y() + 24, "RECOVERABLE ERROR", EYEBROW, ClientShellPalette.ERROR);
        text(drawList, card.x() + 22, card.y() + 51, "The sample connection did not finish", CARD_TITLE,
                ClientShellPalette.TEXT);
        text(drawList, card.x() + 22, card.y() + 73,
                "This is an intentional prototype state, not a live failure.", SMALL, ClientShellPalette.MUTED);
        drawList.roundedSurface(new UiRect(card.x() + 22, card.y() + 98,
                Math.max(0, card.width() - 44), 52), ClientShellPalette.radius(12),
                ClientShellPalette.ERROR.withAlpha(30), io.github.gyai.projects.ui.runtime.UiMaterialTier.GLASS_SOLID);
        icon(drawList, card.x() + 34, card.y() + 112, 22, IconKey.WARNING, ClientShellPalette.ERROR);
        text(drawList, card.x() + 67, card.y() + 116, "Connection preview paused", BODY_STRONG, ClientShellPalette.TEXT);
        text(drawList, card.x() + 67, card.y() + 134, "Choose Retry to replay the launch story.", SMALL, ClientShellPalette.MUTED);
        boolean narrow = card.width() < 520;
        listItem(drawList, card.x() + 22, card.y() + (narrow ? 165 : 177), IconKey.CHECK,
                "Selected sample profile: " + profile.name(), "");
        listItem(drawList, card.x() + 22, card.y() + (narrow ? 190 : 207), IconKey.CHECK,
                "Cause: mock timeout in the visual state", "");
        listItem(drawList, card.x() + 22, card.y() + (narrow ? 215 : 237), IconKey.INFO,
                "No account, file, or server was contacted", "");
    }

    private void drawSnapshot(UiDrawList drawList, UiRect card, ClientShellDataSnapshot data,
                              ClientShellProfile profile, ClientShellState state) {
        card(drawList, card, ClientShellPalette.CONTAINER);
        text(drawList, card.x() + 20, card.y() + 24, "SESSION SNAPSHOT", EYEBROW, ClientShellPalette.MUTED);
        avatar(drawList, card.x() + 20, card.y() + 43, profile, 42);
        text(drawList, card.x() + 74, card.y() + 58, profile.name() + " · sample", BODY_STRONG, ClientShellPalette.TEXT);
        text(drawList, card.x() + 74, card.y() + 76, "Mock profile · local preview", SMALL, ClientShellPalette.MUTED);
        detailRow(drawList, card.x() + 20, card.y() + 108, card.width() - 40, "Server", profile.server(), ClientShellPalette.TEXT);
        detailRow(drawList, card.x() + 20, card.y() + 132, card.width() - 40, "Version", profile.version(), ClientShellPalette.TEXT);
        detailRow(drawList, card.x() + 20, card.y() + 156, card.width() - 40, "Mode", profile.mode(), ClientShellPalette.TEXT);
        String status = switch (state) {
            case LAUNCHING -> model.state().reviewMode() ? "Manual review" : "Local preview";
            case CONNECTING -> "Connecting · mock";
            case CONNECTED -> model.state().clientPreviewOpen() ? "Preview open" : "Connected · mock";
            case RECOVERABLE_ERROR -> "Needs attention";
            case HOME -> "Sample only";
        };
        detailRow(drawList, card.x() + 20, card.y() + 180, card.width() - 40, "Status", status,
                state == ClientShellState.RECOVERABLE_ERROR ? ClientShellPalette.ERROR : ClientShellPalette.PRIMARY);
    }

    private void drawFlowNote(UiDrawList drawList, UiRect card, ClientShellState state) {
        card(drawList, card, ClientShellPalette.RAISED);
        IconKey noteIcon = state == ClientShellState.RECOVERABLE_ERROR ? IconKey.SHIELD : IconKey.EYE;
        icon(drawList, card.x() + 20, card.y() + 21, 20, noteIcon,
                state == ClientShellState.RECOVERABLE_ERROR ? ClientShellPalette.ERROR : ClientShellPalette.PRIMARY);
        String title = state == ClientShellState.RECOVERABLE_ERROR ? "Recoverable by design" : "A calm handoff";
        text(drawList, card.x() + 50, card.y() + 31, title, CARD_TITLE, ClientShellPalette.TEXT);
        String copy = state == ClientShellState.RECOVERABLE_ERROR
                ? "Every action stays inside the offline prototype. The review control can take you directly to any state."
                : "The restrained progress indicator is the only moving element. Select another state above whenever you want to inspect it directly.";
        wrapped(drawList, card.x() + 20, card.y() + 70, copy, BODY, ClientShellPalette.MUTED,
                card.width() - 40);
    }

    private void drawProfileLibraryCard(UiDrawList drawList, UiRect card, ClientShellProfile profile) {
        card(drawList, card, ClientShellPalette.RAISED);
        avatar(drawList, card.x() + 20, card.y() + 22, profile, 52);
        text(drawList, card.x() + 88, card.y() + 38, profile.name() + " · sample", CARD_TITLE,
                ClientShellPalette.TEXT);
        wrapped(drawList, card.x() + 20, card.y() + 98,
                profile.description() + "\n" + profile.server() + " · " + profile.version(),
                BODY, ClientShellPalette.MUTED, card.width() - 40);
        statusLine(drawList, card.x() + 20, card.bottom() - 28,
                "Ready · sample only", ClientShellPalette.SUCCESS);
    }

    private void drawJourney(UiDrawList drawList, double x, double y, double width) {
        String[] labels = {"Hub home", "Launching", "Connecting", "Connected"};
        IconKey[] icons = {IconKey.HOME, IconKey.LOADER, IconKey.MONITOR, IconKey.CHECK};
        double step = width / labels.length;
        drawList.border(new UiRect(x + step / 2, y + 13, Math.max(0, width - step), 1), 0, 1,
                ClientShellPalette.subtle(ClientShellPalette.OUTLINE));
        for (int index = 0; index < labels.length; index++) {
            double cx = x + step * index + step / 2;
            drawList.roundedSurface(new UiRect(cx - 13, y, 26, 26), 13,
                    index == 0 ? ClientShellPalette.PRIMARY : ClientShellPalette.HIGHEST,
                    io.github.gyai.projects.ui.runtime.UiMaterialTier.GLASS_SOLID);
            icon(drawList, cx - 8, y + 5, 16, icons[index], index == 0
                    ? ClientShellPalette.ON_PRIMARY : ClientShellPalette.MUTED);
            textCenter(drawList, cx, y + 38, labels[index], SMALL, ClientShellPalette.MUTED);
        }
    }

    private void drawMetaGrid(UiDrawList drawList, double x, double y, double width,
                              List<String> values, int columns, double height) {
        int itemCount = values.size() / 2;
        double gap = ClientShellPalette.spacing(10);
        double itemWidth = Math.max(0, (width - gap * (columns - 1)) / columns);
        for (int index = 0; index < itemCount; index++) {
            int column = index % columns;
            int row = index / columns;
            double itemX = x + column * (itemWidth + gap);
            double itemY = y + row * (height + gap);
            drawList.roundedSurface(new UiRect(itemX, itemY, itemWidth, height), ClientShellPalette.radius(10),
                    ClientShellPalette.HIGHEST.withAlpha(160), io.github.gyai.projects.ui.runtime.UiMaterialTier.GLASS_THIN);
            text(drawList, itemX + 10, itemY + 16, values.get(index * 2), EYEBROW, ClientShellPalette.MUTED);
            text(drawList, itemX + 10, itemY + 37, fit(values.get(index * 2 + 1), itemWidth - 20, BODY),
                    SMALL_STRONG, ClientShellPalette.TEXT);
        }
    }

    private void detailRow(UiDrawList drawList, double x, double y, double width,
                           String label, String value, UiColor valueColor) {
        text(drawList, x, y, label, SMALL, ClientShellPalette.MUTED);
        textRight(drawList, x + width, y, fit(value, width * .62, SMALL_STRONG), SMALL_STRONG, valueColor);
    }

    private void drawStatusSurface(UiDrawList drawList, double x, double y, double width, double height,
                                   String title, String subtitle, UiColor statusColor) {
        drawList.roundedSurface(new UiRect(x, y, width, height), ClientShellPalette.radius(12),
                ClientShellPalette.HIGHEST.withAlpha(170), io.github.gyai.projects.ui.runtime.UiMaterialTier.GLASS_THIN);
        dot(drawList, x + 13, y + 16, statusColor);
        text(drawList, x + 24, y + 20, title, SMALL_STRONG, statusColor);
        text(drawList, x + 13, y + 38, subtitle, SMALL, ClientShellPalette.MUTED);
    }

    private void drawIntro(UiDrawList drawList, UiRect intro, IconKey icon, String eyebrow,
                           String title, String emphasis, String copy) {
        boolean compact = isCompact();
        TextStyle titleStyle = compact ? style(UiFontWeight.SEMIBOLD,
                layout.breakpoint() == ClientShellLayout.Breakpoint.NARROW ? 24 : 27,
                layout.breakpoint() == ClientShellLayout.Breakpoint.NARROW ? 29 : 32) : TITLE;
        icon(drawList, intro.x(), intro.y() + 7, 14, icon, ClientShellPalette.PRIMARY);
        text(drawList, intro.x() + 22, intro.y() + 16, eyebrow, EYEBROW, ClientShellPalette.PRIMARY);
        double titleY = intro.y() + 48;
        boolean titleWrap = compact && approxWidth(title, titleStyle) + approxWidth(emphasis, titleStyle) + 10 > intro.width();
        text(drawList, intro.x(), titleY, title, titleStyle, ClientShellPalette.TEXT);
        if (titleWrap) {
            text(drawList, intro.x(), titleY + titleStyle.lineHeight(), emphasis, titleStyle, ClientShellPalette.PRIMARY);
        } else {
            text(drawList, intro.x() + approxWidth(title, titleStyle) + 10, titleY,
                    emphasis, titleStyle, ClientShellPalette.PRIMARY);
        }
        double copyY = titleWrap ? titleY + titleStyle.lineHeight() * 2 + 8 : intro.y() + 76;
        wrapped(drawList, intro.x(), copyY, copy, BODY, ClientShellPalette.MUTED,
                Math.min(600, compact ? intro.width() : intro.width() - 210));
    }

    private void connectionStep(UiDrawList drawList, double x, double y, String label,
                                boolean done, boolean current) {
        icon(drawList, x, y, 17, done ? IconKey.CHECK : current ? IconKey.LOADER : IconKey.CHECK,
                done ? ClientShellPalette.SUCCESS : current ? ClientShellPalette.PRIMARY
                        : ClientShellPalette.MUTED);
        text(drawList, x + 28, y + 3, label, BODY, ClientShellPalette.TEXT);
    }

    private void listItem(UiDrawList drawList, double x, double y, IconKey icon,
                          String title, String copy) {
        this.icon(drawList, x, y, 16, icon, ClientShellPalette.PRIMARY);
        text(drawList, x + 26, y + 4, title, BODY_STRONG, ClientShellPalette.TEXT);
        if (!copy.isBlank()) text(drawList, x + 26, y + 21, copy, SMALL, ClientShellPalette.MUTED);
    }

    private void statusLine(UiDrawList drawList, double x, double y, String label, UiColor color) {
        dot(drawList, x + 4, y + 3, color);
        text(drawList, x + 14, y + 7, label, SMALL_STRONG, color);
    }

    private void pill(UiDrawList drawList, double x, double y, double width, double height,
                      String label, UiColor color) {
        drawList.roundedSurface(new UiRect(x, y, Math.max(0, width), height), 999,
                color.withAlpha(30), io.github.gyai.projects.ui.runtime.UiMaterialTier.GLASS_THIN);
        drawList.border(new UiRect(x, y, Math.max(0, width), height), 999, 1, color.withAlpha(100));
        textCenter(drawList, x + width / 2, y + 7, fit(label, width - 12, SMALL), SMALL, color);
    }

    private void avatar(UiDrawList drawList, double x, double y, ClientShellProfile profile, double size) {
        drawList.roundedSurface(new UiRect(x, y, size, size), ClientShellPalette.radius(16),
                profile.accentId().equals("second") ? ClientShellPalette.PRIMARY.withAlpha(40)
                        : ClientShellPalette.ERROR.withAlpha(35), io.github.gyai.projects.ui.runtime.UiMaterialTier.GLASS_SOLID);
        drawList.border(new UiRect(x, y, size, size), ClientShellPalette.radius(16), 1,
                profile.accentId().equals("second") ? ClientShellPalette.PRIMARY.withAlpha(120)
                        : ClientShellPalette.ERROR.withAlpha(120));
        textCenter(drawList, x + size / 2, y + size / 2 - 7, profile.initials(), AVATAR,
                profile.accentId().equals("second") ? ClientShellPalette.PRIMARY : ClientShellPalette.ERROR);
    }

    private void card(UiDrawList drawList, UiRect rect, UiColor fill) {
        drawList.shadow(rect.offset(0, 2), ClientShellPalette.spacing(5), ClientShellPalette.SHADOW);
        drawList.roundedSurface(rect, ClientShellPalette.radius(22), fill,
                io.github.gyai.projects.ui.runtime.UiMaterialTier.GLASS_PANEL);
        drawList.border(rect, ClientShellPalette.radius(22), 1, ClientShellPalette.subtle(ClientShellPalette.OUTLINE));
    }

    private void dot(UiDrawList drawList, double x, double y, UiColor color) {
        drawList.roundedSurface(new UiRect(x, y, 7, 7), 4, color,
                io.github.gyai.projects.ui.runtime.UiMaterialTier.GLASS_SOLID);
    }

    private void icon(UiDrawList drawList, double x, double y, double size, IconKey key, UiColor tint) {
        drawList.icon(new UiRect(x, y, size, size), IconSpec.procedural(key, key.path()), tint);
    }

    private UiRect shifted(String id, double yOffset) {
        UiRect rect = layout.bounds(id);
        return rect.offset(0, yOffset);
    }

    private boolean isCompact() {
        return layout.breakpoint() == ClientShellLayout.Breakpoint.COMPACT
                || layout.breakpoint() == ClientShellLayout.Breakpoint.NARROW;
    }

    boolean visualsReadyForChild() { return visualsReady(); }

    private boolean visualsReady() {
        if (!constructionGate) return false;
        try {
            MinecraftUiRuntimeResources resources = MinecraftUiRuntimeResources.currentOrNull();
            return resources != null && resources.clientShellVisualsReady();
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static String stateLabel(ClientShellState state) {
        return switch (state) {
            case HOME -> "Hub home";
            case LAUNCHING -> "Launching";
            case CONNECTING -> "Client connecting";
            case CONNECTED -> "Connected";
            case RECOVERABLE_ERROR -> "Recoverable error";
        };
    }

    private static TextStyle style(UiFontWeight weight, double size, double lineHeight) {
        return new TextStyle(UiFontFamilyRole.UI_SANS, weight, size, lineHeight, 0,
                UiColorRole.TEXT_PRIMARY);
    }

    private static double approxWidth(String value, TextStyle style) {
        return value == null ? 0 : value.length() * style.size() * .53;
    }

    private static String fit(String value, double width, TextStyle style) {
        if (value == null) return "";
        int max = Math.max(3, (int) Math.floor(width / Math.max(1, style.size() * .54)));
        return value.length() <= max ? value : value.substring(0, Math.max(1, max - 3)) + "...";
    }

    private static void wrapped(UiDrawList drawList, double x, double y, String value,
                                TextStyle style, UiColor color, double width) {
        if (value == null || value.isBlank()) return;
        int maxChars = Math.max(8, (int) Math.floor(width / Math.max(1, style.size() * .54)));
        String[] paragraphs = value.split("\\n", -1);
        int line = 0;
        for (String paragraph : paragraphs) {
            String remaining = paragraph;
            while (remaining.length() > maxChars) {
                int breakAt = remaining.lastIndexOf(' ', maxChars);
                if (breakAt < 1) breakAt = maxChars;
                text(drawList, x, y + line * style.lineHeight(), remaining.substring(0, breakAt), style, color);
                remaining = remaining.substring(Math.min(remaining.length(), breakAt + 1));
                line++;
            }
            text(drawList, x, y + line * style.lineHeight(), remaining, style, color);
            line++;
        }
    }

    private static void text(UiDrawList drawList, double x, double y, String value,
                             TextStyle style, UiColor color) {
        drawList.text(new UiPoint(x, y), value == null ? "" : value, style, color);
    }

    private static void textCenter(UiDrawList drawList, double centerX, double y, String value,
                                   TextStyle style, UiColor color) {
        text(drawList, centerX - approxWidth(value, style) / 2, y, value, style, color);
    }

    private static void textRight(UiDrawList drawList, double rightX, double y, String value,
                                  TextStyle style, UiColor color) {
        text(drawList, rightX - approxWidth(value, style), y, value, style, color);
    }
}

/** A scrollable, clipped content viewport with the shell palette and no platform rendering. */
final class ShellContentNode extends UiNode {
    private final ClientShellRoot owner;
    private final double contentHeight;
    private double scrollY;

    ShellContentNode(ClientShellRoot owner, UiRect bounds, double contentHeight) {
        super("content.viewport", bounds);
        this.owner = Objects.requireNonNull(owner, "owner");
        this.contentHeight = Math.max(bounds.height(), contentHeight);
        setClipToBounds(true);
        setAccessibility(UiAccessibilityMetadata.of(
                UiAccessibilityRole.SCROLL_AREA, "Client hub content", offsetString()));
    }

    void buildControls() {
        ClientShellLayout.Snapshot layout = owner.layoutSnapshot();
        ClientShellPage page = owner.model().state().page();
        ClientShellState state = owner.model().state().state();
        if (page == ClientShellPage.LIBRARY) {
            UiRect home = layout.bounds("library.home");
            addControl("library.home", home, 0, 0, home.width(), home.height(), "Return to hub home",
                    IconKey.ARROW_RIGHT, ShellButton.Kind.TERTIARY,
                    () -> owner.dispatch(ClientShellAction.navigateHome()), "Return to hub home");
        } else if (page == ClientShellPage.SETTINGS) {
            UiRect home = layout.bounds("settings.home");
            addControl("settings.home", home, 0, 0, home.width(), home.height(), "Return to hub home",
                    IconKey.ARROW_RIGHT, ShellButton.Kind.TERTIARY,
                    () -> owner.dispatch(ClientShellAction.navigateHome()), "Return to hub home");
        } else if (state == ClientShellState.HOME) {
            addControl("home.profile", layout.bounds("home.server"),
                    layout.bounds("home.server").width() - 60, 91,
                    40, 38, "", IconKey.CHEVRON_DOWN, ShellButton.Kind.SELECTOR,
                    owner::cycleProfile, "Choose a sample server profile");
            UiRect play = layout.bounds("home.play");
            boolean narrow = layout.breakpoint() == ClientShellLayout.Breakpoint.COMPACT
                    || layout.breakpoint() == ClientShellLayout.Breakpoint.NARROW;
            if (narrow) {
                addControl("home.play.action", play, 20, play.height() - 92,
                        Math.max(0, play.width() - 40), 36, "Play preview", IconKey.PLAY,
                        ShellButton.Kind.PRIMARY, () -> owner.dispatch(ClientShellAction.playPreview()), "Play preview");
                addControl("home.open-client.action", play, 20, play.height() - 48,
                        Math.max(0, play.width() - 40), 36, "Open client preview", IconKey.MONITOR,
                        ShellButton.Kind.SECONDARY, () -> owner.dispatch(ClientShellAction.openClientPreview()),
                        "Open client preview");
            } else {
                addControl("home.play.action", play, 20, play.height() - 48,
                        Math.max(0, play.width() * .46), 36, "Play preview", IconKey.PLAY,
                        ShellButton.Kind.PRIMARY, () -> owner.dispatch(ClientShellAction.playPreview()), "Play preview");
                addControl("home.open-client.action", play, 28 + play.width() * .46, play.height() - 48,
                        Math.max(0, play.width() * .46 - 8), 36, "Open client preview", IconKey.MONITOR,
                        ShellButton.Kind.SECONDARY, () -> owner.dispatch(ClientShellAction.openClientPreview()),
                        "Open client preview");
            }
        } else {
            switch (state) {
                case LAUNCHING -> addControl("launch.cancel.action", layout.bounds("flow.primary"),
                        20, layout.bounds("flow.primary").height() - 48,
                        Math.min(160, layout.bounds("flow.primary").width() - 40), 36,
                        "Cancel preview", IconKey.CLOSE, ShellButton.Kind.SECONDARY,
                        () -> owner.dispatch(ClientShellAction.cancelPreview()), "Cancel preview");
                case CONNECTING -> {
                    UiRect primary = layout.bounds("flow.primary");
                    boolean stacked = primary.width() < 520;
                    if (stacked) {
                        addControl("connecting.open-client.action", primary, 20, primary.height() - 136,
                                Math.max(0, primary.width() - 40), 36, "Open client preview", IconKey.MONITOR,
                                ShellButton.Kind.PRIMARY, () -> owner.dispatch(ClientShellAction.openClientPreview()),
                                "Open client preview");
                        addControl("connecting.error.action", primary, 20, primary.height() - 92,
                                Math.max(0, primary.width() - 40), 36, "Preview error", IconKey.WARNING,
                                ShellButton.Kind.SECONDARY, () -> owner.dispatch(ClientShellAction.previewError()),
                                "Preview a recoverable error");
                        addControl("connecting.cancel.action", primary, 20, primary.height() - 48,
                                Math.max(0, primary.width() - 40), 36, "Cancel", IconKey.CLOSE, ShellButton.Kind.TERTIARY,
                                () -> owner.dispatch(ClientShellAction.cancelPreview()), "Cancel preview");
                    } else {
                        addControl("connecting.open-client.action", primary, 20, primary.height() - 48,
                                Math.max(0, primary.width() * .46), 36, "Open client preview", IconKey.MONITOR,
                                ShellButton.Kind.PRIMARY, () -> owner.dispatch(ClientShellAction.openClientPreview()),
                                "Open client preview");
                        addControl("connecting.error.action", primary, 28 + primary.width() * .46, primary.height() - 48,
                                Math.max(0, primary.width() * .30), 36, "Preview error", IconKey.WARNING,
                                ShellButton.Kind.SECONDARY, () -> owner.dispatch(ClientShellAction.previewError()),
                                "Preview a recoverable error");
                        addControl("connecting.cancel.action", primary, primary.width() - 86, primary.height() - 48,
                                66, 36, "Cancel", IconKey.CLOSE, ShellButton.Kind.TERTIARY,
                                () -> owner.dispatch(ClientShellAction.cancelPreview()), "Cancel preview");
                    }
                }
                case CONNECTED -> {
                    UiRect primary = layout.bounds("flow.primary");
                    boolean narrow = primary.width() < 520;
                    addControl("connected.open-client.action", primary, 20,
                            primary.height() - (narrow ? 92 : 48),
                            Math.max(0, narrow ? primary.width() - 40 : primary.width() * .52), 36,
                            owner.model().state().clientPreviewOpen() ? "Client preview open" : "Open client preview",
                            IconKey.MONITOR, ShellButton.Kind.PRIMARY,
                            () -> owner.dispatch(ClientShellAction.openClientPreview()), "Open client preview");
                    addControl("connected.home.action", primary, narrow ? 20 : 28 + primary.width() * .52,
                            primary.height() - 48, Math.max(0, narrow ? primary.width() - 40 : primary.width() * .40), 36,
                            "Back to hub", IconKey.ARROW_RIGHT, ShellButton.Kind.SECONDARY,
                            () -> owner.dispatch(ClientShellAction.navigateHome()), "Back to hub");
                }
                case RECOVERABLE_ERROR -> {
                    UiRect primary = layout.bounds("flow.primary");
                    boolean narrow = primary.width() < 520;
                    addControl("error.retry.action", primary, 20, primary.height() - (narrow ? 136 : 48),
                            Math.max(0, narrow ? primary.width() - 40 : primary.width() * .35), 36,
                            "Retry preview", IconKey.RETRY,
                            ShellButton.Kind.PRIMARY, () -> owner.dispatch(ClientShellAction.retryPreview()), "Retry preview");
                    addControl("error.open-client.action", primary, narrow ? 20 : 28 + primary.width() * .35,
                            primary.height() - (narrow ? 92 : 48), Math.max(0, narrow ? primary.width() - 40 : primary.width() * .35), 36,
                            "Open client preview", IconKey.MONITOR, ShellButton.Kind.SECONDARY,
                            () -> owner.dispatch(ClientShellAction.openClientPreview()), "Open client preview");
                    addControl("error.home.action", primary, narrow ? 20 : primary.width() - 88,
                            primary.height() - 48, Math.max(0, narrow ? primary.width() - 40 : 68), 36,
                            "Back to hub", IconKey.CLOSE, ShellButton.Kind.TERTIARY,
                            () -> owner.dispatch(ClientShellAction.navigateHome()), "Back to hub");
                }
                case HOME -> { }
            }
        }
        applyOffsets();
    }

    private void addControl(String id, UiRect parentRect, double x, double y,
                            double width, double height, String label, IconKey icon,
                            ShellButton.Kind kind, Runnable action, String accessibleLabel) {
        UiRect absolute = new UiRect(parentRect.x() + x, parentRect.y() + y, Math.max(0, width), Math.max(0, height));
        ShellButton control = new ShellButton(id, absolute, label, icon, kind, action, false, accessibleLabel);
        addChild(control);
    }

    @Override
    public boolean handleEvent(UiEvent event) {
        if (event instanceof UiScrollEvent scroll && globalBounds().contains(scroll.position())) {
            double before = scrollY;
            scrollY = Math.clamp(scrollY - scroll.vertical() * 30, 0, maxScroll());
            applyOffsets();
            setAccessibility(accessibility().withValue(offsetString()));
            return before != scrollY || maxScroll() == 0;
        }
        return super.handleEvent(event);
    }

    double scrollY() { return scrollY; }

    private double maxScroll() { return Math.max(0, contentHeight - bounds().height()); }

    void ensureVisible(ShellButton control) {
        if (control == null || maxScroll() <= 0) return;
        UiRect viewport = globalBounds();
        UiRect target = control.absoluteBounds();
        double next = scrollY;
        if (target.y() < viewport.y()) next -= viewport.y() - target.y();
        else if (target.bottom() > viewport.bottom()) next += target.bottom() - viewport.bottom();
        next = Math.clamp(next, 0, maxScroll());
        if (next != scrollY) {
            scrollY = next;
            applyOffsets();
            setAccessibility(accessibility().withValue(offsetString()));
        }
    }

    private void applyOffsets() {
        UiRect viewport = bounds();
        for (UiNode child : children()) {
            UiRect absolute = child instanceof ShellButton shellButton
                    ? shellButton.absoluteBounds() : child.bounds();
            child.setBounds(new UiRect(absolute.x() - viewport.x(),
                    absolute.y() - viewport.y() - scrollY, absolute.width(), absolute.height()));
        }
    }

    @Override
    protected void appendSelf(UiDrawList drawList, UiTheme theme, UiRect globalBounds, UiRect clip) {
        if (!owner.visualsReadyForChild()) return;
        drawList.pushClip(globalBounds);
        owner.drawContent(drawList, scrollY);
        drawList.popClip();
        if (maxScroll() > 0) {
            double barHeight = Math.max(15, globalBounds.height() * globalBounds.height() / contentHeight);
            double barY = globalBounds.y() + (globalBounds.height() - barHeight)
                    * (scrollY / maxScroll());
            drawList.roundedSurface(new UiRect(globalBounds.right() - 5, barY, 3, barHeight), 2,
                    ClientShellPalette.OUTLINE.withAlpha(180),
                    io.github.gyai.projects.ui.runtime.UiMaterialTier.GLASS_SOLID);
        }
    }

    private String offsetString() { return "scroll-y=" + scrollY; }
}

/** Pure-runtime button skin used by the Client Shell; it never delegates to Minecraft widgets. */
final class ShellButton extends UiButton {
    enum Kind { BRAND, NAV, SIDE, ICON, ACCOUNT, SELECTOR, PRIMARY, SECONDARY, TERTIARY }
    private static final TextStyle BUTTON_STYLE = new TextStyle(
            UiFontFamilyRole.UI_SANS, UiFontWeight.SEMIBOLD, 11, 15, 0,
            UiColorRole.TEXT_PRIMARY);

    private final IconKey icon;
    private final Kind kind;
    private final UiRect absoluteBounds;
    private final double shellRadius;

    ShellButton(String id, UiRect bounds, String label, IconKey icon, Kind kind,
                Runnable action, boolean selected, String accessibleLabel) {
        super(id, bounds, label == null ? "" : label, action);
        this.icon = kind == Kind.ACCOUNT ? null : Objects.requireNonNull(icon, "icon");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.absoluteBounds = bounds;
        setSelected(selected);
        shellRadius = ClientShellPalette.radius(kind == Kind.ACCOUNT ? 17 : 12);
        setRadius(shellRadius);
        setAccessibility(UiAccessibilityMetadata.of(UiAccessibilityRole.BUTTON,
                accessibleLabel == null || accessibleLabel.isBlank() ? id : accessibleLabel,
                label == null ? "" : label));
    }

    UiRect absoluteBounds() { return absoluteBounds; }

    @Override
    public ShellButton setBounds(UiRect nextBounds) {
        super.setBounds(nextBounds);
        return this;
    }

    @Override
    public ShellButton setFocused(boolean nextFocused) {
        super.setFocused(nextFocused);
        if (nextFocused && parent() instanceof ShellContentNode content) content.ensureVisible(this);
        return this;
    }

    @Override
    public boolean handleEvent(UiEvent event) {
        if (event instanceof UiScrollEvent scroll && parent() instanceof ShellContentNode content) {
            return content.handleEvent(scroll);
        }
        return super.handleEvent(event);
    }

    @Override
    protected void appendSelf(UiDrawList drawList, UiTheme theme, UiRect globalBounds, UiRect clip) {
        UiButtonState state = state();
        boolean transparent = kind == Kind.TERTIARY || kind == Kind.NAV || kind == Kind.SIDE
                || (kind == Kind.SELECTOR && getLabel().isBlank());
        UiColor base = switch (kind) {
            case PRIMARY -> ClientShellPalette.PRIMARY;
            case SECONDARY -> ClientShellPalette.RAISED;
            case SELECTOR -> getLabel().isBlank() ? ClientShellPalette.HIGHEST.withAlpha(0)
                    : ClientShellPalette.RAISED;
            case ACCOUNT -> ClientShellPalette.PRIMARY.withAlpha(35);
            case BRAND -> ClientShellPalette.PRIMARY.withAlpha(28);
            case ICON -> ClientShellPalette.HIGHEST.withAlpha(state == UiButtonState.NORMAL ? 0 : 160);
            case NAV, SIDE -> state == UiButtonState.SELECTED
                    ? ClientShellPalette.PRIMARY.withAlpha(38) : ClientShellPalette.HIGHEST.withAlpha(0);
            case TERTIARY -> ClientShellPalette.HIGHEST.withAlpha(state == UiButtonState.NORMAL ? 0 : 130);
        };
        if (state == UiButtonState.HOVER) base = base.mix(ClientShellPalette.PRIMARY, .14);
        if (state == UiButtonState.PRESSED) base = base.mix(ClientShellPalette.ON_PRIMARY, .18);
        if (state == UiButtonState.DISABLED) base = ClientShellPalette.HIGHEST.withAlpha(80);
        if (!transparent || base.alpha() > 0) {
            drawList.roundedSurface(globalBounds, shellRadius, base,
                    io.github.gyai.projects.ui.runtime.UiMaterialTier.GLASS_SOLID);
        }
        UiColor border = state == UiButtonState.FOCUSED || state == UiButtonState.SELECTED
                ? ClientShellPalette.PRIMARY : transparent ? ClientShellPalette.OUTLINE.withAlpha(0)
                        : ClientShellPalette.subtle(ClientShellPalette.OUTLINE);
        if (kind == Kind.ACCOUNT) border = ClientShellPalette.PRIMARY.withAlpha(170);
        drawList.border(globalBounds, shellRadius, 1, border);
        if (state == UiButtonState.FOCUSED) {
            drawList.border(globalBounds.inset(new io.github.gyai.projects.ui.runtime.UiInsets(2)),
                    Math.max(0, shellRadius - 2), 1, ClientShellPalette.PRIMARY.withAlpha(210));
        }
        if (kind == Kind.ACCOUNT) {
            UiColor avatarColor = state == UiButtonState.DISABLED
                    ? ClientShellPalette.MUTED : ClientShellPalette.PRIMARY;
            String initials = "AY";
            double initialsWidth = initials.length() * BUTTON_STYLE.size() * .53;
            drawList.text(new UiPoint(globalBounds.x() + globalBounds.width() / 2 - initialsWidth / 2,
                            globalBounds.y() + Math.max(1, (globalBounds.height() - BUTTON_STYLE.size()) / 2)),
                    initials, BUTTON_STYLE, avatarColor);
            return;
        }
        double iconSize = 15;
        double iconX = globalBounds.x() + (getLabel().isBlank()
                ? (globalBounds.width() - iconSize) / 2 : 12);
        double iconY = globalBounds.y() + (globalBounds.height() - iconSize) / 2;
        UiColor iconColor = state == UiButtonState.DISABLED ? ClientShellPalette.MUTED
                : kind == Kind.PRIMARY ? ClientShellPalette.ON_PRIMARY : ClientShellPalette.PRIMARY;
        drawList.icon(new UiRect(iconX, iconY, iconSize, iconSize),
                IconSpec.procedural(icon, icon.path()), iconColor);
        if (!getLabel().isBlank()) {
            UiColor textColor = kind == Kind.PRIMARY ? ClientShellPalette.ON_PRIMARY
                    : state == UiButtonState.DISABLED ? ClientShellPalette.MUTED : ClientShellPalette.TEXT;
            String label = fit(getLabel(), globalBounds.width() - iconSize - 28, BUTTON_STYLE);
            text(drawList, iconX + iconSize + 7,
                    globalBounds.y() + Math.max(1, (globalBounds.height() - BUTTON_STYLE.size()) / 2),
                    label, BUTTON_STYLE, textColor);
        }
    }

    private String getLabel() { return label(); }

    private static void text(UiDrawList drawList, double x, double y, String value,
                             TextStyle style, UiColor color) {
        drawList.text(new UiPoint(x, y), value, style, color);
    }

    private static String fit(String value, double width, TextStyle style) {
        int max = Math.max(3, (int) Math.floor(width / Math.max(1, style.size() * .54)));
        return value.length() <= max ? value : value.substring(0, Math.max(1, max - 3)) + "...";
    }
}
