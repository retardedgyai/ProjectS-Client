package io.github.gyai.projects.client.shell;

import io.github.gyai.projects.ui.runtime.UiRect;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure responsive geometry for the shell. Coordinates are screen coordinates, while the content
 * canvas may be taller than its viewport and is intentionally scrollable at compact heights.
 */
public final class ClientShellLayout {
    public static final int BREAKPOINT_1120 = 1120;
    public static final int BREAKPOINT_900 = 900;
    public static final int BREAKPOINT_560 = 560;

    public enum Breakpoint { WIDE, TABLET, COMPACT, NARROW }

    public record Snapshot(
            int width,
            int height,
            ClientShellPage page,
            ClientShellState state,
            Breakpoint breakpoint,
            Map<String, UiRect> bounds,
            List<String> focusOrder,
            double contentHeight,
            boolean scrollable
    ) {
        public Snapshot {
            bounds = Map.copyOf(bounds);
            focusOrder = List.copyOf(focusOrder);
        }

        public UiRect bounds(String id) {
            UiRect result = bounds.get(id);
            if (result == null) throw new IllegalArgumentException("Unknown shell bound: " + id);
            return result;
        }

        public boolean contains(String childId, String parentId) {
            UiRect child = bounds(childId);
            UiRect parent = bounds(parentId);
            double epsilon = .01;
            return child.x() >= parent.x() - epsilon && child.y() >= parent.y() - epsilon
                    && child.right() <= parent.right() + epsilon
                    && child.bottom() <= parent.bottom() + epsilon;
        }
    }

    private ClientShellLayout() { }

    public static Snapshot compute(int width, int height) {
        return compute(width, height, ClientShellPage.HOME, ClientShellState.HOME);
    }

    public static Snapshot compute(
            int width, int height, ClientShellPage page, ClientShellState state
    ) {
        int safeWidth = Math.max(1, width);
        int safeHeight = Math.max(1, height);
        Breakpoint breakpoint = breakpoint(safeWidth);
        boolean veryShort = safeHeight <= 240;
        double margin = switch (breakpoint) {
            case NARROW -> 0;
            case COMPACT -> ClientShellPalette.spacing(12);
            case TABLET, WIDE -> ClientShellPalette.spacing(20);
        };
        double shellWidth = breakpoint == Breakpoint.COMPACT
                ? Math.min(Math.max(0, safeWidth - margin * 2), 760)
                : breakpoint == Breakpoint.NARROW
                        ? safeWidth : Math.min(1480, Math.max(0, safeWidth - margin * 2));
        double shellHeight = breakpoint == Breakpoint.NARROW
                ? safeHeight : Math.max(0, safeHeight - margin * 2);
        double shellX = Math.max(0, (safeWidth - shellWidth) / 2);
        double shellY = breakpoint == Breakpoint.NARROW ? 0 : margin;

        double topbarHeight = switch (breakpoint) {
            case WIDE, TABLET -> veryShort ? 52 : ClientShellPalette.spacing(76);
            case COMPACT -> veryShort ? 78 : ClientShellPalette.spacing(118);
            case NARROW -> veryShort ? 78 : ClientShellPalette.spacing(104);
        };
        double footerHeight = veryShort ? (breakpoint == Breakpoint.NARROW
                || breakpoint == Breakpoint.COMPACT ? 32 : 30)
                : breakpoint == Breakpoint.NARROW
                        ? ClientShellPalette.spacing(45) : ClientShellPalette.spacing(45);
        double bodyY = shellY + topbarHeight;
        double bodyHeight = Math.max(1, shellHeight - topbarHeight - footerHeight);
        double footerY = shellY + shellHeight - footerHeight;
        double sidebarWidth = switch (breakpoint) {
            case WIDE -> ClientShellPalette.spacing(224);
            case TABLET -> ClientShellPalette.spacing(218);
            case COMPACT, NARROW -> 0;
        };
        double contentX = shellX + sidebarWidth;
        double contentWidth = Math.max(0, shellWidth - sidebarWidth);
        double contentPadding = veryShort ? 8 : switch (breakpoint) {
            case WIDE -> ClientShellPalette.spacing(34);
            case TABLET -> ClientShellPalette.spacing(26);
            case COMPACT -> ClientShellPalette.spacing(20);
            case NARROW -> ClientShellPalette.spacing(16);
        };
        double toolbarHeight = switch (breakpoint) {
            case WIDE, TABLET -> veryShort ? 34 : ClientShellPalette.spacing(67);
            case COMPACT -> veryShort ? 36 : ClientShellPalette.spacing(62);
            case NARROW -> veryShort ? 36 : ClientShellPalette.spacing(58);
        };
        double viewX = contentX + contentPadding;
        double viewY = bodyY + toolbarHeight;
        double viewWidth = Math.max(0, contentWidth - contentPadding * 2);
        // Supported compact surfaces keep a positive viewport even when the page itself must scroll.
        double viewHeight = Math.max(1, footerY - contentPadding - viewY);

        LinkedHashMap<String, UiRect> result = new LinkedHashMap<>();
        put(result, "shell", shellX, shellY, shellWidth, shellHeight);
        put(result, "topbar", shellX, shellY, shellWidth, topbarHeight);
        put(result, "body", shellX, bodyY, shellWidth, bodyHeight);
        put(result, "sidebar", shellX, bodyY, sidebarWidth, bodyHeight);
        put(result, "content", contentX, bodyY, contentWidth, bodyHeight);
        put(result, "toolbar", contentX, bodyY, contentWidth, toolbarHeight);
        put(result, "view", viewX, viewY, viewWidth, viewHeight);
        put(result, "footer", shellX, footerY, shellWidth, footerHeight);

        addChrome(result, breakpoint, shellX, shellY, shellWidth, topbarHeight,
                contentX, contentWidth, toolbarHeight);
        double contentHeight = addPage(result, page, state, breakpoint,
                viewX, viewY, viewWidth, viewHeight);
        addInteractiveChrome(result, breakpoint);
        addInteractiveContent(result, page, state, breakpoint);
        put(result, "content.canvas", viewX, viewY, viewWidth, Math.max(viewHeight, contentHeight));
        List<String> focus = focusOrder(page, state, breakpoint);
        return new Snapshot(safeWidth, safeHeight, page, state, breakpoint, result, focus,
                contentHeight, contentHeight > viewHeight + .01);
    }

