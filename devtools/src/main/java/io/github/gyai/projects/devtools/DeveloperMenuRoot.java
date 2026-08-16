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

/** Pure Caelestia-style developer dashboard; Minecraft actions stay in its screen host. */
public final class DeveloperMenuRoot extends UiNode {
    private static final UiColor SCRIM = UiColor.hex("#090c0be8");
    private static final UiColor SHELL = UiColor.hex("#292d29f7");
    private static final UiColor SIDEBAR = UiColor.hex("#1e221ff5");
    private static final UiColor SURFACE = UiColor.hex("#343934f2");
    private static final UiColor SURFACE_HOVER = UiColor.hex("#3d443df5");
    private static final UiColor BORDER = UiColor.hex("#d8e5cf25");
    private static final UiColor TEXT = UiColor.hex("#f0f3eb");
    private static final UiColor MUTED = UiColor.hex("#a7afa4");
    private static final UiColor DIM = UiColor.hex("#747c72");
    private static final UiColor ACCENT = UiColor.hex("#cedf9f");
    private static final UiColor ACCENT_DARK = UiColor.hex("#465331");
    private static final UiColor ONLINE = UiColor.hex("#a7d89b");
    private static final UiColor ORANGE = UiColor.hex("#e7b47f");
    private static final UiColor BLUE = UiColor.hex("#91c5d9");
    private static final UiColor VIOLET = UiColor.hex("#c4a7df");
    private static final UiColor SHADOW = UiColor.hex("#05070599");

    private static final TextStyle TITLE = style(UiFontWeight.SEMIBOLD, 23, 28, -.2);
    private static final TextStyle BRAND = style(UiFontWeight.BOLD, 12, 15, 1.1);
    private static final TextStyle LABEL = style(UiFontWeight.SEMIBOLD, 13, 17, 0);
    private static final TextStyle BODY = style(UiFontWeight.NORMAL, 9, 13, 0);
    private static final TextStyle SMALL = style(UiFontWeight.NORMAL, 8, 11, .1);
    private static final TextStyle MONO = new TextStyle(UiFontFamilyRole.TECHNICAL_MONO,
            UiFontWeight.NORMAL, 8, 11, .4, UiColorRole.TEXT_SECONDARY);

    private final ToolCard serverCard;
    private final ToolCard balanceCard;
    private final ToolCard mobCard;
    private final ToolCard skillCard;
    private UiRect shell = new UiRect(0, 0, 1, 1);
    private boolean connected;
    private boolean compact;

    public DeveloperMenuRoot(Runnable openServer, Runnable openBalance,
                             Runnable openMob, Runnable openSkill) {
        super("developer-menu", new UiRect(0, 0, 1, 1));
        setClipToBounds(true);
        setAccessibility(UiAccessibilityMetadata.of(UiAccessibilityRole.SURFACE,
                "ProjectS Developer Overlay"));
        serverCard = card("server", "SERVER DEV MENU", "Run commands and inspect the live server",
                "COMMAND", IconKey.SERVER, ACCENT, openServer);
        balanceCard = card("balance", "BALANCE", "Tune gameplay values and publish updates",
                "SERVER", IconKey.SLIDERS, ORANGE, openBalance);
        mobCard = card("mob", "MOB EDITOR", "Author entities, stats and behaviours",
                "SERVER", IconKey.LIBRARY, BLUE, openMob);
        skillCard = card("skill", "SKILL EDITOR", "Build abilities, motion and visual effects",
                "SERVER", IconKey.SPARKLE, VIOLET, openSkill);
    }

    private ToolCard card(String id, String title, String detail, String badge,
                          IconKey icon, UiColor color, Runnable action) {
        ToolCard card = new ToolCard(id, title, detail, badge, icon, color, action);
        addChild(card);
        return card;
    }

    public void setConnected(boolean next) {
        connected = next;
        serverCard.setEnabled(next);
        balanceCard.setEnabled(next);
        mobCard.setEnabled(next);
        skillCard.setEnabled(next);
    }

