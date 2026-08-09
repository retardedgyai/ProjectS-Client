package io.github.gyai.projects.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class TelegraphRenderer {
    private static final int BASE_ORDER = 15_000;
    private static final double FAR_DISTANCE = 32.0;
    private static final long AMBIENT_PARTICLE_NANOS =
            220_000_000L;

    private TelegraphRenderer() {
    }

    public static void register() {
        LevelRenderEvents.COLLECT_SUBMITS.register(
                TelegraphRenderer::render);
    }

    private static void render(LevelRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null
                || client.player == null
                || client.options.hideGui) {
            return;
        }
        CameraRenderState camera =
                context.levelState().cameraRenderState;
        if (camera == null || camera.pos == null) {
            return;
        }
        long now = System.nanoTime();
        int order = BASE_ORDER;
        for (TelegraphClientState.TrackedTelegraph tracked
                : TelegraphClientState.trackedTelegraphs()) {
            TelegraphPayload.Snapshot snapshot =
                    tracked.snapshot();
            double dx = tracked.x() - camera.pos.x;
            double dy = tracked.y() - camera.pos.y;
            double dz = tracked.z() - camera.pos.z;
            double distanceSquared =
                    dx * dx + dy * dy + dz * dz;
            if (distanceSquared
                    > TelegraphClientSettings.DISPLAY_RANGE
                    * TelegraphClientSettings.DISPLAY_RANGE) {
                continue;
            }
            AABB bounds = bounds(snapshot);
            if (camera.cullFrustum != null
                    && !camera.cullFrustum.isVisible(
                    bounds.inflate(1.0))) {
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
            TelegraphGroundSampler.Mesh mesh =
                    tracked.mesh();
            if (mesh == null
                    || mesh.outline().isEmpty()) {
                continue;
            }
            double distance = Math.sqrt(
                    distanceSquared);
            OrderedSubmitNodeCollector collector =
                    context.submitNodeCollector()
                            .order(order++);
            submit(
                    context,
                    collector,
                    camera,
                    tracked,
                    mesh,
                    now,
                    distance);
        }
    }

    private static void submit(
            LevelRenderContext context,
            OrderedSubmitNodeCollector collector,
            CameraRenderState camera,
            TelegraphClientState.TrackedTelegraph tracked,
            TelegraphGroundSampler.Mesh mesh,
            long now,
            double distance
    ) {
        TelegraphPayload.Snapshot snapshot =
                tracked.snapshot();
        TelegraphVisualMath.Phase phase =
                tracked.phase(now);
        double pulse = pulse(
                snapshot.theme(),
                now,
                tracked.progress(now));
        double effectAlpha =
                tracked.effectAlpha(now);
        double baseAlpha = switch (phase) {
            case WARNING -> 0.24 + pulse * 0.10;
            case IMMINENT -> 0.60 + pulse * 0.24;
            case DETONATION -> 1.0;
            case CANCELLATION -> 0.42;
        };
        if (tracked.lockFlash(now)) {
            baseAlpha = 1.0;
        }
        baseAlpha *= effectAlpha;
        double width = phase
                == TelegraphVisualMath.Phase.IMMINENT
                || phase
                == TelegraphVisualMath.Phase.DETONATION
                ? 0.085
                : 0.040;
        double offsetX = tracked.x()
                - snapshot.centerX();
        double offsetZ = tracked.z()
                - snapshot.centerZ();
        int outlineRgb = outlineRgb(snapshot);
        int noiseRgb = noiseRgb(snapshot);
        int qualityStride = switch (
                TelegraphClientSettings.QUALITY) {
            case LOW -> 2;
            case MEDIUM, HIGH -> 1;
        };
        int stride = (distance > FAR_DISTANCE ? 2 : 1)
                * qualityStride;
        PoseStack poseStack = context.poseStack();
        poseStack.pushPose();
        poseStack.translate(
                -camera.pos.x,
                -camera.pos.y,
                -camera.pos.z);
        double finalBaseAlpha = baseAlpha;
        double finalWidth = width;
        collector.submitCustomGeometry(
                poseStack,
                RenderTypes.debugQuads(),
                (pose, vertices) -> {
                    int index = 0;
                    for (TelegraphGroundSampler.Segment segment
                            : mesh.outline()) {
                        if (index++ % stride != 0
                                || phase
                                == TelegraphVisualMath.Phase
                                .CANCELLATION
                                && index % 3 == 0) {
                            continue;
                        }
                        int color = color(
                                outlineRgb,
                                finalBaseAlpha,
                                segment.brightness());
                        segmentQuad(
                                pose,
                                vertices,
                                segment,
                                offsetX,
                                offsetZ,
                                finalWidth,
                                color);
                    }
                    int noiseStride = phase
                            == TelegraphVisualMath.Phase.IMMINENT
                            ? stride
                            : stride * 2;
                    if (TelegraphClientSettings.QUALITY
                            == TelegraphClientSettings
                            .Quality.LOW) {
                        noiseStride *= 2;
                    } else if (TelegraphClientSettings.QUALITY
                            == TelegraphClientSettings
                            .Quality.HIGH
                            && phase
                            == TelegraphVisualMath.Phase
                            .IMMINENT) {
                        noiseStride = Math.max(
                                1, noiseStride / 2);
                    }
                    index = 0;
                    for (TelegraphGroundSampler.Patch patch
                            : mesh.noise()) {
                        if (index++ % noiseStride != 0) {
                            continue;
                        }
                        int color = color(
                                noiseRgb,
                                finalBaseAlpha * 0.34,
                                patch.brightness());
                        patchQuad(
                                pose,
                                vertices,
                                patch,
                                offsetX,
                                offsetZ,
                                color);
                    }
                });
        poseStack.popPose();
    }

    static void emitTickParticles(
            Minecraft client,
            TelegraphClientState.TrackedTelegraph tracked,
            TelegraphGroundSampler.Mesh mesh,
            long now
    ) {
        ParticleStatus setting =
                client.options.particles().get();
        if (tracked.phase(now)
                == TelegraphVisualMath.Phase.DETONATION
                && !mesh.outline().isEmpty()
                && tracked.consumeDetonationParticles()) {
            int count = setting == ParticleStatus.MINIMAL
                    ? 3
                    : setting == ParticleStatus.DECREASED
                    ? 6
                    : 10;
            count = switch (TelegraphClientSettings.QUALITY) {
                case LOW -> Math.max(2, count / 2);
                case MEDIUM -> count;
                case HIGH -> Math.min(12, count + 2);
            };
            TelegraphGroundSampler.Segment first =
                    mesh.outline().getFirst();
            BlockPos ground = BlockPos.containing(
                    first.x1(),
                    first.y1() - 0.1,
                    first.z1());
            BlockState state =
                    client.level.getBlockState(ground);
            BlockParticleOption block =
                    new BlockParticleOption(
                            ParticleTypes.BLOCK,
                            state);
            for (int index = 0;
                 index < count;
                 index++) {
                double angle = Math.PI * 2.0
                        * TelegraphVisualMath
                        .deterministicUnit(
                                tracked.snapshot().id(),
                                index,
                                30);
                double speed = 0.04
                        + TelegraphVisualMath
                        .deterministicUnit(
                                tracked.snapshot().id(),
                                index,
                                31) * 0.09;
                client.level.addParticle(
                        block,
                        tracked.x(),
                        first.y1() + 0.05,
                        tracked.z(),
                        Math.cos(angle) * speed,
                        0.08 + speed,
                        Math.sin(angle) * speed);
            }
            return;
        }
        if (tracked.phase(now)
                == TelegraphVisualMath.Phase.DETONATION) {
            return;
        }
        if (setting == ParticleStatus.MINIMAL
                || tracked.phase(now)
                == TelegraphVisualMath.Phase
                .CANCELLATION) {
            return;
        }
        long interval = tracked.phase(now)
                == TelegraphVisualMath.Phase.IMMINENT
                ? AMBIENT_PARTICLE_NANOS / 2
                : AMBIENT_PARTICLE_NANOS;
        if (setting == ParticleStatus.DECREASED) {
            interval *= 2;
        }
        if (now - tracked.lastAmbientParticleNanos()
                < interval
                || mesh.noise().isEmpty()) {
            return;
        }
        int index = (int) ((now
                / Math.max(1L, interval))
                % mesh.noise().size());
        TelegraphGroundSampler.Patch patch =
                mesh.noise().get(index);
        double velocityX = tracked.phase(now)
                == TelegraphVisualMath.Phase.IMMINENT
                ? (tracked.x() - patch.x()) * 0.015
                : 0.0;
        double velocityZ = tracked.phase(now)
                == TelegraphVisualMath.Phase.IMMINENT
                ? (tracked.z() - patch.z()) * 0.015
                : 0.0;
        client.level.addParticle(
                ParticleTypes.ASH,
                patch.x(),
                patch.y() + 0.03,
                patch.z(),
                velocityX,
                0.01,
                velocityZ);
        tracked.markAmbientParticles(now);
    }

    private static void segmentQuad(
            PoseStack.Pose pose,
            VertexConsumer vertices,
            TelegraphGroundSampler.Segment segment,
            double offsetX,
            double offsetZ,
            double width,
            int color
    ) {
        double dx = segment.x2() - segment.x1();
        double dz = segment.z2() - segment.z1();
        double length = Math.hypot(dx, dz);
        if (length < 0.000_001) {
            return;
        }
        double sideX = -dz / length * width;
        double sideZ = dx / length * width;
        float x1 = (float) (segment.x1()
                + offsetX);
        float z1 = (float) (segment.z1()
                + offsetZ);
        float x2 = (float) (segment.x2()
                + offsetX);
        float z2 = (float) (segment.z2()
                + offsetZ);
        vertices.addVertex(
                pose,
                (float) (x1 + sideX),
                (float) segment.y1(),
                (float) (z1 + sideZ))
                .setColor(color);
        vertices.addVertex(
                pose,
                (float) (x2 + sideX),
                (float) segment.y2(),
                (float) (z2 + sideZ))
                .setColor(color);
        vertices.addVertex(
                pose,
                (float) (x2 - sideX),
                (float) segment.y2(),
                (float) (z2 - sideZ))
                .setColor(color);
        vertices.addVertex(
                pose,
                (float) (x1 - sideX),
                (float) segment.y1(),
                (float) (z1 - sideZ))
                .setColor(color);
    }

    private static void patchQuad(
            PoseStack.Pose pose,
            VertexConsumer vertices,
            TelegraphGroundSampler.Patch patch,
            double offsetX,
            double offsetZ,
            int color
    ) {
        double cosine = Math.cos(patch.angle());
        double sine = Math.sin(patch.angle());
        double xSide = cosine * patch.size();
        double zSide = sine * patch.size();
        double xForward = -sine
                * patch.size() * 0.35;
        double zForward = cosine
                * patch.size() * 0.35;
        double x = patch.x() + offsetX;
        double z = patch.z() + offsetZ;
        float y = (float) (patch.y() + 0.004);
        vertices.addVertex(
                pose,
                (float) (x - xSide - xForward),
                y,
                (float) (z - zSide - zForward))
                .setColor(color);
        vertices.addVertex(
                pose,
                (float) (x + xSide - xForward),
                y,
                (float) (z + zSide - zForward))
                .setColor(color);
        vertices.addVertex(
                pose,
                (float) (x + xSide + xForward),
                y,
                (float) (z + zSide + zForward))
                .setColor(color);
        vertices.addVertex(
                pose,
                (float) (x - xSide + xForward),
                y,
                (float) (z - zSide + zForward))
                .setColor(color);
    }

    private static AABB bounds(
            TelegraphPayload.Snapshot snapshot
    ) {
        double minX;
        double maxX;
        double minZ;
        double maxZ;
        if (snapshot.shape()
                == TelegraphPayload.Shape.LINE) {
            double endX = snapshot.centerX()
                    + snapshot.directionX()
                    * snapshot.length();
            double endZ = snapshot.centerZ()
                    + snapshot.directionZ()
                    * snapshot.length();
            double margin = snapshot.width() * 0.5;
            minX = Math.min(snapshot.centerX(), endX)
                    - margin;
            maxX = Math.max(snapshot.centerX(), endX)
                    + margin;
            minZ = Math.min(snapshot.centerZ(), endZ)
                    - margin;
            maxZ = Math.max(snapshot.centerZ(), endZ)
                    + margin;
        } else {
            minX = snapshot.centerX()
                    - snapshot.radius();
            maxX = snapshot.centerX()
                    + snapshot.radius();
            minZ = snapshot.centerZ()
                    - snapshot.radius();
            maxZ = snapshot.centerZ()
                    + snapshot.radius();
        }
        return new AABB(
                minX,
                snapshot.centerY() - 3.0,
                minZ,
                maxX,
                snapshot.centerY() + 3.0,
                maxZ);
    }

    private static double pulse(
            TelegraphPayload.VisualTheme theme,
            long now,
            double progress
    ) {
        double seconds = now / 1_000_000_000.0;
        return switch (theme) {
            case DAMAGE -> 0.5
                    + 0.5 * Math.sin(
                    seconds * (2.2 + progress * 9.0));
            case DEBUFF -> 0.5
                    + 0.5 * Math.sin(
                    seconds * 9.7
                            + Math.sin(seconds * 3.1));
            case POISON -> 0.5
                    + 0.5 * Math.sin(seconds * 2.2);
            case SAFE -> 0.5
                    + 0.5 * Math.sin(seconds * 1.8);
            case OPPORTUNITY -> 0.5
                    + 0.5 * Math.sin(seconds * 6.0);
        };
    }

    private static int outlineRgb(
            TelegraphPayload.Snapshot snapshot
    ) {
        return switch (snapshot.theme()) {
            case DAMAGE -> snapshot.style()
                    == TelegraphPayload.VisualStyle
                    .GROHM_STONE_TIDE
                    ? 0xE86B32
                    : 0xF04432;
            case DEBUFF -> 0xA94BDD;
            case POISON -> 0x58C84F;
            case SAFE -> 0x4BB9E8;
            case OPPORTUNITY -> 0xFFE58A;
        };
    }

    private static int noiseRgb(
            TelegraphPayload.Snapshot snapshot
    ) {
        if (snapshot.style()
                == TelegraphPayload.VisualStyle
                .GROHM_STONE_TIDE) {
            return 0x6D8791;
        }
        return outlineRgb(snapshot);
    }

    private static int color(
            int rgb,
            double alpha,
            double brightness
    ) {
        int red = (int) Math.clamp(
                ((rgb >>> 16) & 0xFF) * brightness,
                0.0,
                255.0);
        int green = (int) Math.clamp(
                ((rgb >>> 8) & 0xFF) * brightness,
                0.0,
                255.0);
        int blue = (int) Math.clamp(
                (rgb & 0xFF) * brightness,
                0.0,
                255.0);
        int safeAlpha = (int) Math.clamp(
                alpha * 255.0,
                0.0,
                255.0);
        return safeAlpha << 24
                | red << 16
                | green << 8
                | blue;
    }
}
