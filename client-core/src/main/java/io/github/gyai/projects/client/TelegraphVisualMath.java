package io.github.gyai.projects.client;

import java.util.UUID;

public final class TelegraphVisualMath {
    private TelegraphVisualMath() {
    }

    public static Phase phase(
            int totalWarningTicks,
            double remainingWarningTicks,
            double imminentThreshold,
            boolean detonated,
            boolean cancelled
    ) {
        if (cancelled) {
            return Phase.CANCELLATION;
        }
        if (detonated) {
            return Phase.DETONATION;
        }
        if (totalWarningTicks > 0
                && Double.isFinite(remainingWarningTicks)
                && Double.isFinite(imminentThreshold)
                && imminentThreshold > 0.0
                && imminentThreshold < 1.0
                && remainingWarningTicks
                <= totalWarningTicks * imminentThreshold) {
            return Phase.IMMINENT;
        }
        return Phase.WARNING;
    }

    public static boolean acceptsRevision(
            long currentRevision,
            long incomingRevision
    ) {
        return incomingRevision > currentRevision;
    }

    public static boolean acceptsPositionUpdate(
            boolean locked,
            long currentRevision,
            long incomingRevision
    ) {
        return !locked
                && acceptsRevision(
                currentRevision,
                incomingRevision);
    }

    public static boolean shouldClear(
            boolean connected,
            String knownDimension,
            String currentDimension
    ) {
        return !connected
                || knownDimension != null
                && !knownDimension.equals(currentDimension);
    }

    public static boolean isFloorCandidate(
            boolean leaves,
            boolean logs,
            boolean replaceable,
            boolean hasFluid,
            boolean collisionEmpty,
            boolean sturdyTop,
            boolean hasClearance
    ) {
        return !leaves
                && !logs
                && !replaceable
                && !hasFluid
                && !collisionEmpty
                && sturdyTop
                && hasClearance;
    }

    public static double floorDistance(
            double anchorY,
            double surfaceY
    ) {
        if (!Double.isFinite(anchorY)
                || !Double.isFinite(surfaceY)) {
            return Double.POSITIVE_INFINITY;
        }
        return Math.abs(surfaceY - anchorY);
    }

    public static boolean canConnectSamples(
            boolean firstValid,
            double firstY,
            boolean secondValid,
            double secondY,
            double maximumHeightDifference
    ) {
        return firstValid
                && secondValid
                && Double.isFinite(firstY)
                && Double.isFinite(secondY)
                && Double.isFinite(maximumHeightDifference)
                && maximumHeightDifference >= 0.0
                && Math.abs(firstY - secondY)
                <= maximumHeightDifference;
    }

    public static boolean insideHorizontalShape(
            String shape,
            double centerX,
            double centerZ,
            double directionX,
            double directionZ,
            double radius,
            double innerRadius,
            double width,
            double length,
            double margin,
            double x,
            double z
    ) {
        if (shape == null
                || !allFinite(
                centerX, centerZ,
                directionX, directionZ,
                radius, innerRadius, width, length,
                margin, x, z)
                || margin < 0.0) {
            return false;
        }
        double dx = x - centerX;
        double dz = z - centerZ;
        return switch (shape) {
            case "CIRCLE" -> {
                double usableRadius = radius - margin;
                yield usableRadius >= 0.0
                        && dx * dx + dz * dz
                        <= usableRadius * usableRadius;
            }
            case "DONUT" -> {
                double minimumRadius =
                        innerRadius + margin;
                double maximumRadius = radius - margin;
                double distanceSquared =
                        dx * dx + dz * dz;
                yield maximumRadius >= minimumRadius
                        && distanceSquared
                        >= minimumRadius * minimumRadius
                        && distanceSquared
                        <= maximumRadius * maximumRadius;
            }
            case "LINE" -> {
                double directionLength = Math.hypot(
                        directionX, directionZ);
                if (directionLength < 0.000_001) {
                    yield false;
                }
                double forward =
                        (dx * directionX
                                + dz * directionZ)
                                / directionLength;
                double side =
                        (dx * -directionZ
                                + dz * directionX)
                                / directionLength;
                yield forward >= margin
                        && forward <= length - margin
                        && Math.abs(side)
                        <= width * 0.5 - margin;
            }
            default -> false;
        };
    }

    public static boolean effectFinished(
            long now,
            long detonationStarted,
            long cancellationStarted,
            long detonationDuration,
            long cancellationDuration
    ) {
        return cancellationStarted != Long.MAX_VALUE
                && now - cancellationStarted
                >= cancellationDuration
                || detonationStarted != Long.MAX_VALUE
                && now - detonationStarted
                >= detonationDuration;
    }

    public static long expiryDeadline(
            long receivedAt,
            int remainingTicks
    ) {
        long duration = Math.max(
                0L, remainingTicks) * 50_000_000L;
        if (Long.MAX_VALUE - receivedAt < duration) {
            return Long.MAX_VALUE;
        }
        return receivedAt + duration;
    }

    public static boolean expired(
            long now,
            long deadline
    ) {
        return now >= deadline;
    }

    public static LineFrame lineFrame(
            double directionX,
            double directionZ,
            double width
    ) {
        double length = Math.hypot(
                directionX, directionZ);
        if (length < 0.000_001
                || !Double.isFinite(width)
                || width <= 0.0) {
            throw new IllegalArgumentException(
                    "Invalid line frame");
        }
        double normalizedX = directionX / length;
        double normalizedZ = directionZ / length;
        return new LineFrame(
                normalizedX,
                normalizedZ,
                -normalizedZ * width * 0.5,
                normalizedX * width * 0.5);
    }

    public static double deterministicUnit(
            UUID id,
            int index,
            int lane
    ) {
        long seed = id.getMostSignificantBits()
                ^ Long.rotateLeft(
                id.getLeastSignificantBits(), 23)
                ^ (long) index * 0x9E3779B97F4A7C15L
                ^ (long) lane * 0xD1B54A32D192ED03L;
        long mixed = mix(seed);
        return (mixed >>> 11)
                * 0x1.0p-53;
    }

    public static long groundCacheKey(
            int blockX,
            int blockZ,
            int centerY
    ) {
        long x = blockX & 0x3FFFFFFL;
        long z = blockZ & 0x3FFFFFFL;
        long y = centerY & 0xFFFL;
        return x << 38 | z << 12 | y;
    }

    private static long mix(long value) {
        value = (value ^ value >>> 30)
                * 0xBF58476D1CE4E5B9L;
        value = (value ^ value >>> 27)
                * 0x94D049BB133111EBL;
        return value ^ value >>> 31;
    }

    private static boolean allFinite(double... values) {
        for (double value : values) {
            if (!Double.isFinite(value)) {
                return false;
            }
        }
        return true;
    }

    public enum Phase {
        WARNING,
        IMMINENT,
        DETONATION,
        CANCELLATION
    }

    public record LineFrame(
            double directionX,
            double directionZ,
            double sideX,
            double sideZ
    ) {
    }
}
