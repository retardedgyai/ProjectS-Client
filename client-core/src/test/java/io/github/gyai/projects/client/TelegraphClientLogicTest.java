package io.github.gyai.projects.client;

import java.util.UUID;
import java.util.HashSet;
import java.util.Set;

public final class TelegraphClientLogicTest {
    private TelegraphClientLogicTest() {
    }

    public static void main(String[] args) {
        assert TelegraphVisualMath.phase(
                100, 31, 0.30,
                false, false)
                == TelegraphVisualMath.Phase.WARNING;
        assert TelegraphVisualMath.phase(
                100, 30, 0.30,
                false, false)
                == TelegraphVisualMath.Phase.IMMINENT;
        assert TelegraphVisualMath.phase(
                100, 0, 0.30,
                true, false)
                == TelegraphVisualMath.Phase.DETONATION;
        assert TelegraphVisualMath.phase(
                100, 0, 0.30,
                false, true)
                == TelegraphVisualMath.Phase.CANCELLATION;

        assert TelegraphVisualMath.acceptsRevision(4, 5);
        assert !TelegraphVisualMath.acceptsRevision(5, 5);
        assert !TelegraphVisualMath.acceptsRevision(5, 4);
        assert TelegraphVisualMath.acceptsPositionUpdate(
                false, 5, 6);
        assert !TelegraphVisualMath.acceptsPositionUpdate(
                true, 5, 6);

        UUID id = UUID.randomUUID();
        double first = TelegraphVisualMath
                .deterministicUnit(id, 7, 2);
        assert first == TelegraphVisualMath
                .deterministicUnit(id, 7, 2);
        assert first != TelegraphVisualMath
                .deterministicUnit(id, 8, 2);

        long key = TelegraphVisualMath.groundCacheKey(
                12, -4, 70);
        assert key == TelegraphVisualMath.groundCacheKey(
                12, -4, 70);
        assert key != TelegraphVisualMath.groundCacheKey(
                13, -4, 70);

        assert TelegraphVisualMath.shouldClear(
                false,
                "minecraft:overworld",
                "minecraft:overworld");
        assert TelegraphVisualMath.shouldClear(
                true,
                "minecraft:overworld",
                "minecraft:the_nether");
        assert !TelegraphVisualMath.shouldClear(
                true,
                "minecraft:overworld",
                "minecraft:overworld");

        assert !TelegraphVisualMath.isFloorCandidate(
                true, false, false, false,
                false, true, true);
        assert !TelegraphVisualMath.isFloorCandidate(
                false, true, false, false,
                false, true, true);
        assert TelegraphVisualMath.isFloorCandidate(
                false, false, false, false,
                false, true, true);
        assert TelegraphVisualMath.floorDistance(
                64.0, 63.75)
                < TelegraphVisualMath.floorDistance(
                64.0, 66.0);

        assert TelegraphVisualMath.canConnectSamples(
                true, 64.0,
                true, 65.25,
                1.25);
        assert !TelegraphVisualMath.canConnectSamples(
                true, 64.0,
                true, 65.251,
                1.25);
        assert !TelegraphVisualMath.canConnectSamples(
                true, 64.0,
                false, 64.0,
                1.25);

        assert TelegraphVisualMath.insideHorizontalShape(
                "CIRCLE",
                0, 0,
                0, 1,
                5, 0, 0, 0,
                0.2,
                4.7, 0);
        assert !TelegraphVisualMath.insideHorizontalShape(
                "CIRCLE",
                0, 0,
                0, 1,
                5, 0, 0, 0,
                0.2,
                4.9, 0);
        assert TelegraphVisualMath.insideHorizontalShape(
                "DONUT",
                0, 0,
                0, 1,
                5, 2, 0, 0,
                0.2,
                3, 0);
        assert TelegraphVisualMath.insideHorizontalShape(
                "LINE",
                0, 0,
                1, 0,
                0, 0, 4, 10,
                0.2,
                5, 1.7);
        assert !TelegraphVisualMath.insideHorizontalShape(
                "LINE",
                0, 0,
                1, 0,
                0, 0, 4, 10,
                0.2,
                5, 1.9);
        TelegraphVisualMath.LineFrame frame =
                TelegraphVisualMath.lineFrame(
                        3.0, 4.0, 4.0);
        assert Math.abs(
                frame.directionX() * frame.sideX()
                        + frame.directionZ()
                        * frame.sideZ()) < 0.000_001;
        assert Math.abs(
                Math.hypot(
                        frame.sideX(),
                        frame.sideZ()) - 2.0)
                < 0.000_001;

        long start = 1_000_000_000L;
        assert !TelegraphVisualMath.effectFinished(
                start + 199_999_999L,
                start,
                Long.MAX_VALUE,
                200_000_000L,
                150_000_000L);
        assert TelegraphVisualMath.effectFinished(
                start + 200_000_000L,
                start,
                Long.MAX_VALUE,
                200_000_000L,
                150_000_000L);
        assert TelegraphVisualMath.effectFinished(
                start + 150_000_000L,
                Long.MAX_VALUE,
                start,
                200_000_000L,
                150_000_000L);
        long expiry = TelegraphVisualMath.expiryDeadline(
                start, 20);
        assert !TelegraphVisualMath.expired(
                expiry - 1, expiry);
        assert TelegraphVisualMath.expired(
                expiry, expiry);

        Set<UUID> cached = new HashSet<>();
        TelegraphRegistry<String> registry =
                new TelegraphRegistry<>(
                        cached::remove,
                        cached::clear);
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        registry.put(firstId, "first");
        registry.put(secondId, "second");
        cached.add(firstId);
        cached.add(secondId);
        assert registry.remove(firstId);
        assert registry.get(firstId) == null;
        assert !cached.contains(firstId);
        assert registry.size() == 1;
        registry.clear();
        assert registry.size() == 0;
        assert cached.isEmpty();

        TelegraphRevisionGate gate =
                new TelegraphRevisionGate();
        assert gate.beginDetonation(7);
        assert !gate.beginDetonation(7);
        assert gate.consumeDetonationParticles();
        assert !gate.consumeDetonationParticles();
        assert gate.beginCancellation(8);
        assert !gate.beginCancellation(8);
        assert gate.beginLockFlash(9);
        assert !gate.beginLockFlash(9);
    }
}
