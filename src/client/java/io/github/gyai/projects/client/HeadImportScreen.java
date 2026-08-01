package io.github.gyai.projects.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Arrays;

public final class HeadImportScreen extends Screen {
    private final Screen parent;
    private EditBox id;
    private EditBox name;
    private EditBox tags;
    private EditBox note;
    private EditBox texture;
    private boolean favorite;
    private Button favoriteButton;

    public HeadImportScreen(Screen parent) {
        super(Component.literal("Texture Valueヘッド登録"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int x = (width - 520) / 2;
        int y = (height - 280) / 2;
        id = field(x + 130, y + 46, 360, 64);
        name = field(x + 130, y + 76, 360, 128);
        tags = field(x + 130, y + 106, 360, 256);
        note = field(x + 130, y + 136, 360, 256);
        texture = field(x + 24, y + 184, 472, 16_384);
        favoriteButton = addRenderableWidget(Button.builder(
                Component.literal("お気に入り:OFF"), button -> {
                    favorite = !favorite;
                    favoriteButton.setMessage(Component.literal(
                            "お気に入り:" + (favorite ? "ON" : "OFF")));
                }).bounds(x + 24, y + 238, 118, 22).build());
        addRenderableWidget(Button.builder(
                Component.literal("登録"), button -> save())
                .bounds(x + 160, y + 238, 90, 22).build());
        addRenderableWidget(Button.builder(
                Component.literal("戻る"), button -> onClose())
                .bounds(x + 270, y + 238, 90, 22).build());
    }

    private EditBox field(int x, int y, int width, int maximumLength) {
        EditBox box = addRenderableWidget(new EditBox(
                font, x, y, width, 20, Component.empty()));
        box.setMaxLength(maximumLength);
        return box;
    }

    private void save() {
        MobEditorData.Head head = new MobEditorData.Head(
                1, 0, id.getValue().trim(), name.getValue().trim(),
                MobEditorData.HeadSource.TEXTURE_VALUE,
                "", texture.getValue().trim(), "",
                Arrays.stream(tags.getValue().split(","))
                        .map(String::trim).filter(value -> !value.isBlank()).toList(),
                favorite, note.getValue().trim());
        MobEditorClientState.createHead(head);
        minecraft.setScreen(parent);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float tickProgress
    ) {
        int x = (width - 520) / 2;
        int y = (height - 280) / 2;
        graphics.fill(x, y, x + 520, y + 280, 0xF20B1017);
        graphics.outline(x, y, 520, 280, 0xCC4E5866);
        graphics.centeredText(font, title, width / 2, y + 16, 0xFFF3F7FA);
        label(graphics, "内部Head ID", x + 24, y + 52);
        label(graphics, "表示名", x + 24, y + 82);
        label(graphics, "タグ(,区切り)", x + 24, y + 112);
        label(graphics, "出典メモ", x + 24, y + 142);
        label(graphics, "Texture Value", x + 24, y + 170);
        super.extractRenderState(graphics, mouseX, mouseY, tickProgress);
    }

    private void label(GuiGraphicsExtractor graphics, String text, int x, int y) {
        graphics.text(font, text, x, y, 0xFFB8C4CE, false);
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
