package io.github.gyai.projects.devtools;

import io.github.gyai.projects.client.BalanceClientState;
import io.github.gyai.projects.client.BalanceRequestPayload;
import io.github.gyai.projects.client.BalanceStatePayload;
import io.github.gyai.projects.client.BalanceUpdatePayload;
import io.github.gyai.projects.client.BalanceActionPayload;
import io.github.gyai.projects.client.MobEditorClientState;
import io.github.gyai.projects.client.MobEditorRequestPayload;
import io.github.gyai.projects.client.MobEditorStatePayload;
import io.github.gyai.projects.client.MobEditorV2RequestPayload;
import io.github.gyai.projects.client.MobEditorV2StatePayload;
import io.github.gyai.projects.client.beta.BetaClientRuntime;
import io.github.gyai.projects.client.beta.BetaProtocol;
import io.github.gyai.projects.client.menu.ProjectSMenuExtension;
import io.github.gyai.projects.client.menu.ProjectSMenuExtensions;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import io.github.gyai.projects.devtools.skillvfx.ui.SkillVfxWorldPreviewController;
import io.github.gyai.projects.devtools.skillvfx.ui.SkillVfx3dAuthoringWorldRenderer;

/** Optional developer entrypoint. It adds tools, never grants server permission. */
public final class ProjectSDevTools implements ClientModInitializer {
    @Override public void onInitializeClient() {
        SkillVfxWorldPreviewController.register();
        SkillVfx3dAuthoringWorldRenderer.register();
        BetaClientRuntime.enableCapability(BetaProtocol.Capability.MOB_EDITOR_V2);
        PayloadTypeRegistry.serverboundPlay().register(BalanceRequestPayload.TYPE, BalanceRequestPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(BalanceUpdatePayload.TYPE, BalanceUpdatePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(BalanceActionPayload.TYPE, BalanceActionPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(BalanceStatePayload.TYPE, BalanceStatePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(MobEditorRequestPayload.TYPE, MobEditorRequestPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(MobEditorStatePayload.TYPE, MobEditorStatePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(MobEditorV2RequestPayload.TYPE, MobEditorV2RequestPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(MobEditorV2StatePayload.TYPE, MobEditorV2StatePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SkillEditorRequestPayload.TYPE, SkillEditorRequestPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SkillEditorStatePayload.TYPE, SkillEditorStatePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SkillEditorRequestPayloadV2.TYPE, SkillEditorRequestPayloadV2.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SkillEditorStatePayloadV2.TYPE, SkillEditorStatePayloadV2.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SkillEditorRequestPayloadV3.TYPE, SkillEditorRequestPayloadV3.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SkillEditorStatePayloadV3.TYPE, SkillEditorStatePayloadV3.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(BalanceStatePayload.TYPE, (payload, context) -> BalanceClientState.receive(payload.state()));
        ClientPlayNetworking.registerGlobalReceiver(MobEditorStatePayload.TYPE, (payload, context) -> {
            long generation = MobEditorClientState.captureGeneration();
            context.client().execute(() -> MobEditorClientState.receive(payload.state(), generation));
        });
        ClientPlayNetworking.registerGlobalReceiver(MobEditorV2StatePayload.TYPE, (payload, context) -> {
            long generation = MobEditorClientState.captureGeneration();
            context.client().execute(() -> MobEditorClientState.receiveV2(payload.state(), generation));
        });
        ClientPlayNetworking.registerGlobalReceiver(SkillEditorStatePayload.TYPE, (payload, context) -> context.client().execute(() -> SkillEditorClientState.receive(payload.state())));
        ClientPlayNetworking.registerGlobalReceiver(SkillEditorStatePayloadV2.TYPE, (payload, context) -> context.client().execute(() -> SkillEditorClientState.receiveV2(payload.state())));
        ClientPlayNetworking.registerGlobalReceiver(SkillEditorStatePayloadV3.TYPE, (payload, context) -> context.client().execute(() -> SkillEditorClientState.receiveV3(payload.state())));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> { BalanceClientState.reset(); MobEditorClientState.reset(); SkillEditorClientState.reset(); });
        ProjectSMenuExtensions.register(new ProjectSMenuExtension("projects.devtools", "Developer Tools", "開発者向け機能（サーバー権限が必要です）", () -> true, ProjectSDevToolsMenuScreen::open));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level == null
                    && client.screen instanceof net.minecraft.client.gui.screens.TitleScreen) {
                client.setScreen(new ProjectSDeveloperLaunchScreen());
            }
        });
    }
}
