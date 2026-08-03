package io.github.gyai.projects.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import io.github.gyai.projects.client.ui.screen.ProjectSThemeScreen;
import io.github.gyai.projects.client.ui.screen.ProjectSUiKitScreen;

public final class ProjectSMenuScreen extends Screen {
    private static final int PANEL_WIDTH = 248;
    private static final int PANEL_HEIGHT = 296;
    private static final int ACCENT = 0xFF48C9E8;

    private final Screen parent;
    private int knownBalanceRevision;

    public ProjectSMenuScreen(Screen parent) {
        super(Component.literal("ProjectS"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        knownBalanceRevision = BalanceClientState.localRevision();
        if (!BalanceClientState.received()
                && BalanceClientState.supportedByConnection()) {
            BalanceClientState.probe();
        }
        boolean compact = height < PANEL_HEIGHT + 8;
        int panelHeight = compact ? Math.min(172, height - 8) : PANEL_HEIGHT;
        int panelX = (width - PANEL_WIDTH) / 2;
        int panelY = (height - panelHeight) / 2;
        int buttonWidth = compact ? 96 : PANEL_WIDTH - 48;
        int[] actionX = new int[7];
        int[] actionY = new int[7];
        for (int index = 0; index < actionX.length; index++) {
            actionX[index] = compact
                    ? panelX + 24 + index % 2 * (buttonWidth + 8)
                    : panelX + 24;
            actionY[index] = compact
                    ? panelY + 52 + index / 2 * 24
                    : panelY + 58 + index * 28;
        }

        addRenderableWidget(Button.builder(
                        Component.literal("スキル一覧"),
                        button -> minecraft.setScreen(new SkillListScreen(this))
                )
                .bounds(actionX[0], actionY[0], buttonWidth, compact ? 20 : 22)
                .tooltip(Tooltip.create(Component.literal(
                        "クラス別のスキル説明と詳細ツールチップを表示します")))
                .build());

        Button loadoutButton = addRenderableWidget(Button.builder(
                        Component.literal("スキル装備"),
                        button -> WarriorLoadoutClientState.requestOpen(this))
                .bounds(actionX[1], actionY[1], buttonWidth, compact ? 20 : 22)
                .tooltip(Tooltip.create(Component.literal(
                        "戦闘外でウォーリアーのQ・E・R・Fを変更します")))
                .build());
        loadoutButton.active = WarriorLoadoutClientState.supported();

        Button devMenuButton = addRenderableWidget(Button.builder(
                        Component.literal("Dev Menu"),
                        button -> openDevMenu()
                )
                .bounds(actionX[6], actionY[6], buttonWidth, compact ? 20 : 22)
                .tooltip(Tooltip.create(Component.literal(
                        "サーバーの開発メニューを開きます（projects.dev 権限が必要）")))
                .build());
        devMenuButton.active = ClientPlayNetworking.canSend(SkillInputPayload.TYPE);

        Button balanceButton = addRenderableWidget(Button.builder(
                        Component.literal("バランス調整"),
                        button -> BalanceClientState.requestOpen(this))
                .bounds(actionX[2], actionY[2], buttonWidth, compact ? 20 : 22)
                .tooltip(Tooltip.create(Component.literal(
                        BalanceClientState.state().permitted()
                                ? "武器・スキルのグローバル基礎値を調整します"
                                : "projects.dev 権限と対応サーバーが必要です")))
                .build());
        balanceButton.active = BalanceClientState.supportedByConnection()
                && BalanceClientState.state().supported()
                && BalanceClientState.state().permitted();

        Button mobEditorButton = addRenderableWidget(Button.builder(
                        Component.literal("Mob Editor"),
                        button -> MobEditorClientState.requestOpen(this))
                .bounds(actionX[3], actionY[3], buttonWidth, compact ? 20 : 22)
                .tooltip(Tooltip.create(Component.literal(
                        "Mob定義・外見・実描画プレビューを編集します")))
                .build());
        mobEditorButton.active = MobEditorClientState.supported();

        addRenderableWidget(Button.builder(
                        Component.literal("テーマ"),
                        button -> minecraft.setScreen(new ProjectSThemeScreen(this)))
                .bounds(actionX[4], actionY[4], buttonWidth, compact ? 20 : 22)
                .tooltip(Tooltip.create(Component.literal(
                        "ProjectS UIテーマの確認と切り替えを行います")))
                .build());

        Button uiKitButton = addRenderableWidget(Button.builder(
                        Component.literal("UI Kit"),
                        button -> minecraft.setScreen(new ProjectSUiKitScreen(this)))
                .bounds(actionX[5], actionY[5], buttonWidth, compact ? 20 : 22)
                .tooltip(Tooltip.create(Component.literal(
                        BalanceClientState.state().permitted()
                                ? "共通UI部品の開発確認画面を開きます"
                                : "projects.dev 権限が必要です")))
                .build());
        uiKitButton.active = BalanceClientState.state().permitted();

        addRenderableWidget(Button.builder(
                        Component.literal("戻る"),
                        button -> onClose()
                )
                .bounds(panelX + (PANEL_WIDTH - 84) / 2,
                        panelY + panelHeight - 26, 84, 20)
                .build());
    }

    @Override
    public void tick() {
        super.tick();
        if (knownBalanceRevision != BalanceClientState.localRevision()) {
            rebuildWidgets();
        }
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float tickProgress
    ) {
        int panelHeight = height < PANEL_HEIGHT + 8
                ? Math.min(172, height - 8) : PANEL_HEIGHT;
        int panelX = (width - PANEL_WIDTH) / 2;
        int panelY = (height - panelHeight) / 2;

        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + panelHeight, 0xE90B1017);
        graphics.outline(panelX, panelY, PANEL_WIDTH, panelHeight, 0xCC344351);
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + 2, ACCENT);
        graphics.centeredText(font, title, width / 2, panelY + 16, 0xFFF3F7FA);
        graphics.centeredText(
                font,
                Component.literal("クラス情報と開発機能"),
                width / 2,
                panelY + 34,
                0xFF8FA2B3
        );

        super.extractRenderState(graphics, mouseX, mouseY, tickProgress);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void openDevMenu() {
        if (ProjectSClient.sendInput("OPEN_DEV_MENU")) {
            minecraft.setScreen(null);
        }
    }
}
