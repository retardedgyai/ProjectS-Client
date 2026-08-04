package io.github.gyai.projects.client.beta.ui;

import io.github.gyai.projects.client.beta.BetaClientRuntime;
import io.github.gyai.projects.client.beta.BetaUiViewModels;

public final class BetaEquipmentDetailScreen extends BetaStateScreen {
    public BetaEquipmentDetailScreen() {
        super("ProjectS Equipment", () -> BetaUiViewModels.equipmentDetail(
                BetaClientRuntime.stores().equipment()));
    }
}
