package io.github.gyai.projects.ui.runtime;

public sealed interface IconSource permits ProceduralIcon, AtlasIcon {
    default boolean isProcedural() { return this instanceof ProceduralIcon; }

    default boolean isAtlasBacked() { return this instanceof AtlasIcon; }
}
