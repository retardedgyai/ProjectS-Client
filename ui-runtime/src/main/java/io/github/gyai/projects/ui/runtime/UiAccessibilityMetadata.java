package io.github.gyai.projects.ui.runtime;

/** Minimal narration/accessibility data, with no platform narration dependency. */
public record UiAccessibilityMetadata(
        UiAccessibilityRole role,
        String label,
        String value,
        boolean enabled,
        boolean selected,
        boolean focused
) {
    public UiAccessibilityMetadata {
        if (role == null || label == null || value == null) {
            throw new IllegalArgumentException("Accessibility role/label/value are required");
        }
    }

    public static UiAccessibilityMetadata of(UiAccessibilityRole role, String label) {
        return new UiAccessibilityMetadata(role, label, "", true, false, false);
    }

    public UiAccessibilityMetadata withEnabled(boolean next) {
        return new UiAccessibilityMetadata(role, label, value, next, selected, focused);
    }

    public UiAccessibilityMetadata withFocused(boolean next) {
        return new UiAccessibilityMetadata(role, label, value, enabled, selected, next);
    }

    public UiAccessibilityMetadata withSelected(boolean next) {
        return new UiAccessibilityMetadata(role, label, value, enabled, next, focused);
    }

    public UiAccessibilityMetadata withValue(String next) {
        return new UiAccessibilityMetadata(role, label, next, enabled, selected, focused);
    }
}
