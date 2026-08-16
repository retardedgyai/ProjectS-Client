package io.github.gyai.projects.ui.runtime;

/** Tier 1 liquid-glass surface component; all visual values come from theme/material tokens. */
public class UiSurface extends UiNode {
    private UiMaterialTier materialTier;
    private double radius;
    private UiInsets padding;
    private double borderWidth;

    public UiSurface(String id, UiRect bounds, UiMaterialTier materialTier) {
        this(id, bounds, materialTier, 8, UiInsets.zero());
    }

    public UiSurface(String id, UiRect bounds, UiMaterialTier materialTier,
                     double radius, UiInsets padding) {
        super(id, bounds);
        if (materialTier == null || !Double.isFinite(radius) || radius < 0 || padding == null) {
            throw new IllegalArgumentException("surface");
        }
        this.materialTier = materialTier;
        this.radius = radius;
        this.padding = padding;
        this.borderWidth = 1;
        setAccessibility(UiAccessibilityMetadata.of(UiAccessibilityRole.SURFACE, id));
    }

    public UiMaterialTier materialTier() { return materialTier; }
    public double radius() { return radius; }
    public UiInsets padding() { return padding; }
    public double borderWidth() { return borderWidth; }
    public UiRect contentBounds() { return bounds().inset(padding); }

    public UiSurface setMaterialTier(UiMaterialTier next) {
        if (next == null) throw new NullPointerException("materialTier");
        materialTier = next;
        return this;
    }

    public UiSurface setRadius(double next) {
        if (!Double.isFinite(next) || next < 0) throw new IllegalArgumentException("radius");
        radius = next;
        return this;
    }

    public UiSurface setPadding(UiInsets next) {
        if (next == null) throw new NullPointerException("padding");
        padding = next;
        return this;
    }

    public UiSurface setBorderWidth(double next) {
        if (!Double.isFinite(next) || next < 0) throw new IllegalArgumentException("borderWidth");
        borderWidth = next;
        return this;
    }

    @Override
    protected void appendSelf(UiDrawList drawList, UiTheme theme, UiRect globalBounds, UiRect clip) {
        UiMaterialStyle style = UiMaterialStyle.resolve(materialTier, theme);
        drawList.shadow(globalBounds.offset(0, style.shadowSpread()), style.shadowSpread(), style.shadow());
        drawList.roundedSurface(globalBounds, radius, style.fill(), materialTier);
        drawList.gradient(globalBounds, radius, style.gradientTop(), style.gradientBottom());
        drawList.border(globalBounds, radius, borderWidth, style.border());
        drawList.border(globalBounds.inset(new UiInsets(.5)), Math.max(0, radius - .5), .5,
                style.edgeHighlight());
    }
}