    public void layout(int width, int height) {
        setBounds(new UiRect(0, 0, Math.max(1, width), Math.max(1, height)));
        compact = width < 760 || height < 420;
        double margin = compact ? 12 : 24;
        shell = new UiRect(margin, margin, Math.max(1, width - margin * 2),
                Math.max(1, height - margin * 2));
        double sidebarWidth = compact ? 116 : 174;
        double gutter = compact ? 14 : 24;
        double x = shell.x() + sidebarWidth + gutter;
        double right = shell.right() - gutter;
        double y = shell.y() + (compact ? 76 : 104);
        double gap = compact ? 8 : 12;
        double cardWidth = Math.max(1, (right - x - gap) / 2);
        double cardHeight = Math.max(56, (shell.bottom() - y - gutter - gap) / 2);
        serverCard.setBounds(new UiRect(x, y, cardWidth, cardHeight));
        balanceCard.setBounds(new UiRect(x + cardWidth + gap, y, cardWidth, cardHeight));
        mobCard.setBounds(new UiRect(x, y + cardHeight + gap, cardWidth, cardHeight));
        skillCard.setBounds(new UiRect(x + cardWidth + gap, y + cardHeight + gap, cardWidth, cardHeight));
    }

    @Override
    protected void appendSelf(UiDrawList drawList, UiTheme theme,
                              UiRect globalBounds, UiRect clip) {
        double sidebarWidth = compact ? 116 : 174;
        double gutter = compact ? 14 : 24;
        double contentX = shell.x() + sidebarWidth + gutter;
        drawList.fillRect(globalBounds, SCRIM);
        drawList.shadow(shell.offset(0, 7), 18, SHADOW);
        drawList.roundedSurface(shell, compact ? 14 : 20, SHELL, UiMaterialTier.GLASS_PANEL);
        drawList.border(shell, compact ? 14 : 20, 1, BORDER);
        UiRect sidebar = new UiRect(shell.x(), shell.y(), sidebarWidth, shell.height());
        drawList.roundedSurface(sidebar, compact ? 14 : 20, SIDEBAR, UiMaterialTier.GLASS_PANEL);
        drawList.fillRect(new UiRect(sidebar.right() - 1, shell.y(), 1, shell.height()), BORDER);

        drawBrand(drawList, shell.x() + 17, shell.y() + 20);
        drawNavigation(drawList, shell.x() + 16, shell.y() + (compact ? 70 : 91));
        drawStatus(drawList, shell.x() + 16, shell.bottom() - 48, sidebarWidth - 32);

        drawList.text(new UiPoint(contentX, shell.y() + (compact ? 17 : 24)),
                "DEVELOPER OVERLAY", compact ? LABEL : TITLE, TEXT);
        drawList.text(new UiPoint(contentX, shell.y() + (compact ? 38 : 55)),
                "Choose a workspace to continue", compact ? SMALL : BODY, MUTED);
        drawList.text(new UiPoint(shell.right() - (compact ? 100 : 123), shell.y() + (compact ? 23 : 31)),
                "R-SHIFT  CLOSE", MONO, DIM);
    }

    private void drawBrand(UiDrawList drawList, double x, double y) {
        double size = compact ? 24 : 29;
        drawList.roundedSurface(new UiRect(x, y, size, size), 8, ACCENT, UiMaterialTier.ACCENT_GLASS);
        drawList.icon(new UiRect(x + 5, y + 5, size - 10, size - 10), icon(IconKey.BRAND), SIDEBAR);
        drawList.text(new UiPoint(x + size + 9, y + 2), "PROJECTS", BRAND, TEXT);
        drawList.text(new UiPoint(x + size + 9, y + (compact ? 15 : 17)), "DEV CLIENT", MONO, ACCENT);
    }

    private void drawNavigation(UiDrawList drawList, double x, double y) {
        drawList.text(new UiPoint(x, y), "WORKSPACE", MONO, DIM);
        double rowY = y + 17;
        double rowWidth = compact ? 84 : 142;
        drawList.roundedSurface(new UiRect(x, rowY, rowWidth, compact ? 27 : 31),
                7, ACCENT_DARK, UiMaterialTier.ACCENT_GLASS);
        drawList.icon(new UiRect(x + 8, rowY + (compact ? 7 : 8), 13, 13),
                icon(IconKey.HOME), ACCENT);
        drawList.text(new UiPoint(x + 28, rowY + (compact ? 7 : 9)), "Dashboard", BODY, TEXT);
        if (compact) return;
        String[] labels = {"Server", "Balancing", "Entities", "Abilities"};
        IconKey[] icons = {IconKey.SERVER, IconKey.SLIDERS, IconKey.LIBRARY, IconKey.SPARKLE};
        for (int index = 0; index < labels.length; index++) {
            double itemY = rowY + 43 + index * 30;
            drawList.icon(new UiRect(x + 9, itemY + 4, 12, 12), icon(icons[index]), DIM);
            drawList.text(new UiPoint(x + 28, itemY + 4), labels[index], BODY, MUTED);
        }
    }

