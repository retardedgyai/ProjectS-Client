package io.github.gyai.projects.devtools.studio.assets;

/** Presentation-only search state; text input integration remains a host concern. */
public record StudioAssetSearchChrome(String query, boolean focused, String placeholder) {
    public StudioAssetSearchChrome {
        query = query == null ? "" : query;
        placeholder = placeholder == null || placeholder.isBlank() ? "検索" : placeholder;
    }

    public boolean hasQuery() { return !query.isBlank(); }

    public boolean active() { return focused || hasQuery(); }
}
