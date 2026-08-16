package io.github.gyai.projects.minecraft.adapter;

import io.github.gyai.projects.minecraft.adapter.icon.MinecraftIconRenderer;
import io.github.gyai.projects.minecraft.adapter.shell.ShellSurfaceKind;
import io.github.gyai.projects.ui.runtime.IconKey;
import io.github.gyai.projects.ui.runtime.UiColor;
import io.github.gyai.projects.ui.runtime.UiDrawList;
import io.github.gyai.projects.ui.runtime.UiGradientSpan;
import io.github.gyai.projects.ui.runtime.UiRect;
import io.github.gyai.projects.ui.runtime.UiRasterSpan;
import io.github.gyai.projects.ui.runtime.UiRenderBackend;
import io.github.gyai.projects.ui.runtime.UiRenderCommand;
import io.github.gyai.projects.ui.runtime.UiRoundedRaster;
import io.github.gyai.projects.ui.runtime.icon.IconState;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayDeque;
import java.util.Deque;

/** Tier 1 backend: alpha fills, approximated rounded/chamfer surfaces, gradients and clips. */
public final class MinecraftUiRenderBackend implements UiRenderBackend {
    private final GuiGraphicsExtractor graphics;
    private final Font font;
    private final MinecraftUiRuntimeResources resources;
    private final MinecraftUiRenderProfile profile;
    private final MinecraftScissorBridge scissor = new MinecraftScissorBridge();
    private final Deque<UiRect> clipStack = new ArrayDeque<>();

    public MinecraftUiRenderBackend(GuiGraphicsExtractor graphics, Font font) {
        this(graphics, font, null, MinecraftUiRenderProfile.LEGACY);
    }

    public MinecraftUiRenderBackend(GuiGraphicsExtractor graphics, Font font,
                                    MinecraftUiRuntimeResources resources) {
        this(graphics, font, resources, MinecraftUiRenderProfile.LEGACY);
    }

    /** Profile-aware boundary; the old constructors deliberately remain LEGACY. */
    public MinecraftUiRenderBackend(GuiGraphicsExtractor graphics, Font font,
                                    MinecraftUiRuntimeResources resources,
                                    MinecraftUiRenderProfile profile) {
        this.graphics = graphics;
        this.font = font;
        this.resources = resources;
        this.profile = profile == null ? MinecraftUiRenderProfile.LEGACY : profile;
    }

    /** Equivalent parameter order for hosts that keep the profile next to the screen type. */
    public MinecraftUiRenderBackend(GuiGraphicsExtractor graphics, Font font,
                                    MinecraftUiRenderProfile profile,
                                    MinecraftUiRuntimeResources resources) {
        this(graphics, font, resources, profile);
    }

    public MinecraftUiRenderProfile profile() { return profile; }

    @Override
    public void render(UiDrawList drawList) {
        if (drawList == null) throw new NullPointerException("drawList");
        clipStack.clear();
        if (profile == MinecraftUiRenderProfile.CAELESTIA_SHELL && resources != null) {
            resources.beginFrame(graphics);
        }
        try {
            for (UiRenderCommand command : drawList.commands()) render(command);
            if (drawList.clipDepth() != 0 || !clipStack.isEmpty()) {
                throw new IllegalStateException("Unbalanced UI draw clips");
            }
        } finally {
            if (profile == MinecraftUiRenderProfile.CAELESTIA_SHELL && resources != null) {
                resources.endFrame();
            }
        }
    }

    private void render(UiRenderCommand command) {
        switch (command) {
            case UiRenderCommand.FillRect fill -> fill(fill.bounds(), fill.color());
            case UiRenderCommand.RoundedSurface surface -> rounded(surface.bounds(), surface.radius(), surface.color());
            case UiRenderCommand.Border border -> border(border.bounds(), border.radius(), border.width(), border.color());
            case UiRenderCommand.Gradient gradient -> gradient(gradient.bounds(), gradient.radius(), gradient.top(), gradient.bottom());
            case UiRenderCommand.Text text -> text(text);
            case UiRenderCommand.Icon icon -> icon(icon.bounds(), icon.icon().key(), IconState.NORMAL, icon.tint());
            case UiRenderCommand.StatefulIcon icon -> icon(icon.bounds(), icon.icon().key(), icon.state(), icon.tint());
            case UiRenderCommand.Shadow shadow -> shadow(shadow.bounds(), shadow.radius(), shadow.color());
            case UiRenderCommand.PushClip push -> {
                clipStack.push(push.clip());
                scissor.push(graphics, push.clip());
            }
            case UiRenderCommand.PopClip ignored -> {
                if (clipStack.isEmpty()) throw new IllegalStateException("Clip pop without push");
                UiRect poppedClip = clipStack.pop();
                if (!poppedClip.isEmpty()) scissor.pop(graphics);
            }
        }
    }

