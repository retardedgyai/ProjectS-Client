package io.github.gyai.projects.ui.runtime;

/** Pure render vocabulary emitted by the UI tree and consumed by an adapter backend. */
public sealed interface UiRenderCommand permits
        UiRenderCommand.FillRect,
        UiRenderCommand.RoundedSurface,
        UiRenderCommand.Border,
        UiRenderCommand.Gradient,
        UiRenderCommand.Text,
        UiRenderCommand.Icon,
        UiRenderCommand.Shadow,
        UiRenderCommand.PushClip,
        UiRenderCommand.PopClip {
    record FillRect(UiRect bounds, UiColor color) implements UiRenderCommand {
        public FillRect { require(bounds, color); }
    }

    record RoundedSurface(UiRect bounds, double radius, UiColor color, UiMaterialTier tier)
            implements UiRenderCommand {
        public RoundedSurface {
            require(bounds, color);
            if (tier == null || !Double.isFinite(radius) || radius < 0) throw new IllegalArgumentException("surface");
        }
    }

    record Border(UiRect bounds, double radius, double width, UiColor color)
            implements UiRenderCommand {
        public Border {
            require(bounds, color);
            if (!Double.isFinite(radius) || radius < 0 || !Double.isFinite(width) || width < 0) {
                throw new IllegalArgumentException("border");
            }
        }
    }

    record Gradient(UiRect bounds, double radius, UiColor top, UiColor bottom) implements UiRenderCommand {
        public Gradient {
            require(bounds, top);
            if (bottom == null || !Double.isFinite(radius) || radius < 0) throw new IllegalArgumentException("gradient");
        }

        public Gradient(UiRect bounds, UiColor top, UiColor bottom) {
            this(bounds, 0, top, bottom);
        }
    }

    record Text(UiPoint origin, String value, TextStyle style, UiColor color) implements UiRenderCommand {
        public Text {
            if (origin == null || value == null || style == null || color == null) throw new IllegalArgumentException("text");
        }
    }

    record Icon(UiRect bounds, IconSpec icon, UiColor tint) implements UiRenderCommand {
        public Icon { require(bounds, tint); if (icon == null) throw new IllegalArgumentException("icon"); }
    }

    record Shadow(UiRect bounds, double radius, UiColor color) implements UiRenderCommand {
        public Shadow {
            require(bounds, color);
            if (!Double.isFinite(radius) || radius < 0) throw new IllegalArgumentException("shadow");
        }
    }

    record PushClip(UiRect clip) implements UiRenderCommand {
        public PushClip { if (clip == null) throw new IllegalArgumentException("clip"); }
    }

    record PopClip() implements UiRenderCommand { }

    private static void require(UiRect bounds, UiColor color) {
        if (bounds == null || color == null) throw new IllegalArgumentException("bounds/color");
    }
}