    public static Breakpoint breakpoint(int width) {
        if (width <= BREAKPOINT_560) return Breakpoint.NARROW;
        if (width <= BREAKPOINT_900) return Breakpoint.COMPACT;
        if (width <= BREAKPOINT_1120) return Breakpoint.TABLET;
        return Breakpoint.WIDE;
    }

    public static List<String> focusOrder(ClientShellPage page, ClientShellState state) {
        return focusOrder(page, state, Breakpoint.WIDE);
    }

    public static List<String> focusOrder(ClientShellPage page, ClientShellState state,
                                          Breakpoint breakpoint) {
        ArrayList<String> result = new ArrayList<>(List.of(
                "brand", "tab.home", "tab.library", "tab.settings",
                "action.settings", "action.account"));
        if (breakpoint == Breakpoint.WIDE || breakpoint == Breakpoint.TABLET) {
            result.addAll(List.of("side.home", "side.library", "side.settings"));
        }
        result.add("toolbar.review");
        if (page == ClientShellPage.HOME) {
            switch (state) {
                case HOME -> result.addAll(List.of(
                        "home.profile", "home.play.action", "home.open-client.action"));
                case LAUNCHING -> result.add("launch.cancel.action");
                case CONNECTING -> result.addAll(List.of(
                        "connecting.open-client.action", "connecting.error.action", "connecting.cancel.action"));
                case CONNECTED -> result.addAll(List.of(
                        "connected.open-client.action", "connected.home.action"));
                case RECOVERABLE_ERROR -> result.addAll(List.of(
                        "error.retry.action", "error.open-client.action", "error.home.action"));
            }
        } else if (page == ClientShellPage.LIBRARY) {
            result.add("library.home");
        } else {
            result.add("settings.home");
        }
        return List.copyOf(result);
    }

