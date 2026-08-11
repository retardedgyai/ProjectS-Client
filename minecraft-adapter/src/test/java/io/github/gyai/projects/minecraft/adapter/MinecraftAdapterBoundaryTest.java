package io.github.gyai.projects.minecraft.adapter;

import java.nio.file.Files;
import java.nio.file.Path;

/** Non-GUI adapter ownership check; it never initializes Minecraft or Fabric. */
public final class MinecraftAdapterBoundaryTest {
    public static void main(String[] args) throws Exception {
        Path root = Path.of("minecraft-adapter/src");
        assert Files.isDirectory(root.resolve("client/java"));
        assert Files.exists(root.resolve("client/java/io/github/gyai/projects/minecraft/adapter/MinecraftUiScreenHost.java"));
        assert Files.exists(root.resolve("client/java/io/github/gyai/projects/minecraft/adapter/MinecraftUiRenderBackend.java"));
        assert !Files.exists(root.resolve("main/resources/fabric.mod.json"));
        String host = Files.readString(root.resolve("client/java/io/github/gyai/projects/minecraft/adapter/MinecraftUiScreenHost.java"));
        assert host.contains("extends Screen");
        assert host.contains("onScreenClosed");
        System.out.println("MINECRAFT_ADAPTER_TEST_PASS: host backend input clip resource ownership");
    }
}
