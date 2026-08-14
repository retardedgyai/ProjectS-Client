package io.github.gyai.projects.client.ui.mobeditor;

import io.github.gyai.projects.client.ui.icon.ProjectSIcon;

import java.util.List;
import java.util.Locale;

/** Pure filtering and visual mapping shared by Mob Editor views and tests. */
public final class MobEditorUiLogic {
    private MobEditorUiLogic() { }

    public static boolean matches(
            String id, String displayName, Iterable<String> tags, String query
    ) {
        String needle = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
        if (needle.isEmpty()) return true;
        if (lower(id).contains(needle) || lower(displayName).contains(needle)) return true;
        if (tags != null) for (String tag : tags) if (lower(tag).contains(needle)) return true;
        return false;
    }

    public static ProjectSIcon entityIcon(String entityType, String category) {
        String type = upper(entityType);
        if (type.contains("ZOMBIE") || type.equals("HUSK") || type.equals("DROWNED")) {
            return ProjectSIcon.ZOMBIE;
        }
        if (type.contains("SKELETON") || type.equals("STRAY") || type.equals("BOGGED")) {
            return ProjectSIcon.SKELETON;
        }
        return switch (type) {
            case "CREEPER" -> ProjectSIcon.CREEPER;
            case "SPIDER", "CAVE_SPIDER" -> ProjectSIcon.SPIDER;
            case "ENDERMAN" -> ProjectSIcon.ENDERMAN;
            case "VILLAGER", "WANDERING_TRADER" -> ProjectSIcon.VILLAGER;
            case "WOLF", "CAT", "HORSE", "SHEEP", "COW", "PIG" -> ProjectSIcon.ANIMAL;
            default -> switch (upper(category)) {
                case "BOSS" -> ProjectSIcon.BOSS;
                case "ELITE" -> ProjectSIcon.ELITE;
                case "NORMAL" -> ProjectSIcon.NORMAL_MOB;
                default -> ProjectSIcon.MOB_GENERIC;
            };
        };
    }

    public static ProjectSIcon categoryIcon(String category) {
        return switch (upper(category)) {
            case "BOSS" -> ProjectSIcon.BOSS;
            case "ELITE" -> ProjectSIcon.ELITE;
            default -> ProjectSIcon.NORMAL_MOB;
        };
    }

    public static String categoryBadge(String category) {
        return switch (upper(category)) {
            case "BOSS" -> "BOSS";
            case "ELITE" -> "ELITE";
            default -> "NORMAL";
        };
    }

    public static int pageCount(int itemCount, int pageSize) {
        int size = Math.max(1, pageSize);
        int count = Math.max(0, itemCount);
        return Math.max(1, count / size + (count % size == 0 ? 0 : 1));
    }

    public static int clampPage(int page, int itemCount, int pageSize) {
        return Math.clamp(page, 0, pageCount(itemCount, pageSize) - 1);
    }

    public static boolean categoryMatches(String category, String filter) {
        return filter == null || filter.isBlank() || upper(filter).equals("ALL")
                || upper(category).equals(upper(filter));
    }

    public static boolean headMatches(
            String id, String displayName, Iterable<String> tags,
            boolean favorite, String query, boolean favoritesOnly
    ) {
        return (!favoritesOnly || favorite) && matches(id, displayName, tags, query);
    }

    public static int maxScroll(int contentHeight, int viewportHeight) {
        long difference = (long) contentHeight - Math.max(0, viewportHeight);
        return (int) Math.clamp(difference, 0L, Integer.MAX_VALUE);
    }

    public static int clampScroll(int scroll, int contentHeight, int viewportHeight) {
        return Math.clamp(scroll, 0, maxScroll(contentHeight, viewportHeight));
    }

    public static boolean equipmentSupported(String entityType) {
        String value = upper(entityType);
        return value.contains("ZOMBIE") || value.contains("SKELETON")
                || List.of("HUSK", "DROWNED", "PIGLIN", "PIGLIN_BRUTE",
                "ZOMBIFIED_PIGLIN", "STRAY", "BOGGED", "VILLAGER",
                "VINDICATOR", "PILLAGER", "WITCH").contains(value);
    }

    public static boolean needsAppearanceConfirmation(
            String previousEntityType, String nextEntityType,
            boolean hasVariantOrEquipment
    ) {
        return hasVariantOrEquipment && !upper(previousEntityType).equals(upper(nextEntityType));
    }

    public static List<ProjectSIcon> previewToolbarIcons() {
        return List.of(ProjectSIcon.RESET, ProjectSIcon.CAMERA,
                ProjectSIcon.BACKGROUND, ProjectSIcon.GRID, ProjectSIcon.HITBOX,
                ProjectSIcon.EYE_LINE, ProjectSIcon.PLAY);
    }

    public static List<ProjectSIcon> actionBarIcons() {
        return List.of(ProjectSIcon.UNDO, ProjectSIcon.RELOAD,
                ProjectSIcon.SUCCESS, ProjectSIcon.SAVE,
                ProjectSIcon.APPLY, ProjectSIcon.SPAWN);
    }

    public static String formatNumber(double value, int maximumDecimals) {
        if (!Double.isFinite(value)) return "0";
        String format = "%." + Math.clamp(maximumDecimals, 0, 6) + "f";
        String rendered = String.format(Locale.ROOT, format, value);
        int decimal = rendered.indexOf('.');
        if (decimal < 0) return rendered;
        int end = rendered.length();
        while (end > decimal && rendered.charAt(end - 1) == '0') end--;
        if (end > decimal && rendered.charAt(end - 1) == '.') end--;
        String result = rendered.substring(0, Math.max(decimal == 0 ? 1 : decimal, end));
        return result.equals("-0") ? "0" : result;
    }

    public static String formatPercent(double ratio) {
        return formatNumber(ratio * 100, 2) + "%";
    }

    public static boolean actionBarFits(int screenWidth, int usedWidth, int padding) {
        int safePadding = Math.max(0, padding);
        long available = (long) screenWidth - safePadding * 2L;
        return usedWidth >= 0 && usedWidth <= Math.max(0L, available);
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static String upper(String value) {
        return value == null ? "" : value.toUpperCase(Locale.ROOT);
    }
}
