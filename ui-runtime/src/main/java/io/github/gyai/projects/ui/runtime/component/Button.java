package io.github.gyai.projects.ui.runtime.component;

import io.github.gyai.projects.ui.runtime.UiButton;
import io.github.gyai.projects.ui.runtime.UiButtonState;
import io.github.gyai.projects.ui.runtime.UiMaterialTier;
import io.github.gyai.projects.ui.runtime.UiRect;

/** Stage 2 name for the backward-compatible pure UiButton control. */
public class Button extends UiButton {
    public Button(String id, UiRect bounds, String label) {
        super(id, bounds, label);
    }

    public Button(String id, UiRect bounds, String label, Runnable action) {
        super(id, bounds, label, action);
    }

    public Button(String id, UiRect bounds, String label, UiMaterialTier materialTier,
                  Runnable action) {
        super(id, bounds, label, action);
        setMaterialTier(materialTier);
    }

    @Override
    public Button setLabel(String next) {
        super.setLabel(next);
        return this;
    }

    @Override
    public Button setMaterialTier(UiMaterialTier next) {
        super.setMaterialTier(next);
        return this;
    }

    @Override
    public Button setRadius(double next) {
        super.setRadius(next);
        return this;
    }

    @Override
    public Button setSelected(boolean next) {
        super.setSelected(next);
        return this;
    }

    /** Semantic alias useful to component consumers that use the selected state as a mode. */
    public UiButtonState componentState() { return state(); }
}
