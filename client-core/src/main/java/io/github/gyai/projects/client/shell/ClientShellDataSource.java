package io.github.gyai.projects.client.shell;

/** Internal seam for replacing local sample values with an immutable future snapshot source. */
@FunctionalInterface
public interface ClientShellDataSource {
    ClientShellDataSnapshot snapshot();
}
