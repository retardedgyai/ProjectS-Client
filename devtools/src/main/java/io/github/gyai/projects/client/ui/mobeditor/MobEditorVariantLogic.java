package io.github.gyai.projects.client.ui.mobeditor;

import java.util.List;
import java.util.Locale;

/** Whitelisted entity-variant keys and values exposed by the appearance picker. */
public final class MobEditorVariantLogic {
    private static final List<String> DYES = List.of("WHITE", "ORANGE", "MAGENTA",
            "LIGHT_BLUE", "YELLOW", "LIME", "PINK", "GRAY", "LIGHT_GRAY", "CYAN",
            "PURPLE", "BLUE", "BROWN", "GREEN", "RED", "BLACK");

    private MobEditorVariantLogic() { }

    public static List<String> keys(String entityType) {
        return switch (upper(entityType)) {
            case "SLIME" -> List.of("size");
            case "SHEEP" -> List.of("color", "sheared");
            case "WOLF" -> List.of("variant", "collar-color", "angry");
            case "CAT" -> List.of("variant", "collar-color");
            case "HORSE" -> List.of("color");
            case "VILLAGER" -> List.of("profession", "villager-type");
            default -> List.of();
        };
    }

    public static List<String> options(String entityType, String key) {
        String type = upper(entityType);
        if (key == null) return List.of();
        if (key.equals("sheared") || key.equals("angry")) return List.of("false", "true");
        if (key.equals("collar-color") || key.equals("color") && type.equals("SHEEP")) {
            return DYES;
        }
        if (type.equals("CAT") && key.equals("variant")) return List.of("TABBY", "BLACK",
                "RED", "SIAMESE", "BRITISH_SHORTHAIR", "CALICO", "PERSIAN", "RAGDOLL",
                "WHITE", "JELLIE", "ALL_BLACK");
        if (type.equals("WOLF") && key.equals("variant")) return List.of("PALE", "SPOTTED",
                "SNOWY", "BLACK", "ASHEN", "RUSTY", "WOODS", "CHESTNUT", "STRIPED");
        if (type.equals("HORSE") && key.equals("color")) return List.of("WHITE", "CREAMY",
                "CHESTNUT", "BROWN", "BLACK", "GRAY", "DARK_BROWN");
        if (type.equals("VILLAGER") && key.equals("profession")) return List.of("NONE",
                "ARMORER", "BUTCHER", "CARTOGRAPHER", "CLERIC", "FARMER", "FISHERMAN",
                "FLETCHER", "LEATHERWORKER", "LIBRARIAN", "MASON", "NITWIT", "SHEPHERD",
                "TOOLSMITH", "WEAPONSMITH");
        if (type.equals("VILLAGER") && key.equals("villager-type")) return List.of("DESERT",
                "JUNGLE", "PLAINS", "SAVANNA", "SNOW", "SWAMP", "TAIGA");
        return List.of();
    }

    public static boolean supported(String entityType, String key) {
        return keys(entityType).contains(key);
    }

    private static String upper(String value) {
        return value == null ? "" : value.toUpperCase(Locale.ROOT);
    }
}
