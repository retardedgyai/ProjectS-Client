package io.github.gyai.projects.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

public final class MonsterUiRenderer {
    private static final int FULL_LIGHT = 0x00F000F0;

    private MonsterUiRenderer() {
    }

    public static void register() {
        LevelRenderEvents.COLLECT_SUBMITS.register(
                MonsterUiRenderer::render);
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
        int order = 20_000;
        for (MonsterUiClientState.TrackedMonster tracked
                : MonsterUiClientState.trackedMonsters()) {
            Entity entity = client.level.getEntity(
                    tracked.networkEntityId());
            if (entity == null
                    || !entity.isAlive()
                    || entity.isRemoved()
                    || !entity.getUUID().equals(
                            tracked.entityId())) {
                continue;
            }
            double distanceSquared =
                    entity.distanceToSqr(camera.pos);
            double displayRange =
                    tracked.snapshot().displayRange();
            if (distanceSquared
                    > displayRange * displayRange
                    || camera.cullFrustum != null
                    && !camera.cullFrustum.isVisible(
                            entity.getBoundingBox().inflate(0.5))) {
                continue;
            }
            tracked.updateAnimation(now);
            int alpha = alphaForDistance(
                    Math.sqrt(distanceSquared),
                    displayRange);
            if (alpha <= 0) {
                continue;
            }
            renderMonster(
                    context,
                    context.submitNodeCollector().order(order++),
                    client.font,
                    camera,
                    entity,
                    tracked,
                    now,
                    alpha);
        }
    }

    private static void renderMonster(
            LevelRenderContext context,
            OrderedSubmitNodeCollector collector,
            Font font,
            CameraRenderState camera,
            Entity entity,
            MonsterUiClientState.TrackedMonster tracked,
            long now,
            int alpha
    ) {
        MonsterUiPayload.Entry snapshot = tracked.snapshot();
        float partialTick = Minecraft.getInstance()
                .getDeltaTracker()
                .getGameTimeDeltaPartialTick(false);
        Vec3 entityPosition = entity.getPosition(partialTick);
        PoseStack poseStack = context.poseStack();
        poseStack.pushPose();
        poseStack.translate(
                entityPosition.x - camera.pos.x,
                entityPosition.y - camera.pos.y
                        + entity.getBbHeight() + 0.55,
                entityPosition.z - camera.pos.z);
        poseStack.mulPose(camera.orientation);
        float scale = switch (snapshot.rank()) {
            case NORMAL -> 0.0165f;
            case ELITE -> 0.0175f;
            case BOSS -> 0.0185f;
        };
        poseStack.scale(-scale, -scale, scale);

        int width = MonsterUiVisuals.barWidth(snapshot.rank());
        int y = 0;
        MonsterUiPayload.HardControl hard =
                snapshot.hardControl();
        double hardRemaining = hard == null
                ? 0.0
                : tracked.remainingTicksExact(
                        hard.remainingTicks(), now);
        if (hard != null && hardRemaining > 0) {
            String label = "["
                    + MonsterUiVisuals.hardControlName(hard.type())
                    + "] "
                    + formatSeconds(hardRemaining);
            drawCenteredText(
                    collector,
                    poseStack,
                    font,
                    label,
                    y,
                    MonsterUiVisuals.hardControlColor(
                            hard.type(), alpha));
            y += 10;
            float ratio = (float) Math.clamp(
                    hardRemaining / hard.totalTicks(),
                    0.0f,
                    1.0f);
            submitBarGeometry(
                    collector,
                    poseStack,
                    width,
                    y,
                    4,
                    ratio,
                    ratio,
                    MonsterUiVisuals.hardControlColor(
                            hard.type(), alpha),
                    0,
                    false,
                    alpha);
            y += 8;
        }

        drawNameLine(
                collector,
                poseStack,
                font,
                snapshot,
                y,
                alpha);
        y += 11;

        double maximum = snapshot.maximumHealth();
        float displayedRatio = (float) Math.clamp(
                tracked.displayedHealth() / maximum,
                0.0,
                1.0);
        float trailingRatio = (float) Math.clamp(
                tracked.trailingHealth() / maximum,
                displayedRatio,
                1.0);
        submitBarGeometry(
                collector,
                poseStack,
                width,
                y,
                10,
                displayedRatio,
                trailingRatio,
                MonsterUiVisuals.withAlpha(
                        0xFFD93636, alpha),
                MonsterUiVisuals.withAlpha(
                        0xFFFF8A72, alpha),
                tracked.healFlash(now),
                alpha);
        String health = formatHealth(
                tracked.displayedHealth(), maximum);
        drawCenteredText(
                collector,
                poseStack,
                font,
                health,
                y + 1,
                MonsterUiVisuals.withAlpha(
                        0xFFFFFFFF, alpha));
        y += 13;
        drawStatuses(
                collector,
                poseStack,
                font,
                tracked,
                now,
                y,
                alpha);
        poseStack.popPose();
    }

