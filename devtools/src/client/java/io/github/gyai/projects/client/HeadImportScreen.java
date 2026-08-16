package io.github.gyai.projects.client;

import io.github.gyai.projects.client.ui.icon.ProjectSIcon;
import io.github.gyai.projects.client.ui.render.ProjectSIconRenderer;
import io.github.gyai.projects.client.ui.render.ProjectSUiDraw;
import io.github.gyai.projects.client.ui.screen.ProjectSThemedScreen;
import io.github.gyai.projects.client.ui.theme.ProjectSThemeManager;
import io.github.gyai.projects.client.ui.widget.ProjectSButton;
import io.github.gyai.projects.client.ui.widget.ProjectSTextField;
import io.github.gyai.projects.client.ui.widget.ProjectSToggle;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import io.github.gyai.projects.client.ui.mobeditor.MobEditorInputLogic;

/** Obsidian form for registering a Texture Value custom head. */
public final class HeadImportScreen extends ProjectSThemedScreen {
    private final Screen parent;
    private EditBox id;
    private EditBox name;
    private EditBox tags;
    private EditBox note;
    private EditBox texture;
    private boolean favorite;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private final List<AbstractWidget> formWidgets = new ArrayList<>();
    private int formScroll;
    private int formMaxScroll;
    private int formTop;
    private int formBottom;
    private String status = "";
    private boolean submitted;
    private int submitRevision;

