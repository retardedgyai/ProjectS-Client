package io.github.gyai.projects.minecraft.adapter.icon;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.gyai.projects.ui.runtime.icon.ShellIconCatalog;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
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
        try (var input = resource.get().open()) {
            byte[] encoded = input.readAllBytes();
            if (!descriptor.expectedSha256().isBlank()
                    && !descriptor.expectedSha256().equalsIgnoreCase(sha256(encoded))) {
                return Optional.empty();
            }
            if (!manifestMatches(resourceManager, descriptor)) return Optional.empty();
            try (var image = NativeImage.read(new ByteArrayInputStream(encoded))) {
                if (image.getWidth() != descriptor.atlasWidth()
                        || image.getHeight() != descriptor.atlasHeight()) {
                    return Optional.empty();
                }
                if (descriptor.shellAtlas() && !hasAntialiasing(image, descriptor)) return Optional.empty();
                return Optional.of(MinecraftIconAtlasBinding.from(descriptor));
            }
        } catch (IOException | RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private static boolean manifestMatches(ResourceManager resourceManager,
                                           MinecraftIconAtlasDescriptor descriptor) throws IOException {
        var manifest = resourceManager.getResource(descriptor.manifest());
        if (manifest.isEmpty()) return false;
        String compact;
        try (var input = manifest.get().open()) {
            compact = new String(input.readAllBytes(), StandardCharsets.UTF_8)
                    .replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
        }
        String resource = descriptor.texture().getPath();
        String expectedHash = descriptor.expectedSha256().toLowerCase(Locale.ROOT);
        String dimensions = "[" + descriptor.atlasWidth() + "," + descriptor.atlasHeight() + "]";
        boolean provenance = !descriptor.shellAtlas()
                || (compact.contains("mockups/projects-client-shell/index.html")
                && compact.contains("\"sourcesha256\":")
                && compact.contains("\"inlinesvggeometry\":"));
        StringBuilder keys = new StringBuilder("\"keys\":[");
        for (int index = 0; index < ShellIconCatalog.keys().size(); index++) {
            if (index > 0) keys.append(',');
            keys.append('"').append(ShellIconCatalog.keys().get(index).id()).append('"');
        }
        keys.append(']');
        boolean shellContract = !descriptor.shellAtlas()
                || (compact.contains("\"schema\":\"projects-client-shell-renderer-assets-v1\"")
                && compact.contains("\"offlineruntime\":true")
                && compact.contains("\"dimensions\":[576,384]")
                && compact.contains("\"cellsize\":96")
                && compact.contains("\"columns\":6")
                && compact.contains("\"rows\":4")
                && compact.contains("\"supersample\":4")
                && compact.contains("\"antialiasedalphaonly\":true")
                && compact.contains(keys)
                && compact.contains("\"symbols\":[")
                && compact.contains("\"fragmentsha256\":\""));
        return compact.contains(resource.toLowerCase(Locale.ROOT))
                && compact.contains(dimensions)
                && (expectedHash.isBlank() || compact.contains(expectedHash))
                && provenance
                && shellContract
                && (compact.contains("\"runtime_downloads\":false")
                || compact.contains("\"runtimedownloads\":false"))
                && (compact.contains("\"third_party_artwork\":false")
                || compact.contains("\"thirdpartyartwork\":false"));
    }

    private static boolean hasAntialiasing(NativeImage image, MinecraftIconAtlasDescriptor descriptor) {
        int cells = descriptor.shellAtlas() ? ShellIconCatalog.keys().size()
                : descriptor.columns() * descriptor.rows();
        for (int index = 0; index < cells; index++) {
            int row = index / descriptor.columns();
            int column = index % descriptor.columns();
            boolean intermediate = false;
            boolean visible = false;
            int left = column * descriptor.cellSize();
            int top = row * descriptor.cellSize();
            for (int y = top; y < top + descriptor.cellSize(); y++) {
                for (int x = left; x < left + descriptor.cellSize(); x++) {
                    int alpha = (image.getPixel(x, y) >>> 24) & 0xFF;
                    visible |= alpha > 0;
                    intermediate |= alpha > 0 && alpha < 255;
                }
            }
            if (!visible || !intermediate) return false;
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
