package io.github.gyai.projects.client.ui.mobeditor;

import io.github.gyai.projects.client.ui.icon.ProjectSIcon;
import io.github.gyai.projects.client.ui.render.ProjectSUiDraw;
import io.github.gyai.projects.client.ui.render.ProjectSTextRenderer;
import io.github.gyai.projects.client.ui.screen.ProjectSThemedScreen;
import io.github.gyai.projects.client.ui.theme.ProjectSThemeManager;
import io.github.gyai.projects.client.ui.widget.ProjectSButton;
import io.github.gyai.projects.client.ui.widget.ProjectSTextField;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

/** HSV/hex picker for leather armor, with live updates and cancel restoration. */
public final class LeatherColorPickerScreen extends ProjectSThemedScreen {
    private static final List<Integer> SWATCHES = List.of(0x111111, 0x444444, 0xFFFFFF,
            0x7A4B2A, 0xD83A35, 0xF58231, 0xF5D442, 0x3BA55D, 0x3F72C6, 0x8E44AD);
    private final Screen parent;
    private final int original;
    private final Consumer<String> live;
    private final Runnable cancelAction;
    private double hue;
    private double saturation;
    private double value;
    private ProjectSTextField hex;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int svX;
    private int svY;
    private int svWidth;
    private int svHeight;
    private int hueX;
    private int hueY;
    private int hueWidth;
    private boolean compact;

    public LeatherColorPickerScreen(Screen parent, String initial, Consumer<String> live,
                                    Runnable cancelAction) {
        super(Component.literal("革防具の色"));
        this.parent = parent;
        int parsed = MobEditorColorLogic.parseHex(initial);
        original = parsed < 0 ? 0xA06540 : parsed;
        var hsv = MobEditorColorLogic.rgbToHsv(original);
        hue = hsv.hue();
        saturation = hsv.saturation();
        value = hsv.value();
        this.live = live == null ? ignored -> { } : live;
        this.cancelAction = cancelAction == null ? () -> { } : cancelAction;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(520, Math.max(1, width - 24));
        panelHeight = Math.min(390, Math.max(1, height - 24));
        compact = panelHeight < 340;
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        svX = panelX + 20;
        svY = panelY + 62;
        svWidth = Math.max(1, panelWidth - 40);
        svHeight = compact ? Math.max(40, panelHeight - 220) : 150;
        hueX = svX;
        hueY = svY + svHeight + 14;
        hueWidth = svWidth;
        hex = addRenderableWidget(new ProjectSTextField(font, panelX + 20,
                hueY + (compact ? 26 : 44), 150, 28,
                Component.literal("HEX"), Component.literal("#RRGGBB")));
        hex.setMaxLength(7);
        hex.setValue(currentHex());
        hex.setResponder(text -> {
            int parsed = MobEditorColorLogic.parseHex(text);
            if (parsed >= 0) {
                var hsv = MobEditorColorLogic.rgbToHsv(parsed);
                hue = hsv.hue();
                saturation = hsv.saturation();
                value = hsv.value();
                publish();
                hex.error(Component.empty());
            } else {
                hex.error(Component.literal("#RRGGBBで入力してください"));
            }
        });
        int bottom = panelY + panelHeight - 42;
        addRenderableWidget(new ProjectSButton(panelX + 20, bottom, 84, 28,
                Component.literal("リセット"), ProjectSButton.Kind.GHOST,
                ProjectSIcon.RESET, () -> select(0xA06540)));
        addRenderableWidget(new ProjectSButton(panelX + panelWidth - 212, bottom, 92, 28,
                Component.literal("キャンセル"), ProjectSButton.Kind.GHOST,
                ProjectSIcon.CLOSE, this::cancel));
        addRenderableWidget(new ProjectSButton(panelX + panelWidth - 108, bottom, 88, 28,
                Component.literal("適用"), ProjectSButton.Kind.PRIMARY,
                ProjectSIcon.SUCCESS, () -> minecraft.setScreen(parent)));
    }

    private void select(int rgb) {
        var hsv = MobEditorColorLogic.rgbToHsv(rgb);
        hue = hsv.hue();
        saturation = hsv.saturation();
        value = hsv.value();
        if (hex != null) hex.setValue(currentHex());
        publish();
    }

