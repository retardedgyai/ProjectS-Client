package io.github.gyai.projects.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import io.github.gyai.projects.client.ui.theme.ProjectSThemeManager;
import io.github.gyai.projects.client.ui.render.ProjectSIconAtlas;
import io.github.gyai.projects.client.beta.BetaCapabilityAcknowledgementPayload;
import io.github.gyai.projects.client.beta.BetaCapabilityAdvertisementPayload;
import io.github.gyai.projects.client.beta.BetaClientRuntime;
import io.github.gyai.projects.client.beta.BetaCommandPayload;
import io.github.gyai.projects.client.beta.BetaProtocol;
import io.github.gyai.projects.client.beta.BetaStatePayload;
import io.github.gyai.projects.client.beta.ClientWorldLifecycleGuard;
import io.github.gyai.projects.client.beta.ui.BetaHudOverlay;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.lwjgl.glfw.GLFW;

public final class ProjectSClient implements ClientModInitializer {
    public static final String MOD_ID = "projects_client";

    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(MOD_ID, "combat")
    );

    private static KeyMapping skill1;
    private static KeyMapping skill2;
    private static KeyMapping skill3;
    private static KeyMapping ultimate;
    private static KeyMapping dodge;
    private boolean attackHeld;
    private static final ClientWorldLifecycleGuard WORLD_LIFECYCLE =
            new ClientWorldLifecycleGuard();

    @Override
    public void onInitializeClient() {
        ProjectSThemeManager.initialize(FabricLoader.getInstance().getConfigDir());
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(
                Identifier.fromNamespaceAndPath(MOD_ID, "icon_atlas_cache"),
                (ResourceManagerReloadListener) manager ->
                        ProjectSIconAtlas.invalidateAvailability());
        PayloadTypeRegistry.serverboundPlay().register(SkillInputPayload.TYPE, SkillInputPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(HudStatePayload.TYPE, HudStatePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(
                WarriorLoadoutRequestPayload.TYPE,
                WarriorLoadoutRequestPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(
                WarriorLoadoutSelectPayload.TYPE,
                WarriorLoadoutSelectPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(
                WarriorLoadoutStatePayload.TYPE,
                WarriorLoadoutStatePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(
                MonsterUiPayload.TYPE, MonsterUiPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(
                TelegraphPayload.TYPE, TelegraphPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(
                AbilityVfxPayload.TYPE, AbilityVfxPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(
                TelegraphHelloPayload.TYPE,
                TelegraphHelloPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(
                BetaCapabilityAdvertisementPayload.TYPE,
                BetaCapabilityAdvertisementPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(
                BetaCapabilityAcknowledgementPayload.TYPE,
                BetaCapabilityAcknowledgementPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(
                BetaStatePayload.TYPE,
                BetaStatePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(
                BetaCommandPayload.TYPE,
                BetaCommandPayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(
                HudStatePayload.TYPE,
                (payload, context) -> ProjectSSkillHud.update(payload.state())
        );
        ClientPlayNetworking.registerGlobalReceiver(
                WarriorLoadoutStatePayload.TYPE,
                (payload, context) ->
                        WarriorLoadoutClientState.receive(payload.state())
        );
        ClientPlayNetworking.registerGlobalReceiver(
                MonsterUiPayload.TYPE,
                (payload, context) -> context.client().execute(
                        () -> MonsterUiClientState.receive(
                                payload.update()))
        );
        ClientPlayNetworking.registerGlobalReceiver(
                TelegraphPayload.TYPE,
                (payload, context) -> context.client().execute(
                        () -> TelegraphClientState.receive(
                                  payload.update()))
        );
        ClientPlayNetworking.registerGlobalReceiver(
                AbilityVfxPayload.TYPE,
                (payload, context) -> context.client().execute(
                        () -> AbilityVfxClientState.receive(payload.decoded()))
        );
        ClientPlayNetworking.registerGlobalReceiver(
                BetaCapabilityAdvertisementPayload.TYPE,
                (payload, context) -> context.client().execute(() ->
                        BetaClientRuntime.receive(payload).ifPresent(acknowledgement -> {
                            if (ClientPlayNetworking.canSend(
                                    BetaCapabilityAcknowledgementPayload.TYPE)) {
                                ClientPlayNetworking.send(acknowledgement);
                            }
                        })));
        ClientPlayNetworking.registerGlobalReceiver(
                BetaStatePayload.TYPE,
                (payload, context) -> context.client().execute(
                        () -> BetaClientRuntime.receive(payload)));
        ClientPlayConnectionEvents.DISCONNECT.register(
                (handler, client) -> {
                    MonsterUiClientState.clear();
                    TelegraphClientState.clear();
                    AbilityVfxClientState.resetConnection();
                    BetaClientRuntime.disconnect();
                    WORLD_LIFECYCLE.reset();
                });
        ClientPlayConnectionEvents.JOIN.register(
                (handler, sender, client) ->
                        client.execute(() -> {
                            BetaClientRuntime.beginConnection();
                            WORLD_LIFECYCLE.reset();
                            AbilityVfxClientState.resetConnection();
                            if (ClientPlayNetworking.canSend(
                                    TelegraphHelloPayload.TYPE)) {
                                ClientPlayNetworking.send(
                                        new TelegraphHelloPayload());
                            }
                        }));
        ProjectSSkillHud.register();
        BetaHudOverlay.register();
        ProjectSScreenManager.register();
        MonsterUiRenderer.register();
        TelegraphRenderer.register();
        AbilityVfxRenderer.register();

        // Minecraftの移動キーWとの衝突を避けるため、初期値はQ/E/R/F。
        // 設定 > 操作設定 > キー割り当て から自由に変更可能。
        skill1 = register("key.projects_client.skill_1", GLFW.GLFW_KEY_Q);
        skill2 = register("key.projects_client.skill_2", GLFW.GLFW_KEY_E);
        skill3 = register("key.projects_client.skill_3", GLFW.GLFW_KEY_R);
        ultimate = register("key.projects_client.ultimate", GLFW.GLFW_KEY_F);
        dodge = register("key.projects_client.dodge", GLFW.GLFW_KEY_LEFT_SHIFT);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Object worldIdentity = client.level == null ? null : client.level.dimension();
            if (WORLD_LIFECYCLE.observe(worldIdentity)) {
                BetaClientRuntime.clearElementTarget();
            }
            MonsterUiClientState.tick(client);
            TelegraphClientState.tick(client);
            AbilityVfxClientState.tick(client);
            // チャット・インベントリ・各種画面を開いている間は誤発動させない。
            if (client.player == null || client.getConnection() == null || client.screen != null) {
                releaseAttack();
                return;
            }

            updateAttack(client);
            consumeAndSend(skill1, "SKILL_1");
            consumeAndSend(skill2, "SKILL_2");
            consumeAndSend(skill3, "SKILL_3");
            consumeAndSend(ultimate, "ULTIMATE");
            consumeAndSend(dodge, "DODGE");
        });
    }

    private void updateAttack(Minecraft client) {
        boolean currentlyHeld = client.options.keyAttack.isDown();
        if (currentlyHeld == attackHeld) {
            return;
        }

        attackHeld = currentlyHeld;
        sendInput(currentlyHeld ? "BOW_FIRE_START" : "BOW_FIRE_STOP");
    }

    private void releaseAttack() {
        if (!attackHeld) {
            return;
        }
        attackHeld = false;
        sendInput("BOW_FIRE_STOP");
    }

    private KeyMapping register(String translationKey, int glfwKey) {
        return KeyMappingHelper.registerKeyMapping(new KeyMapping(
                translationKey,
                InputConstants.Type.KEYSYM,
                glfwKey,
                CATEGORY
        ));
    }

    private void consumeAndSend(KeyMapping mapping, String inputId) {
        while (mapping.consumeClick()) {
            sendInput(inputId);
        }
    }

    public static boolean sendInput(String inputId) {
        if (ClientPlayNetworking.canSend(SkillInputPayload.TYPE)) {
            ClientPlayNetworking.send(new SkillInputPayload(inputId));
            return true;
        }
        return false;
    }

    public static boolean sendBetaCommand(
            BetaProtocol.Capability capability,
            long targetRevision,
            byte[] payload
    ) {
        var command = BetaClientRuntime.command(capability, targetRevision, payload);
        if (command.isEmpty() || !ClientPlayNetworking.canSend(BetaCommandPayload.TYPE)) {
            return false;
        }
        ClientPlayNetworking.send(command.orElseThrow());
        return true;
    }

    public static String resolveInputLabel(String input) {
        String first = keyName(skill1, "Q");
        String second = keyName(skill2, "E");
        String third = keyName(skill3, "R");
        String fourth = keyName(ultimate, "F");
        String dodgeKey = keyName(dodge, "左Shift");

        return switch (input) {
            case "Q" -> first;
            case "E" -> second;
            case "R" -> third;
            case "F" -> fourth;
            case "Q / E / R" -> first + " / " + second + " / " + third;
            case "Q → Q" -> first + " → " + first;
            case "Q → E" -> first + " → " + second;
            case "Q → R" -> first + " → " + third;
            case "E → Q" -> second + " → " + first;
            case "E → E" -> second + " → " + second;
            case "E → R" -> second + " → " + third;
            case "R → Q" -> third + " → " + first;
            case "R → E" -> third + " → " + second;
            case "R → R" -> third + " → " + third;
            case "左Shift" -> dodgeKey;
            case "Q / 右クリック" -> first + " / 右クリック";
            default -> input;
        };
    }

    private static String keyName(KeyMapping mapping, String fallback) {
        return mapping == null ? fallback : mapping.getTranslatedKeyMessage().getString();
    }
}
