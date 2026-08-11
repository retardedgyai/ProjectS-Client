package io.github.gyai.projects.minecraft.adapter.icon;

import io.github.gyai.projects.ui.runtime.IconKey;
import io.github.gyai.projects.ui.runtime.UiColor;
import io.github.gyai.projects.ui.runtime.UiRect;
import io.github.gyai.projects.ui.runtime.UiTheme;
import io.github.gyai.projects.ui.runtime.icon.IconDrawTarget;
import io.github.gyai.projects.ui.runtime.icon.IconRenderPlan;
import io.github.gyai.projects.ui.runtime.icon.IconRenderer;
import io.github.gyai.projects.ui.runtime.icon.IconState;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Adapter-only facade exposed for Integration wiring; the shared backend is untouched. */
public final class MinecraftIconRenderer {
    private MinecraftIconRenderer() { }

    public static void render(
            GuiGraphicsExtractor graphics, IconRenderPlan plan, MinecraftIconAtlasBinding atlas
    ) {
        if (graphics == null || plan == null) throw new IllegalArgumentException("graphics/plan");
        IconDrawTarget target = new MinecraftIconDrawTarget(graphics, atlas);
        IconRenderer.render(plan, target);
    }

    public static void render(
            GuiGraphicsExtractor graphics, IconKey key, UiRect bounds, IconState state,
            UiTheme theme, MinecraftIconAtlasBinding atlas
    ) {
        if (graphics == null || bounds == null || state == null || theme == null) {
            throw new IllegalArgumentException("graphics/bounds/state/theme");
        }
        IconRenderPlan plan = IconRenderer.plan(key, bounds, state, theme, atlas != null);
        render(graphics, plan, atlas);
    }

    /** Draw-list boundary overload: the shared component has already resolved its tint token. */
    public static void render(
            GuiGraphicsExtractor graphics, IconKey key, UiRect bounds, IconState state,
            UiColor tint, MinecraftIconAtlasBinding atlas
    ) {
        if (graphics == null || key == null || bounds == null || state == null || tint == null) {
            throw new IllegalArgumentException("graphics/key/bounds/state/tint");
        }
        IconRenderPlan plan = IconRenderer.plan(key, bounds, state, tint, atlas != null);
        render(graphics, plan, atlas);
    }
}
