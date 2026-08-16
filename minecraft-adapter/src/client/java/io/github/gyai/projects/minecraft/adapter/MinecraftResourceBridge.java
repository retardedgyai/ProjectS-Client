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

    /** Converts a bundled classpath-style asset path to a Minecraft resource identity. */
    public Identifier resolveAsset(String assetPath) {
        if (assetPath == null || assetPath.isBlank() || assetPath.contains("..")) {
            throw new IllegalArgumentException("assetPath");
        }
        String prefix = "assets/projects_client/";
        if (!assetPath.startsWith(prefix)) {
            throw new IllegalArgumentException("ProjectS asset path must start with " + prefix);
        }
        return Identifier.fromNamespaceAndPath("projects_client", assetPath.substring(prefix.length()));
    }
}
