package io.github.gyai.projects.ui.runtime.typography;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/** Bounded access-ordered glyph metrics cache. It never grows past its configured capacity. */
public final class GlyphCache {
    private final int capacity;
    private final LinkedHashMap<GlyphKey, GlyphMetrics> entries;

    public GlyphCache(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity");
        this.capacity = capacity;
        this.entries = new LinkedHashMap<>(capacity, .75f, true);
    }

    public synchronized GlyphMetrics get(GlyphKey key) { return entries.get(key); }

    public synchronized GlyphMetrics getOrCompute(GlyphKey key, Supplier<GlyphMetrics> factory) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(factory, "factory");
        GlyphMetrics existing = entries.get(key);
        if (existing != null) return existing;
        GlyphMetrics created = Objects.requireNonNull(factory.get(), "factory returned null");
        entries.put(key, created);
        trim();
        return created;
    }

    public synchronized void put(GlyphKey key, GlyphMetrics metrics) {
        entries.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(metrics, "metrics"));
        trim();
    }

    public synchronized void clear() { entries.clear(); }
    public synchronized int size() { return entries.size(); }
    public int capacity() { return capacity; }
    public synchronized boolean contains(GlyphKey key) { return entries.containsKey(key); }
    public synchronized Map<GlyphKey, GlyphMetrics> snapshot() { return Map.copyOf(entries); }

    private void trim() {
        while (entries.size() > capacity) entries.remove(entries.keySet().iterator().next());
    }
}
