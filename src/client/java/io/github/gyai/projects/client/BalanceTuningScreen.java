package io.github.gyai.projects.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class BalanceTuningScreen extends Screen {
    private static final int MAX_PANEL_WIDTH = 560;
    private static final int MAX_PANEL_HEIGHT = 340;
    private static final int ACCENT = 0xFFE0A33A;

    private final Screen parent;
    private boolean skillsTab;
    private int selectedWeapon;
    private int selectedSkill;
    private int knownLocalRevision;
    private EditBox firstValue;
    private EditBox secondValue;
    private String localError = "";

    public BalanceTuningScreen(Screen parent) {
        super(Component.literal("バランス調整"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        knownLocalRevision = BalanceClientState.localRevision();
        var state = BalanceClientState.state();
        selectedWeapon = Math.clamp(
                selectedWeapon, 0, Math.max(0, state.weapons().size() - 1));
        selectedSkill = Math.clamp(
                selectedSkill, 0, Math.max(0, state.skills().size() - 1));
        int x = panelX();
        int y = panelY();
        int panelWidth = panelWidth();
        int leftWidth = Math.clamp(panelWidth / 3, 120, 180);
        int rightX = x + leftWidth + 18;
        int rightWidth = panelWidth - leftWidth - 30;

        Button weapons = addRenderableWidget(Button.builder(
                        Component.literal("武器"),
                        button -> {
                            skillsTab = false;
                            rebuild();
                        })
                .bounds(x + 12, y + 34, (leftWidth - 16) / 2, 20)
                .build());
        weapons.active = skillsTab;
        Button skills = addRenderableWidget(Button.builder(
                        Component.literal("スキル"),
                        button -> {
                            skillsTab = true;
                            rebuild();
                        })
                .bounds(x + 16 + (leftWidth - 16) / 2, y + 34,
                        (leftWidth - 16) / 2, 20)
                .build());
        skills.active = !skillsTab;

        if (skillsTab) {
            for (int index = 0; index < state.skills().size(); index++) {
                int valueIndex = index;
                var value = state.skills().get(index);
                Button button = addRenderableWidget(Button.builder(
                                Component.literal(value.displayName()),
                                clicked -> {
                                    selectedSkill = valueIndex;
                                    rebuild();
                                })
                        .bounds(x + 12, y + 60 + index * 24,
                                leftWidth - 12, 20)
                        .tooltip(Tooltip.create(Component.literal(value.id())))
                        .build());
                button.active = index != selectedSkill;
            }
        } else {
            for (int index = 0; index < state.weapons().size(); index++) {
                int valueIndex = index;
                var value = state.weapons().get(index);
                Button button = addRenderableWidget(Button.builder(
                                Component.literal(stripColor(value.displayName())),
                                clicked -> {
                                    selectedWeapon = valueIndex;
                                    rebuild();
                                })
                        .bounds(x + 12, y + 60 + index * 24,
                                leftWidth - 12, 20)
                        .tooltip(Tooltip.create(Component.literal(value.id())))
                        .build());
                button.active = index != selectedWeapon;
            }
        }

        double currentFirst = 0;
        double currentSecondPercent = 0;
        if (skillsTab && !state.skills().isEmpty()) {
            var value = state.skills().get(selectedSkill);
            currentFirst = value.currentBaseDamage();
            currentSecondPercent = value.currentScaling() * 100.0;
        } else if (!skillsTab && !state.weapons().isEmpty()) {
            var value = state.weapons().get(selectedWeapon);
            currentFirst = value.currentAttackPower();
            currentSecondPercent = value.currentAttackSpeed() * 100.0;
        }
        int fieldWidth = Math.max(90, rightWidth - 16);
        firstValue = addRenderableWidget(new EditBox(
                font, rightX, y + 82, fieldWidth, 20,
                Component.literal("基礎値")));
        firstValue.setMaxLength(24);
        firstValue.setValue(format(currentFirst));
        secondValue = addRenderableWidget(new EditBox(
                font, rightX, y + 151, fieldWidth, 20,
                Component.literal("百分率")));
        secondValue.setMaxLength(24);
        secondValue.setValue(format(currentSecondPercent));

        addStepButtons(rightX, y + 105, fieldWidth,
                new double[]{-10, -1, 1, 10}, firstValue);
        addStepButtons(rightX, y + 174, fieldWidth,
                skillsTab
                        ? new double[]{-10, -5, 5, 10}
                        : new double[]{-10, -1, 1, 10},
                secondValue);

        int actionY = y + panelHeight() - 48;
        int gap = 4;
        int usable = panelWidth - 24;
        int actionWidth = Math.max(52, (usable - gap * 5) / 6);
        addActionButton(x + 12, actionY, actionWidth,
                "適用", button -> apply());
        addActionButton(x + 12 + (actionWidth + gap), actionY, actionWidth,
                "保存", button -> action(BalanceActionPayload.SAVE));
        addActionButton(x + 12 + (actionWidth + gap) * 2, actionY, actionWidth,
                "選択リセット", button -> resetSelected());
        addActionButton(x + 12 + (actionWidth + gap) * 3, actionY, actionWidth,
                "再読込", button -> action(BalanceActionPayload.RELOAD));
        addActionButton(x + 12 + (actionWidth + gap) * 4, actionY, actionWidth,
                "全リセット", button -> confirmResetAll());
        addRenderableWidget(Button.builder(
                        Component.literal("戻る"), button -> onClose())
                .bounds(x + 12 + (actionWidth + gap) * 5,
                        actionY, actionWidth, 20)
                .build());
    }

    private void addStepButtons(
            int x, int y, int width,
            double[] steps, EditBox field
    ) {
        int gap = 3;
        int buttonWidth = (width - gap * 3) / 4;
        for (int index = 0; index < steps.length; index++) {
            double step = steps[index];
            addRenderableWidget(Button.builder(
                            Component.literal(step > 0
                                    ? "+" + format(step) : format(step)),
                            button -> adjust(field, step))
                    .bounds(x + index * (buttonWidth + gap),
                            y, buttonWidth, 18)
                    .build());
        }
    }

    private void addActionButton(
            int x, int y, int width,
            String label,
            Button.OnPress action
    ) {
        Button button = addRenderableWidget(Button.builder(
                        Component.literal(label), action)
                .bounds(x, y, width, 20)
                .build());
        button.active = BalanceClientState.canEdit();
    }

    @Override
    public void tick() {
        super.tick();
        if (!BalanceClientState.supportedByConnection()) {
            minecraft.setScreen(parent);
            return;
        }
        if (knownLocalRevision != BalanceClientState.localRevision()) {
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
        int x = panelX();
        int y = panelY();
        int panelWidth = panelWidth();
        int panelHeight = panelHeight();
        int leftWidth = Math.clamp(panelWidth / 3, 120, 180);
        int rightX = x + leftWidth + 18;
        var state = BalanceClientState.state();

        graphics.fill(x, y, x + panelWidth, y + panelHeight, 0xF20B1017);
        graphics.outline(x, y, panelWidth, panelHeight, 0xCC4E5866);
        graphics.fill(x, y, x + panelWidth, y + 2, ACCENT);
        graphics.text(font, title, x + 12, y + 14, 0xFFF3F7FA, false);
        String status = BalanceClientState.communicating()
                ? "通信中"
                : state.dirty() ? "未保存の変更あり" : "保存済み";
        int statusColor = BalanceClientState.communicating()
                ? 0xFFFFD166 : state.dirty() ? 0xFFFFA94D : 0xFF69D6A5;
        graphics.text(font, status, x + panelWidth - 12 - font.width(status),
                y + 14, statusColor, false);

        if (skillsTab && !state.skills().isEmpty()) {
            var value = state.skills().get(selectedSkill);
            graphics.text(font, value.displayName() + "  [" + value.id() + "]",
                    rightX, y + 47, 0xFFE8B95C, false);
            graphics.text(font, "基礎ダメージ  初期 "
                            + format(value.defaultBaseDamage())
                            + " / 現在 " + format(value.currentBaseDamage()),
                    rightX, y + 68, 0xFFB8C4CE, false);
            graphics.text(font, "攻撃力反映率(%)  初期 "
                            + format(value.defaultScaling() * 100)
                            + "% / 現在 " + format(value.currentScaling() * 100) + "%",
                    rightX, y + 137, 0xFFB8C4CE, false);
            graphics.text(font, "計算式: " + format(value.currentBaseDamage())
                            + " ＋ 攻撃力 × " + format(value.currentScaling()),
                    rightX, y + 205, 0xFFE4D29C, false);
        } else if (!skillsTab && !state.weapons().isEmpty()) {
            var value = state.weapons().get(selectedWeapon);
            graphics.text(font, stripColor(value.displayName())
                            + "  [" + value.id() + "]",
                    rightX, y + 47, 0xFFE8B95C, false);
            graphics.text(font, "基礎攻撃力  初期 "
                            + format(value.defaultAttackPower())
                            + " / 現在 " + format(value.currentAttackPower()),
                    rightX, y + 68, 0xFFB8C4CE, false);
            graphics.text(font, "基礎攻撃速度(%)  初期 "
                            + format(value.defaultAttackSpeed() * 100)
                            + "% / 現在 " + format(value.currentAttackSpeed() * 100) + "%",
                    rightX, y + 137, 0xFFB8C4CE, false);
            graphics.text(font,
                    "個体補正・強化値はこの変更と別に保持されます",
                    rightX, y + 205, 0xFF8FA2B3, false);
        }
        String message = localError.isBlank() ? state.message() : localError;
        if (!message.isBlank()) {
            graphics.text(
                    font,
                    font.plainSubstrByWidth(message, panelWidth - 24),
                    x + 12, y + panelHeight - 65,
                    localError.isBlank() && state.success()
                            ? 0xFF69D6A5 : 0xFFFF6B6B,
                    false);
        }
        super.extractRenderState(graphics, mouseX, mouseY, tickProgress);
    }

    private void apply() {
        Double first = parse(firstValue);
        Double secondPercent = parse(secondValue);
        if (first == null || secondPercent == null) return;
        List<BalanceUpdatePayload.Edit> edits = new ArrayList<>(2);
        if (skillsTab) {
            var value = BalanceClientState.state().skills().get(selectedSkill);
            edits.add(new BalanceUpdatePayload.Edit(1, value.id(), 0, first));
            edits.add(new BalanceUpdatePayload.Edit(
                    1, value.id(), 1, secondPercent / 100.0));
        } else {
            var value = BalanceClientState.state().weapons().get(selectedWeapon);
            edits.add(new BalanceUpdatePayload.Edit(0, value.id(), 0, first));
            edits.add(new BalanceUpdatePayload.Edit(
                    0, value.id(), 1, secondPercent / 100.0));
        }
        localError = "";
        BalanceClientState.apply(edits);
    }

    private void resetSelected() {
        String id;
        int target;
        if (skillsTab) {
            id = BalanceClientState.state().skills().get(selectedSkill).id();
            target = 1;
        } else {
            id = BalanceClientState.state().weapons().get(selectedWeapon).id();
            target = 0;
        }
        BalanceClientState.action(
                BalanceActionPayload.RESET_SELECTED, target, id);
    }

    private void action(int action) {
        BalanceClientState.action(action, 0, "");
    }

    private void confirmResetAll() {
        minecraft.setScreen(new ConfirmScreen(
                confirmed -> {
                    minecraft.setScreen(this);
                    if (confirmed) action(BalanceActionPayload.RESET_ALL);
                },
                Component.literal("全項目を初期値へ戻しますか？"),
                Component.literal("保存するまでは未保存の変更として扱われます")));
    }

    private void adjust(EditBox field, double amount) {
        Double value = parse(field);
        if (value != null) field.setValue(format(value + amount));
    }

    private Double parse(EditBox field) {
        try {
            double value = Double.parseDouble(field.getValue().trim());
            if (!Double.isFinite(value)) throw new NumberFormatException();
            localError = "";
            return value;
        } catch (NumberFormatException exception) {
            localError = "有限な数値を入力してください";
            return null;
        }
    }

    private void rebuild() {
        clearWidgets();
        init();
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value)
                .replaceAll("\\.?0+$", "");
    }

    private static String stripColor(String value) {
        return value.replaceAll("§.", "");
    }

    private int panelWidth() {
        return Math.min(MAX_PANEL_WIDTH, Math.max(300, width - 16));
    }

    private int panelHeight() {
        return Math.min(MAX_PANEL_HEIGHT, Math.max(260, height - 16));
    }

    private int panelX() {
        return (width - panelWidth()) / 2;
    }

    private int panelY() {
        return (height - panelHeight()) / 2;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
