package io.github.gyai.projects.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import java.util.Collection;
import java.util.UUID;

public final class TelegraphClientState {
    public static final double IMMINENT_THRESHOLD = 0.30;
    public static final long CANCELLATION_NANOS =
            150_000_000L;
    public static final long DETONATION_NANOS =
            200_000_000L;
    private static final long LOCK_FLASH_NANOS =
            180_000_000L;
    private static final long STALE_NANOS =
            65_000_000_000L;
    private static final int MAX_ACTIVE = 32;

    private static final TelegraphRegistry<TrackedTelegraph>
            TELEGRAPHS = new TelegraphRegistry<>(
                    TelegraphGroundSampler::remove,
                    TelegraphGroundSampler::clear);
    private static long lastSequence = Long.MIN_VALUE;
    private static String dimension;

    private TelegraphClientState() {
    }

    public static void receive(
            TelegraphPayload.Update update
    ) {
        if (!update.valid()
                || update.sequence() <= lastSequence) {
            return;
        }
        lastSequence = update.sequence();
        if (update.operation()
                == TelegraphPayload.Operation.CLEAR) {
            clearTracked();
            return;
        }
        TelegraphPayload.Snapshot snapshot =
                update.snapshot();
        if (snapshot == null) {
            return;
        }
        TrackedTelegraph existing =
                TELEGRAPHS.get(snapshot.id());
        if (update.operation()
                == TelegraphPayload.Operation.REMOVE) {
            if (existing != null
                    && snapshot.revision()
                    >= existing.revision()) {
                TELEGRAPHS.remove(snapshot.id());
            }
            return;
        }
        if (existing != null
                && !TelegraphVisualMath.acceptsRevision(
                existing.revision(),
                snapshot.revision())) {
            return;
        }
        if (existing != null
                && update.operation()
                == TelegraphPayload.Operation.UPDATE
                && !TelegraphVisualMath
                .acceptsPositionUpdate(
                        existing.snapshot().locked(),
                        existing.revision(),
                        snapshot.revision())) {
            return;
        }
        if (existing == null
                && TELEGRAPHS.size() >= MAX_ACTIVE) {
            return;
        }
        long receivedAt = System.nanoTime();
        if (existing == null) {
            existing = new TrackedTelegraph(
                    snapshot,
                    receivedAt);
            TELEGRAPHS.put(
                    snapshot.id(),
                    existing);
        } else {
            existing.update(snapshot, receivedAt);
        }
        existing.applyOperation(
                update.operation(),
                receivedAt);
    }

    public static void tick(Minecraft client) {
        if (client.level == null) {
            if (TelegraphVisualMath.shouldClear(
                    false, dimension, null)) {
                clear();
            }
            return;
        }
        String currentDimension = client.level
                .dimension()
                .identifier()
                .toString();
        if (TelegraphVisualMath.shouldClear(
                true, dimension, currentDimension)) {
            clear();
        }
        dimension = currentDimension;
        long now = System.nanoTime();
        for (TrackedTelegraph tracked
                : TELEGRAPHS.values()) {
            if (!tracked.snapshot().dimension()
                    .equals(currentDimension)
                    || now - tracked.receivedAtNanos()
                    > STALE_NANOS
                    || tracked.expired(now)
                    || tracked.effectFinished(now)) {
                TELEGRAPHS.remove(
                        tracked.snapshot().id());
                continue;
            }
            Entity source = client.level.getEntity(
                    tracked.snapshot()
                            .sourceNetworkId());
            if (source == null
                    || !source.getUUID().equals(
                    tracked.snapshot().sourceId())) {
                TELEGRAPHS.remove(
                        tracked.snapshot().id());
                continue;
            }
            tracked.updateAnimation(now);
            if (tracked.meshDirty()
                    || tracked.mesh() == null) {
                tracked.mesh(
                        TelegraphGroundSampler.build(
                                client.level,
                                tracked));
            }
            if (tracked.mesh() != null) {
                TelegraphRenderer.emitTickParticles(
                        client,
                        tracked,
                        tracked.mesh(),
                        now);
            }
        }
    }

