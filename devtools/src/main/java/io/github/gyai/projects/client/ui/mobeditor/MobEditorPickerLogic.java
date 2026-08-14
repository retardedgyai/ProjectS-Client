package io.github.gyai.projects.client.ui.mobeditor;

import java.util.List;
import java.util.Locale;

/** Pure filtering, paging, and equipment compatibility rules for pickers. */
public final class MobEditorPickerLogic {
    private MobEditorPickerLogic() { }

    public static boolean matches(String id, String label, String query) {
        String needle = lower(query).strip();
        return needle.isBlank() || lower(id).contains(needle) || lower(label).contains(needle);
    }

    public static boolean materialFits(String material, String slot) {
        String item = upper(material);
        String target = upper(slot);
        return switch (target) {
            case "HEAD" -> item.endsWith("_HELMET") || item.endsWith("_HEAD")
                    || item.endsWith("_SKULL") || item.equals("CARVED_PUMPKIN");
            case "CHEST" -> item.endsWith("_CHESTPLATE") || item.equals("ELYTRA");
            case "LEGS" -> item.endsWith("_LEGGINGS");
            case "FEET" -> item.endsWith("_BOOTS");
            case "MAIN_HAND", "OFF_HAND" -> !item.equals("AIR") && !item.isBlank();
            default -> false;
        };
    }

    public static String materialCategory(String material) {
        String item = upper(material);
        if (item.endsWith("_HELMET") || item.endsWith("_HEAD")
                || item.endsWith("_SKULL")) return "頭装備";
        if (item.endsWith("_CHESTPLATE")) return "胴装備";
        if (item.endsWith("_LEGGINGS")) return "脚装備";
        if (item.endsWith("_BOOTS")) return "足装備";
        if (item.endsWith("_SWORD") || item.endsWith("_AXE")
                || item.equals("BOW") || item.equals("CROSSBOW")
                || item.equals("TRIDENT")) return "武器";
        if (item.equals("SHIELD")) return "盾";
        if (item.endsWith("_PICKAXE") || item.endsWith("_SHOVEL")
                || item.endsWith("_HOE")) return "道具";
        return "その他";
    }

    public static <T> List<T> page(List<T> values, int page, int pageSize) {
        List<T> safeValues = values == null ? List.of() : values;
        int size = Math.max(1, pageSize);
        int maximumPage = Math.max(0, (safeValues.size() - 1) / size);
        long startLong = (long) Math.clamp(page, 0, maximumPage) * size;
        int start = (int) Math.min(safeValues.size(), startLong);
        int end = (int) Math.min(safeValues.size(), startLong + size);
        return List.copyOf(safeValues.subList(start, end));
    }

    /** External catalog is intentionally represented as a disabled UI choice. */
    public static boolean externalCatalogEnabled() {
        return false;
    }

    public static String externalCatalogDisabledReason() {
        return "外部カタログは未設定です。安全のため利用できません";
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static String upper(String value) {
        return value == null ? "" : value.toUpperCase(Locale.ROOT);
    }
}
