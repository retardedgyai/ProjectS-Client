package io.github.gyai.projects.minecraft.adapter;

import io.github.gyai.projects.ui.runtime.TextMeasurer;
import io.github.gyai.projects.ui.runtime.TextMetrics;
import io.github.gyai.projects.ui.runtime.TextStyle;
import net.minecraft.client.gui.Font;

/** Temporary Minecraft Font implementation of the runtime measurement port. */
public final class MinecraftTextMeasurer implements TextMeasurer {
    private final Font font;

    public MinecraftTextMeasurer(Font font) {
        this.font = font == null ? null : font;
    }

    @Override
    public TextMetrics measure(String text, TextStyle style) {
        if (text == null || style == null) throw new IllegalArgumentException("text/style");
        if (font == null || text.isEmpty()) return text.isEmpty()
                ? TextMetrics.empty() : new TextMetrics(text.length() * style.size() * .5, style.lineHeight(), style.size(), 1);
        String[] lines = text.split("\\R", -1);
        int width = 0;
        for (String line : lines) width = Math.max(width, font.width(line));
        double height = Math.max(style.lineHeight(), lines.length * font.lineHeight);
        return new TextMetrics(width, height, Math.min(height, font.lineHeight * .8), lines.length);
    }

    public Font font() { return font; }
}
