package io.github.gyai.projects.client;

import io.github.gyai.projects.client.beta.BetaClientRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class MonsterUiClientState {
    private static final long STALE_NANOS =
            5_000_000_000L;
    private static final long DAMAGE_TRAIL_DELAY_NANOS =
            250_000_000L;
    private static final long HEAL_FLASH_NANOS =
            300_000_000L;

    private static final Map<UUID, TrackedMonster> MONSTERS =
            new HashMap<>();
    private static long lastSequence = Long.MIN_VALUE;
    private static ResourceKey<Level> dimension;

    private MonsterUiClientState() {
    }

    public static void receive(MonsterUiPayload.Update update) {
        if (!update.valid()
                || update.sequence() <= lastSequence) {
            return;
        }
        lastSequence = update.sequence();
        if (update.operation()
                == MonsterUiPayload.Operation.CLEAR) {
            MONSTERS.clear();
            BetaClientRuntime.clearElementTarget();
            return;
        }
        if (update.operation()
                == MonsterUiPayload.Operation.REMOVE) {
            for (MonsterUiPayload.Entry entry : update.entries()) {
                TrackedMonster current =
                        MONSTERS.get(entry.entityId());
                if (current != null
                        && current.networkEntityId()
                        == entry.networkEntityId()) {
                    MONSTERS.remove(entry.entityId());
                    BetaClientRuntime.clearElementTarget(entry.networkEntityId());
                }
            }
            return;
        }
        long receivedAt = System.nanoTime();
        for (MonsterUiPayload.Entry entry : update.entries()) {
            MONSTERS.compute(
                    entry.entityId(),
                    (entityId, existing) -> {
                        if (existing == null
                                || existing.networkEntityId()
                                != entry.networkEntityId()) {
                            return new TrackedMonster(
                                    entry, receivedAt);
                        }
                        existing.update(entry, receivedAt);
                        return existing;
                    });
        }
    }

    public static void tick(Minecraft client) {
        if (client.level == null) {
            clear();
            return;
        }
        ResourceKey<Level> currentDimension =
                client.level.dimension();
        if (dimension != null
                && !dimension.equals(currentDimension)) {
            clear();
        }
        dimension = currentDimension;
        long now = System.nanoTime();
        Iterator<TrackedMonster> iterator =
                MONSTERS.values().iterator();
        while (iterator.hasNext()) {
            TrackedMonster tracked = iterator.next();
            if (now - tracked.receivedAtNanos() > STALE_NANOS) {
                BetaClientRuntime.clearElementTarget(tracked.networkEntityId());
                iterator.remove();
                continue;
            }
            Entity entity = client.level.getEntity(
                    tracked.networkEntityId());
            if (entity == null) {
                continue;
            }
            if (entity.isRemoved()
                    || !entity.isAlive()
                    || !entity.getUUID().equals(tracked.entityId())) {
                BetaClientRuntime.clearElementTarget(tracked.networkEntityId());
                iterator.remove();
            }
        }
    }

    public static Collection<TrackedMonster> trackedMonsters() {
        return MONSTERS.values();
    }

    public static boolean tracksNetworkEntity(int networkEntityId) {
        for (TrackedMonster tracked : MONSTERS.values()) {
            if (tracked.networkEntityId() == networkEntityId) {
                return true;
            }
        }
        return false;
    }

    public static void clear() {
        MONSTERS.clear();
        lastSequence = Long.MIN_VALUE;
        dimension = null;
        BetaClientRuntime.clearElementTarget();
    }

    public static final class TrackedMonster {
        private MonsterUiPayload.Entry snapshot;
        private long receivedAtNanos;
        private double displayedHealth;
        private double trailingHealth;
        private long damageTrailDelayUntil;
        private long healFlashUntil;
        private long lastAnimationNanos;

        private TrackedMonster(
                MonsterUiPayload.Entry snapshot,
                long receivedAtNanos
        ) {
            this.snapshot = snapshot;
            this.receivedAtNanos = receivedAtNanos;
            this.displayedHealth = snapshot.currentHealth();
            this.trailingHealth = snapshot.currentHealth();
            this.lastAnimationNanos = receivedAtNanos;
        }

        private void update(
                MonsterUiPayload.Entry updated,
                long receivedAt
        ) {
            updateAnimation(receivedAt);
            double previousAuthoritative =
                    snapshot.currentHealth();
            if (updated.currentHealth() < previousAuthoritative) {
                trailingHealth = Math.max(
                        trailingHealth,
                        Math.max(displayedHealth, previousAuthoritative));
                damageTrailDelayUntil =
                        receivedAt + DAMAGE_TRAIL_DELAY_NANOS;
            } else if (updated.currentHealth()
                    > previousAuthoritative) {
                healFlashUntil =
                        receivedAt + HEAL_FLASH_NANOS;
            }
            snapshot = updated;
            receivedAtNanos = receivedAt;
        }

        public void updateAnimation(long now) {
            double deltaSeconds = Math.clamp(
                    (now - lastAnimationNanos)
                            / 1_000_000_000.0,
                    0.0,
                    0.25);
            lastAnimationNanos = now;
            displayedHealth = approach(
                    displayedHealth,
                    snapshot.currentHealth(),
                    1.0 - Math.exp(-deltaSeconds * 11.0));
            if (now >= damageTrailDelayUntil) {
                trailingHealth = approach(
                        trailingHealth,
                        snapshot.currentHealth(),
                        1.0 - Math.exp(-deltaSeconds * 5.0));
            }
            trailingHealth = Math.max(
                    displayedHealth,
                    Math.clamp(
                            trailingHealth,
                            0.0,
                            snapshot.maximumHealth()));
        }

        public int remainingTicks(
                int receivedRemainingTicks,
                long now
        ) {
            return (int) Math.ceil(remainingTicksExact(
                    receivedRemainingTicks, now));
        }

        public double remainingTicksExact(
                int receivedRemainingTicks,
                long now
        ) {
            double elapsedTicks = Math.max(
                    0.0,
                    (now - receivedAtNanos) / 50_000_000.0);
            return Math.max(
                    0.0, receivedRemainingTicks - elapsedTicks);
        }

        public UUID entityId() {
            return snapshot.entityId();
        }

        public int networkEntityId() {
            return snapshot.networkEntityId();
        }

        public MonsterUiPayload.Entry snapshot() {
            return snapshot;
        }

        public long receivedAtNanos() {
            return receivedAtNanos;
        }

        public double displayedHealth() {
            return displayedHealth;
        }

        public double trailingHealth() {
            return trailingHealth;
        }

        public boolean healFlash(long now) {
            return now < healFlashUntil;
        }

        private static double approach(
                double current,
                double target,
                double factor
        ) {
            if (Math.abs(current - target) < 0.01) {
                return target;
            }
            return current + (target - current)
                    * Math.clamp(factor, 0.0, 1.0);
        }
    }
}