    private static void drawNameLine(
            OrderedSubmitNodeCollector collector,
            PoseStack poseStack,
            Font font,
            MonsterUiPayload.Entry snapshot,
            int y,
            int alpha
    ) {
        String prefix =
                MonsterUiVisuals.rankPrefix(snapshot.rank());
        String level = "Lv." + snapshot.monsterLevel() + " ";
        String name = snapshot.displayName();
        int prefixWidth = font.width(prefix);
        int levelWidth = font.width(level);
        int nameWidth = font.width(name);
        float x = -(prefixWidth + levelWidth + nameWidth) / 2.0f;
        int rankColor = MonsterUiVisuals.rankColor(
                snapshot.rank(), alpha);
        if (!prefix.isEmpty()) {
            drawText(
                    collector, poseStack, font,
                    prefix, x, y, rankColor);
            x += prefixWidth;
        }
        drawText(
                collector,
                poseStack,
                font,
                level,
                x,
                y,
                MonsterUiVisuals.threatColor(
                        snapshot.threatBand(), alpha));
        x += levelWidth;
        drawText(
                collector,
                poseStack,
                font,
                name,
                x,
                y,
                rankColor);
    }

    private static void drawStatuses(
            OrderedSubmitNodeCollector collector,
            PoseStack poseStack,
            Font font,
            MonsterUiClientState.TrackedMonster tracked,
            long now,
            int y,
            int alpha
    ) {
        int visibleCount = 0;
        int totalWidth = 0;
        for (MonsterUiPayload.Status status
                : tracked.snapshot().statuses()) {
            double remaining = tracked.remainingTicksExact(
                    status.remainingTicks(), now);
            if (remaining <= 0 || visibleCount >= 4) {
                continue;
            }
            String text = statusText(status, remaining);
            totalWidth += font.width(text);
            if (visibleCount > 0) {
                totalWidth += 5;
            }
            visibleCount++;
        }
        if (visibleCount == 0) {
            return;
        }
        float x = -totalWidth / 2.0f;
        int drawn = 0;
        for (MonsterUiPayload.Status status
                : tracked.snapshot().statuses()) {
            double remaining = tracked.remainingTicksExact(
                    status.remainingTicks(), now);
            if (remaining <= 0 || drawn >= 4) {
                continue;
            }
            if (drawn > 0) {
                x += 5;
            }
            String text = statusText(status, remaining);
            drawText(
                    collector,
                    poseStack,
                    font,
                    text,
                    x,
                    y,
                    MonsterUiVisuals.statusColor(
                            status.type(), alpha));
            x += font.width(text);
            drawn++;
        }
    }

