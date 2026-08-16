package io.github.gyai.projects.minecraft.adapter.icon;

import io.github.gyai.projects.ui.runtime.icon.IconAtlasCache;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.List;
import java.util.Optional;

/** Bounded, reloadable adapter cache for validated Minecraft icon atlas bindings. */
public final class MinecraftIconAtlasStore implements AutoCloseable {
    private final IconAtlasCache<MinecraftIconAtlasBinding> cache;
    private final MinecraftIconResourceLoader loader;

    public MinecraftIconAtlasStore() { this(2, new MinecraftIconResourceLoader()); }

    public MinecraftIconAtlasStore(int capacity) { this(capacity, new MinecraftIconResourceLoader()); }

    public MinecraftIconAtlasStore(int capacity, MinecraftIconResourceLoader loader) {
        if (loader == null) throw new NullPointerException("loader");
        this.cache = new IconAtlasCache<>(capacity);
        this.loader = loader;
    }

    public Optional<MinecraftIconAtlasBinding> resolve(
            ResourceManager resourceManager, MinecraftIconAtlasDescriptor descriptor
    ) {
        if (resourceManager == null || descriptor == null) throw new IllegalArgumentException("resourceManager/descriptor");
        Optional<MinecraftIconAtlasBinding> existing = cache.get(descriptor.atlasId());
        if (existing.isPresent()) return existing;
        Optional<MinecraftIconAtlasBinding> loaded = loader.inspect(resourceManager, descriptor);
        loaded.ifPresent(binding -> cache.put(descriptor.atlasId(), binding));
        return loaded;
    }

    /** Call from the client resource-reload seam before the next frame. */
    public long onResourceReload() { return cache.reload(); }

    public int size() { return cache.size(); }

    public int capacity() { return cache.capacity(); }

    public long revision() { return cache.generation(); }

    public List<String> cachedAtlasIds() { return cache.cachedIds(); }

    @Override
    public void close() { cache.close(); }
}