    private void updateAt(double mouseX, double mouseY) {
        if (mouseY >= svY && mouseY <= svY + svHeight && mouseX >= svX
                && mouseX <= svX + svWidth) {
            saturation = Math.clamp((mouseX - svX) / svWidth, 0, 1);
            value = 1 - Math.clamp((mouseY - svY) / svHeight, 0, 1);
            if (hex != null) hex.setValue(currentHex());
            publish();
        } else if (mouseY >= hueY && mouseY <= hueY + 18 && mouseX >= hueX
                && mouseX <= hueX + hueWidth) {
            hue = Math.clamp((mouseX - hueX) / hueWidth, 0, 1) * 359.999;
            if (hex != null) hex.setValue(currentHex());
            publish();
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int swatchY = hueY + 90;
            for (int index = 0; !compact && index < SWATCHES.size(); index++) {
                int left = panelX + 20 + index * 34;
                if (event.x() >= left && event.x() < left + 28
                        && event.y() >= swatchY && event.y() < swatchY + 24) {
                    select(SWATCHES.get(index));
                    return true;
                }
            }
            updateAt(event.x(), event.y());
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (event.button() == 0) {
            updateAt(event.x(), event.y());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    private String currentHex() {
        return MobEditorColorLogic.formatHex(
                MobEditorColorLogic.hsvToRgb(hue, saturation, value));
    }

    private void publish() {
        live.accept(currentHex());
    }

    private void cancel() {
        cancelAction.run();
        minecraft.setScreen(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics,
            int mouseX, int mouseY, float tickProgress) {
        var theme = ProjectSThemeManager.get().activeTheme();
        var tokens = theme.tokens();
        graphics.fill(0, 0, width, height, tokens.background());
        ProjectSUiDraw.cutPanel(graphics, panelX, panelY, panelWidth, panelHeight,
                theme.metrics().modalCornerCut(), tokens.surfaceRaised(), tokens.borderStrong());
        ProjectSTextRenderer.drawStrong(graphics, "革防具の色",
                panelX + 20, panelY + 18, 11, tokens.textPrimary());
        for (int x = 0; x < svWidth; x += 3) {
            for (int y = 0; y < svHeight; y += 3) {
                int rgb = MobEditorColorLogic.hsvToRgb(hue, x / (double) svWidth,
                        1 - y / (double) svHeight);
                graphics.fill(svX + x, svY + y, svX + Math.min(svWidth, x + 3),
                        svY + Math.min(svHeight, y + 3), 0xFF000000 | rgb);
            }
        }
        for (int x = 0; x < hueWidth; x += 3) {
            int rgb = MobEditorColorLogic.hsvToRgb(x * 360.0 / hueWidth, 1, 1);
            graphics.fill(hueX + x, hueY, hueX + Math.min(hueWidth, x + 3),
                    hueY + 18, 0xFF000000 | rgb);
        }
        int current = MobEditorColorLogic.hsvToRgb(hue, saturation, value);
        if (!compact) {
            graphics.fill(panelX + 188, hueY + 44, panelX + 242, hueY + 72,
                    0xFF000000 | original);
            graphics.fill(panelX + 250, hueY + 44, panelX + 304, hueY + 72,
                    0xFF000000 | current);
            ProjectSTextRenderer.draw(graphics, "変更前",
                    panelX + 188, hueY + 76, 8, tokens.textMuted());
            ProjectSTextRenderer.draw(graphics, "現在",
                    panelX + 250, hueY + 76, 8, tokens.textMuted());
            ProjectSTextRenderer.drawMono(graphics,
                    "RGB  " + (current >> 16 & 255) + ", "
                            + (current >> 8 & 255) + ", " + (current & 255),
                    panelX + 320, hueY + 53, 8, tokens.textSecondary());
        }
        for (int index = 0; !compact && index < SWATCHES.size(); index++) {
            graphics.fill(panelX + 24 + index * 34, hueY + 94,
                    panelX + 44 + index * 34, hueY + 110,
                    0xFF000000 | SWATCHES.get(index));
        }
        super.extractRenderState(graphics, mouseX, mouseY, tickProgress);
    }

    @Override
    public void onClose() {
        cancel();
    }
}
