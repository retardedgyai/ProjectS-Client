package io.github.gyai.projects.minecraft.adapter.typography;

import java.io.IOException;

/** Offline resource seam. Integration supplies a Minecraft resource-manager backed implementation. */
@FunctionalInterface
public interface FontResourceSource {
    byte[] read(String assetPath) throws IOException;
}
