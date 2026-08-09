package io.github.gyai.projects.client.ui.widget;

import io.github.gyai.projects.client.ui.render.ProjectSColorMath;
import io.github.gyai.projects.client.ui.render.ProjectSEasing;
import io.github.gyai.projects.client.ui.render.ProjectSIconRenderer;
import io.github.gyai.projects.client.ui.icon.ProjectSIcon;
import io.github.gyai.projects.client.ui.theme.ProjectSThemeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public final class ProjectSToggle extends AbstractButton {
    private static final long ANIMATION_NANOS = 130_000_000L;
    private final Consumer<Boolean> changed;
    private final Component helper;
    private final String onLabel;
    private final String offLabel;
    private final boolean helperBlank;
    private boolean value;
    private boolean animationFrom;
    private long animationStarted;

    public ProjectSToggle(
            int x, int y, int width,
            Component label, Component helper,
            boolean value, Consumer<Boolean> changed
    ) {
        super(x, y, width, 28, label);
        this.helper = helper == null ? Component.empty() : helper;
        onLabel = "ON  " + label.getString();
        offLabel = "OFF " + label.getString();
        helperBlank = this.helper.getString().isBlank();
        this.value = value;
        this.animationFrom = value;
        this.changed = changed == null ? ignored -> { } : changed;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        animationFrom = value;
        value = !value;
        animationStarted = System.nanoTime();
        changed.accept(value);
    }

    @Override
    protected void extractContents(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickProgress
    ) {
        var theme = ProjectSThemeManager.get().activeTheme();
        var tokens = theme.tokens();
        int trackWidth = 34;
        int trackHeight = 16;
        int trackX = getX() + width - trackWidth;
        int trackY = getY() + 5;
        double target = value ? 1 : 0;
        double start = animationFrom ? 1 : 0;
        double t = animationStarted == 0 ? 1 : ProjectSEasing.smooth(
                ProjectSEasing.progress(System.nanoTime(), animationStarted, ANIMATION_NANOS));
        double position = start + (target - start) * t;
        int fill = ProjectSColorMath.lerpArgb(
                tokens.surfacePressed(), tokens.accentPrimary(), position);
        int border = isFocused() ? tokens.borderSelected()
                : value ? tokens.accentPrimary() : tokens.borderSubtle();
        graphics.fill(trackX, trackY, trackX + trackWidth, trackY + trackHeight, fill);
        graphics.outline(trackX, trackY, trackWidth, trackHeight, border);
        int knobX = trackX + 3 + (int) Math.round(position * (trackWidth - 14));
        graphics.fill(knobX, trackY + 3, knobX + 10, trackY + 13,
                active ? tokens.textPrimary() : tokens.textDisabled());
        graphics.text(Minecraft.getInstance().font,
                value ? onLabel : offLabel,
                getX() + 16, getY() + 4,
                active ? tokens.textPrimary() : tokens.textDisabled(), false);
        ProjectSIconRenderer.draw(graphics,
                value ? ProjectSIcon.CHECKBOX_CHECKED : ProjectSIcon.CHECKBOX_EMPTY,
                getX(), getY() + 3, 12, tokens, !active, value);
        if (!helperBlank) {
            graphics.text(Minecraft.getInstance().font, helper,
                    getX() + 16, getY() + 18, tokens.textMuted(), false);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
        String detail = "状態: " + (value ? "ON" : "OFF");
        if (!helperBlank) detail += ". " + helper.getString();
        output.add(NarratedElementType.HINT, detail);
    }

    public boolean value() {
        return value;
    }
}