    private void drawStatus(UiDrawList drawList, double x, double y, double width) {
        UiColor color = connected ? ONLINE : ORANGE;
        drawList.roundedSurface(new UiRect(x, y, width, 32), 8, SURFACE, UiMaterialTier.GLASS_THIN);
        drawList.roundedSurface(new UiRect(x + 10, y + 12, 7, 7), 4, color, UiMaterialTier.ACCENT_GLASS);
        drawList.text(new UiPoint(x + 25, y + (compact ? 10 : 7)),
                connected ? "SERVER ONLINE" : "OFFLINE", MONO, color);
        if (!compact) drawList.text(new UiPoint(x + 25, y + 18),
                connected ? "Live tools available" : "Join a world to unlock", SMALL, DIM);
    }

    private static io.github.gyai.projects.ui.runtime.IconSpec icon(IconKey key) {
        return ShellIconCatalog.definition(key).spec();
    }

    private static TextStyle style(UiFontWeight weight, double size,
                                   double lineHeight, double spacing) {
        return new TextStyle(UiFontFamilyRole.UI_SANS, weight, size, lineHeight,
                spacing, UiColorRole.TEXT_PRIMARY);
    }

    private static final class ToolCard extends UiButton {
        private final String detail;
        private final String badge;
        private final IconKey icon;
        private final UiColor color;

        private ToolCard(String id, String title, String detail, String badge,
                         IconKey icon, UiColor color, Runnable action) {
            super("developer-tool-" + id, new UiRect(0, 0, 1, 1), title, action);
            this.detail = detail;
            this.badge = badge;
            this.icon = icon;
            this.color = color;
            setRadius(11);
            setMaterialTier(UiMaterialTier.GLASS_PANEL);
        }

        @Override
        protected void appendSelf(UiDrawList drawList, UiTheme theme,
                                  UiRect bounds, UiRect clip) {
            UiButtonState state = state();
            UiColor fill = state == UiButtonState.HOVER || state == UiButtonState.FOCUSED
                    ? SURFACE_HOVER : SURFACE;
            if (!enabled()) fill = fill.multiplyAlpha(.58);
            drawList.shadow(bounds.offset(0, 3), 8, SHADOW.multiplyAlpha(.5));
            drawList.roundedSurface(bounds, 11, fill, UiMaterialTier.GLASS_PANEL);
            drawList.border(bounds, 11, 1,
                    enabled() && (state == UiButtonState.HOVER || state == UiButtonState.FOCUSED)
                            ? color.withAlpha(185) : BORDER);
            double iconSize = Math.min(38, Math.max(28, bounds.height() * .34));
            double iconX = bounds.x() + 14;
            double iconY = bounds.y() + 14;
            drawList.roundedSurface(new UiRect(iconX, iconY, iconSize, iconSize), 9,
                    enabled() ? color.withAlpha(42) : BORDER.withAlpha(32), UiMaterialTier.GLASS_THIN);
            drawList.icon(new UiRect(iconX + 8, iconY + 8, iconSize - 16, iconSize - 16),
                    icon(icon), enabled() ? color : DIM);
            double textX = iconX + iconSize + 11;
            drawList.text(new UiPoint(textX, bounds.y() + 14), label(), LABEL, enabled() ? TEXT : DIM);
            if (bounds.height() >= 78) {
                drawList.text(new UiPoint(textX, bounds.y() + 33), detail, SMALL, enabled() ? MUTED : DIM);
                drawList.text(new UiPoint(textX, bounds.bottom() - 20), badge, MONO, enabled() ? color : DIM);
                drawList.text(new UiPoint(bounds.right() - 51, bounds.bottom() - 20), "OPEN  >", MONO,
                        enabled() ? color : DIM);
            }
        }
    }
}
