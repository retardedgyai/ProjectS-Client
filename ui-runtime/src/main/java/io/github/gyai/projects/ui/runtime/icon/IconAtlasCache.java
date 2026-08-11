package io.github.gyai.projects.ui.runtime.icon;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/** Bounded LRU cache with explicit reload invalidation and close-aware resource release. */
public final class IconAtlasCache<T> implements AutoCloseable {
    private final int capacity;
    private final LinkedHashMap<String, T> entries = new LinkedHashMap<>(16, .75f, true);
    private long generation;
    private boolean closed;

    public IconAtlasCache(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity");
        this.capacity = capacity;
    }

    public synchronized int capacity() { return capacity; }

    public synchronized int size() { return entries.size(); }

    public synchronized long generation() { return generation; }

    public synchronized long revision() { return generation; }

    public synchronized boolean isClosed() { return closed; }

    public synchronized boolean contains(String id) {
        return id != null && entries.containsKey(id);
    }

    public synchronized Optional<T> get(String id) {
        if (closed || id == null) return Optional.empty();
        return Optional.ofNullable(entries.get(id));
    }

    public synchronized T getOrLoad(String id, Supplier<? extends T> loader) {
        requireId(id);
        Objects.requireNonNull(loader, "loader");
        ensureOpen();
        T existing = entries.get(id);
        if (existing != null) return existing;
        T loaded = Objects.requireNonNull(loader.get(), "loader returned null");
        putInternal(id, loaded);
        return loaded;
    }

    public synchronized void put(String id, T value) {
        requireId(id);
        Objects.requireNonNull(value, "value");
        ensureOpen();
        putInternal(id, value);
    }

    public synchronized boolean invalidate(String id) {
        if (id == null) return false;
        T removed = entries.remove(id);
        if (removed == null) return false;
        release(removed);
        generation++;
        return true;
    }

    /** Clears every cached atlas and increments the reload generation. */
    public synchronized long reload() {
        releaseAll();
        generation++;
        return generation;
    }

    public synchronized List<String> cachedIds() {
        return List.copyOf(new ArrayList<>(entries.keySet()));
    }

    public synchronized void clear() { releaseAll(); }

    @Override
    public synchronized void close() {
        if (closed) return;
        releaseAll();
        closed = true;
        generation++;
    }

    private void putInternal(String id, T value) {
        T previous = entries.put(id, value);
        if (previous != null && previous != value) release(previous);
        while (entries.size() > capacity) {
            String eldestId = entries.keySet().iterator().next();
            T eldest = entries.remove(eldestId);
            release(eldest);
        }
    }

    private void releaseAll() {
        for (T value : entries.values()) release(value);
        entries.clear();
    }

    private static void release(Object value) {
        if (!(value instanceof AutoCloseable closeable)) return;
        try {
            closeable.close();
        } catch (Exception ignored) {
            // A reload must invalidate the cache even if one platform resource fails to close.
        }
    }

    private void ensureOpen() {
        if (closed) throw new IllegalStateException("Icon atlas cache is closed");
    }

    private static void requireId(String id) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id");
    }
}
