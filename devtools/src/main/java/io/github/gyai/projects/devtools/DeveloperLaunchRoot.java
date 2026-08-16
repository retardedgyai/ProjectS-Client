package io.github.gyai.projects.devtools;

import io.github.gyai.projects.ui.runtime.IconKey;
import io.github.gyai.projects.ui.runtime.TextStyle;
import io.github.gyai.projects.ui.runtime.UiAccessibilityMetadata;
import io.github.gyai.projects.ui.runtime.UiAccessibilityRole;
import io.github.gyai.projects.ui.runtime.UiButton;
import io.github.gyai.projects.ui.runtime.UiButtonState;
import io.github.gyai.projects.ui.runtime.UiColor;
import io.github.gyai.projects.ui.runtime.UiColorRole;
import io.github.gyai.projects.ui.runtime.UiDrawList;
import io.github.gyai.projects.ui.runtime.UiFontFamilyRole;
import io.github.gyai.projects.ui.runtime.UiFontWeight;
import io.github.gyai.projects.ui.runtime.UiMaterialTier;
import io.github.gyai.projects.ui.runtime.UiNode;
import io.github.gyai.projects.ui.runtime.UiPoint;
import io.github.gyai.projects.ui.runtime.UiRect;
import io.github.gyai.projects.ui.runtime.UiTheme;
import io.github.gyai.projects.ui.runtime.icon.ShellIconCatalog;

final class DeveloperLaunchRoot extends UiNode {
    enum ServerStatus {
        CHECKING("Checking local server", "Probing 127.0.0.1:25565", UiColor.hex("#91a7ff")),
        READY("Local server ready", "ProjectS is accepting connections", UiColor.hex("#82d8b4")),
        OFFLINE("Local server offline", "Start Paper, then retry the connection", UiColor.hex("#ff9b9b"));

        private final String label;
        private final String detail;
        private final UiColor color;

        ServerStatus(String label, String detail, UiColor color) {
            this.label = label;
            this.detail = detail;
            this.color = color;
        }
    }

    private static final UiColor PAGE = UiColor.hex("#e8e1cc");
    private static final UiColor PAGE_LIGHT = UiColor.hex("#f3eedf");
    private static final UiColor SURFACE = UiColor.hex("#2f332ff2");
    private static final UiColor SURFACE_SOFT = UiColor.hex("#37423ddd");
    private static final UiColor BORDER = UiColor.hex("#c5ddd62e");
    private static final UiColor TEXT = UiColor.hex("#dce7e4");
    private static final UiColor MUTED = UiColor.hex("#a1adaa");
    private static final UiColor ACCENT = UiColor.hex("#9ed8d1");
    private static final UiColor ACCENT_DARK = UiColor.hex("#385650");
    private static final UiColor SHADOW = UiColor.hex("#1115107a");

    private static final TextStyle EYEBROW = style(UiFontWeight.BOLD, 10, 14, 1.3);
    private static final TextStyle HERO = style(UiFontWeight.SEMIBOLD, 34, 40, -.6);
    private static final TextStyle SUBTITLE = style(UiFontWeight.NORMAL, 13, 19, 0);
    private static final TextStyle LABEL = style(UiFontWeight.SEMIBOLD, 13, 18, 0);
    private static final TextStyle SMALL = style(UiFontWeight.NORMAL, 10, 14, .1);
    private static final TextStyle MONO = new TextStyle(
            UiFontFamilyRole.TECHNICAL_MONO, UiFontWeight.NORMAL,
            10, 14, .2, UiColorRole.TEXT_SECONDARY);

    private final LaunchButton connect;
    private final LaunchButton workspace;
    private final LaunchButton exit;
    private ServerStatus status = ServerStatus.CHECKING;
    private String profile = "Developer profile";
    private String version = "Minecraft 26.1.2";
    private UiRect shell = new UiRect(0, 0, 1, 1);
    private UiRect statusCard = new UiRect(0, 0, 1, 1);
    private boolean compact;

    DeveloperLaunchRoot() {
        super("developer-launch", new UiRect(0, 0, 1, 1));
        setClipToBounds(true);
        setAccessibility(UiAccessibilityMetadata.of(
                UiAccessibilityRole.SURFACE, "ProjectS Developer Client"));
        connect = new LaunchButton("connect-local", "Connect local server",
                "127.0.0.1:25565", IconKey.SERVER, true);
        workspace = new LaunchButton("open-workspace", "Open offline workspace",
                "Use authoring tools without joining a world", IconKey.LAYERS, false);
        exit = new LaunchButton("exit-client", "Exit", "",
                IconKey.CLOSE, false);
        addChild(connect);
        addChild(workspace);
        addChild(exit);
    }

    void bindActions(Runnable connectAction, Runnable workspaceAction, Runnable exitAction) {
        connect.setAction(connectAction);
        workspace.setAction(workspaceAction);
        exit.setAction(exitAction);
    }

