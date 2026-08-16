package io.github.gyai.projects.ui.runtime.typography;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Fixed-page shelf allocator for adapter glyph atlases; allocation failure is explicit and bounded. */
public final class GlyphAtlas {
    public record Allocation(int page, int x, int y, int width, int height, long generation) {
        public Allocation {
            if (page < 0 || x < 0 || y < 0 || width <= 0 || height <= 0 || generation < 0) {
                throw new IllegalArgumentException("Invalid atlas allocation");
            }
        }
    }

    private static final class Page {
        private int cursorX;
        private int cursorY;
        private int rowHeight;
    }

    private final int pageWidth;
    private final int pageHeight;
    private final int maxPages;
    private final int maxAllocations;
    private final List<Page> pages = new ArrayList<>();
    private final Map<GlyphKey, Allocation> allocations = new LinkedHashMap<>();
    private long generation;

    public GlyphAtlas(int pageWidth, int pageHeight, int maxPages, int maxAllocations) {
        if (pageWidth <= 0 || pageHeight <= 0 || maxPages <= 0 || maxAllocations <= 0) {
            throw new IllegalArgumentException("Invalid atlas bounds");
        }
        this.pageWidth = pageWidth;
        this.pageHeight = pageHeight;
        this.maxPages = maxPages;
        this.maxAllocations = maxAllocations;
    }

    public synchronized Optional<Allocation> allocate(GlyphKey key, int width, int height) {
        Objects.requireNonNull(key, "key");
        if (width <= 0 || height <= 0 || width >= pageWidth || height >= pageHeight) return Optional.empty();
        Allocation cached = allocations.get(key);
        if (cached != null) return Optional.of(cached);
        if (allocations.size() >= maxAllocations) return Optional.empty();
        for (int index = 0; index < pages.size(); index++) {
            Allocation allocation = tryAllocate(index, width, height);
            if (allocation != null) {
                allocations.put(key, allocation);
                return Optional.of(allocation);
            }
        }
        if (pages.size() >= maxPages) return Optional.empty();
        pages.add(new Page());
        Allocation allocation = tryAllocate(pages.size() - 1, width, height);
        if (allocation == null) throw new IllegalStateException("New atlas page could not allocate glyph");
        allocations.put(key, allocation);
        return Optional.of(allocation);
    }

    public synchronized Optional<Allocation> find(GlyphKey key) { return Optional.ofNullable(allocations.get(key)); }
    public synchronized void clear() { allocations.clear(); pages.clear(); generation++; }
    public synchronized int allocationCount() { return allocations.size(); }
    public synchronized int pageCount() { return pages.size(); }
    public int maxPages() { return maxPages; }
    public int maxAllocations() { return maxAllocations; }
    public int pageWidth() { return pageWidth; }
    public int pageHeight() { return pageHeight; }
    public synchronized long generation() { return generation; }

    private Allocation tryAllocate(int pageIndex, int width, int height) {
        Page page = pages.get(pageIndex);
        int paddedWidth = width + 1;
        int paddedHeight = height + 1;
        if (page.cursorX + paddedWidth > pageWidth) {
            page.cursorX = 0;
            page.cursorY += page.rowHeight;
            page.rowHeight = 0;
        }
        if (page.cursorY + paddedHeight > pageHeight) return null;
        Allocation allocation = new Allocation(pageIndex, page.cursorX, page.cursorY, width, height, generation);
        page.cursorX += paddedWidth;
        page.rowHeight = Math.max(page.rowHeight, paddedHeight);
        return allocation;
    }
}
