package io.github.gyai.projects.client.beta.ui;

import io.github.gyai.projects.client.beta.BetaClientRuntime;
import io.github.gyai.projects.client.beta.BetaUiViewModels;

public final class BetaEnhancementScreen extends BetaStateScreen {
    public BetaEnhancementScreen() {
        super("ProjectS Enhancement", () -> BetaUiViewModels.enhancementScreen(
                BetaClientRuntime.stores().enhancement()));
    }
}
