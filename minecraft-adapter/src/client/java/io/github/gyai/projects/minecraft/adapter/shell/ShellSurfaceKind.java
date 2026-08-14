package io.github.gyai.projects.minecraft.adapter.shell;

/** Alpha-mask tile selected by the resource-backed Client Shell surface renderer. */
public enum ShellSurfaceKind {
    FILL(0),
    /** Border source columns are selected from binding metadata by requested width. */
    BORDER(-1),
    SHADOW(MinecraftShellSurfaceDescriptor.SHADOW_COLUMN);

    private final int atlasColumn;

    ShellSurfaceKind(int atlasColumn) { this.atlasColumn = atlasColumn; }

    public int atlasColumn() { return atlasColumn; }
}
