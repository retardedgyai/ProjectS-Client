package io.github.gyai.projects.client;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TelegraphGroundSampler {
    private static final int MAX_CACHE_ENTRIES = 4_096;
    private static final long CACHE_NANOS =
            1_000_000_000L;
    private static final double GROUND_OFFSET = 0.025;
    public static final double MAX_CONNECTION_HEIGHT = 1.25;
    private static final int CONNECTION_PROBES = 4;
    private static final Map<UUID, Map<Long, CachedGround>> CACHE =
            new HashMap<>();

    private TelegraphGroundSampler() {
    }

    public static Mesh build(
            Level level,
            TelegraphClientState.TrackedTelegraph tracked
    ) {
        TelegraphPayload.Snapshot snapshot =
                tracked.snapshot();
        List<Segment> outline = new ArrayList<>();
        if (snapshot.shape()
                == TelegraphPayload.Shape.LINE) {
            sampleLine(level, snapshot, outline);
        } else {
            sampleCircle(
                    level,
                    snapshot,
                    snapshot.radius(),
                    outline,
                    0);
            if (snapshot.shape()
                    == TelegraphPayload.Shape.DONUT) {
                sampleCircle(
                        level,
                        snapshot,
                        snapshot.innerRadius(),
                        outline,
                        1);
            }
        }
        List<Patch> noise = sampleNoise(
                level, snapshot);
        return new Mesh(
                List.copyOf(outline),
                List.copyOf(noise));
    }

    public static void clear() {
        CACHE.clear();
    }

    public static void remove(UUID telegraphId) {
        CACHE.remove(telegraphId);
    }

    private static void sampleCircle(
            Level level,
            TelegraphPayload.Snapshot snapshot,
            double radius,
            List<Segment> output,
            int lane
    ) {
        int segmentCount = Math.clamp(
                (int) Math.ceil(radius * 8.0),
                24,
                96);
        for (int index = 0;
             index < segmentCount;
             index++) {
            double firstAngle = Math.PI * 2.0
                    * index / segmentCount;
            double secondAngle = Math.PI * 2.0
                    * (index + 1) / segmentCount;
            double x1 = snapshot.centerX()
                    + Math.cos(firstAngle) * radius;
            double z1 = snapshot.centerZ()
                    + Math.sin(firstAngle) * radius;
            double x2 = snapshot.centerX()
                    + Math.cos(secondAngle) * radius;
            double z2 = snapshot.centerZ()
                    + Math.sin(secondAngle) * radius;
            addSegment(
                    level,
                    snapshot.id(),
                    snapshot.centerY(),
                    x1, z1, x2, z2,
                    0.78
                            + TelegraphVisualMath
                            .deterministicUnit(
                                    snapshot.id(),
                                    index,
                                    lane) * 0.22,
                    output);
        }
    }

    private static void sampleLine(
            Level level,
            TelegraphPayload.Snapshot snapshot,
            List<Segment> output
    ) {
        TelegraphVisualMath.LineFrame frame =
                TelegraphVisualMath.lineFrame(
                        snapshot.directionX(),
                        snapshot.directionZ(),
                        snapshot.width());
        double directionX = frame.directionX();
        double directionZ = frame.directionZ();
        double sideX = frame.sideX();
        double sideZ = frame.sideZ();
        int segmentCount = Math.clamp(
                (int) Math.ceil(snapshot.length() / 0.75),
                4,
                128);
        for (int index = 0;
             index < segmentCount;
             index++) {
            double first = snapshot.length()
                    * index / segmentCount;
            double second = snapshot.length()
                    * (index + 1) / segmentCount;
            for (int sign : new int[]{-1, 1}) {
                addSegment(
                        level,
                        snapshot.id(),
                        snapshot.centerY(),
                        snapshot.centerX()
                                + directionX * first
                                + sideX * sign,
                        snapshot.centerZ()
                                + directionZ * first
                                + sideZ * sign,
                        snapshot.centerX()
                                + directionX * second
                                + sideX * sign,
                        snapshot.centerZ()
                                + directionZ * second
                                + sideZ * sign,
                        0.78
                                + TelegraphVisualMath
                                .deterministicUnit(
                                        snapshot.id(),
                                        index,
                                        sign + 2)
                                * 0.22,
                        output);
            }
        }
        addSegment(
                level,
                snapshot.id(),
                snapshot.centerY(),
                snapshot.centerX() + sideX,
                snapshot.centerZ() + sideZ,
                snapshot.centerX() - sideX,
                snapshot.centerZ() - sideZ,
                0.9,
                output);
        double endX = snapshot.centerX()
                + directionX * snapshot.length();
        double endZ = snapshot.centerZ()
                + directionZ * snapshot.length();
        addSegment(
                level,
                snapshot.id(),
                snapshot.centerY(),
                endX + sideX,
                endZ + sideZ,
                endX - sideX,
                endZ - sideZ,
                1.0,
                output);
    }

    private static List<Patch> sampleNoise(
            Level level,
            TelegraphPayload.Snapshot snapshot
    ) {
        int attempts = snapshot.shape()
                == TelegraphPayload.Shape.LINE
                ? 24
                : Math.clamp(
                (int) Math.ceil(snapshot.radius() * 5.0),
                18,
                48);
        List<Patch> patches =
                new ArrayList<>(attempts);
        for (int index = 0;
             index < attempts;
             index++) {
            double first = TelegraphVisualMath
                    .deterministicUnit(
                            snapshot.id(), index, 10);
            double second = TelegraphVisualMath
                    .deterministicUnit(
                            snapshot.id(), index, 11);
            double x;
            double z;
            double size = 0.08
                    + TelegraphVisualMath
                    .deterministicUnit(
                            snapshot.id(), index, 12)
                    * 0.18;
            if (snapshot.shape()
                    == TelegraphPayload.Shape.LINE) {
                double directionLength = Math.hypot(
                        snapshot.directionX(),
                        snapshot.directionZ());
                double directionX =
                        snapshot.directionX()
                                / directionLength;
                double directionZ =
                        snapshot.directionZ()
                                / directionLength;
                double side =
                        (second - 0.5)
                                * Math.max(
                                0.0,
                                snapshot.width()
                                        - size * 2.0);
                double forward = size
                        + first * Math.max(
                        0.0,
                        snapshot.length()
                                - size * 2.0);
                x = snapshot.centerX()
                        + directionX
                        * forward
                        - directionZ * side;
                z = snapshot.centerZ()
                        + directionZ
                        * forward
                        + directionX * side;
            } else {
                double inner = snapshot.shape()
                        == TelegraphPayload.Shape.DONUT
                        ? snapshot.innerRadius() + size
                        : size;
                double outer = Math.max(
                        inner,
                        snapshot.radius() - size);
                double distance = Math.sqrt(
                        inner * inner
                                + first
                                * (outer
                                * outer
                                - inner * inner));
                double angle = Math.PI * 2.0 * second;
                x = snapshot.centerX()
                        + Math.cos(angle) * distance;
                z = snapshot.centerZ()
                        + Math.sin(angle) * distance;
            }
            if (!TelegraphVisualMath.insideHorizontalShape(
                    snapshot.shape().name(),
                    snapshot.centerX(),
                    snapshot.centerZ(),
                    snapshot.directionX(),
                    snapshot.directionZ(),
                    snapshot.radius(),
                    snapshot.innerRadius(),
                    snapshot.width(),
                    snapshot.length(),
                    size,
                    x,
                    z)) {
                continue;
            }
            Double y = groundY(
                    level,
                    snapshot.id(),
                    x,
                    z,
                    snapshot.centerY());
            if (y == null) {
                continue;
            }
            double angle = Math.PI * 2.0
                    * TelegraphVisualMath
                    .deterministicUnit(
                            snapshot.id(), index, 13);
            patches.add(new Patch(
                    x, y, z,
                    size,
                    angle,
                    0.55
                            + TelegraphVisualMath
                            .deterministicUnit(
                                    snapshot.id(),
                                    index,
                                    14) * 0.35));
        }
        return patches;
    }

    private static void addSegment(
            Level level,
            UUID telegraphId,
            double centerY,
            double x1,
            double z1,
            double x2,
            double z2,
            double brightness,
            List<Segment> output
    ) {
        Double y1 = groundY(
                level, telegraphId, x1, z1, centerY);
        if (y1 == null) {
            return;
        }
        double previousY = y1;
        Double y2 = null;
        for (int probe = 1;
             probe <= CONNECTION_PROBES;
             probe++) {
            double factor =
                    probe / (double) CONNECTION_PROBES;
            double sampleX = x1 + (x2 - x1) * factor;
            double sampleZ = z1 + (z2 - z1) * factor;
            Double currentY = groundY(
                    level,
                    telegraphId,
                    sampleX,
                    sampleZ,
                    centerY);
            if (currentY == null
                    || !TelegraphVisualMath
                    .canConnectSamples(
                            true,
                            previousY,
                            true,
                            currentY,
                            MAX_CONNECTION_HEIGHT)) {
                return;
            }
            previousY = currentY;
            y2 = currentY;
        }
        if (y2 == null) {
            return;
        }
        output.add(new Segment(
                x1, y1, z1,
                x2, y2, z2,
                brightness));
    }

    private static Double groundY(
            Level level,
            UUID telegraphId,
            double x,
            double z,
            double centerY
    ) {
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);
        int baseY = (int) Math.floor(centerY);
        long key = TelegraphVisualMath.groundCacheKey(
                blockX, blockZ, baseY);
        long now = System.nanoTime();
        Map<Long, CachedGround> telegraphCache =
                CACHE.computeIfAbsent(
                        telegraphId,
                        ignored -> new HashMap<>());
        CachedGround cached = telegraphCache.get(key);
        if (cached != null
                && now - cached.sampledAtNanos()
                <= CACHE_NANOS) {
            return cached.valid()
                    ? cached.y()
                    : null;
        }
        if (telegraphCache.size() >= MAX_CACHE_ENTRIES) {
            telegraphCache.clear();
        }
        BlockPos.MutableBlockPos pos =
                new BlockPos.MutableBlockPos();
        Double bestY = null;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (int y = baseY
                - TelegraphClientSettings
                .GROUND_SEARCH_DOWN;
             y <= baseY
                     + TelegraphClientSettings
                     .GROUND_SEARCH_UP;
             y++) {
            pos.set(blockX, y, blockZ);
            BlockState state = level.getBlockState(pos);
            VoxelShape shape =
                    state.getCollisionShape(level, pos);
            double top = shape.isEmpty()
                    ? Double.NaN
                    : shape.max(Direction.Axis.Y);
            boolean clearance =
                    collisionClear(level, pos.above())
                            && collisionClear(
                            level, pos.above(2));
            boolean candidate =
                    TelegraphVisualMath.isFloorCandidate(
                            state.is(BlockTags.LEAVES),
                            state.is(BlockTags.LOGS),
                            state.canBeReplaced(),
                            !state.getFluidState().isEmpty(),
                            shape.isEmpty(),
                            state.isFaceSturdy(
                                    level,
                                    pos,
                                    Direction.UP),
                            clearance);
            if (!candidate) {
                continue;
            }
            if (!Double.isFinite(top)
                    || top <= 0.0) {
                continue;
            }
            double result = y + top
                    + GROUND_OFFSET;
            double distance =
                    TelegraphVisualMath.floorDistance(
                            centerY,
                            result);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestY = result;
            }
        }
        if (bestY != null) {
            telegraphCache.put(
                    key,
                    new CachedGround(true, bestY, now));
            return bestY;
        }
        telegraphCache.put(
                key,
                new CachedGround(
                        false,
                        Double.NaN,
                        now));
        return null;
    }

    private static boolean collisionClear(
            Level level,
            BlockPos pos
    ) {
        BlockState state = level.getBlockState(pos);
        return state.getFluidState().isEmpty()
                && state.getCollisionShape(
                level, pos).isEmpty();
    }

    public record Mesh(
            List<Segment> outline,
            List<Patch> noise
    ) {
    }

    public record Segment(
            double x1,
            double y1,
            double z1,
            double x2,
            double y2,
            double z2,
            double brightness
    ) {
    }

    public record Patch(
            double x,
            double y,
            double z,
            double size,
            double angle,
            double brightness
    ) {
    }

    private record CachedGround(
            boolean valid,
            double y,
            long sampledAtNanos
    ) {
    }
}