    private static void addChrome(
            Map<String, UiRect> result, Breakpoint breakpoint,
            double shellX, double shellY, double shellWidth, double topbarHeight,
            double contentX, double contentWidth, double toolbarHeight
    ) {
        boolean wrapped = breakpoint == Breakpoint.COMPACT || breakpoint == Breakpoint.NARROW;
        boolean shortTopbar = topbarHeight <= 80;
        double topPadding = breakpoint == Breakpoint.NARROW
                ? ClientShellPalette.spacing(14) : ClientShellPalette.spacing(19);
        double brandWidth = breakpoint == Breakpoint.NARROW
                ? (shortTopbar ? 132 : 154) : 190;
        put(result, "brand", shellX + topPadding, shellY + 11, brandWidth,
                breakpoint == Breakpoint.NARROW ? (shortTopbar ? 32 : 42) : 44);
        if (wrapped) {
            double tabHeight = shortTopbar ? 30 : 40;
            put(result, "tabs", shellX + topPadding, shellY + topbarHeight - tabHeight - 4,
                    Math.min(270, Math.max(0, shellWidth - topPadding * 2)), tabHeight);
            put(result, "actions", shellX + shellWidth - (breakpoint == Breakpoint.NARROW ? 84 : 218),
                    shellY + (shortTopbar ? 7 : 12), breakpoint == Breakpoint.NARROW ? 74 : 204,
                    shortTopbar ? 32 : 35);
        } else {
            put(result, "tabs", shellX + topPadding + brandWidth + 20, shellY,
                    276, topbarHeight);
            put(result, "actions", shellX + shellWidth - 214, shellY + 15, 198, 38);
        }
        if (breakpoint == Breakpoint.NARROW) {
            put(result, "toolbar.breadcrumb", contentX + 8, shellY + topbarHeight,
                    0, toolbarHeight);
            put(result, "toolbar.review", contentX + 8, shellY + topbarHeight + 1,
                    Math.max(1, contentWidth - 16), Math.max(1, toolbarHeight - 2));
        } else {
            put(result, "toolbar.breadcrumb", contentX + 12, shellY + topbarHeight + 1,
                    Math.max(0, contentWidth * .45), toolbarHeight - 2);
            double reviewWidth = 256;
            put(result, "toolbar.review", contentX + contentWidth - reviewWidth - 12,
                    shellY + topbarHeight + (shortTopbar ? 1 : 14),
                    Math.max(1, Math.min(reviewWidth, contentWidth - 24)),
                    Math.max(1, toolbarHeight - (shortTopbar ? 2 : 20)));
        }
    }

    private static void addInteractiveChrome(Map<String, UiRect> result, Breakpoint breakpoint) {
        UiRect tabs = result.get("tabs");
        double tabWidth = Math.max(1, tabs.width() / 3);
        put(result, "tab.home", tabs.x(), tabs.y(), tabWidth, tabs.height());
        put(result, "tab.library", tabs.x() + tabWidth, tabs.y(), tabWidth, tabs.height());
        put(result, "tab.settings", tabs.x() + tabWidth * 2, tabs.y(),
                Math.max(0, tabs.width() - tabWidth * 2), tabs.height());
        UiRect actions = result.get("actions");
        boolean compactActions = breakpoint == Breakpoint.COMPACT || breakpoint == Breakpoint.NARROW;
        double actionHeight = Math.min(34, actions.height());
        put(result, "action.settings", actions.right() - (compactActions ? 72 : 76),
                actions.y(), 34, actionHeight);
        put(result, "action.account", actions.right() - 34, actions.y(), 34, actionHeight);
        UiRect sidebar = result.get("sidebar");
        if (sidebar.width() > 0) {
            double sideX = sidebar.x() + ClientShellPalette.spacing(12);
            double sideWidth = Math.max(0, sidebar.width() - ClientShellPalette.spacing(24));
            put(result, "side.home", sideX, sidebar.y() + 45, sideWidth, 40);
            put(result, "side.library", sideX, sidebar.y() + 93, sideWidth, 40);
            put(result, "side.settings", sideX, sidebar.bottom() - 55, sideWidth, 40);
        }
    }

