package io.github.gyai.projects.client;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public final class TelegraphRegistry<T> {
    private final Map<UUID, T> active = new HashMap<>();
    private final Consumer<UUID> removeCache;
    private final Runnable clearCaches;

    public TelegraphRegistry(
            Consumer<UUID> removeCache,
            Runnable clearCaches
    ) {
        this.removeCache = removeCache;
        this.clearCaches = clearCaches;
    }

    public T get(UUID id) {
        return active.get(id);
    }

    public void put(UUID id, T value) {
        active.put(id, value);
    }

    public boolean remove(UUID id) {
        T removed = active.remove(id);
        removeCache.accept(id);
        return removed != null;
    }

    public void clear() {
        active.clear();
        clearCaches.run();
    }

    public int size() {
        return active.size();
    }

    public Collection<T> values() {
        return List.copyOf(active.values());
    }
}
