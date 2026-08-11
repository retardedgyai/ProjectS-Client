package io.github.gyai.projects.minecraft.adapter;

import io.github.gyai.projects.ui.runtime.AtlasIcon;
import io.github.gyai.projects.ui.runtime.IconSource;
import io.github.gyai.projects.ui.runtime.ProceduralIcon;
import io.github.gyai.projects.ui.runtime.UiColor;
import io.github.gyai.projects.ui.runtime.UiDrawList;
import io.github.gyai.projects.ui.runtime.UiGradientSpan;
import io.github.gyai.projects.ui.runtime.UiRect;
import io.github.gyai.projects.ui.runtime.UiRasterSpan;
import io.github.gyai.projects.ui.runtime.UiRenderBackend;
import io.github.gyai.projects.ui.runtime.UiRenderCommand;
import io.github.gyai.projects.ui.runtime.UiRoundedRaster;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayDeque;
import java.util.Deque;

/** Tier 1 backend: alpha fills, approximated rounded/chamfer surfaces, gradients and clips. */
public final class MinecraftUiRenderBackend implements UiRenderBackend {
    private final GuiGraphicsExtractor graphics;
    private final Font font;
    private final MinecraftScissorBridge scissor = new MinecraftScissorBridge();
    private final Deque<UiRect> clipStack = new ArrayDeque<>();

    public MinecraftUiRenderBackend(GuiGraphicsExtractor graphics, Font font) {
        this.graphics = graphics;
        this.font = font;
    }

    @Override
    public void render(UiDrawList drawList) {
        if (drawList == null) throw new NullPointerException("drawList");
        clipStack.clear();
        for (UiRenderCommand command : drawList.commands()) render(command);
        if (drawList.clipDepth() != 0 || !clipStack.isEmpty()) {
            throw new IllegalStateException("Unbalanced UI draw clips");
        }
    }

    private void render(UiRenderCommand command) {
        switch (command) {
            case UiRenderCommand.FillRect fill -> fill(fill.bounds(), fill.color());
            case UiRenderCommand.RoundedSurface surface -> rounded(surface.bounds(), surface.radius(), surface.color());
            case UiRenderCommand.Border border -> border(border.bounds(), border.radius(), border.width(), border.color());
            case UiRenderCommand.Gradient gradient -> gradient(gradient.bounds(), gradient.radius(), gradient.top(), gradient.bottom());
            case UiRenderCommand.Text text -> text(text.origin().x(), text.origin().y(), text.value(), text.color());
            case UiRenderCommand.Icon icon -> icon(icon.bounds(), icon.icon().source(), icon.tint());
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
        for (UiRasterSpan span : UiRoundedRaster.fillSpans(rect, radius)) {
            graphics.fill(span.left(), span.y(), span.right(), span.y() + 1, color.argb());
        }
    }

    private void border(UiRect rect, double radius, double width, UiColor color) {
        for (UiRasterSpan span : UiRoundedRaster.borderSpans(rect, radius, width)) {
            graphics.fill(span.left(), span.y(), span.right(), span.y() + 1, color.argb());
        }
    }

    private void gradient(UiRect rect, double radius, UiColor top, UiColor bottom) {
        for (UiGradientSpan gradientSpan : UiRoundedRaster.gradientSpans(rect, radius)) {
            UiRasterSpan span = gradientSpan.span();
            graphics.fill(span.left(), span.y(), span.right(), span.y() + 1,
                    top.mix(bottom, gradientSpan.amount()).argb());
        }
    }

    private void text(double x, double y, String value, UiColor color) {
        if (font != null) graphics.text(font, value, (int) Math.round(x), (int) Math.round(y), color.argb(), false);
    }

    private void icon(UiRect rect, IconSource source, UiColor tint) {
        if (source instanceof ProceduralIcon) {
            graphics.outline(left(rect), top(rect), Math.max(1, right(rect) - left(rect)),
                    Math.max(1, bottom(rect) - top(rect)), tint.argb());
        } else if (source instanceof AtlasIcon) {
            // Atlas binding is intentionally a Stage 2 resource operation; this is a visible skeleton.
            graphics.outline(left(rect), top(rect), Math.max(1, right(rect) - left(rect)),
                    Math.max(1, bottom(rect) - top(rect)), tint.argb());
        }
    }

    private void shadow(UiRect rect, double spread, UiColor color) {
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
}
