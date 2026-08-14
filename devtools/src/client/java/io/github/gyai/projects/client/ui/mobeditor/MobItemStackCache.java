package io.github.gyai.projects.client.ui.mobeditor;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Material ItemStacks are created once and reused by picker renders. */
public final class MobItemStackCache {
    private static final int MAXIMUM = 512;
    private static final Map<String, ItemStack> CACHE = new LinkedHashMap<>(32, .75f, true);

    private MobItemStackCache() { }

    public static synchronized ItemStack get(String material) {
        String key = material == null ? "" : material.strip().toUpperCase(Locale.ROOT);
        ItemStack cached = CACHE.get(key);
        if (cached != null) return cached.copy();
        Identifier id = Identifier.tryBuild("minecraft", key.toLowerCase(Locale.ROOT));
        var item = id == null ? null : BuiltInRegistries.ITEM.getValue(id);
        ItemStack created = item == null ? ItemStack.EMPTY : new ItemStack(item);
        if (CACHE.size() >= MAXIMUM) {
            String oldest = CACHE.keySet().iterator().next();
            CACHE.remove(oldest);
        }
        CACHE.put(key, created);
        return created.copy();
    }

    public static synchronized int size() {
        return CACHE.size();
    }

    public static synchronized void clear() {
        CACHE.clear();
    }
}
