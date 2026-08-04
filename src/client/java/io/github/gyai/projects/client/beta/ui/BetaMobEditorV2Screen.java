package io.github.gyai.projects.client.beta.ui;

import io.github.gyai.projects.client.beta.BetaClientRuntime;
import io.github.gyai.projects.client.beta.BetaUiViewModels;

public final class BetaMobEditorV2Screen extends BetaStateScreen {
    public BetaMobEditorV2Screen() {
        super("ProjectS Mob Editor v2", () -> BetaUiViewModels.mobEditorV2Screen(
                BetaClientRuntime.stores().mobEditorV2()));
    }
}
