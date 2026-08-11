package io.github.gyai.projects.ui.runtime;

/** Platform port for executing a pure draw list. */
@FunctionalInterface
public interface UiRenderBackend {
    void render(UiDrawList drawList);
}
