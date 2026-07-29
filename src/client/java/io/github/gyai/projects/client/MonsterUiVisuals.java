package io.github.gyai.projects.client;

public final class MonsterUiVisuals {
    private MonsterUiVisuals() {
    }

    public static int barWidth(
            MonsterUiPayload.MonsterRank rank
    ) {
        return switch (rank) {
            case NORMAL -> 96;
            case ELITE -> 116;
            case BOSS -> 148;
        };
    }

    public static int rankColor(
            MonsterUiPayload.MonsterRank rank,
            int alpha
    ) {
        return withAlpha(switch (rank) {
            case NORMAL -> 0xFFF2F2F2;
            case ELITE -> 0xFFFFC94A;
            case BOSS -> 0xFFFF5A45;
        }, alpha);
    }

    public static String rankPrefix(
            MonsterUiPayload.MonsterRank rank
    ) {
        return switch (rank) {
            case NORMAL -> "";
            case ELITE -> "◆ ";
            case BOSS -> "BOSS ";
        };
    }

    public static int threatColor(
            MonsterUiPayload.ThreatBand threat,
            int alpha
    ) {
        return withAlpha(switch (threat) {
            case GRAY -> 0xFF9A9A9A;
            case WHITE -> 0xFFF5F5F5;
            case YELLOW -> 0xFFFFDD45;
            case RED -> 0xFFFF4D4D;
        }, alpha);
    }

    public static int hardControlColor(
            MonsterUiPayload.HardControlType type,
            int alpha
    ) {
        return withAlpha(switch (type) {
            case STUN -> 0xFFFFB52E;
            case FEAR -> 0xFFB36BFF;
            case CHARM -> 0xFFFF79C9;
            case ROOT -> 0xFF66D9FF;
        }, alpha);
    }

    public static String hardControlName(
            MonsterUiPayload.HardControlType type
    ) {
        return switch (type) {
            case STUN -> "スタン";
            case FEAR -> "恐怖";
            case CHARM -> "魅了";
            case ROOT -> "ルート";
        };
    }

    public static String statusLabel(
            MonsterUiPayload.StatusType type
    ) {
        return switch (type) {
            case SLOW -> "❄";
            case POISON -> "毒";
            case BLEED -> "血";
            case BURN -> "炎";
            case DEFENSE_DOWN -> "防↓";
            case ATTACK_DOWN -> "攻↓";
        };
    }

    public static int statusColor(
            MonsterUiPayload.StatusType type,
            int alpha
    ) {
        return withAlpha(switch (type) {
            case SLOW -> 0xFF78D8FF;
            case POISON -> 0xFF82DB62;
            case BLEED -> 0xFFFF6262;
            case BURN -> 0xFFFF9A3D;
            case DEFENSE_DOWN -> 0xFFC89BFF;
            case ATTACK_DOWN -> 0xFFFFD16A;
        }, alpha);
    }

    public static int withAlpha(int color, int alpha) {
        return (Math.clamp(alpha, 0, 255) << 24)
                | color & 0x00FFFFFF;
    }
}
