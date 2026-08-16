package io.github.gyai.projects.client.beta.ui;

import io.github.gyai.projects.client.beta.BetaClientRuntime;
import io.github.gyai.projects.devtools.BetaMobEditorV2ViewModel;

public final class BetaMobEditorV2Screen extends BetaStateScreen {
    public BetaMobEditorV2Screen() {
        super("ProjectS Mob Editor v2", () -> BetaMobEditorV2ViewModel.panel(
                BetaClientRuntime.stores().document(
                        io.github.gyai.projects.client.beta.BetaProtocol.Capability.MOB_EDITOR_V2)));
    }
}
