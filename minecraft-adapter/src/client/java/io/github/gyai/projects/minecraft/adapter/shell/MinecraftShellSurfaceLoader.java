package io.github.gyai.projects.minecraft.adapter.shell;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Optional;

/** Offline resource audit for the antialiased shell fill/border/shadow mask atlas. */
public final class MinecraftShellSurfaceLoader {
    public Optional<MinecraftShellSurfaceBinding> inspect(ResourceManager resourceManager) {
        if (resourceManager == null) throw new IllegalArgumentException("resourceManager");
        var resource = resourceManager.getResource(MinecraftShellSurfaceDescriptor.texture());
        if (resource.isEmpty()) return Optional.empty();
        try (var input = resource.get().open()) {
            byte[] encoded = input.readAllBytes();
            if (!MinecraftShellSurfaceDescriptor.SHA256.equalsIgnoreCase(sha256(encoded))) {
                return Optional.empty();
            }
            if (!manifestMatches(resourceManager)) return Optional.empty();
            try (var image = NativeImage.read(new ByteArrayInputStream(encoded))) {
                if (image.getWidth() != MinecraftShellSurfaceDescriptor.ATLAS_WIDTH
                        || image.getHeight() != MinecraftShellSurfaceDescriptor.ATLAS_HEIGHT
                        || !hasAntialiasedMasks(image)) {
                    return Optional.empty();
                }
            }
            return Optional.of(new MinecraftShellSurfaceBinding(
                    MinecraftShellSurfaceDescriptor.texture(),
                    MinecraftShellSurfaceDescriptor.ATLAS_WIDTH,
                     MinecraftShellSurfaceDescriptor.ATLAS_HEIGHT,
                     MinecraftShellSurfaceDescriptor.TILE_SIZE,
                     MinecraftShellSurfaceDescriptor.LOGICAL_TILE_SIZE,
                     MinecraftShellSurfaceDescriptor.SOURCE_CAP,
                     MinecraftShellSurfaceDescriptor.SOURCE_RADIUS,
                     MinecraftShellSurfaceDescriptor.SHADOW_BLUR_RADIUS,
                     MinecraftShellSurfaceDescriptor.AVAILABLE_BORDER_WIDTHS));
        } catch (IOException | RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private static boolean manifestMatches(ResourceManager resourceManager) throws IOException {
        var manifest = resourceManager.getResource(MinecraftShellSurfaceDescriptor.manifest());
        if (manifest.isEmpty()) return false;
        String compact;
        try (var input = manifest.get().open()) {
            compact = new String(input.readAllBytes(), StandardCharsets.UTF_8)
                    .replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
        }
        return compact.contains("shell_surface_masks_4x.png")
                && compact.contains("[3200,320]")
                && compact.contains("\"schema\":\"projects-client-shell-renderer-assets-v1\"")
                && compact.contains("\"offlineruntime\":true")
                && compact.contains("\"columns\":10")
                && compact.contains("\"tilesize\":320")
                && compact.contains("\"logicaltilesize\":80.0")
                && compact.contains("\"borderwidths\":[1,2,3,4,5,6,7,8]")
                && compact.contains("\"tilelayout\":{\"fill\":0,\"borderfirstcolumn\":1,\"shadow\":9}")
                && compact.contains(MinecraftShellSurfaceDescriptor.SHA256)
                && compact.contains("mockups/projects-client-shell/index.html")
                && compact.contains("\"sourcesha256\":")
                && compact.contains("\"antialiasedalphaonly\":true")
                && compact.contains("\"sourcecell\":80")
                && compact.contains("\"sourcecap\":32")
                && compact.contains("\"sourceradius\":30")
                && compact.contains("\"shadowblurradius\":8")
                && compact.contains("\"borderwidths\":[1,2,3,4,5,6,7,8]")
                && compact.contains("\"runtimedownloads\":false")
                && compact.contains("\"thirdpartyartwork\":false");
    }

    private static boolean hasAntialiasedMasks(NativeImage image) {
        for (int kind = 0; kind < MinecraftShellSurfaceDescriptor.TILE_COLUMNS; kind++) {
            int left = kind * MinecraftShellSurfaceDescriptor.TILE_SIZE;
            boolean visible = false;
            boolean transparent = false;
            boolean intermediate = false;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = left; x < left + MinecraftShellSurfaceDescriptor.TILE_SIZE; x++) {
                    int alpha = (image.getPixel(x, y) >>> 24) & 0xFF;
                    visible |= alpha > 0;
                    transparent |= alpha == 0;
                    intermediate |= alpha > 0 && alpha < 255;
                }
            }
            if (!visible || !transparent || !intermediate) return false;
        }
        return true;
    }

    private static String sha256(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte value : digest) result.append(String.format("%02x", value));
            return result.toString();
        } catch (java.security.NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 unavailable", error);
        }
    }
}
