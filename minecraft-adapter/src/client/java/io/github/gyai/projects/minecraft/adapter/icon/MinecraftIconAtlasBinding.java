package io.github.gyai.projects.minecraft.adapter.icon;

import io.github.gyai.projects.ui.runtime.icon.IconAtlasRegion;
import net.minecraft.resources.Identifier;

/** Validated adapter-side binding passed to the renderer only after resource inspection. */
public record MinecraftIconAtlasBinding(
        String atlasId,
        Identifier texture,
        int atlasWidth,
        int atlasHeight
) {
    public MinecraftIconAtlasBinding {
        if (atlasId == null || atlasId.isBlank() || texture == null
                || atlasWidth <= 0 || atlasHeight <= 0) {
            throw new IllegalArgumentException("Invalid icon atlas binding");
        }
    }

    public static MinecraftIconAtlasBinding from(MinecraftIconAtlasDescriptor descriptor) {
        if (descriptor == null) throw new NullPointerException("descriptor");
        return new MinecraftIconAtlasBinding(descriptor.atlasId(), descriptor.texture(),
                descriptor.atlasWidth(), descriptor.atlasHeight());
    }

    public boolean accepts(IconAtlasRegion region) {
        return region != null && region.atlasWidth() == atlasWidth
                && region.atlasHeight() == atlasHeight && region.isValid();
    }
}
