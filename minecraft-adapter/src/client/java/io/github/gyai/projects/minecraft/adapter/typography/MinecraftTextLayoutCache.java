package io.github.gyai.projects.minecraft.adapter.typography;

import io.github.gyai.projects.ui.runtime.TextStyle;
import io.github.gyai.projects.ui.runtime.typography.TextLayout;
import io.github.gyai.projects.ui.runtime.typography.TextLayoutOptions;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/** Bounded generation-aware layout cache for the Minecraft render boundary. */
public final class MinecraftTextLayoutCache {
    public static final int DEFAULT_CAPACITY = 256;

    private record Key(String text, TextStyle style, TextLayoutOptions options, long generation) {
        private Key {
            Objects.requireNonNull(text, "text");
            Objects.requireNonNull(style, "style");
            Objects.requireNonNull(options, "options");
            if (generation < 0) throw new IllegalArgumentException("generation");
        }
    }

    private final int capacity;
    private final LinkedHashMap<Key, TextLayout> entries;
    private long generation;
    private long hits;
    private long misses;

    public MinecraftTextLayoutCache() { this(DEFAULT_CAPACITY); }

    public MinecraftTextLayoutCache(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity");
        this.capacity = capacity;
        entries = new LinkedHashMap<>(capacity, .75f, true);
    }

    public synchronized TextLayout getOrCompute(String text, TextStyle style,
                                                 TextLayoutOptions options, long nextGeneration,
                                                 Supplier<TextLayout> factory) {
        Objects.requireNonNull(factory, "factory");
        invalidateForGeneration(nextGeneration);
        Key key = new Key(text, style, options, nextGeneration);
        TextLayout cached = entries.get(key);
        if (cached != null) {
            hits++;
            return cached;
        }
        misses++;
        TextLayout created = Objects.requireNonNull(factory.get(), "factory returned null");
        if (created.generation() != nextGeneration) {
            throw new IllegalArgumentException("layout generation mismatch");
        }
        entries.put(key, created);
        while (entries.size() > capacity) entries.remove(entries.keySet().iterator().next());
        return created;
    }

    public synchronized void invalidateForGeneration(long nextGeneration) {
        if (nextGeneration < 0) throw new IllegalArgumentException("generation");
        if (generation != nextGeneration) {
            entries.clear();
            generation = nextGeneration;
        }
    }

    public synchronized void clear() { entries.clear(); }
    public int capacity() { return capacity; }
    public synchronized int size() { return entries.size(); }
    public synchronized long generation() { return generation; }
    public synchronized long hits() { return hits; }
    public synchronized long misses() { return misses; }

    public synchronized void resetCounters() {
        hits = 0;
        misses = 0;
    }
}