    private static void submitBarGeometry(
            OrderedSubmitNodeCollector collector,
            PoseStack poseStack,
            int width,
            int y,
            int height,
            float primaryRatio,
            float trailingRatio,
            int primaryColor,
            int trailingColor,
            boolean healFlash,
            int alpha
    ) {
        float left = -width / 2.0f;
        float right = left + width;
        float top = y;
        float bottom = y + height;
        float primaryRight = left + width * primaryRatio;
        float trailingRight = left + width * trailingRatio;
        int background = MonsterUiVisuals.withAlpha(
                0xD90A0D12, Math.round(alpha * 0.85f));
        int border = MonsterUiVisuals.withAlpha(
                0xFF7B8592, alpha);
        int flash = MonsterUiVisuals.withAlpha(
                0xFFFFE89A, Math.round(alpha * 0.7f));
        collector.submitCustomGeometry(
                poseStack,
                RenderTypes.debugQuads(),
                (pose, vertices) -> {
                    quad(pose, vertices, left, top, right, bottom,
                            0.012f, background);
                    if (trailingColor != 0
                            && trailingRight > left) {
                        quad(pose, vertices, left, top,
                                trailingRight, bottom,
                                0.010f, trailingColor);
                    }
                    if (primaryRight > left) {
                        quad(pose, vertices, left, top,
                                primaryRight, bottom,
                                0.008f, primaryColor);
                    }
                    if (healFlash && primaryRight > left) {
                        quad(pose, vertices, left, top,
                                primaryRight, top + 2,
                                0.006f, flash);
                    }
                    quad(pose, vertices, left, top,
                            right, top + 1,
                            0.004f, border);
                    quad(pose, vertices, left, bottom - 1,
                            right, bottom,
                            0.004f, border);
                    quad(pose, vertices, left, top,
                            left + 1, bottom,
                            0.004f, border);
                    quad(pose, vertices, right - 1, top,
                            right, bottom,
                            0.004f, border);
                });
    }

    private static void quad(
            PoseStack.Pose pose,
            VertexConsumer vertices,
            float left,
            float top,
            float right,
            float bottom,
            float z,
            int color
    ) {
        vertices.addVertex(pose, left, bottom, z).setColor(color);
        vertices.addVertex(pose, right, bottom, z).setColor(color);
        vertices.addVertex(pose, right, top, z).setColor(color);
        vertices.addVertex(pose, left, top, z).setColor(color);
    }

    private static void drawCenteredText(
            OrderedSubmitNodeCollector collector,
            PoseStack poseStack,
            Font font,
            String text,
            int y,
            int color
    ) {
        drawText(
                collector,
                poseStack,
                font,
                text,
                -font.width(text) / 2.0f,
                y,
                color);
    }

    private static void drawText(
            OrderedSubmitNodeCollector collector,
            PoseStack poseStack,
            Font font,
            String text,
            float x,
            float y,
            int color
    ) {
        FormattedCharSequence sequence =
                Component.literal(text).getVisualOrderText();
        collector.submitText(
                poseStack,
                x,
                y,
                sequence,
                true,
                Font.DisplayMode.NORMAL,
                color,
                0,
                FULL_LIGHT,
                0);
    }

    private static String statusText(
            MonsterUiPayload.Status status,
            double remainingTicks
    ) {
        return MonsterUiVisuals.statusLabel(status.type())
                + " " + formatSeconds(remainingTicks);
    }

    private static String formatSeconds(double ticks) {
        return String.format(
                Locale.ROOT, "%.1fs", ticks / 20.0);
    }

    private static String formatHealth(
            double current,
            double maximum
    ) {
        long currentValue = current > 0.0
                ? Math.max(1L, Math.round(current))
                : 0L;
        long maximumValue = Math.max(1L, Math.round(maximum));
        return String.format(
                Locale.ROOT,
                "%,d / %,d",
                currentValue,
                maximumValue);
    }

    private static int alphaForDistance(
            double distance,
            double displayRange
    ) {
        double fadeStartDistance = displayRange * 0.75;
        if (distance <= fadeStartDistance) {
            return 255;
        }
        double ratio = 1.0
                - (distance - fadeStartDistance)
                / (displayRange - fadeStartDistance);
        return (int) Math.round(
                Math.clamp(ratio, 0.0, 1.0) * 255.0);
    }
}
