package io.github.gyai.projects.ui.runtime;

/** Namespaced icon identity independent of an atlas or procedural renderer. */
public record IconKey(String namespace, String path) {
    public IconKey {
        if (namespace == null || path == null || namespace.isBlank() || path.isBlank()
                || namespace.contains(" ") || path.contains(" ")) {
            throw new IllegalArgumentException("Icon key needs a non-blank namespace/path");
        }
    }

    public static IconKey of(String value) {
        if (value == null) throw new NullPointerException("value");
        int separator = value.indexOf(':');
        return separator < 1 ? new IconKey("projects", value)
                : new IconKey(value.substring(0, separator), value.substring(separator + 1));
    }

    public String id() { return namespace + ':' + path; }
}
