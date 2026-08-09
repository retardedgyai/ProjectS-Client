package io.github.gyai.projects.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class WarriorLoadoutClientState {
    private static WarriorLoadoutStatePayload.State state =
            WarriorLoadoutStatePayload.State.unavailable();
    private static Screen pendingParent;
    private static boolean openRequested;
    private static int revision;

    private WarriorLoadoutClientState() {
    }

    public static void receive(
            WarriorLoadoutStatePayload.State updated
    ) {
        state = updated;
        revision++;
        Minecraft client = Minecraft.getInstance();
        if (openRequested) {
            openRequested = false;
            if (updated.available()) {
                client.setScreen(new WarriorLoadoutScreen(pendingParent));
            } else {
                client.gui.setOverlayMessage(
                        net.minecraft.network.chat.Component.literal(
                                updated.reason().isBlank()
                                        ? "スキル装備を利用できません"
                                        : updated.reason()),
                        false);
            }
            pendingParent = null;
        }
    }

    public static boolean requestOpen(Screen parent) {
        if (!ClientPlayNetworking.canSend(
                WarriorLoadoutRequestPayload.TYPE)) {
            return false;
        }
        pendingParent = parent;
        openRequested = true;
        ClientPlayNetworking.send(new WarriorLoadoutRequestPayload(
                WarriorLoadoutRequestPayload.OPEN));
        return true;
    }

    public static void select(int slot, String skillId) {
        if (ClientPlayNetworking.canSend(
                WarriorLoadoutSelectPayload.TYPE)) {
            ClientPlayNetworking.send(
                    new WarriorLoadoutSelectPayload(slot, skillId));
        }
    }

    public static void reset() {
        if (ClientPlayNetworking.canSend(
                WarriorLoadoutRequestPayload.TYPE)) {
            ClientPlayNetworking.send(new WarriorLoadoutRequestPayload(
                    WarriorLoadoutRequestPayload.RESET));
        }
    }

    public static WarriorLoadoutStatePayload.State state() {
        return state;
    }

    public static int revision() {
        return revision;
    }

    public static boolean supported() {
        return ClientPlayNetworking.canSend(
                WarriorLoadoutRequestPayload.TYPE)
                && ClientPlayNetworking.canSend(
                WarriorLoadoutSelectPayload.TYPE);
    }
}
