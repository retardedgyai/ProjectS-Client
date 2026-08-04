package io.github.gyai.projects.client.beta.ui;

import io.github.gyai.projects.client.beta.BetaClientRuntime;
import io.github.gyai.projects.client.beta.BetaUiViewModels;

public final class BetaCraftingScreen extends BetaStateScreen {
    public BetaCraftingScreen() {
        super("ProjectS Crafting", () -> BetaUiViewModels.craftingScreen(
                BetaClientRuntime.stores().crafting()));
    }
}
