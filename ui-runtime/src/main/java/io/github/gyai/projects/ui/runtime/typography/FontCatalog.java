package io.github.gyai.projects.ui.runtime.typography;

import io.github.gyai.projects.ui.runtime.UiFontFamilyRole;
import io.github.gyai.projects.ui.runtime.UiFontWeight;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Ordered font-face catalog. Resolution is deterministic and never consults the host system. */
public final class FontCatalog {
    private final List<FontFaceMetadata> faces;
    private final List<FontFaceMetadata> missingFaces;

    public FontCatalog(List<FontFaceMetadata> faces) {
        Objects.requireNonNull(faces, "faces");
        if (faces.isEmpty()) throw new IllegalArgumentException("Font catalog must not be empty");
        this.faces = List.copyOf(faces);
        this.missingFaces = List.of(FontFaceMetadata.missing(UiFontFamilyRole.UI_SANS),
                FontFaceMetadata.missing(UiFontFamilyRole.TECHNICAL_MONO));
    }

    public static Builder builder() { return new Builder(); }

    public static FontCatalog bundledDefaults() {
        Builder builder = builder();
        List<UnicodeRange> latin = FontFaceMetadata.latinCoverage();
        List<UnicodeRange> cjk = FontFaceMetadata.cjkCoverage();
        String interUrl = "https://github.com/rsms/inter/releases/download/v4.1/Inter-4.1.zip";
        String notoUrl = "https://github.com/notofonts/noto-cjk/releases/download/Sans2.004/02_NotoSansCJK-TTF-VF.zip";
        String monoUrl = "https://github.com/JetBrains/JetBrainsMono/releases/download/v2.304/JetBrainsMono-2.304.zip";
        for (UiFontWeight weight : UiFontWeight.values()) {
            builder.add(FontFaceMetadata.bundled(new FontKey("projects:inter", UiFontFamilyRole.UI_SANS,
                            weight, FontStyle.NORMAL), "Inter " + weight,
                    "assets/projects_client/fonts/inter/inter-" + weightName(weight) + ".ttf", interUrl,
                    "OFL-1.1-Inter-4.1", latin, 0));
            builder.add(FontFaceMetadata.bundled(new FontKey("projects:jetbrains-mono", UiFontFamilyRole.TECHNICAL_MONO,
                            weight, FontStyle.NORMAL), "JetBrains Mono " + weight,
                    "assets/projects_client/fonts/jetbrains-mono/jetbrainsmono-" + weightName(weight) + ".ttf",
                    monoUrl, "OFL-1.1-JetBrainsMono-2.304", latin, 0));
            builder.add(FontFaceMetadata.bundled(new FontKey("projects:noto-sans-cjk-jp", UiFontFamilyRole.UI_SANS,
                            weight, FontStyle.NORMAL), "Noto Sans CJK JP " + weight,
                    "assets/projects_client/fonts/noto-sans-cjk-jp/notosanscjkjp-vf.ttf", notoUrl,
                    "OFL-1.1-NotoSansCJK-2.004", cjk, 100));
            builder.add(FontFaceMetadata.bundled(new FontKey("projects:noto-sans-cjk-jp", UiFontFamilyRole.TECHNICAL_MONO,
                            weight, FontStyle.NORMAL), "Noto Sans CJK JP " + weight + " fallback",
                    "assets/projects_client/fonts/noto-sans-cjk-jp/notosanscjkjp-vf.ttf", notoUrl,
                    "OFL-1.1-NotoSansCJK-2.004", cjk, 100));
        }
        return builder.build();
    }

    public List<FontFaceMetadata> faces() { return faces; }

    public FontFaceMetadata resolve(UiFontFamilyRole role, UiFontWeight weight, int codePoint) {
        return resolve(role, weight, FontStyle.NORMAL, codePoint);
    }

    public FontFaceMetadata resolve(FontKey requested, int codePoint) {
        Objects.requireNonNull(requested, "requested");
        return resolve(requested.role(), requested.weight(), requested.style(), codePoint);
    }

    public FontFaceMetadata resolve(UiFontFamilyRole role, UiFontWeight weight, FontStyle style, int codePoint) {
        if (role == null || weight == null || style == null || !Character.isValidCodePoint(codePoint)) {
            throw new IllegalArgumentException("Invalid font resolution request");
        }
        FontFaceMetadata best = faces.stream()
                .filter(face -> face.key().role() == role && face.key().style() == style && face.supports(codePoint))
                .min(faceComparator(weight))
                .orElse(null);
        if (best != null) return best;
        return missingFaces.stream().filter(face -> face.key().role() == role).findFirst().orElseThrow();
    }

    public List<FontFaceMetadata> fallbackOrder(UiFontFamilyRole role, UiFontWeight weight, FontStyle style) {
        if (role == null || weight == null || style == null) throw new NullPointerException("fallback order");
        List<FontFaceMetadata> candidates = faces.stream()
                .filter(face -> face.key().role() == role && face.key().style() == style)
                .sorted(faceComparator(weight))
                .toList();
        List<FontFaceMetadata> result = new ArrayList<>();
        java.util.Set<String> families = new java.util.HashSet<>();
        for (FontFaceMetadata candidate : candidates) {
            if (families.add(candidate.key().familyId())) result.add(candidate);
        }
        result.add(missingFaces.stream().filter(face -> face.key().role() == role).findFirst().orElseThrow());
        return List.copyOf(result);
    }

    private static Comparator<FontFaceMetadata> faceComparator(UiFontWeight requested) {
        return Comparator.comparingInt(FontFaceMetadata::fallbackPriority)
                .thenComparingInt(face -> Math.abs(face.key().weight().numericWeight() - requested.numericWeight()))
                .thenComparing(face -> face.key().familyId())
                .thenComparing(face -> face.assetPath());
    }

    private static String weightName(UiFontWeight weight) {
        return switch (weight) {
            case NORMAL -> "regular";
            case MEDIUM -> "medium";
            case SEMIBOLD -> "semibold";
            case BOLD -> "bold";
        };
    }

    public static final class Builder {
        private final List<FontFaceMetadata> faces = new ArrayList<>();

        public Builder add(FontFaceMetadata face) {
            faces.add(Objects.requireNonNull(face, "face"));
            return this;
        }

        public FontCatalog build() { return new FontCatalog(faces); }
    }
}
