package io.github.gyai.projects.client;

import java.util.Locale;

public final class MonsterUiVisuals {
    private static final float NORMAL_SCALE = 0.0090f;
    private static final float ELITE_SCALE = 0.0095f;
    private static final float BOSS_SCALE = 0.0100f;
    private static final int HEALTH_BAR_HEIGHT = 8;

    private MonsterUiVisuals() {
    }

    public static float scale(
            MonsterUiPayload.MonsterRank rank
    ) {
        return switch (rank) {
            case NORMAL -> NORMAL_SCALE;
            case ELITE -> ELITE_SCALE;
            case BOSS -> BOSS_SCALE;
        };
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

    public static int healthBarHeight() {
        return HEALTH_BAR_HEIGHT;
    }

    public static boolean hasValidMaximumHealth(double maximum) {
        return Double.isFinite(maximum) && maximum > 0.0;
    }

    public static double clampHealth(
            double health,
            double maximum
    ) {
        if (!hasValidMaximumHealth(maximum)
                || !Double.isFinite(health)) {
            return 0.0;
        }
        return Math.clamp(health, 0.0, maximum);
    }

    public static float healthRatio(
            double health,
            double maximum
    ) {
        if (!hasValidMaximumHealth(maximum)) {
            return 0.0f;
        }
        return clampRatio((float) (
                clampHealth(health, maximum) / maximum));
    }

    public static float clampRatio(float ratio) {
        if (!Float.isFinite(ratio)) {
            return 0.0f;
        }
        return Math.clamp(ratio, 0.0f, 1.0f);
    }

    public static float fillWidth(
            int barWidth,
            float ratio
    ) {
        if (barWidth <= 0) {
            return 0.0f;
        }
        return Math.clamp(
                barWidth * clampRatio(ratio),
                0.0f,
                (float) barWidth);
    }

    public static String formatHealth(
            double current,
            double maximum
    ) {
        if (!hasValidMaximumHealth(maximum)) {
            return "0 / 0";
        }
        double safeCurrent = clampHealth(current, maximum);
        long currentValue = safeCurrent > 0.0
                ? Math.max(1L, Math.round(safeCurrent))
                : 0L;
        long maximumValue = Math.max(1L, Math.round(maximum));
        return String.format(
                Locale.ROOT,
                "%,d / %,d",
                currentValue,
                maximumValue);
    }

    public static int alphaForDistance(
            double distance,
            double displayRange
    ) {
        if (!Double.isFinite(distance)
                || !Double.isFinite(displayRange)
                || displayRange <= 0.0) {
            return 0;
        }
        double safeDistance = Math.max(0.0, distance);
        double fadeStartDistance = displayRange * 0.75;
        if (safeDistance <= fadeStartDistance) {
            return 255;
        }
        double ratio = 1.0
                - (safeDistance - fadeStartDistance)
                / (displayRange - fadeStartDistance);
        return (int) Math.round(
                Math.clamp(ratio, 0.0, 1.0) * 255.0);
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
