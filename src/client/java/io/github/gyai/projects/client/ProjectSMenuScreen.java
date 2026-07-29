package io.github.gyai.projects.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ProjectSMenuScreen extends Screen {
    private static final int PANEL_WIDTH = 248;
    private static final int PANEL_HEIGHT = 212;
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
        int panelX = (width - PANEL_WIDTH) / 2;
        int panelY = (height - PANEL_HEIGHT) / 2;
        int buttonX = panelX + 24;
        int buttonWidth = PANEL_WIDTH - 48;

        addRenderableWidget(Button.builder(
                        Component.literal("スキル一覧"),
                        button -> minecraft.setScreen(new SkillListScreen(this))
                )
                .bounds(buttonX, panelY + 58, buttonWidth, 22)
                .tooltip(Tooltip.create(Component.literal(
                        "クラス別のスキル説明と詳細ツールチップを表示します")))
                .build());

        Button loadoutButton = addRenderableWidget(Button.builder(
                        Component.literal("スキル装備"),
                        button -> WarriorLoadoutClientState.requestOpen(this))
                .bounds(buttonX, panelY + 86, buttonWidth, 22)
                .tooltip(Tooltip.create(Component.literal(
                        "戦闘外でウォーリアーのQ・E・R・Fを変更します")))
                .build());
        loadoutButton.active = WarriorLoadoutClientState.supported();

        Button devMenuButton = addRenderableWidget(Button.builder(
                        Component.literal("Dev Menu"),
                        button -> openDevMenu()
                )
                .bounds(buttonX, panelY + 142, buttonWidth, 22)
                .tooltip(Tooltip.create(Component.literal(
                        "サーバーの開発メニューを開きます（projects.dev 権限が必要）")))
                .build());
        devMenuButton.active = ClientPlayNetworking.canSend(SkillInputPayload.TYPE);

        Button balanceButton = addRenderableWidget(Button.builder(
                        Component.literal("バランス調整"),
                        button -> BalanceClientState.requestOpen(this))
                .bounds(buttonX, panelY + 114, buttonWidth, 22)
                .tooltip(Tooltip.create(Component.literal(
                        BalanceClientState.state().permitted()
                                ? "武器・スキルのグローバル基礎値を調整します"
                                : "projects.dev 権限と対応サーバーが必要です")))
                .build());
        balanceButton.active = BalanceClientState.supportedByConnection()
                && BalanceClientState.state().supported()
                && BalanceClientState.state().permitted();

        addRenderableWidget(Button.builder(
                        Component.literal("戻る"),
                        button -> onClose()
                )
                .bounds(panelX + (PANEL_WIDTH - 84) / 2, panelY + 178, 84, 20)
                .build());
    }

    @Override
    public void tick() {
        super.tick();
        if (knownBalanceRevision != BalanceClientState.localRevision()) {
            clearWidgets();
            init();
        }
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float tickProgress
    ) {
        int panelX = (width - PANEL_WIDTH) / 2;
        int panelY = (height - PANEL_HEIGHT) / 2;

        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xE90B1017);
        graphics.outline(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 0xCC344351);
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
