package io.github.gyai.projects.devtools.studio.inspector;

/** Presentation-only label/value pair; it is not an editor property schema. */
public record StudioInspectorValue(String label, String value, boolean technical) {
    public StudioInspectorValue {
        if (label == null || label.isBlank() || value == null || value.isBlank()) {
            throw new IllegalArgumentException("label/value");
        }
    }

    public static StudioInspectorValue text(String label, String value) {
        return new StudioInspectorValue(label, value, false);
    }

    public static StudioInspectorValue technical(String label, String value) {
        return new StudioInspectorValue(label, value, true);
    }
}
