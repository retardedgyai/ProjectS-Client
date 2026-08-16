package io.github.gyai.projects.client.ui.render;

import io.github.gyai.projects.minecraft.adapter.MinecraftUiRuntimeResources;
import io.github.gyai.projects.ui.runtime.TextStyle;
import io.github.gyai.projects.ui.runtime.UiColor;
import io.github.gyai.projects.ui.runtime.UiColorRole;
import io.github.gyai.projects.ui.runtime.UiFontFamilyRole;
import io.github.gyai.projects.ui.runtime.UiFontWeight;
import io.github.gyai.projects.ui.runtime.UiPoint;
import io.github.gyai.projects.ui.runtime.UiRenderCommand;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** High-resolution ProjectS typography with a fail-safe Minecraft fallback. */
public final class ProjectSTextRenderer {
    private ProjectSTextRenderer() { }

    public static void draw(GuiGraphicsExtractor graphics, String value,
                            double x, double y, double size, int argb) {
        draw(graphics, value, x, y, size, argb, UiFontWeight.NORMAL,
                UiFontFamilyRole.UI_SANS);
    }

    public static void drawStrong(GuiGraphicsExtractor graphics, String value,
                                  double x, double y, double size, int argb) {
        draw(graphics, value, x, y, size, argb, UiFontWeight.SEMIBOLD,
                UiFontFamilyRole.UI_SANS);
    }

    public static void drawMono(GuiGraphicsExtractor graphics, String value,
                                double x, double y, double size, int argb) {
        draw(graphics, value, x, y, size, argb, UiFontWeight.NORMAL,
                UiFontFamilyRole.TECHNICAL_MONO);
    }

    public static double width(String value, double size, boolean strong) {
        return width(value, size, strong ? UiFontWeight.SEMIBOLD : UiFontWeight.NORMAL,
                UiFontFamilyRole.UI_SANS);
    }

    public static double monoWidth(String value, double size) {
        return width(value, size, UiFontWeight.NORMAL, UiFontFamilyRole.TECHNICAL_MONO);
    }

    public static String fit(String value, double size, double maximumWidth, boolean mono) {
        String text = value == null ? "" : value;
        if (text.isEmpty() || maximumWidth <= 0) return "";
        if (measure(text, size, mono) <= maximumWidth) return text;
        int low = 0;
        int high = text.length();
        while (low < high) {
            int middle = (low + high + 1) >>> 1;
            if (measure(text.substring(0, middle), size, mono) <= maximumWidth) low = middle;
            else high = middle - 1;
        }
        return text.substring(0, low);
    }

    private static double measure(String value, double size, boolean mono) {
        return width(value, size, UiFontWeight.NORMAL,
                mono ? UiFontFamilyRole.TECHNICAL_MONO : UiFontFamilyRole.UI_SANS);
    }

    private static double width(String value, double size, UiFontWeight weight,
                                UiFontFamilyRole family) {
        MinecraftUiRuntimeResources resources = MinecraftUiRuntimeResources.currentOrNull();
        TextStyle style = style(size, weight, family);
        if (resources != null && resources.typographyReady()) {
            return resources.typographyRuntime().measure(value == null ? "" : value, style).width();
        }
        return Minecraft.getInstance().font.width(value == null ? "" : value);
    }

    private static void draw(GuiGraphicsExtractor graphics, String value,
                             double x, double y, double size, int argb,
                             UiFontWeight weight, UiFontFamilyRole family) {
        String text = value == null ? "" : value;
        MinecraftUiRuntimeResources resources = MinecraftUiRuntimeResources.currentOrNull();
        TextStyle style = style(size, weight, family);
        if (resources != null && resources.renderText(graphics,
                new UiRenderCommand.Text(new UiPoint(x, y), text, style, UiColor.argb(argb)))) {
            return;
        }
        graphics.text(Minecraft.getInstance().font, text,
                (int) Math.round(x), (int) Math.round(y), argb, false);
    }

    private static TextStyle style(double size, UiFontWeight weight, UiFontFamilyRole family) {
        return new TextStyle(family, weight, size, Math.max(size + 3, size * 1.3),
                0, UiColorRole.TEXT_PRIMARY);
    }
}
