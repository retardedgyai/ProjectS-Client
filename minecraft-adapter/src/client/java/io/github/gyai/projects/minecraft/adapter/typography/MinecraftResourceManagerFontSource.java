package io.github.gyai.projects.minecraft.adapter.typography;

import io.github.gyai.projects.minecraft.adapter.MinecraftResourceBridge;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.util.Objects;

/** Offline font source backed by the active Minecraft resource manager. */
public final class MinecraftResourceManagerFontSource implements FontResourceSource {
    private final ResourceManager resourceManager;
    private final MinecraftResourceBridge bridge = new MinecraftResourceBridge();

    public MinecraftResourceManagerFontSource(ResourceManager resourceManager) {
        this.resourceManager = Objects.requireNonNull(resourceManager, "resourceManager");
    }

    @Override
    public byte[] read(String assetPath) throws IOException {
        var resource = resourceManager.getResource(bridge.resolveAsset(assetPath));
        if (resource.isEmpty()) throw new IOException("Missing bundled font resource: " + assetPath);
        try (var input = resource.orElseThrow().open()) {
            return input.readAllBytes();
        }
    }
}
