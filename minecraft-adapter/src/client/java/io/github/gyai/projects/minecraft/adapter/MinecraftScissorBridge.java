package io.github.gyai.projects.minecraft.adapter;

import io.github.gyai.projects.ui.runtime.UiRect;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Converts logical UI clips to Minecraft's scissor bridge. */
public final class MinecraftScissorBridge {
    public void push(GuiGraphicsExtractor graphics, UiRect clip) {
        if (graphics == null || clip == null || clip.isEmpty()) return;
        int left = safeInt(Math.floor(clip.x()));
        int top = safeInt(Math.floor(clip.y()));
        int right = safeInt(Math.ceil(clip.right()));
        int bottom = safeInt(Math.ceil(clip.bottom()));
        if (right > left && bottom > top) graphics.enableScissor(left, top, right, bottom);
    }

    public void pop(GuiGraphicsExtractor graphics) {
        if (graphics != null) graphics.disableScissor();
    }

    private static int safeInt(double value) {
        if (value <= Integer.MIN_VALUE) return Integer.MIN_VALUE;
        if (value >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return (int) value;
    }
}
