package io.github.gyai.projects.ui.runtime.typography;

import java.util.List;
import java.util.Objects;

/** Immutable contiguous run whose glyphs use the same resolved face. */
public record GlyphRun(FontFaceMetadata face, List<GlyphPlacement> glyphs, double width) {
    public GlyphRun {
        Objects.requireNonNull(face, "face");
        glyphs = List.copyOf(Objects.requireNonNull(glyphs, "glyphs"));
        if (!Double.isFinite(width) || width < 0) throw new IllegalArgumentException("width");
        for (GlyphPlacement glyph : glyphs) if (!face.equals(glyph.face())) {
            throw new IllegalArgumentException("run contains multiple faces");
        }
    }

    public boolean isEmpty() { return glyphs.isEmpty(); }
}