    private static void addInteractiveContent(
            Map<String, UiRect> result, ClientShellPage page, ClientShellState state,
            Breakpoint breakpoint
    ) {
        if (page == ClientShellPage.LIBRARY) {
            copy(result, "library.home");
            return;
        }
        if (page == ClientShellPage.SETTINGS) {
            copy(result, "settings.home");
            return;
        }
        if (state == ClientShellState.HOME) {
            UiRect server = result.get("home.server");
            put(result, "home.profile", server.x() + server.width() - 60, server.y() + 91, 40, 38);
            UiRect play = result.get("home.play");
            boolean narrow = breakpoint == Breakpoint.COMPACT || breakpoint == Breakpoint.NARROW;
            if (narrow) {
                put(result, "home.play.action", play.x() + 20, play.bottom() - 92,
                        Math.max(0, play.width() - 40), 36);
                put(result, "home.open-client.action", play.x() + 20, play.bottom() - 48,
                        Math.max(0, play.width() - 40), 36);
            } else {
                put(result, "home.play.action", play.x() + 20, play.bottom() - 48,
                        Math.max(0, play.width() * .46), 36);
                put(result, "home.open-client.action", play.x() + 28 + play.width() * .46,
                        play.bottom() - 48, Math.max(0, play.width() * .46 - 8), 36);
            }
            return;
        }
        UiRect primary = result.get("flow.primary");
        switch (state) {
            case LAUNCHING -> put(result, "launch.cancel.action", primary.x() + 20,
                    primary.bottom() - 48, Math.min(160, Math.max(0, primary.width() - 40)), 36);
            case CONNECTING -> {
                boolean stacked = primary.width() < 520;
                if (stacked) {
                    put(result, "connecting.open-client.action", primary.x() + 20, primary.bottom() - 136,
                            Math.max(0, primary.width() - 40), 36);
                    put(result, "connecting.error.action", primary.x() + 20, primary.bottom() - 92,
                            Math.max(0, primary.width() - 40), 36);
                    put(result, "connecting.cancel.action", primary.x() + 20, primary.bottom() - 48,
                            Math.max(0, primary.width() - 40), 36);
                } else {
                    put(result, "connecting.open-client.action", primary.x() + 20, primary.bottom() - 48,
                            Math.max(0, primary.width() * .46), 36);
                    put(result, "connecting.error.action", primary.x() + 28 + primary.width() * .46,
                            primary.bottom() - 48, Math.max(0, primary.width() * .30), 36);
                    put(result, "connecting.cancel.action", primary.right() - 86, primary.bottom() - 48, 66, 36);
                }
            }
            case CONNECTED -> {
                boolean narrow = primary.width() < 520;
                put(result, "connected.open-client.action", primary.x() + 20,
                        primary.bottom() - (narrow ? 92 : 48),
                        Math.max(0, narrow ? primary.width() - 40 : primary.width() * .52), 36);
                put(result, "connected.home.action", primary.x() + (narrow ? 20 : 28 + primary.width() * .52),
                        primary.bottom() - 48,
                        Math.max(0, narrow ? primary.width() - 40 : primary.width() * .40), 36);
            }
            case RECOVERABLE_ERROR -> {
                boolean narrow = primary.width() < 520;
                put(result, "error.retry.action", primary.x() + 20,
                        primary.bottom() - (narrow ? 136 : 48),
                        Math.max(0, narrow ? primary.width() - 40 : primary.width() * .35), 36);
                put(result, "error.open-client.action", primary.x() + (narrow ? 20 : 28 + primary.width() * .35),
                        primary.bottom() - (narrow ? 92 : 48),
                        Math.max(0, narrow ? primary.width() - 40 : primary.width() * .35), 36);
                put(result, "error.home.action", primary.x() + (narrow ? 20 : primary.width() - 88),
                        primary.bottom() - 48, Math.max(0, narrow ? primary.width() - 40 : 68), 36);
            }
            case HOME -> { }
        }
    }

    private static void copy(Map<String, UiRect> result, String id) {
        UiRect bounds = result.get(id);
        put(result, id, bounds.x(), bounds.y(), bounds.width(), bounds.height());
    }

    private static double addPage(
            Map<String, UiRect> result, ClientShellPage page, ClientShellState state,
            Breakpoint breakpoint, double x, double y, double width, double viewportHeight
    ) {
        boolean singleColumn = breakpoint == Breakpoint.COMPACT || breakpoint == Breakpoint.NARROW;
        double gap = ClientShellPalette.spacing(18);
        double introHeight = breakpoint == Breakpoint.NARROW ? 190 : singleColumn ? 140 : 82;
        put(result, "content.intro", x, y, width, introHeight);
        if (page == ClientShellPage.LIBRARY) {
            return addLibrary(result, breakpoint, x, y, width, introHeight, gap);
        }
        if (page == ClientShellPage.SETTINGS) {
            return addSettings(result, breakpoint, x, y, width, introHeight, gap);
        }
        return state == ClientShellState.HOME
                ? addHome(result, breakpoint, x, y, width, introHeight, gap)
                : addFlow(result, state, breakpoint, x, y, width, introHeight, gap);
    }

    private static double addHome(
            Map<String, UiRect> result, Breakpoint breakpoint,
            double x, double y, double width, double introHeight, double gap
    ) {
        boolean single = breakpoint == Breakpoint.COMPACT || breakpoint == Breakpoint.NARROW;
        double top = y + introHeight + gap;
        if (!single) {
            double rightWidth = Math.max(0, Math.min(330, width * .32));
            double leftWidth = Math.max(0, width - rightWidth - gap);
            double rowHeight = breakpoint == Breakpoint.TABLET ? 252 : 274;
            put(result, "home.play", x, top, leftWidth, rowHeight);
            put(result, "home.server", x + leftWidth + gap, top, rightWidth, rowHeight);
            double lowerTop = top + rowHeight + gap;
            double journeyWidth = Math.max(0, width * .58);
            put(result, "home.journey", x, lowerTop, journeyWidth, 148);
            put(result, "home.sample", x + journeyWidth + gap, lowerTop,
                    Math.max(0, width - journeyWidth - gap), 148);
            return lowerTop + 148 - y;
        }
        double cardWidth = width;
        double playHeight = breakpoint == Breakpoint.NARROW ? 260 : 276;
        double serverHeight = breakpoint == Breakpoint.NARROW ? 246 : 260;
        put(result, "home.play", x, top, cardWidth, playHeight);
        double serverTop = top + playHeight + gap;
        put(result, "home.server", x, serverTop, cardWidth, serverHeight);
        double journeyTop = serverTop + serverHeight + gap;
        put(result, "home.journey", x, journeyTop, cardWidth, 154);
        double sampleTop = journeyTop + 154 + gap;
        put(result, "home.sample", x, sampleTop, cardWidth, 142);
        return sampleTop + 142 - y;
    }

