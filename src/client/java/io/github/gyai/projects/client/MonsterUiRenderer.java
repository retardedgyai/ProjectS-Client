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
    private static final float BACKGROUND_Z = 0.000f;
    private static final float TRAILING_Z = 0.001f;
    private static final float PRIMARY_Z = 0.002f;
    private static final float HEAL_FLASH_Z = 0.003f;
    private static final float BORDER_Z = 0.004f;
    private static final float TEXT_Z = 0.006f;

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
            int alpha = MonsterUiVisuals.alphaForDistance(
                    Math.sqrt(distanceSquared),
                    displayRange);
            if (alpha <= 0) {
                continue;
            }
            OrderedSubmitNodeCollector geometryCollector =
                    context.submitNodeCollector().order(order++);
            OrderedSubmitNodeCollector textCollector =
                    context.submitNodeCollector().order(order++);
            renderMonster(
                    context,
                    geometryCollector,
                    textCollector,
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
            OrderedSubmitNodeCollector geometryCollector,
            OrderedSubmitNodeCollector textCollector,
            Font font,
            CameraRenderState camera,
            Entity entity,
            MonsterUiClientState.TrackedMonster tracked,
            long now,
            int alpha
    ) {
        MonsterUiPayload.Entry snapshot = tracked.snapshot();
        double maximum = snapshot.maximumHealth();
        if (!MonsterUiVisuals.hasValidMaximumHealth(maximum)) {
            return;
        }
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
        float scale = MonsterUiVisuals.scale(snapshot.rank());
        poseStack.scale(scale, -scale, scale);

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
                    textCollector,
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
                    geometryCollector,
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
                textCollector,
                poseStack,
                font,
                snapshot,
                y,
                alpha);
        y += 11;

        double displayedHealth = MonsterUiVisuals.clampHealth(
                tracked.displayedHealth(), maximum);
        double trailingHealth = Math.max(
                displayedHealth,
                MonsterUiVisuals.clampHealth(
                        tracked.trailingHealth(), maximum));
        float displayedRatio = MonsterUiVisuals.healthRatio(
                displayedHealth, maximum);
        float trailingRatio = Math.max(
                displayedRatio,
                MonsterUiVisuals.healthRatio(
                        trailingHealth, maximum));
        submitBarGeometry(
                geometryCollector,
                poseStack,
                width,
                y,
                MonsterUiVisuals.healthBarHeight(),
                displayedRatio,
                trailingRatio,
                MonsterUiVisuals.withAlpha(
                        0xFFD93636, alpha),
                MonsterUiVisuals.withAlpha(
                        0xFFFF8A72, alpha),
                tracked.healFlash(now),
                alpha);
        String health = MonsterUiVisuals.formatHealth(
                displayedHealth, maximum);
        drawCenteredText(
                textCollector,
                poseStack,
                font,
                health,
                y,
                MonsterUiVisuals.withAlpha(
                        0xFFFFFFFF, alpha));
        y += MonsterUiVisuals.healthBarHeight() + 3;
        drawStatuses(
                textCollector,
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
        float primaryRight = left + MonsterUiVisuals.fillWidth(
                width, primaryRatio);
        float trailingRight = left + MonsterUiVisuals.fillWidth(
                width, Math.max(primaryRatio, trailingRatio));
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
                            BACKGROUND_Z, background);
                    if (trailingColor != 0
                            && trailingRight > left) {
                        quad(pose, vertices, left, top,
                                trailingRight, bottom,
                                TRAILING_Z, trailingColor);
                    }
                    if (primaryRight > left) {
                        quad(pose, vertices, left, top,
                                primaryRight, bottom,
                                PRIMARY_Z, primaryColor);
                    }
                    if (healFlash && primaryRight > left) {
                        quad(pose, vertices, left, top,
                                primaryRight, top + 2,
                                HEAL_FLASH_Z, flash);
                    }
                    quad(pose, vertices, left, top,
                            right, top + 1,
                            BORDER_Z, border);
                    quad(pose, vertices, left, bottom - 1,
                            right, bottom,
                            BORDER_Z, border);
                    quad(pose, vertices, left, top,
                            left + 1, bottom,
                            BORDER_Z, border);
                    quad(pose, vertices, right - 1, top,
                            right, bottom,
                            BORDER_Z, border);
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
        poseStack.pushPose();
        poseStack.translate(0.0f, 0.0f, TEXT_Z);
        collector.submitText(
                poseStack,
                x,
                y,
                sequence,
                true,
                Font.DisplayMode.NORMAL,
                FULL_LIGHT,
                color,
                0,
                0);
        poseStack.popPose();
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

}
