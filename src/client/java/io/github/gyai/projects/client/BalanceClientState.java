package io.github.gyai.projects.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class BalanceClientState {
    private static BalanceStatePayload.State state =
            BalanceStatePayload.State.unavailable("");
    private static Screen pendingParent;
    private static boolean openRequested;
    private static boolean communicating;
    private static boolean received;
    private static int localRevision;

    private BalanceClientState() {
    }

    public static void receive(BalanceStatePayload.State updated) {
        state = updated;
        received = updated.supported();
        communicating = updated.message().endsWith("中...");
        localRevision++;
        Minecraft client = Minecraft.getInstance();
        if (openRequested) {
            openRequested = false;
            if (updated.supported() && updated.permitted()) {
                client.setScreen(new BalanceTuningScreen(pendingParent));
            } else if (client.player != null) {
                client.gui.setOverlayMessage(Component.literal(
                        updated.message().isBlank()
                                ? "バランス調整を利用できません"
                                : updated.message()), false);
            }
            pendingParent = null;
        }
    }

    public static void probe() {
        if (supportedByConnection() && !communicating) {
            communicating = true;
            ClientPlayNetworking.send(new BalanceRequestPayload());
        }
    }

    public static boolean requestOpen(Screen parent) {
        if (!supportedByConnection()) return false;
        pendingParent = parent;
        openRequested = true;
        communicating = true;
        ClientPlayNetworking.send(new BalanceRequestPayload());
        return true;
    }

    public static void apply(List<BalanceUpdatePayload.Edit> edits) {
        if (!canEdit()) return;
        communicating = true;
        ClientPlayNetworking.send(new BalanceUpdatePayload(
                state.revision(), List.copyOf(edits)));
    }

    public static void action(int action, int target, String id) {
        if (!canEdit()) return;
        communicating = true;
        ClientPlayNetworking.send(new BalanceActionPayload(
                state.revision(), action, target, id == null ? "" : id));
    }

    public static boolean supportedByConnection() {
        return ClientPlayNetworking.canSend(BalanceRequestPayload.TYPE)
                && ClientPlayNetworking.canSend(BalanceUpdatePayload.TYPE)
                && ClientPlayNetworking.canSend(BalanceActionPayload.TYPE);
    }

    public static boolean canEdit() {
        return supportedByConnection()
                && state.supported() && state.permitted()
                && !communicating;
    }

    public static BalanceStatePayload.State state() {
        return state;
    }

    public static boolean communicating() {
        return communicating;
    }

    public static boolean received() {
        return received;
    }

    public static int localRevision() {
        return localRevision;
    }

    public static void reset() {
        state = BalanceStatePayload.State.unavailable("");
        pendingParent = null;
        openRequested = false;
        communicating = false;
        received = false;
        localRevision++;
    }

    public static String skillDescription(SkillCatalog.Skill skill) {
        if (!received) return skill.description();
        BalanceStatePayload.Skill balance = state.skills().stream()
                .filter(value -> value.id().equals(skill.id()))
                .findFirst().orElse(null);
        if (balance == null) return skill.description();
        String formula = "%.1f＋攻撃力×%.2f".formatted(
                balance.currentBaseDamage(), balance.currentScaling());
        return switch (skill.id()) {
            case "spin_slash" -> "周囲の敵へ" + formula
                    + "のダメージ。異なる敵1体につき闘気を1獲得する。";
            case "sweeping_slash" -> "前方の扇状範囲へ" + formula + "のダメージ。";
            case "warrior_charge" -> "視線方向へ軌跡を描いてダッシュし、通過した敵へ"
                    + formula + "のダメージ。";
            case "execution_leap" -> "照準中の敵の近くへ跳躍し" + formula
                    + "のダメージ。撃破時はクールダウンを解消。";
            case "earth_shatter" -> "周囲へ" + formula
                    + "のダメージを与え、Mobを減速・打ち上げする。";
            case "fighting_spirit_release" -> "周囲へ" + formula
                    + "＋消費闘気×0.25のダメージ。";
            case "end_war_strike" -> "前方広範囲へ" + formula
                    + "＋消費闘気×0.35。闘気100時は減少HP率で威力上昇。";
            default -> skill.description();
        };
    }
}