    public static Collection<TrackedTelegraph>
    trackedTelegraphs() {
        return TELEGRAPHS.values();
    }

    public static void clear() {
        clearTracked();
        lastSequence = Long.MIN_VALUE;
        dimension = null;
    }

    private static void clearTracked() {
        TELEGRAPHS.clear();
    }

    public static final class TrackedTelegraph {
        private TelegraphPayload.Snapshot snapshot;
        private long receivedAtNanos;
        private long lastAnimationNanos;
        private long lockFlashUntilNanos;
        private long detonationStartedNanos =
                Long.MAX_VALUE;
        private long cancellationStartedNanos =
                Long.MAX_VALUE;
        private long expireAtNanos;
        private double interpolatedX;
        private double interpolatedY;
        private double interpolatedZ;
        private boolean meshDirty = true;
        private TelegraphGroundSampler.Mesh mesh;
        private final TelegraphRevisionGate revisionGate =
                new TelegraphRevisionGate();
        private long lastAmbientParticleNanos;

        private TrackedTelegraph(
                TelegraphPayload.Snapshot snapshot,
                long receivedAtNanos
        ) {
            this.snapshot = snapshot;
            this.receivedAtNanos = receivedAtNanos;
            expireAtNanos =
                    TelegraphVisualMath.expiryDeadline(
                    receivedAtNanos,
                    snapshot.remainingExpireTicks());
            lastAnimationNanos = receivedAtNanos;
            interpolatedX = snapshot.centerX();
            interpolatedY = snapshot.centerY();
            interpolatedZ = snapshot.centerZ();
        }

        private void update(
                TelegraphPayload.Snapshot updated,
                long receivedAt
        ) {
            updateAnimation(receivedAt);
            double dx = updated.centerX()
                    - snapshot.centerX();
            double dy = updated.centerY()
                    - snapshot.centerY();
            double dz = updated.centerZ()
                    - snapshot.centerZ();
            if (dx * dx + dy * dy + dz * dz
                    > 0.0001
                    || updated.locked()
                    != snapshot.locked()) {
                meshDirty = true;
            }
            snapshot = updated;
            receivedAtNanos = receivedAt;
            expireAtNanos =
                    TelegraphVisualMath.expiryDeadline(
                    receivedAt,
                    updated.remainingExpireTicks());
            if (updated.locked()) {
                interpolatedX = updated.centerX();
                interpolatedY = updated.centerY();
                interpolatedZ = updated.centerZ();
            }
        }

        private void applyOperation(
                TelegraphPayload.Operation operation,
                long receivedAt
        ) {
            long revision = snapshot.revision();
            if (snapshot.cancelled()
                    && revisionGate.beginCancellation(
                    revision)) {
                cancellationStartedNanos = receivedAt;
            } else if (snapshot.detonated()
                    && revisionGate.beginDetonation(
                    revision)) {
                detonationStartedNanos = receivedAt;
            }
            switch (operation) {
                case LOCK -> {
                    if (!revisionGate.beginLockFlash(
                            revision)) {
                        break;
                    }
                        lockFlashUntilNanos =
                                receivedAt
                                        + LOCK_FLASH_NANOS;
                }
                case DETONATE -> {
                    if (revisionGate.beginDetonation(
                            revision)) {
                        detonationStartedNanos = receivedAt;
                    }
                }
                case CANCEL -> {
                    if (revisionGate.beginCancellation(
                            revision)) {
                        cancellationStartedNanos =
                                receivedAt;
                    }
                }
                default -> {
                }
            }
        }

