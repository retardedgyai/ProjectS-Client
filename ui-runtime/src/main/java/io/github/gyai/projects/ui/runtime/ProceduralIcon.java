package io.github.gyai.projects.ui.runtime;

/** Convenience top-level alias for the procedural icon contract. */
public record ProceduralIcon(String shape) implements IconSource {
    public ProceduralIcon {
        if (shape == null || shape.isBlank()) throw new IllegalArgumentException("shape");
    }

    public static ProceduralIcon of(String shape) { return new ProceduralIcon(shape); }

    public String shapeId() { return shape; }
}
