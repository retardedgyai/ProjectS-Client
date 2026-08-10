package io.github.gyai.projects.client.beta;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Display-only projection of the authoritative Server cold/freeze snapshot. */
public final class IceStatusClientStore {
    public static final long STAGE_PULSE_MILLIS = 180L;
    public static final long FREEZE_FLASH_MILLIS = 260L;
    public static final long SHATTER_FLASH_MILLIS = 260L;

    private Snapshot snapshot;
    private long stagePulseUntilMillis;
    private long freezeFlashUntilMillis;
    private long shatterFlashUntilMillis;

    public synchronized boolean receive(BetaDisplayDocument document, long receivedAtMillis) {
        if (document == null || receivedAtMillis < 0) return false;
        if (document.status() != BetaDisplayDocument.Status.READY) {
            clear();
            return false;
        }
        Snapshot replacement;
        try {
            replacement = parse(document);
        } catch (IllegalArgumentException failure) {
            clear();
            return false;
        }
        if (replacement.expiresAtMillis() <= receivedAtMillis) {
            clear();
            return false;
        }
        if (snapshot != null && replacement.stateRevision() <= snapshot.stateRevision()) {
            return false;
        }

        boolean targetChanged = snapshot != null
                && snapshot.targetNetworkId() != replacement.targetNetworkId();
        if (targetChanged) clearTransitions();
        if (snapshot != null && !targetChanged) {
            if (replacement.coldStage() != snapshot.coldStage()) {
                stagePulseUntilMillis = replacement.coldStage().ordinal()
                        > snapshot.coldStage().ordinal()
                        ? safeAdd(receivedAtMillis, STAGE_PULSE_MILLIS) : 0L;
            }
            if (!snapshot.frozen() && replacement.frozen()) {
                shatterFlashUntilMillis = 0L;
                freezeFlashUntilMillis = safeAdd(receivedAtMillis, FREEZE_FLASH_MILLIS);
            }
            if (snapshot.frozen() && !replacement.frozen()) {
                freezeFlashUntilMillis = 0L;
                shatterFlashUntilMillis = replacement.immuneUntilMillis() > receivedAtMillis
                        ? safeAdd(receivedAtMillis, SHATTER_FLASH_MILLIS) : 0L;
            }
        }
        snapshot = replacement;
        if (replacement.coldGauge() == 0.0
                && replacement.coldStage() == ElementStatePayloadV1.ColdStage.NONE
                && !replacement.frozen()
                && replacement.immuneUntilMillis() <= receivedAtMillis) {
            clearTransitions();
        }
        return true;
    }

    public synchronized Optional<View> view(long nowMillis) {
        if (nowMillis < 0) return Optional.empty();
        if (snapshot == null || snapshot.expiresAtMillis() <= nowMillis) {
            clear();
            return Optional.empty();
        }
        long immunityRemainingMillis = Math.max(
                0L, snapshot.immuneUntilMillis() - nowMillis);
        if (snapshot.coldGauge() == 0.0
                && snapshot.coldStage() == ElementStatePayloadV1.ColdStage.NONE
                && !snapshot.frozen()
                && immunityRemainingMillis == 0L) {
            return Optional.empty();
        }
        return Optional.of(new View(
                snapshot.targetNetworkId(),
                snapshot.stateRevision(),
                snapshot.coldGauge(),
                snapshot.coldStage(),
                snapshot.frozen(),
                immunityRemainingMillis,
                nowMillis < stagePulseUntilMillis,
                nowMillis < freezeFlashUntilMillis,
                nowMillis < shatterFlashUntilMillis));
    }

    public synchronized void clear() {
        snapshot = null;
        clearTransitions();
    }

    public synchronized void clearTarget(int targetNetworkId) {
        if (snapshot != null && snapshot.targetNetworkId() == targetNetworkId) clear();
    }

    private void clearTransitions() {
        stagePulseUntilMillis = 0L;
        freezeFlashUntilMillis = 0L;
        shatterFlashUntilMillis = 0L;
    }

    private static Snapshot parse(BetaDisplayDocument document) {
        Map<String, String> fields = document.fields();
        int target = integer(fields, "target-network-id", 0, Integer.MAX_VALUE);
        long stateRevision = number(fields, "state-revision", 0, Long.MAX_VALUE);
        if (stateRevision != document.revision()) {
            throw new IllegalArgumentException("document/state revision mismatch");
        }
        double coldGauge = decimal(fields, "cold-gauge", 0, Double.MAX_VALUE);
        ElementStatePayloadV1.ColdStage coldStage;
        try {
            coldStage = ElementStatePayloadV1.ColdStage.valueOf(
                    required(fields, "cold-stage").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException("cold-stage", failure);
        }
        boolean frozen = bool(fields, "frozen");
        // The v1 wire field is an absolute Server timestamp, not a duration.
        long immuneUntilMillis = number(
                fields, "refreeze-immunity", 0, Long.MAX_VALUE);
        long expiry = number(
                fields, "snapshot-expires-at-millis", 0, Long.MAX_VALUE);
        return new Snapshot(target, stateRevision, coldGauge, coldStage,
                frozen, immuneUntilMillis, expiry);
    }

    private static int integer(Map<String, String> values, String key, int min, int max) {
        return Math.toIntExact(number(values, key, min, max));
    }

    private static long number(Map<String, String> values, String key, long min, long max) {
        long value;
        try {
            value = Long.parseLong(required(values, key));
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException(key, failure);
        }
        if (value < min || value > max) throw new IllegalArgumentException(key);
        return value;
    }

    private static double decimal(Map<String, String> values, String key,
                                  double min, double max) {
        double value;
        try {
            value = Double.parseDouble(required(values, key));
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException(key, failure);
        }
        if (!Double.isFinite(value) || value < min || value > max) {
            throw new IllegalArgumentException(key);
        }
        return value;
    }

    private static boolean bool(Map<String, String> values, String key) {
        return switch (required(values, key)) {
            case "true" -> true;
            case "false" -> false;
            default -> throw new IllegalArgumentException(key);
        };
    }

    private static String required(Map<String, String> values, String key) {
        String value = values.get(key);
        if (value == null) throw new IllegalArgumentException("missing " + key);
        return value;
    }

    private static long safeAdd(long left, long right) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException ignored) {
            return Long.MAX_VALUE;
        }
    }

    private record Snapshot(
            int targetNetworkId,
            long stateRevision,
            double coldGauge,
            ElementStatePayloadV1.ColdStage coldStage,
            boolean frozen,
            long immuneUntilMillis,
            long expiresAtMillis
    ) {
    }

    public record View(
            int targetNetworkId,
            long stateRevision,
            double coldGauge,
            ElementStatePayloadV1.ColdStage coldStage,
            boolean frozen,
            long immunityRemainingMillis,
            boolean stagePulse,
            boolean freezeFlash,
            boolean shatterFlash
    ) {
        public View {
            if (targetNetworkId < 0 || stateRevision < 0
                    || !Double.isFinite(coldGauge) || coldGauge < 0
                    || coldStage == null || immunityRemainingMillis < 0) {
                throw new IllegalArgumentException("invalid ice view");
            }
        }
    }
}
