package io.github.gyai.projects.ui.runtime.typography;

import io.github.gyai.projects.ui.runtime.UiFontFamilyRole;
import io.github.gyai.projects.ui.runtime.UiFontWeight;

/** Immutable identity for an actual font face, including its requested role and weight. */
public record FontKey(
        String familyId,
        UiFontFamilyRole role,
        UiFontWeight weight,
        FontStyle style
) {
    public FontKey {
        if (familyId == null || familyId.isBlank() || role == null || weight == null || style == null) {
            throw new IllegalArgumentException("Invalid font key");
        }
    }

    public static FontKey requested(UiFontFamilyRole role, UiFontWeight weight) {
        return requested(role, weight, FontStyle.NORMAL);
    }

    public static FontKey requested(UiFontFamilyRole role, UiFontWeight weight, FontStyle style) {
        if (role == null || weight == null || style == null) throw new NullPointerException("font key");
        String family = role == UiFontFamilyRole.TECHNICAL_MONO ? "projects:jetbrains-mono" : "projects:inter";
        return new FontKey(family, role, weight, style);
    }
}
