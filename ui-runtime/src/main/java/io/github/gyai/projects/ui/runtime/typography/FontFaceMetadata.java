package io.github.gyai.projects.ui.runtime.typography;

import io.github.gyai.projects.ui.runtime.UiFontFamilyRole;
import io.github.gyai.projects.ui.runtime.UiFontWeight;

import java.util.List;
import java.util.Objects;

/** Immutable, adapter-independent description of one bundled or supplied font face. */
public final class FontFaceMetadata {
    private final FontKey key;
    private final String displayName;
    private final String version;
    private final String assetPath;
    private final String sourceUrl;
    private final String licenseId;
    private final int unitsPerEm;
    private final double ascent;
    private final double descent;
    private final double lineGap;
    private final List<UnicodeRange> coverage;
    private final int fallbackPriority;
    private final boolean missingGlyphFace;

    public FontFaceMetadata(
            FontKey key,
            String displayName,
            String version,
            String assetPath,
            String sourceUrl,
            String licenseId,
            int unitsPerEm,
            double ascent,
            double descent,
            double lineGap,
            List<UnicodeRange> coverage,
            int fallbackPriority,
            boolean missingGlyphFace
    ) {
        this.key = Objects.requireNonNull(key, "key");
        this.displayName = requireText(displayName, "displayName");
        this.version = requireText(version, "version");
        this.assetPath = requireText(assetPath, "assetPath");
        this.sourceUrl = requireText(sourceUrl, "sourceUrl");
        this.licenseId = requireText(licenseId, "licenseId");
        if (unitsPerEm <= 0 || !Double.isFinite(ascent) || ascent <= 0
                || !Double.isFinite(descent) || descent < 0
                || !Double.isFinite(lineGap) || lineGap < 0 || fallbackPriority < 0) {
            throw new IllegalArgumentException("Invalid font face metrics");
        }
        this.unitsPerEm = unitsPerEm;
        this.ascent = ascent;
        this.descent = descent;
        this.lineGap = lineGap;
        this.coverage = List.copyOf(Objects.requireNonNull(coverage, "coverage"));
        this.fallbackPriority = fallbackPriority;
        this.missingGlyphFace = missingGlyphFace;
    }

    public FontKey key() { return key; }
    public String displayName() { return displayName; }
    public String version() { return version; }
    public String assetPath() { return assetPath; }
    public String sourceUrl() { return sourceUrl; }
    public String licenseId() { return licenseId; }
    public int unitsPerEm() { return unitsPerEm; }
    public double ascent() { return ascent; }
    public double descent() { return descent; }
    public double lineGap() { return lineGap; }
    public List<UnicodeRange> coverage() { return coverage; }
    public int fallbackPriority() { return fallbackPriority; }
    public boolean missingGlyphFace() { return missingGlyphFace; }

    public boolean supports(int codePoint) {
        if (!Character.isValidCodePoint(codePoint)) return false;
        if (missingGlyphFace) return codePoint == 0xFFFD;
        for (UnicodeRange range : coverage) if (range.contains(codePoint)) return true;
        return false;
    }

    public double lineHeightAt(double size) {
        if (!Double.isFinite(size) || size <= 0) throw new IllegalArgumentException("size");
        return size * (ascent + descent + lineGap);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FontFaceMetadata face
                && key.equals(face.key)
                && assetPath.equals(face.assetPath)
                && version.equals(face.version);
    }

    @Override
    public int hashCode() { return Objects.hash(key, assetPath, version); }

    @Override
    public String toString() { return displayName + " " + version + " (" + key + ")"; }

    static FontFaceMetadata bundled(
            FontKey key,
            String displayName,
            String assetPath,
            String sourceUrl,
            String licenseId,
            List<UnicodeRange> coverage,
            int fallbackPriority
    ) {
        return new FontFaceMetadata(key, displayName, key.familyId().contains("noto") ? "2.004" :
                key.familyId().contains("jetbrains") ? "2.304" : "4.1", assetPath, sourceUrl,
                licenseId, 2048, .93, .24, .08, coverage, fallbackPriority, false);
    }

    static FontFaceMetadata missing(UiFontFamilyRole role) {
        FontKey key = new FontKey("projects:missing-glyph", role, UiFontWeight.NORMAL, FontStyle.NORMAL);
        return new FontFaceMetadata(key, "ProjectS Missing Glyph", "runtime", "runtime:missing-glyph", "runtime", "runtime",
                1000, .8, .2, 0, List.of(new UnicodeRange(0xFFFD, 0xFFFD)), Integer.MAX_VALUE, true);
    }

    static List<UnicodeRange> latinCoverage() {
        return List.of(
                new UnicodeRange(0x0000, 0x024F),
                new UnicodeRange(0x1E00, 0x1EFF),
                new UnicodeRange(0x2000, 0x206F),
                new UnicodeRange(0x20A0, 0x20CF),
                new UnicodeRange(0x2100, 0x214F),
                new UnicodeRange(0x2190, 0x21FF),
                new UnicodeRange(0x2200, 0x22FF),
                new UnicodeRange(0x2300, 0x23FF),
                new UnicodeRange(0x2500, 0x259F),
                new UnicodeRange(0x25A0, 0x25FF),
                new UnicodeRange(0x2600, 0x26FF)
        );
    }

    static List<UnicodeRange> cjkCoverage() {
        return List.of(
                new UnicodeRange(0x0000, 0x00FF),
                new UnicodeRange(0x2000, 0x206F),
                new UnicodeRange(0x3000, 0x303F),
                new UnicodeRange(0x3040, 0x30FF),
                new UnicodeRange(0x31F0, 0x31FF),
                new UnicodeRange(0x3200, 0x32FF),
                new UnicodeRange(0x3300, 0x33FF),
                new UnicodeRange(0x3400, 0x4DBF),
                new UnicodeRange(0x4E00, 0x9FFF),
                new UnicodeRange(0xF900, 0xFAFF),
                new UnicodeRange(0xFF00, 0xFFEF),
                new UnicodeRange(0x20000, 0x2FA1F)
        );
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name);
        return value;
    }
}
