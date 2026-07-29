package io.github.gyai.projects.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class WarriorLoadoutScreen extends Screen {
    private static final int MAXIMUM_PANEL_WIDTH = 346;
    private static final int PANEL_HEIGHT = 226;
    private static final int ACCENT = 0xFFE0A33A;
    private static final List<List<String>> CANDIDATES = List.of(
            List.of("spin_slash", "sweeping_slash"),
            List.of("warrior_charge", "execution_leap", "earth_shatter"),
            List.of("indomitable_spirit", "battlefield_aura", "endure"),
            List.of("fighting_spirit_release", "blood_battle",
                    "end_war_strike"));
    private static final String[] SLOT_KEYS = {"Q", "E", "R", "F"};

    private final Screen parent;
    private int selectedSlot;
    private int knownRevision = -1;

    public WarriorLoadoutScreen(Screen parent) {
        super(Component.literal("ウォーリアー スキル装備"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        knownRevision = WarriorLoadoutClientState.revision();
        int panelWidth = panelWidth();
        int panelX = (width - panelWidth) / 2;
        int panelY = (height - PANEL_HEIGHT) / 2;
        var state = WarriorLoadoutClientState.state();

        for (int slot = 0; slot < SLOT_KEYS.length; slot++) {
            int currentSlot = slot;
            SkillCatalog.Skill equipped =
                    SkillCatalog.findById(state.skill(slot));
            String label = SLOT_KEYS[slot] + "  "
                    + (equipped == null ? "未設定" : equipped.name());
            Button button = addRenderableWidget(Button.builder(
                            Component.literal(label),
                            clicked -> {
                                selectedSlot = currentSlot;
                                rebuild();
                            })
                    .bounds(panelX + 16, panelY + 48 + slot * 30,
                            116, 23)
                    .build());
            button.active = state.available();
        }

        List<String> candidates = CANDIDATES.get(selectedSlot);
        for (int index = 0; index < candidates.size(); index++) {
            String skillId = candidates.get(index);
            SkillCatalog.Skill skill = SkillCatalog.findById(skillId);
            boolean equipped = skillId.equals(state.skill(selectedSlot));
            String label = (equipped ? "✓ " : "") + (skill == null
                    ? skillId : skill.name());
            Button button = addRenderableWidget(Button.builder(
                            Component.literal(label),
                            clicked -> WarriorLoadoutClientState.select(
                                    selectedSlot, skillId))
                    .bounds(panelX + 148, panelY + 52 + index * 34,
                            panelWidth - 164, 24)
                    .tooltip(skill == null ? null : Tooltip.create(
                            Component.literal(
                                    skill.stats() + "\n"
                                            + skill.description())))
                    .build());
            button.active = state.available() && !equipped;
        }

        Button reset = addRenderableWidget(Button.builder(
                        Component.literal("初期装備に戻す"),
                        clicked -> WarriorLoadoutClientState.reset())
                .bounds(panelX + 16, panelY + 174, 116, 20)
                .tooltip(Tooltip.create(Component.literal(
                        "Q・E・R・Fを初期ロードアウトへ戻します")))
                .build());
        reset.active = state.available();

        addRenderableWidget(Button.builder(
                        Component.literal("戻る"),
                        clicked -> onClose())
                .bounds(panelX + panelWidth - 80, panelY + 174, 64, 20)
                .build());
    }

    @Override
    public void tick() {
        super.tick();
        if (ProjectSSkillHud.state().inCombat()
                || (ProjectSSkillHud.state().visible()
                && !"warrior".equals(
                ProjectSSkillHud.state().classId()))) {
            minecraft.setScreen(parent);
            return;
        }
        if (knownRevision != WarriorLoadoutClientState.revision()) {
            rebuild();
        }
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float tickProgress
    ) {
        int panelWidth = panelWidth();
        int panelX = (width - panelWidth) / 2;
        int panelY = (height - PANEL_HEIGHT) / 2;
        graphics.fill(
                panelX, panelY,
                panelX + panelWidth, panelY + PANEL_HEIGHT,
                0xF00B1017);
        graphics.outline(
                panelX, panelY, panelWidth, PANEL_HEIGHT,
                0xCC4E5866);
        graphics.fill(
                panelX, panelY,
                panelX + panelWidth, panelY + 2,
                ACCENT);
        graphics.centeredText(
                font, title, width / 2, panelY + 14,
                0xFFF3F7FA);
        graphics.text(
                font, "装備スロット",
                panelX + 16, panelY + 34,
                0xFF9BA9B7, false);
        graphics.text(
                font, SLOT_KEYS[selectedSlot] + " の候補",
                panelX + 148, panelY + 34,
                0xFFE8B95C, false);

        var state = WarriorLoadoutClientState.state();
        SkillCatalog.Skill equipped =
                SkillCatalog.findById(state.skill(selectedSlot));
        if (equipped != null) {
            int detailWidth = Math.max(60, panelWidth - 164);
            graphics.text(
                    font,
                    font.plainSubstrByWidth(
                            equipped.stats(), detailWidth),
                    panelX + 148, panelY + 151,
                    0xFF9BA9B7, false);
            graphics.text(
                    font,
                    font.plainSubstrByWidth(
                            equipped.description(), detailWidth),
                    panelX + 148, panelY + 161,
                    0xFFE3D4AD, false);
        }
        if (state.inCombat()) {
            graphics.centeredText(
                    font,
                    Component.literal("戦闘中は変更できません"),
                    width / 2, panelY + 196,
                    0xFFFF6B6B);
        } else if (!state.reason().isBlank()) {
            graphics.centeredText(
                    font,
                    Component.literal(state.reason()),
                    width / 2, panelY + 196,
                    state.success() ? 0xFF69D6A5 : 0xFFFF6B6B);
        } else {
            graphics.centeredText(
                    font,
                    Component.literal(
                            "ロードアウトはサーバー側で検証・確定されます"),
                    width / 2, panelY + 196,
                    0xFF8796A5);
        }
        super.extractRenderState(
                graphics, mouseX, mouseY, tickProgress);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void rebuild() {
        clearWidgets();
        init();
    }

    private int panelWidth() {
        return Math.min(
                MAXIMUM_PANEL_WIDTH, Math.max(240, width - 16));
    }
}