    private void fill(UiRect rect, UiColor color) {
        if (rect.isEmpty()) return;
        graphics.fill(left(rect), top(rect), right(rect), bottom(rect), color.argb());
    }

    private void rounded(UiRect rect, double radius, UiColor color) {
        if (shellVisualsReady()) {
            resources.renderShellSurface(graphics, rect, radius, color, ShellSurfaceKind.FILL);
            return;
        }
        if (profile == MinecraftUiRenderProfile.CAELESTIA_SHELL) return;
        for (UiRasterSpan span : UiRoundedRaster.fillSpans(rect, radius)) {
            graphics.fill(span.left(), span.y(), span.right(), span.y() + 1, color.argb());
        }
    }

    private void border(UiRect rect, double radius, double width, UiColor color) {
        if (shellVisualsReady()) {
            resources.renderShellSurface(graphics, rect, radius, width, color, ShellSurfaceKind.BORDER);
            return;
        }
        if (profile == MinecraftUiRenderProfile.CAELESTIA_SHELL) return;
        for (UiRasterSpan span : UiRoundedRaster.borderSpans(rect, radius, width)) {
            graphics.fill(span.left(), span.y(), span.right(), span.y() + 1, color.argb());
        }
    }

    private void gradient(UiRect rect, double radius, UiColor top, UiColor bottom) {
        if (shellVisualsReady()) {
            resources.renderShellGradient(graphics, rect, radius, top, bottom);
            return;
        }
        if (profile == MinecraftUiRenderProfile.CAELESTIA_SHELL) return;
        for (UiGradientSpan gradientSpan : UiRoundedRaster.gradientSpans(rect, radius)) {
            UiRasterSpan span = gradientSpan.span();
            graphics.fill(span.left(), span.y(), span.right(), span.y() + 1,
                    top.mix(bottom, gradientSpan.amount()).argb());
        }
    }

    private void text(UiRenderCommand.Text command) {
        if (profile == MinecraftUiRenderProfile.CAELESTIA_SHELL) {
            if (!shellVisualsReady()) return;
            resources.renderText(graphics, command);
            return;
        }
        if (font != null) {
            graphics.text(font, command.value(), (int) Math.round(command.origin().x()),
                    (int) Math.round(command.origin().y()), command.color().argb(), false);
        }
    }

    private void icon(UiRect rect, IconKey key, IconState state, UiColor tint) {
        if (resources != null) {
            resources.renderIcon(graphics, rect, key, state, tint, profile);
        } else if (profile == MinecraftUiRenderProfile.CAELESTIA_SHELL
                && MinecraftIconRenderer.isShellIcon(key)) {
            // Shell icons fail closed outside the resource lifecycle as well.
        } else if (profile == MinecraftUiRenderProfile.LEGACY) {
            MinecraftIconRenderer.renderLegacy(graphics, key, rect, state, tint, null);
        } else {
            // A shell profile never silently falls back to the Studio/procedural icon path.
        }
    }

    private void shadow(UiRect rect, double spread, UiColor color) {
        if (shellVisualsReady()) {
            resources.renderShellSurface(graphics, rect, spread, color, ShellSurfaceKind.SHADOW);
            return;
        }
        if (profile == MinecraftUiRenderProfile.CAELESTIA_SHELL) return;
        int layers = Math.max(1, Math.min(4, (int) Math.ceil(spread)));
        for (int index = layers; index >= 1; index--) {
            double inset = -index;
            rounded(new UiRect(rect.x() + inset, rect.y() + inset,
                    rect.width() - inset * 2, rect.height() - inset * 2), spread + index,
                    color.withAlpha(Math.max(1, color.alpha() / (layers + 1))));
        }
    }

    private static int left(UiRect rect) { return (int) Math.floor(rect.x()); }
    private static int top(UiRect rect) { return (int) Math.floor(rect.y()); }
    private static int right(UiRect rect) { return (int) Math.ceil(rect.right()); }
    private static int bottom(UiRect rect) { return (int) Math.ceil(rect.bottom()); }

    static boolean shellPresentationEnabled(MinecraftUiRenderProfile profile, boolean resourcesReady) {
        return profile == MinecraftUiRenderProfile.CAELESTIA_SHELL && resourcesReady;
    }

    private boolean shellVisualsReady() {
        return resources != null && shellPresentationEnabled(profile, resources.clientShellVisualsReady());
    }
}