    void setEnvironment(String profileName, String launchedVersion) {
        profile = profileName == null || profileName.isBlank()
                ? "Developer profile" : profileName;
        version = launchedVersion == null || launchedVersion.isBlank()
                ? "Minecraft 26.1.2" : "Minecraft " + launchedVersion;
    }

    void setServerStatus(ServerStatus next) {
        status = next == null ? ServerStatus.OFFLINE : next;
    }

    ServerStatus serverStatus() {
        return status;
    }

    void layout(int width, int height) {
        setBounds(new UiRect(0, 0, Math.max(1, width), Math.max(1, height)));
        compact = width < 760 || height < 430;
        double margin = compact ? 18 : 38;
        shell = new UiRect(margin, margin,
                Math.max(1, width - margin * 2), Math.max(1, height - margin * 2));
        if (compact) {
            double contentWidth = Math.min(shell.width() - 36, 520);
            double x = shell.x() + 18;
            statusCard = new UiRect(x, shell.y() + 120, contentWidth, 56);
            double buttonY = statusCard.bottom() + 10;
            connect.setBounds(new UiRect(x, buttonY, contentWidth, 46));
            workspace.setBounds(new UiRect(x, buttonY + 54, contentWidth, 46));
            exit.setBounds(new UiRect(x, buttonY + 108, Math.min(180, contentWidth), 30));
        } else {
            double left = shell.x() + 52;
            double contentWidth = Math.min(430, shell.width() - 410);
            double buttonY = shell.bottom() - 154;
            connect.setBounds(new UiRect(left, buttonY, Math.max(240, contentWidth), 48));
            workspace.setBounds(new UiRect(left, buttonY + 58, Math.max(240, contentWidth), 48));
            exit.setBounds(new UiRect(shell.right() - 142, shell.y() + 24, 110, 36));
            statusCard = new UiRect(shell.right() - 310, shell.y() + 92, 258, 224);
        }
    }

    @Override
    protected void appendSelf(UiDrawList drawList, UiTheme theme,
                              UiRect globalBounds, UiRect clip) {
        drawList.fillRect(globalBounds, PAGE);
        drawList.roundedSurface(new UiRect(
                        globalBounds.x() - globalBounds.width() * .16,
                        globalBounds.y() - globalBounds.height() * .38,
                        globalBounds.width() * .82, globalBounds.height() * .9),
                globalBounds.height() * .42, PAGE_LIGHT.withAlpha(138), UiMaterialTier.GLASS_THIN);
        drawList.roundedSurface(new UiRect(
                        globalBounds.right() - globalBounds.width() * .48,
                        globalBounds.bottom() - globalBounds.height() * .42,
                        globalBounds.width() * .62, globalBounds.height() * .72),
                globalBounds.height() * .34, ACCENT_DARK.withAlpha(84), UiMaterialTier.GLASS_THIN);

        drawList.shadow(shell.offset(0, 8), 20, SHADOW);
        drawList.roundedSurface(shell, compact ? 20 : 28, SURFACE, UiMaterialTier.GLASS_PANEL);
        drawList.border(shell, compact ? 20 : 28, 1, BORDER);

        double left = shell.x() + (compact ? 18 : 52);
        double top = shell.y() + (compact ? 22 : 34);
        drawList.icon(new UiRect(left, top, 26, 26), icon(IconKey.BRAND), ACCENT);
        drawList.text(new UiPoint(left + 38, top + 3), "PROJECTS", EYEBROW, TEXT);
        drawList.text(new UiPoint(left + 38, top + 17), "DEVELOPER CLIENT", MONO, MUTED);

        double heroY = shell.y() + (compact ? 82 : 118);
        drawList.text(new UiPoint(left, heroY), "Build the world", HERO, TEXT);
        if (!compact) {
            drawList.text(new UiPoint(left, heroY + 42),
                    "A focused environment for authoring, balancing and runtime inspection.",
                    SUBTITLE, MUTED);
            drawList.text(new UiPoint(left, heroY + 64),
                    "Connect to the local ProjectS server or continue with offline tools.",
                    SUBTITLE, MUTED);
        }

        drawStatusCard(drawList);
        if (!compact) {
            drawList.text(new UiPoint(shell.x() + 22, shell.bottom() - 20),
                    "MAIN / DEVELOPMENT CHANNEL", MONO, MUTED.withAlpha(175));
            drawList.text(new UiPoint(shell.right() - 178, shell.bottom() - 20),
                    version.toUpperCase(), MONO, MUTED.withAlpha(175));
        }
    }

