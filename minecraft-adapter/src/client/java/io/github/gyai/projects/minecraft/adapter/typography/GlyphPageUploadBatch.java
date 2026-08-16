package io.github.gyai.projects.minecraft.adapter.typography;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** Small bounded dirty-page accumulator shared by the GPU glyph store and its tests. */
final class GlyphPageUploadBatch {
    private final int maxPages;
    private final LinkedHashSet<Integer> dirtyPages = new LinkedHashSet<>();
    private long flushCount;
    private long uploadedPageCount;

    GlyphPageUploadBatch(int maxPages) {
        if (maxPages <= 0) throw new IllegalArgumentException("maxPages");
        this.maxPages = maxPages;
    }

    void markDirty(int page) {
        if (page < 0 || page >= maxPages) throw new IllegalArgumentException("page");
        dirtyPages.add(page);
    }

    List<Integer> drain() {
        List<Integer> pages = new ArrayList<>(dirtyPages);
        dirtyPages.clear();
        if (!pages.isEmpty()) {
            flushCount++;
            uploadedPageCount += pages.size();
        }
        return List.copyOf(pages);
    }

    int dirtyPageCount() { return dirtyPages.size(); }
    long flushCount() { return flushCount; }
    long uploadedPageCount() { return uploadedPageCount; }
    void clear() { dirtyPages.clear(); }
}
