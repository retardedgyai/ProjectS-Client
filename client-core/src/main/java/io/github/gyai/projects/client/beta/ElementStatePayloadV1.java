package io.github.gyai.projects.client.beta;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;

/** Independent Client decoder for the Server-owned projects:elements payload v1. */
public record ElementStatePayloadV1(
        int targetNetworkId,
        long stateRevision,
        double fireFractionalGauge,
        int fireStacks,
        double fireThreshold,
        double fireFractionalProgress,
        boolean fireDecayActive,
        long fireDecayStartsInMillis,
        long detonationPulseRevision,
        long snapshotExpiresAtMillis,
        double coldGauge,
        ColdStage coldStage,
        boolean frozen,
        long refreezeImmunityMillis
) {
    public static final int WIRE_BYTES = 83;
    public enum ColdStage { NONE, CHILLED, DEEP_CHILL, FROZEN }

    public ElementStatePayloadV1 {
        if (targetNetworkId < 0 || stateRevision < 0 || fireStacks < 0 || fireStacks > 10
                || !finiteNonNegative(fireFractionalGauge)
                || !Double.isFinite(fireThreshold) || fireThreshold <= 0
                || !Double.isFinite(fireFractionalProgress)
                || fireFractionalProgress < 0 || fireFractionalProgress > 1
                || fireDecayStartsInMillis < 0 || detonationPulseRevision < 0
                || snapshotExpiresAtMillis < 0 || !finiteNonNegative(coldGauge)
                || coldStage == null || refreezeImmunityMillis < 0
                || Math.abs(fireFractionalProgress
                - fireFractionalGauge / fireThreshold) > 1.0e-9) {
            throw new IllegalArgumentException("invalid elements payload v1");
        }
    }

    public static ElementStatePayloadV1 decode(byte[] payload) throws IOException {
        if (payload == null || payload.length != WIRE_BYTES) {
            throw new IOException("Unexpected elements payload size");
        }
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload))) {
            int target = input.readInt();
            long revision = input.readLong();
            double gauge = input.readDouble();
            int stacks = input.readInt();
            double threshold = input.readDouble();
            double progress = input.readDouble();
            boolean decay = input.readBoolean();
            long decayStarts = input.readLong();
            long pulse = input.readLong();
            long expiry = input.readLong();
            double cold = input.readDouble();
            int stage = input.readUnsignedByte();
            boolean frozen = input.readBoolean();
            long immunity = input.readLong();
            ColdStage[] stages = ColdStage.values();
            if (stage >= stages.length || input.available() != 0) {
                throw new IOException("Malformed elements payload");
            }
            return new ElementStatePayloadV1(target, revision, gauge, stacks,
                    threshold, progress, decay, decayStarts, pulse, expiry,
                    cold, stages[stage], frozen, immunity);
        } catch (EOFException failure) {
            throw new IOException("Truncated elements payload", failure);
        } catch (IllegalArgumentException failure) {
            throw new IOException("Invalid elements payload", failure);
        }
    }

    public BetaDisplayDocument toDisplayDocument() {
        LinkedHashMap<String, String> fields = new LinkedHashMap<>();
        fields.put("target-network-id", Integer.toString(targetNetworkId));
        fields.put("state-revision", Long.toString(stateRevision));
        fields.put("fire-stacks", Integer.toString(fireStacks));
        fields.put("fire-fractional-gauge", Double.toString(fireFractionalGauge));
        fields.put("fire-threshold", Double.toString(fireThreshold));
        fields.put("fire-progress-ratio", Double.toString(fireFractionalProgress));
        fields.put("fire-decay-active", Boolean.toString(fireDecayActive));
        fields.put("fire-decay-starts-in-millis", Long.toString(fireDecayStartsInMillis));
        fields.put("fire-detonation-pulse-revision", Long.toString(detonationPulseRevision));
        fields.put("snapshot-expires-at-millis", Long.toString(snapshotExpiresAtMillis));
        fields.put("cold-gauge", Double.toString(coldGauge));
        fields.put("cold-stage", coldStage.name().toLowerCase(java.util.Locale.ROOT));
        fields.put("frozen", Boolean.toString(frozen));
        fields.put("refreeze-immunity", Long.toString(refreezeImmunityMillis));
        return new BetaDisplayDocument(stateRevision, BetaDisplayDocument.Status.READY,
                "", fields, List.of());
    }

    private static boolean finiteNonNegative(double value) {
        return Double.isFinite(value) && value >= 0;
    }
}