    public HeadImportScreen(Screen parent) {
        super(Component.literal("Texture Valueヘッド登録"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        String oldId = id == null ? "" : id.getValue();
        String oldName = name == null ? "" : name.getValue();
        String oldTags = tags == null ? "" : tags.getValue();
        String oldNote = note == null ? "" : note.getValue();
        String oldTexture = texture == null ? "" : texture.getValue();
        panelWidth = Math.min(560, Math.max(1, width - 24));
        panelHeight = Math.min(330, Math.max(1, height - 24));
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        formWidgets.clear();
        int left = panelX + 24;
        int innerWidth = panelWidth - 48;
        int twoColumn = Math.max(100, (innerWidth - 12) / 2);
        formTop = panelY + 58;
        formBottom = panelY + panelHeight - 58;
        int y = panelY + 68 - formScroll;
        int childStart = children().size();
        id = field(left, y, twoColumn, "内部Head ID", "projects:head_id", 64);
        name = field(left + twoColumn + 12, y, twoColumn,
                "表示名", "ゲーム内表示名", 128);
        tags = field(left, y + 48, twoColumn, "タグ（,区切り）", "boss, undead", 256);
        note = field(left + twoColumn + 12, y + 48, twoColumn,
                "出典メモ", "取得元・作者", 256);
        texture = field(left, y + 104, innerWidth, "Texture Value",
                "Base64 texture value", 16_384);
        if (!oldId.isBlank()) id.setValue(oldId);
        if (!oldName.isBlank()) name.setValue(oldName);
        if (!oldTags.isBlank()) tags.setValue(oldTags);
        if (!oldNote.isBlank()) note.setValue(oldNote);
        if (!oldTexture.isBlank()) texture.setValue(oldTexture);
        addRenderableWidget(new ProjectSToggle(left, y + 150, 170,
                Component.literal("お気に入り"),
                Component.literal("Head Gridの上位に表示"), favorite,
                value -> favorite = value));
        for (int index = childStart; index < children().size(); index++) {
            if (children().get(index) instanceof AbstractWidget widget) {
                formWidgets.add(widget);
            }
        }
        updateFormViewport();
        addRenderableWidget(new ProjectSButton(
                panelX + panelWidth - 220, panelY + panelHeight - 48,
                92, 28, Component.literal("戻る"), ProjectSButton.Kind.GHOST,
                ProjectSIcon.BACK, this::onClose));
        ProjectSButton submit = new ProjectSButton(
                panelX + panelWidth - 116, panelY + panelHeight - 48,
                92, 28, Component.literal("登録"), ProjectSButton.Kind.PRIMARY,
                ProjectSIcon.UPLOAD, this::save);
        submit.active = !submitted;
        addRenderableWidget(submit);
    }

    private EditBox field(int x, int y, int width, String label,
                          String placeholder, int maximumLength) {
        EditBox box = addRenderableWidget(new ProjectSTextField(
                font, x, y, width, 26, Component.literal(label),
                Component.literal(placeholder)));
        box.setMaxLength(maximumLength);
        return box;
    }

    private void save() {
        if (submitted || MobEditorClientState.communicating()) {
            status = "通信中です。現在の登録処理が完了するまでお待ちください";
            return;
        }
        if (id.getValue().trim().isBlank() || name.getValue().trim().isBlank()
                || texture.getValue().trim().isBlank()) {
            status = "ID・表示名・Texture Valueは必須です";
            return;
        }
        String idValue = id.getValue().trim();
        String nameValue = name.getValue().trim();
        String textureValue = texture.getValue().trim();
        List<String> tagValues = Arrays.stream(tags.getValue().split(","))
                .map(String::trim).filter(value -> !value.isBlank()).toList();
        if (!MobEditorInputLogic.validHeadImport(idValue, nameValue, textureValue,
                note.getValue().trim(), tagValues)) {
            status = "入力がUTF-8バイト上限または件数上限を超えています";
            return;
        }
        MobEditorData.Head head = new MobEditorData.Head(
                1, 0, idValue, nameValue,
                MobEditorData.HeadSource.TEXTURE_VALUE,
                "", textureValue, "", tagValues, favorite, note.getValue().trim());
        int before = MobEditorClientState.localRevision();
        MobEditorClientState.createHead(head);
        if (MobEditorClientState.communicating()) {
            submitted = true;
            submitRevision = before;
            status = "登録中...";
        } else {
            status = MobEditorClientState.state().message();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!submitted || MobEditorClientState.communicating()
                || MobEditorClientState.localRevision() == submitRevision) return;
        submitted = false;
        if (MobEditorClientState.state().success()
                && !MobEditorClientState.state().revisionConflict()) {
            minecraft.setScreen(parent);
        } else {
            status = MobEditorClientState.state().message();
        }
    }

    private void updateFormViewport() {
        int contentBottom = formTop;
        for (AbstractWidget widget : formWidgets) {
            contentBottom = Math.max(contentBottom, widget.getBottom() + formScroll);
        }
        formMaxScroll = Math.max(0, contentBottom - formBottom + 4);
        int clamped = Math.clamp(formScroll, 0, formMaxScroll);
        int correction = clamped - formScroll;
        formScroll = clamped;
        for (AbstractWidget widget : formWidgets) {
            if (correction != 0) widget.setY(widget.getY() - correction);
            widget.visible = widget.getY() >= formTop + 12
                    && widget.getBottom() <= formBottom;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY,
                                 double horizontal, double vertical) {
        if (super.mouseScrolled(mouseX, mouseY, horizontal, vertical)) return true;
        if (mouseX < panelX || mouseX >= panelX + panelWidth
                || mouseY < formTop || mouseY >= formBottom) return false;
        int next = Math.clamp(formScroll + (int) Math.round(-vertical * 28),
                0, formMaxScroll);
        int delta = next - formScroll;
        formScroll = next;
        for (AbstractWidget widget : formWidgets) widget.setY(widget.getY() - delta);
        updateFormViewport();
        return true;
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float tickProgress
    ) {
        var theme = ProjectSThemeManager.get().activeTheme();
        var tokens = theme.tokens();
        graphics.fill(0, 0, width, height, tokens.background());
        ProjectSUiDraw.cutPanel(graphics, panelX, panelY, panelWidth, panelHeight,
                theme.metrics().modalCornerCut(), tokens.surfaceRaised(), tokens.borderStrong());
        ProjectSIconRenderer.draw(graphics, ProjectSIcon.HEAD,
                panelX + 24, panelY + 20, 22, tokens, false, true);
        graphics.text(font, "Custom Head Import", panelX + 56, panelY + 18,
                tokens.textPrimary(), false);
        graphics.text(font, "Texture ValueをProjectS Head Libraryへ登録します",
                panelX + 56, panelY + 34, tokens.textMuted(), false);
        if (!status.isBlank()) {
            graphics.text(font, font.plainSubstrByWidth(status, Math.max(1, panelWidth - 40)),
                    panelX + 20, panelY + panelHeight - 70,
                    submitted ? tokens.textMuted() : tokens.danger(), false);
        }
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
}
