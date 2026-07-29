package io.github.gyai.projects.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
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

    @Override
    public void onInitializeClient() {
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
        ClientPlayNetworking.registerGlobalReceiver(
                HudStatePayload.TYPE,
                (payload, context) -> ProjectSSkillHud.update(payload.state())
        );
        ClientPlayNetworking.registerGlobalReceiver(
                WarriorLoadoutStatePayload.TYPE,
                (payload, context) ->
                        WarriorLoadoutClientState.receive(payload.state())
        );
        ProjectSSkillHud.register();
        ProjectSScreenManager.register();

        // Minecraftの移動キーWとの衝突を避けるため、初期値はQ/E/R/F。
        // 設定 > 操作設定 > キー割り当て から自由に変更可能。
        skill1 = register("key.projects_client.skill_1", GLFW.GLFW_KEY_Q);
        skill2 = register("key.projects_client.skill_2", GLFW.GLFW_KEY_E);
        skill3 = register("key.projects_client.skill_3", GLFW.GLFW_KEY_R);
        ultimate = register("key.projects_client.ultimate", GLFW.GLFW_KEY_F);
        dodge = register("key.projects_client.dodge", GLFW.GLFW_KEY_LEFT_SHIFT);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
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
