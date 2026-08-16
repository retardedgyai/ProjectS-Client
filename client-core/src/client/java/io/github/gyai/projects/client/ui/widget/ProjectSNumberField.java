package io.github.gyai.projects.client.ui.widget;

import io.github.gyai.projects.client.ui.icon.ProjectSIcon;
import io.github.gyai.projects.client.ui.icon.ProjectSStandardIcons;
import io.github.gyai.projects.client.ui.render.ProjectSTextRenderer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.function.DoubleConsumer;

public final class ProjectSNumberField extends ProjectSTextField {
    public static final ProjectSIcon DECREMENT_ICON = ProjectSStandardIcons.DECREMENT;
    public static final ProjectSIcon INCREMENT_ICON = ProjectSStandardIcons.INCREMENT;
    private final double minimum;
    private final double maximum;
    private final double step;
    private final int decimals;
    private final String unit;
    private final DoubleConsumer onCommit;
    private double committedValue;

    public ProjectSNumberField(
            Font font, int x, int y, int width, int height,
            Component label, double value,
            double minimum, double maximum, double step,
            int decimals, String unit, DoubleConsumer onCommit
    ) {
        super(font, x, y, width, height, label,
                Component.literal(unit == null ? "" : unit));
        this.minimum = minimum;
        this.maximum = maximum;
        this.step = step;
        this.decimals = Math.clamp(decimals, 0, 8);
        this.unit = unit == null ? "" : unit;
        this.onCommit = onCommit == null ? ignored -> { } : onCommit;
        committedValue = ProjectSNumberLogic.clamp(value, minimum, maximum);
        setValue(format(committedValue));
        setResponder(this::validateTyping);
    }

    private void validateTyping(String value) {
        if (ProjectSNumberLogic.isIntermediate(value)) {
            error(Component.empty());
            return;
        }
        error(ProjectSNumberLogic.parseFinite(value) == null
                ? Component.literal("有限な数値を入力してください")
                : Component.empty());
    }

    public void increment(boolean shift) {
        applyStep(step, shift);
    }

    public void decrement(boolean shift) {
        applyStep(-step, shift);
    }

    private void applyStep(double amount, boolean shift) {
        Double parsed = ProjectSNumberLogic.parseFinite(getValue());
        double base = parsed == null ? committedValue : parsed;
        commit(ProjectSNumberLogic.step(
                base, amount, shift, minimum, maximum));
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER
                || event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
            validateAndCommit();
            return true;
        }
        if (event.isUp()) {
            increment(event.hasShiftDown());
            return true;
        }
        if (event.isDown()) {
            decrement(event.hasShiftDown());
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void setFocused(boolean focused) {
        boolean wasFocused = isFocused();
        super.setFocused(focused);
        if (focused && !wasFocused) {
            setCursorPosition(0);
            setHighlightPos(getValue().length());
        }
        if (wasFocused && !focused) validateAndCommit();
    }

    public boolean validateAndCommit() {
        Double parsed = ProjectSNumberLogic.parseFinite(getValue());
        if (parsed == null) {
            setValue(format(committedValue));
            error(Component.literal("直前の有効値へ戻しました"));
            return false;
        }
        commit(ProjectSNumberLogic.clamp(parsed, minimum, maximum));
        error(Component.empty());
        return true;
    }

    private void commit(double value) {
        if (Double.compare(committedValue, value) == 0) {
            setValue(format(value));
            return;
        }
        committedValue = value;
        setValue(format(value));
        onCommit.accept(value);
    }

    private String format(double value) {
        return String.format(Locale.ROOT, "%." + decimals + "f", value);
    }

    @Override
    public void extractWidgetRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickProgress
    ) {
        super.extractWidgetRenderState(graphics, mouseX, mouseY, tickProgress);
        if (!unit.isBlank()) {
            var tokens = io.github.gyai.projects.client.ui.theme.ProjectSThemeManager
                    .get().activeTheme().tokens();
            int unitWidth = (int) Math.ceil(ProjectSTextRenderer.monoWidth(unit, 8));
            ProjectSTextRenderer.drawMono(graphics, unit,
                    getRight() - unitWidth - 7,
                    getY() + (getHeight() - 9) / 2.0, 8, tokens.textMuted());
        }
    }

    public double committedValue() {
        return committedValue;
    }
}
