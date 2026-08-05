package io.github.gyai.projects.client.beta;

import java.util.Map;
import java.util.Optional;

/** Display-only projection of the authoritative Server fire snapshot. */
public final class FireStatusClientStore {
    public static final long STACK_PULSE_MILLIS = 180L;
    public static final long DETONATION_FLASH_MILLIS = 260L;

    private Snapshot snapshot;
    private long stackPulseUntilMillis;
    private long detonationFlashUntilMillis;

    public synchronized boolean receive(BetaDisplayDocument document, long receivedAtMillis) {
        if (document == null || receivedAtMillis < 0) return false;
        if (document.status() != BetaDisplayDocument.Status.READY) {
            clear();
            return false;
        }
        Snapshot replacement;
        try { replacement = parse(document); }
        catch (IllegalArgumentException failure) {
            clear();
            return false;
        }
        if (replacement.expiresAtMillis() <= receivedAtMillis) { clear(); return false; }
        if (snapshot != null && replacement.stateRevision() <= snapshot.stateRevision()) {
            return false;
        }
        boolean targetChanged = snapshot != null
                && snapshot.targetNetworkId() != replacement.targetNetworkId();
        if (targetChanged) {
            stackPulseUntilMillis = 0;
            detonationFlashUntilMillis = 0;
        }
        if (snapshot != null && !targetChanged
                && replacement.fireStacks() > snapshot.fireStacks()) {
            stackPulseUntilMillis = safeAdd(receivedAtMillis, STACK_PULSE_MILLIS);
        }
        if (snapshot != null && !targetChanged
                && replacement.detonationPulseRevision()
                > snapshot.detonationPulseRevision()) {
            detonationFlashUntilMillis = safeAdd(
                    receivedAtMillis, DETONATION_FLASH_MILLIS);
        }
        snapshot = replacement;
        return true;
    }

    public synchronized Optional<View> view(long nowMillis) {
        if (nowMillis < 0) return Optional.empty();
        if (snapshot == null || snapshot.expiresAtMillis() <= nowMillis) {
            clear();
            return Optional.empty();
        }
        if (snapshot.fireStacks() == 0) return Optional.empty();
        return Optional.of(new View(snapshot.targetNetworkId(), snapshot.stateRevision(),
                snapshot.fireStacks(), snapshot.fractionalProgress(), snapshot.decayActive(),
                snapshot.fireStacks() >= 9, nowMillis < stackPulseUntilMillis,
                nowMillis < detonationFlashUntilMillis));
    }

    public synchronized void clear() {
        snapshot = null;
        stackPulseUntilMillis = 0;
        detonationFlashUntilMillis = 0;
    }

    public synchronized void clearTarget(int targetNetworkId) {
        if (snapshot != null && snapshot.targetNetworkId() == targetNetworkId) clear();
    }

    private static Snapshot parse(BetaDisplayDocument document) {
        Map<String, String> fields = document.fields();
        int target = integer(fields, "target-network-id", 0, Integer.MAX_VALUE);
        long stateRevision = number(fields, "state-revision", 0, Long.MAX_VALUE);
        if (stateRevision != document.revision()) {
            throw new IllegalArgumentException("document/state revision mismatch");
        }
        int stacks = integer(fields, "fire-stacks", 0, 10);
        double gauge = decimal(fields, "fire-fractional-gauge", 0, Double.MAX_VALUE);
        double threshold = decimal(fields, "fire-threshold", Math.nextUp(0.0), Double.MAX_VALUE);
        if (gauge >= threshold) throw new IllegalArgumentException("gauge exceeds threshold");
        double progress = decimal(fields, "fire-progress-ratio", 0, 1);
        if (Math.abs(progress - gauge / threshold) > 1.0e-9) {
            throw new IllegalArgumentException("fractional progress mismatch");
        }
        boolean decay = bool(fields, "fire-decay-active");
        long decayStarts = number(fields, "fire-decay-starts-in-millis", 0, Long.MAX_VALUE);
        long pulse = number(fields, "fire-detonation-pulse-revision", 0, Long.MAX_VALUE);
        long expiry = number(fields, "snapshot-expires-at-millis", 0, Long.MAX_VALUE);
        return new Snapshot(target, stateRevision, stacks, gauge, threshold, progress,
                decay, decayStarts, pulse, expiry);
    }

    private static int integer(Map<String, String> values, String key, int min, int max) {
        long value = number(values, key, min, max);
        return Math.toIntExact(value);
    }

    private static long number(Map<String, String> values, String key, long min, long max) {
        String raw = required(values, key);
        long value;
        try { value = Long.parseLong(raw); }
        catch (NumberFormatException failure) { throw new IllegalArgumentException(key, failure); }
        if (value < min || value > max) throw new IllegalArgumentException(key);
        return value;
    }

    private static double decimal(Map<String, String> values, String key,
                                  double min, double max) {
        double value;
        try { value = Double.parseDouble(required(values, key)); }
        catch (NumberFormatException failure) { throw new IllegalArgumentException(key, failure); }
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
        try { return Math.addExact(left, right); }
        catch (ArithmeticException ignored) { return Long.MAX_VALUE; }
    }

    private record Snapshot(int targetNetworkId, long stateRevision, int fireStacks,
                            double fractionalGauge, double threshold,
                            double fractionalProgress, boolean decayActive,
                            long decayStartsInMillis, long detonationPulseRevision,
                            long expiresAtMillis) { }

    public record View(int targetNetworkId, long stateRevision, int fireStacks,
                       double fractionalProgress, boolean decayActive,
                       boolean warning, boolean stackPulse, boolean detonationFlash) {
        public View {
            if (targetNetworkId < 0 || stateRevision < 0 || fireStacks < 1
                    || fireStacks > 10 || !Double.isFinite(fractionalProgress)
                    || fractionalProgress < 0 || fractionalProgress > 1) {
                throw new IllegalArgumentException("invalid fire view");
            }
        }
    }
}
