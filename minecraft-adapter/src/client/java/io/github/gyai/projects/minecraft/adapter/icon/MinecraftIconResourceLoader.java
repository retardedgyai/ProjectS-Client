package io.github.gyai.projects.minecraft.adapter.icon;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.util.Optional;

/**
 * Client-resource audit for icon atlases. Decoding and dimension validation stay
 * in the adapter; a malformed resource simply disables atlas use.
 */
public final class MinecraftIconResourceLoader {
    public Optional<MinecraftIconAtlasBinding> inspect(
            ResourceManager resourceManager, MinecraftIconAtlasDescriptor descriptor
    ) {
        if (resourceManager == null || descriptor == null) throw new IllegalArgumentException("resourceManager/descriptor");
        var resource = resourceManager.getResource(descriptor.texture());
        if (resource.isEmpty()) return Optional.empty();
        try (var input = resource.get().open(); var image = NativeImage.read(input)) {
            if (image.getWidth() != descriptor.atlasWidth()
                    || image.getHeight() != descriptor.atlasHeight()) {
                return Optional.empty();
            }
            return Optional.of(MinecraftIconAtlasBinding.from(descriptor));
        } catch (IOException | RuntimeException ignored) {
            return Optional.empty();
        }
    }
}