    private static double addFlow(
            Map<String, UiRect> result, ClientShellState state, Breakpoint breakpoint,
            double x, double y, double width, double introHeight, double gap
    ) {
        boolean single = breakpoint == Breakpoint.COMPACT || breakpoint == Breakpoint.NARROW;
        double top = y + introHeight + gap;
        if (single) {
            double primaryHeight = state == ClientShellState.CONNECTING ? 390
                    : state == ClientShellState.CONNECTED || state == ClientShellState.RECOVERABLE_ERROR
                            ? 390 : 330;
            double sideHeight = 220;
            put(result, "flow.primary", x, top, width, primaryHeight);
            put(result, "flow.snapshot", x, top + primaryHeight + gap, width, sideHeight);
            put(result, "flow.note", x, top + primaryHeight + gap * 2 + sideHeight, width, 150);
            return top + primaryHeight + gap * 2 + sideHeight + 150 - y;
        }
        double sideWidth = Math.max(230, Math.min(330, width * .32));
        double primaryWidth = Math.max(0, width - sideWidth - gap);
        double primaryHeight = primaryWidth < 520 ? 390 : state == ClientShellState.CONNECTING ? 346 : 328;
        put(result, "flow.primary", x, top, primaryWidth, primaryHeight);
        put(result, "flow.snapshot", x + primaryWidth + gap, top, sideWidth, 208);
        double noteHeight = state == ClientShellState.CONNECTING ? 166 : 150;
        put(result, "flow.note", x + primaryWidth + gap, top + 208 + gap, sideWidth, noteHeight);
        return Math.max(top + primaryHeight, top + 208 + gap + noteHeight) - y;
    }

    private static double addLibrary(
            Map<String, UiRect> result, Breakpoint breakpoint,
            double x, double y, double width, double introHeight, double gap
    ) {
        boolean single = breakpoint == Breakpoint.COMPACT || breakpoint == Breakpoint.NARROW;
        double top = y + introHeight + gap;
        if (single) {
            double cardHeight = 176;
            put(result, "library.horizon", x, top, width, cardHeight);
            put(result, "library.atelier", x, top + cardHeight + gap, width, cardHeight);
            put(result, "library.home", x, top + (cardHeight + gap) * 2, width, 42);
            return top + (cardHeight + gap) * 2 + 42 - y;
        }
        double cardWidth = Math.max(0, (width - gap) / 2);
        put(result, "library.horizon", x, top, cardWidth, 190);
        put(result, "library.atelier", x + cardWidth + gap, top, cardWidth, 190);
        put(result, "library.home", x, top + 190 + gap, 220, 42);
        return top + 190 + gap + 42 - y;
    }

    private static double addSettings(
            Map<String, UiRect> result, Breakpoint breakpoint,
            double x, double y, double width, double introHeight, double gap
    ) {
        boolean single = breakpoint == Breakpoint.COMPACT || breakpoint == Breakpoint.NARROW;
        double top = y + introHeight + gap;
        if (single) {
            double cardHeight = 192;
            put(result, "settings.motion", x, top, width, cardHeight);
            put(result, "settings.privacy", x, top + cardHeight + gap, width, cardHeight);
            put(result, "settings.home", x, top + (cardHeight + gap) * 2, width, 42);
            return top + (cardHeight + gap) * 2 + 42 - y;
        }
        double cardWidth = Math.max(0, (width - gap) / 2);
        put(result, "settings.motion", x, top, cardWidth, 200);
        put(result, "settings.privacy", x + cardWidth + gap, top, cardWidth, 200);
        put(result, "settings.home", x, top + 200 + gap, 220, 42);
        return top + 200 + gap + 42 - y;
    }

    private static void put(Map<String, UiRect> map, String id,
                            double x, double y, double width, double height) {
        map.put(id, new UiRect(Math.max(0, x), Math.max(0, y),
                Math.max(0, width), Math.max(0, height)));
    }
}
