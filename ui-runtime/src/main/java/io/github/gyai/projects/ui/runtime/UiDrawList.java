package io.github.gyai.projects.ui.runtime;

import java.util.ArrayList;
import java.util.List;

/** Small immutable-after-build command list with balanced clip tracking. */
public final class UiDrawList {
    private final List<UiRenderCommand> commands = new ArrayList<>();
    private final UiClipStack clips = new UiClipStack();

    public UiDrawList fillRect(UiRect bounds, UiColor color) {
        commands.add(new UiRenderCommand.FillRect(bounds, color));
        return this;
    }

    public UiDrawList roundedSurface(UiRect bounds, double radius, UiColor color, UiMaterialTier tier) {
        commands.add(new UiRenderCommand.RoundedSurface(bounds, radius, color, tier));
        return this;
    }

    public UiDrawList border(UiRect bounds, double radius, double width, UiColor color) {
        commands.add(new UiRenderCommand.Border(bounds, radius, width, color));
        return this;
    }

    public UiDrawList gradient(UiRect bounds, UiColor top, UiColor bottom) {
        return gradient(bounds, 0, top, bottom);
    }

    public UiDrawList gradient(UiRect bounds, double radius, UiColor top, UiColor bottom) {
        commands.add(new UiRenderCommand.Gradient(bounds, radius, top, bottom));
        return this;
    }

    public UiDrawList text(UiPoint origin, String value, TextStyle style, UiColor color) {
        commands.add(new UiRenderCommand.Text(origin, value, style, color));
        return this;
    }

    public UiDrawList icon(UiRect bounds, IconSpec icon, UiColor tint) {
        commands.add(new UiRenderCommand.Icon(bounds, icon, tint));
        return this;
    }

    public UiDrawList icon(UiRect bounds, IconSpec icon, UiColor tint,
                           io.github.gyai.projects.ui.runtime.icon.IconState state) {
        commands.add(new UiRenderCommand.StatefulIcon(bounds, icon, state, tint));
        return this;
    }

    public UiDrawList shadow(UiRect bounds, double radius, UiColor color) {
        commands.add(new UiRenderCommand.Shadow(bounds, radius, color));
        return this;
    }

    public UiRect pushClip(UiRect clip) {
        UiRect effective = clips.push(clip);
        commands.add(new UiRenderCommand.PushClip(effective));
        return effective;
    }

    public UiRect popClip() {
        UiRect previous = clips.pop();
        commands.add(new UiRenderCommand.PopClip());
        return previous;
    }

    public List<UiRenderCommand> commands() { return List.copyOf(commands); }
    public int clipDepth() { return clips.depth(); }

    public void assertBalanced() {
        if (clips.depth() != 0) throw new IllegalStateException("Unbalanced draw-list clips");
    }
}
