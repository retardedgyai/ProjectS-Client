package io.github.gyai.projects.ui.runtime.typography;

import io.github.gyai.projects.ui.runtime.UiFontFamilyRole;

/** Stable fallback metrics used before an adapter has rasterized a glyph. */
public final class DeterministicGlyphMetricsProvider implements GlyphMetricsProvider {
    @Override
    public GlyphMetrics metrics(FontFaceMetadata face, int codePoint, double size) {
        if (face == null || !Character.isValidCodePoint(codePoint) || !Double.isFinite(size) || size <= 0) {
            throw new IllegalArgumentException("face/codePoint/size");
        }
        if (face.missingGlyphFace()) {
            double width = Math.max(1, size * .72);
            return new GlyphMetrics(width, width, size * .78, 0, size * .78, true);
        }
        if (isCombining(codePoint)) return new GlyphMetrics(0, 0, size * .78, 0, size * .78, false);
        if (Character.isWhitespace(codePoint)) {
            double width = codePoint == '\t' ? size * 1.2 : size * .34;
            return new GlyphMetrics(width, 0, 0, 0, 0, false);
        }
        double advance;
        if (isWide(codePoint)) {
            advance = size;
        } else if (face.key().role() == UiFontFamilyRole.TECHNICAL_MONO) {
            advance = size * .60;
        } else if (isPunctuation(codePoint)) {
            advance = size * .46;
        } else if (Character.isUpperCase(codePoint) || Character.isDigit(codePoint)) {
            advance = size * .60;
        } else {
            advance = size * .54;
        }
        return new GlyphMetrics(advance, Math.max(0, advance * .9), size * .78,
                0, size * .78, false);
    }

    private static boolean isWide(int codePoint) {
        return (codePoint >= 0x1100 && codePoint <= 0x11FF)
                || (codePoint >= 0x2E80 && codePoint <= 0xA4CF)
                || (codePoint >= 0xAC00 && codePoint <= 0xD7AF)
                || (codePoint >= 0xF900 && codePoint <= 0xFAFF)
                || (codePoint >= 0xFE10 && codePoint <= 0xFE6F)
                || (codePoint >= 0xFF01 && codePoint <= 0xFF60)
                || (codePoint >= 0xFFE0 && codePoint <= 0xFFE6)
                || (codePoint >= 0x20000 && codePoint <= 0x3FFFD);
    }

    private static boolean isPunctuation(int codePoint) {
        return Character.getType(codePoint) == Character.CONNECTOR_PUNCTUATION
                || Character.getType(codePoint) == Character.DASH_PUNCTUATION
                || Character.getType(codePoint) == Character.START_PUNCTUATION
                || Character.getType(codePoint) == Character.END_PUNCTUATION
                || Character.getType(codePoint) == Character.OTHER_PUNCTUATION;
    }

    private static boolean isCombining(int codePoint) {
        int type = Character.getType(codePoint);
        return type == Character.NON_SPACING_MARK || type == Character.COMBINING_SPACING_MARK
                || type == Character.ENCLOSING_MARK;
    }
}