        public void updateAnimation(long now) {
            double deltaSeconds = Math.clamp(
                    (now - lastAnimationNanos)
                            / 1_000_000_000.0,
                    0.0,
                    0.25);
            lastAnimationNanos = now;
            if (snapshot.locked()) {
                interpolatedX = snapshot.centerX();
                interpolatedY = snapshot.centerY();
                interpolatedZ = snapshot.centerZ();
                return;
            }
            double factor = 1.0
                    - Math.exp(-deltaSeconds * 18.0);
            interpolatedX = approach(
                    interpolatedX,
                    snapshot.centerX(),
                    factor);
            interpolatedY = approach(
                    interpolatedY,
                    snapshot.centerY(),
                    factor);
            interpolatedZ = approach(
                    interpolatedZ,
                    snapshot.centerZ(),
                    factor);
        }

        public double remainingWarningTicks(
                long now
        ) {
            double elapsedTicks = Math.max(
                    0.0,
                    (now - receivedAtNanos)
                            / 50_000_000.0);
            return Math.max(
                    0.0,
                    snapshot.remainingDetonationTicks()
                            - elapsedTicks);
        }

        public double progress(long now) {
            return Math.clamp(
                    1.0 - remainingWarningTicks(now)
                            / snapshot.totalWarningTicks(),
                    0.0,
                    1.0);
        }

        public TelegraphVisualMath.Phase phase(
                long now
        ) {
            return TelegraphVisualMath.phase(
                    snapshot.totalWarningTicks(),
                    remainingWarningTicks(now),
                    IMMINENT_THRESHOLD,
                    detonationStartedNanos
                            != Long.MAX_VALUE,
                    cancellationStartedNanos
                            != Long.MAX_VALUE);
        }

        public double effectAlpha(long now) {
            if (cancellationStartedNanos
                    != Long.MAX_VALUE) {
                return 1.0 - Math.clamp(
                        (now - cancellationStartedNanos)
                                / (double)
                                CANCELLATION_NANOS,
                        0.0,
                        1.0);
            }
            if (detonationStartedNanos
                    != Long.MAX_VALUE) {
                return 1.0 - Math.clamp(
                        (now - detonationStartedNanos)
                                / (double)
                                DETONATION_NANOS,
                        0.0,
                        1.0);
            }
            return 1.0;
        }

        public boolean lockFlash(long now) {
            return now < lockFlashUntilNanos;
        }

        public boolean effectFinished(long now) {
            return TelegraphVisualMath.effectFinished(
                    now,
                    detonationStartedNanos,
                    cancellationStartedNanos,
                    DETONATION_NANOS,
                    CANCELLATION_NANOS);
        }

        public boolean expired(long now) {
            return TelegraphVisualMath.expired(
                    now, expireAtNanos);
        }

        public TelegraphPayload.Snapshot snapshot() {
            return snapshot;
        }

        public long revision() {
            return snapshot.revision();
        }

        public long receivedAtNanos() {
            return receivedAtNanos;
        }

        public double x() { return interpolatedX; }
        public double y() { return interpolatedY; }
        public double z() { return interpolatedZ; }

        public boolean meshDirty() { return meshDirty; }

        public void mesh(
                TelegraphGroundSampler.Mesh mesh
        ) {
            this.mesh = mesh;
            meshDirty = false;
        }

        public TelegraphGroundSampler.Mesh mesh() {
            return mesh;
        }

        public boolean consumeDetonationParticles() {
            if (detonationStartedNanos
                    == Long.MAX_VALUE) {
                return false;
            }
            return revisionGate
                    .consumeDetonationParticles();
        }

        public long lastAmbientParticleNanos() {
            return lastAmbientParticleNanos;
        }

        public void markAmbientParticles(long now) {
            lastAmbientParticleNanos = now;
        }

        private static double approach(
                double current,
                double target,
                double factor
        ) {
            if (Math.abs(current - target) < 0.001) {
                return target;
            }
            return current + (target - current)
                    * Math.clamp(factor, 0.0, 1.0);
        }

    }
}