    private void drawStatusCard(UiDrawList drawList) {
        double radius = compact ? 14 : 20;
        drawList.roundedSurface(statusCard, radius, SURFACE_SOFT, UiMaterialTier.GLASS_PANEL);
        drawList.border(statusCard, radius, 1, BORDER);
        double x = statusCard.x() + (compact ? 16 : 20);
        double y = statusCard.y() + (compact ? 14 : 20);
        drawList.roundedSurface(new UiRect(x, y + 2, 8, 8), 4,
                status.color, UiMaterialTier.ACCENT_GLASS);
        drawList.text(new UiPoint(x + 18, y), status.label, LABEL, TEXT);
        drawList.text(new UiPoint(x + 18, y + 19), status.detail, SMALL, MUTED);
        if (compact) return;

        double dividerY = statusCard.y() + 82;
        drawList.fillRect(new UiRect(x, dividerY, statusCard.width() - 40, 1), BORDER);
        drawList.text(new UiPoint(x, dividerY + 20), "SESSION", EYEBROW, MUTED);
        drawList.text(new UiPoint(x, dividerY + 42), profile, LABEL, TEXT);
        drawList.text(new UiPoint(x, dividerY + 64), "Authenticated developer profile", SMALL, MUTED);
        drawList.text(new UiPoint(x, dividerY + 100), "TARGET", EYEBROW, MUTED);
        drawList.text(new UiPoint(x, dividerY + 122), "127.0.0.1:25565", MONO, ACCENT);
    }

    private static io.github.gyai.projects.ui.runtime.IconSpec icon(IconKey key) {
        return ShellIconCatalog.definition(key).spec();
    }

    private static TextStyle style(UiFontWeight weight, double size,
                                   double lineHeight, double spacing) {
        return new TextStyle(UiFontFamilyRole.UI_SANS, weight, size, lineHeight,
                spacing, UiColorRole.TEXT_PRIMARY);
    }

    private static final class LaunchButton extends UiButton {
        private final String detail;
        private final IconKey icon;
        private final boolean primary;
        private Runnable action = () -> { };

        private LaunchButton(String id, String label, String detail,
                             IconKey icon, boolean primary) {
            super(id, new UiRect(0, 0, 1, 1), label);
            this.detail = detail;
            this.icon = icon;
            this.primary = primary;
            setMaterialTier(primary ? UiMaterialTier.ACCENT_GLASS : UiMaterialTier.GLASS_PANEL);
            setRadius(14);
        }

        private void setAction(Runnable next) {
            action = next == null ? () -> { } : next;
        }

        @Override
        public boolean handleEvent(io.github.gyai.projects.ui.runtime.UiEvent event) {
            boolean wasPressed = pressed();
            boolean handled = super.handleEvent(event);
            if (handled && wasPressed && !pressed()
                    && event instanceof io.github.gyai.projects.ui.runtime.UiPointerEvent pointer
                    && pointer.type() == io.github.gyai.projects.ui.runtime.UiPointerType.UP
                    && globalBounds().contains(pointer.position())) {
                action.run();
            } else if (handled && wasPressed && !pressed()
                    && event instanceof io.github.gyai.projects.ui.runtime.UiKeyEvent key
                    && key.action() == io.github.gyai.projects.ui.runtime.UiKeyAction.UP) {
                action.run();
            }
            return handled;
        }

        @Override
        protected void appendSelf(UiDrawList drawList, UiTheme theme,
                                  UiRect bounds, UiRect clip) {
            UiButtonState state = state();
            UiColor fill = primary ? ACCENT_DARK.withAlpha(225) : SURFACE_SOFT.withAlpha(205);
            if (state == UiButtonState.HOVER || state == UiButtonState.FOCUSED) {
                fill = primary ? UiColor.hex("#476b65") : UiColor.hex("#414d47");
            } else if (state == UiButtonState.PRESSED) {
                fill = primary ? UiColor.hex("#304944") : UiColor.hex("#2c3531");
            }
            drawList.shadow(bounds.offset(0, 3), 8, SHADOW.multiplyAlpha(.6));
            drawList.roundedSurface(bounds, 14, fill,
                    primary ? UiMaterialTier.ACCENT_GLASS : UiMaterialTier.GLASS_PANEL);
            drawList.border(bounds, 14, 1,
                    state == UiButtonState.FOCUSED ? ACCENT.withAlpha(220) : BORDER);
            boolean compactLabel = detail.isBlank();
            double iconSize = compactLabel ? 18 : 20;
            double iconY = compactLabel
                    ? bounds.y() + (bounds.height() - iconSize) / 2 : bounds.y() + 14;
            drawList.icon(new UiRect(bounds.x() + 16, iconY, iconSize, iconSize),
                    icon(icon), primary ? ACCENT : MUTED);
            double labelY = compactLabel
                    ? bounds.y() + (bounds.height() - 15) / 2 : bounds.y() + 9;
            drawList.text(new UiPoint(bounds.x() + (compactLabel ? 42 : 48), labelY),
                    label(), LABEL, TEXT);
            if (!compactLabel) {
                drawList.text(new UiPoint(bounds.x() + 48, bounds.y() + 27),
                        detail, SMALL, MUTED);
            }
        }
    }
}
