package io.github.gyai.projects.minecraft.adapter;

import io.github.gyai.projects.ui.runtime.IconKey;
import net.minecraft.resources.Identifier;

/** Resource identity bridge; loading policy remains outside the pure UI runtime. */
public final class MinecraftResourceBridge {
    public Identifier resolve(IconKey key) {
        if (key == null) throw new NullPointerException("key");
        return Identifier.fromNamespaceAndPath(key.namespace(), key.path());
    }

    public Identifier resolve(String namespace, String path) {
        return Identifier.fromNamespaceAndPath(namespace, path);
    }
}
