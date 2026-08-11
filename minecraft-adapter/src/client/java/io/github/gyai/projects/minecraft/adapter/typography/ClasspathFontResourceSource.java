package io.github.gyai.projects.minecraft.adapter.typography;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/** Reads bundled fonts from the client classpath; it never downloads at runtime. */
public final class ClasspathFontResourceSource implements FontResourceSource {
    private final ClassLoader loader;

    public ClasspathFontResourceSource() {
        this(Thread.currentThread().getContextClassLoader() == null
                ? ClasspathFontResourceSource.class.getClassLoader()
                : Thread.currentThread().getContextClassLoader());
    }

    public ClasspathFontResourceSource(ClassLoader loader) {
        this.loader = Objects.requireNonNull(loader, "loader");
    }

    @Override
    public byte[] read(String assetPath) throws IOException {
        if (assetPath == null || assetPath.isBlank() || assetPath.startsWith("/")) {
            throw new IllegalArgumentException("assetPath");
        }
        try (InputStream stream = loader.getResourceAsStream(assetPath)) {
            if (stream == null) throw new IOException("Missing bundled font resource: " + assetPath);
            return stream.readAllBytes();
        }
    }
}
