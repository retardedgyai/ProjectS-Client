package io.github.gyai.projects.client.ui.mobeditor;

import java.util.List;

/** Targeted executable coverage for the Mob Editor helper/picker contracts. */
public final class MobEditorUxHelpersTest {
    private MobEditorUxHelpersTest() { }

    public static void main(String[] args) {
        pickerLogic();
        colorLogic();
        inputLogic();
        variantLogic();
        visualLogic();
        viewState();
        layoutContract();
        System.out.println("MobEditorUxHelpersTest passed");
    }

    private static void pickerLogic() {
        assert MobEditorPickerLogic.matches("IRON_SWORD", "Iron Sword", "sword");
        assert MobEditorPickerLogic.matches("starter_sword", "スターターソード", "STARTER");
        assert MobEditorPickerLogic.materialFits("IRON_HELMET", "head");
        assert MobEditorPickerLogic.materialFits("ELYTRA", "CHEST");
        assert MobEditorPickerLogic.materialFits("BOW", "MAIN_HAND");
        assert !MobEditorPickerLogic.materialFits("IRON_SWORD", "HEAD");
        assert MobEditorPickerLogic.page(List.of(1, 2, 3, 4, 5), 1, 2)
                .equals(List.of(3, 4));
        assert MobEditorPickerLogic.page(List.of(1, 2, 3), 100, 2)
                .equals(List.of(3));
        expectImmutable(MobEditorPickerLogic.page(List.of(1, 2, 3), 0, 2));
        assert !MobEditorPickerLogic.externalCatalogEnabled();
        assert !MobEditorPickerLogic.externalCatalogDisabledReason().isBlank();
    }

    private static void colorLogic() {
        var hsv = MobEditorColorLogic.rgbToHsv(0x3366CC);
        assert MobEditorColorLogic.hsvToRgb(hsv.hue(), hsv.saturation(), hsv.value())
                == 0x3366CC;
        assert MobEditorColorLogic.parseHex("#00aaff") == 0x00AAFF;
        assert MobEditorColorLogic.parseHex("00AAFF") == -1;
        assert MobEditorColorLogic.parseHex("#GG0000") == -1;
        assert MobEditorColorLogic.formatHex(0xA06540).equals("#A06540");
        assert MobEditorColorLogic.hsvToRgb(Double.NaN, Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY) == 0;
    }

    private static void variantLogic() {
        assert MobEditorVariantLogic.keys("SLIME").equals(List.of("size"));
        assert MobEditorVariantLogic.options("SHEEP", "color").contains("RED");
        assert MobEditorVariantLogic.options("WOLF", "variant").contains("PALE");
        assert MobEditorVariantLogic.options("VILLAGER", "profession")
                .contains("LIBRARIAN");
        assert !MobEditorVariantLogic.supported("CREEPER", "variant");
    }

    private static void inputLogic() {
        assert MobEditorInputLogic.validHeadImport("projects:head", "Head", "texture", "note",
                List.of("undead", "boss"));
        assert !MobEditorInputLogic.validHeadImport("projects:head", "Head", "texture", "note",
                List.of("same", "same"));
        assert !MobEditorInputLogic.utf8Within("あ", 2);
        assert MobEditorInputLogic.utf8Within("あ", 3);
    }

    private static void visualLogic() {
        assert MobEditorUiLogic.matches("zombie_boss", "Zombie Boss", List.of("undead"),
                "undead");
        assert MobEditorUiLogic.entityIcon("ZOMBIFIED_PIGLIN", "NORMAL") != null;
        assert MobEditorUiLogic.categoryBadge("boss").equals("BOSS");
        assert MobEditorUiLogic.pageCount(0, 10) == 1;
        assert MobEditorUiLogic.clampPage(9, 11, 5) == 2;
        assert MobEditorUiLogic.formatNumber(1.7500, 4).equals("1.75");
        assert MobEditorUiLogic.formatNumber(0, 4).equals("0");
        assert MobEditorUiLogic.formatPercent(.05).equals("5%");
    }

    private static void viewState() {
        MobEditorViewState initial = MobEditorViewState.initial();
        assert initial.tab() == MobEditorViewState.Tab.BASIC;
        assert java.util.Arrays.asList(MobEditorViewState.Tab.values()).equals(List.of(
                MobEditorViewState.Tab.BASIC, MobEditorViewState.Tab.STATS,
                MobEditorViewState.Tab.AI, MobEditorViewState.Tab.ABILITIES,
                MobEditorViewState.Tab.APPEARANCE, MobEditorViewState.Tab.TEST));
        assert initial.select(null).selectedMobId().isEmpty();
        assert initial.select("mob").edit().dirty();
        assert initial.select("mob").saved(4).revision() == 4;
        assert initial.changeTab(MobEditorViewState.Tab.APPEARANCE).tab()
                == MobEditorViewState.Tab.APPEARANCE;
        assert initial.changeTab(MobEditorViewState.Tab.ABILITIES).tab()
                == MobEditorViewState.Tab.ABILITIES;
        assert initial.canApply(true);
        assert !initial.edit().canApply(true);
        assert initial.canTestSpawn(true, true);
        assert !initial.communicating(true).canTestSpawn(true, true);
    }

    private static void layoutContract() {
        MobEditorLayout layout = MobEditorLayout.of(1280, 720);
        assert layout.header().contains(layout.header().x(), layout.header().y());
        assert layout.mobList().width() > 0;
        assert layout.tabs().width() > 0;
        assert layout.propertyScrollMaximum(1000) >= 0;
        assert layout.clampPropertyScroll(-10, 1000) == 0;
        assert MobEditorMetrics.fieldBaseline(10) > 10;
        assert MobEditorMetrics.toolbarRows(140, 7) == 2;
        for (int width : List.of(1, 2, 8, 64, 640)) {
            for (int height : List.of(1, 2, 8, 64, 360)) {
                MobEditorLayout boundedLayout = MobEditorLayout.of(width, height);
                assertBounded(boundedLayout.header(), width, height);
                assertBounded(boundedLayout.mobList(), width, height);
                assertBounded(boundedLayout.tabs(), width, height);
                assertBounded(boundedLayout.property(), width, height);
                assertBounded(boundedLayout.preview(), width, height);
                assertBounded(boundedLayout.actionBar(), width, height);
            }
        }
    }

    private static void expectImmutable(List<Integer> values) {
        try {
            values.add(4);
            throw new AssertionError("Expected immutable picker page");
        } catch (UnsupportedOperationException expected) {
            // Expected snapshot contract.
        }
    }

    private static void assertBounded(MobEditorLayout.Bounds bounds, int width, int height) {
        assert bounds.x() >= 0 && bounds.y() >= 0;
        assert bounds.right() <= width && bounds.bottom() <= height;
        assert bounds.width() >= 0 && bounds.height() >= 0;
    }
}
