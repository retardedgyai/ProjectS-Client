package io.github.gyai.projects.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.gyai.projects.client.beta.BetaClientRuntime;
import io.github.gyai.projects.client.beta.ElementStatePayloadV1;
import io.github.gyai.projects.client.beta.ElementStatusRenderRoute;
import io.github.gyai.projects.client.beta.FireStatusClientStore;
import io.github.gyai.projects.client.beta.IceStatusClientStore;
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
import net.minecraft.world.phys.EntityHitResult;
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
    private static final double STANDALONE_DISPLAY_RANGE = 64.0;
    private static final double STANDALONE_NAME_CLEARANCE = 0.80;

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
        long nowMillis = System.currentTimeMillis();
        FireStatusClientStore.View fire = BetaClientRuntime.fireStatus(nowMillis).orElse(null);
        IceStatusClientStore.View ice = BetaClientRuntime.iceStatus(nowMillis).orElse(null);
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
                    alpha,
                    fire,
                    ice);
        }
        renderStandaloneElements(
                context,
                client,
                camera,
                fire,
                ice,
                order);
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
            int alpha,
            FireStatusClientStore.View fire,
            IceStatusClientStore.View ice
    ) {
        MonsterUiPayload.Entry snapshot = tracked.snapshot();
        double maximum = snapshot.maximumHealth();
        if (!MonsterUiVisuals.hasValidMaximumHealth(maximum)) {
            return;
        }
        PoseStack poseStack = context.poseStack();
        poseStack.pushPose();
        positionEntityBillboard(
                poseStack,
                camera,
                entity,
                entity.getBbHeight() + 0.55,
                MonsterUiVisuals.scale(snapshot.rank()));

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
        if (fire != null
                && fire.targetNetworkId() == tracked.networkEntityId()) {
            drawFireStatus(geometryCollector, textCollector, poseStack, font,
                    fire, y, alpha);
            y += 11;
        }
        if (ice != null
                && ice.targetNetworkId() == tracked.networkEntityId()) {
            y += drawIceStatus(geometryCollector, textCollector, poseStack, font,
                    ice, y, alpha);
        }
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

    private static void renderStandaloneElements(
            LevelRenderContext context,
            Minecraft client,
            CameraRenderState camera,
            FireStatusClientStore.View fire,
            IceStatusClientStore.View ice,
            int order
    ) {
        if (client.level == null) {
            return;
        }
        ElementStatusRenderRoute.Selection selection = ElementStatusRenderRoute.select(
                fire == null ? null : new ElementStatusRenderRoute.Status(
                        fire.targetNetworkId(), fire.stateRevision()),
                ice == null ? null : new ElementStatusRenderRoute.Status(
                        ice.targetNetworkId(), ice.stateRevision())).orElse(null);
        if (selection == null) return;
        int targetNetworkId = selection.targetNetworkId();
        Entity entity = client.level.getEntity(targetNetworkId);
        if (entity == null || entity.isRemoved() || !entity.isAlive()) {
            BetaClientRuntime.clearElementTarget(targetNetworkId);
            return;
        }
        double distanceSquared = entity.distanceToSqr(camera.pos);
        boolean withinDisplayRange = distanceSquared
                <= STANDALONE_DISPLAY_RANGE * STANDALONE_DISPLAY_RANGE;
        boolean visibleInFrustum = camera.cullFrustum == null
                || camera.cullFrustum.isVisible(
                        entity.getBoundingBox().inflate(0.5));
        boolean selectedTarget = client.hitResult
                instanceof EntityHitResult entityHit
                && entityHit.getEntity().getId() == targetNetworkId;
        ElementStatusRenderRoute.Route route = ElementStatusRenderRoute.decide(
                new ElementStatusRenderRoute.Input(
                        targetNetworkId,
                        true,
                        false,
                        entity.getId(),
                        true,
                        entity.isRemoved(),
                        entity.isAlive(),
                        true,
                        withinDisplayRange && visibleInFrustum,
                        client.options.hideGui,
                        selectedTarget,
                        MonsterUiClientState.tracksNetworkEntity(
                                targetNetworkId)));
        if (route != ElementStatusRenderRoute.Route.STANDALONE) {
            return;
        }
        int alpha = MonsterUiVisuals.alphaForDistance(
                Math.sqrt(distanceSquared),
                STANDALONE_DISPLAY_RANGE);
        if (alpha <= 0) {
            return;
        }
        OrderedSubmitNodeCollector geometryCollector =
                context.submitNodeCollector().order(order);
        OrderedSubmitNodeCollector textCollector =
                context.submitNodeCollector().order(order + 1);
        PoseStack poseStack = context.poseStack();
        poseStack.pushPose();
        positionEntityBillboard(
                poseStack,
                camera,
                entity,
                entity.getBbHeight() + STANDALONE_NAME_CLEARANCE,
                MonsterUiVisuals.scale(
                        MonsterUiPayload.MonsterRank.NORMAL));
        int y = 0;
        if (selection.fireVisible() && fire != null
                && fire.targetNetworkId() == targetNetworkId) {
            drawFireStatus(geometryCollector, textCollector, poseStack,
                    client.font, fire, y, alpha);
            y += 11;
        }
        if (selection.iceVisible() && ice != null
                && ice.targetNetworkId() == targetNetworkId) {
            drawIceStatus(geometryCollector, textCollector, poseStack,
                    client.font, ice, y, alpha);
        }
        poseStack.popPose();
    }

    private static void positionEntityBillboard(
            PoseStack poseStack,
            CameraRenderState camera,
            Entity entity,
            double heightOffset,
            float scale
    ) {
        float partialTick = Minecraft.getInstance()
                .getDeltaTracker()
                .getGameTimeDeltaPartialTick(false);
        Vec3 entityPosition = entity.getPosition(partialTick);
        poseStack.translate(
                entityPosition.x - camera.pos.x,
                entityPosition.y - camera.pos.y + heightOffset,
                entityPosition.z - camera.pos.z);
        poseStack.mulPose(camera.orientation);
        poseStack.scale(scale, -scale, scale);
    }

    private static void drawFireStatus(
            OrderedSubmitNodeCollector geometry,
            OrderedSubmitNodeCollector text,
            PoseStack poseStack,
            Font font,
            FireStatusClientStore.View fire,
            int y,
            int alpha
    ) {
        int iconColor = fire.detonationFlash() ? 0xFFFFF1A8
                : fire.warning() ? 0xFFFF493D
                : fire.stackPulse() ? 0xFFFFB347 : 0xFFFF7A32;
        if (fire.decayActive() && !fire.detonationFlash()) iconColor = 0xFFD66B3A;
        iconColor = MonsterUiVisuals.withAlpha(iconColor, alpha);
        int progressColor = MonsterUiVisuals.withAlpha(0xFFFFB347, alpha);
        String stack = Integer.toString(fire.fireStacks())
                + (fire.decayActive() ? " ↓" : "");
        int stackWidth = font.width(stack);
        float left = -(8 + 3 + stackWidth) / 2.0f;
        float iconLeft = left;
        float iconTop = y;
        float fillRight = iconLeft + 8.0f * (float) fire.fractionalProgress();
        int finalIconColor = iconColor;
        geometry.submitCustomGeometry(poseStack, RenderTypes.debugQuads(),
                (pose, vertices) -> {
                    // ProjectS flame silhouette; this is not Minecraft's fire overlay.
                    quad(pose, vertices, iconLeft + 3, iconTop,
                            iconLeft + 6, iconTop + 3, PRIMARY_Z, finalIconColor);
                    quad(pose, vertices, iconLeft + 1, iconTop + 3,
                            iconLeft + 7, iconTop + 8, PRIMARY_Z, finalIconColor);
                    quad(pose, vertices, iconLeft + 3, iconTop + 5,
                            iconLeft + 5, iconTop + 8, HEAL_FLASH_Z,
                            MonsterUiVisuals.withAlpha(0xFFFFD37A, alpha));
                    quad(pose, vertices, iconLeft, iconTop + 9,
                            iconLeft + 8, iconTop + 10, BACKGROUND_Z,
                            MonsterUiVisuals.withAlpha(0xFF30140E, alpha));
                    if (fillRight > iconLeft) {
                        quad(pose, vertices, iconLeft, iconTop + 9,
                                fillRight, iconTop + 10, PRIMARY_Z, progressColor);
                    }
                    if (fire.detonationFlash()) {
                        quad(pose, vertices, iconLeft - 1, iconTop - 1,
                                iconLeft + 9, iconTop, BORDER_Z, finalIconColor);
                        quad(pose, vertices, iconLeft - 1, iconTop + 9,
                                iconLeft + 9, iconTop + 10, BORDER_Z, finalIconColor);
                    }
                });
        drawText(text, poseStack, font, stack, left + 11, y + 1,
                fire.warning() ? MonsterUiVisuals.withAlpha(0xFFFF8A72, alpha)
                        : MonsterUiVisuals.withAlpha(0xFFFFD7B0, alpha));
    }

    private static int drawIceStatus(
            OrderedSubmitNodeCollector geometry,
            OrderedSubmitNodeCollector text,
            PoseStack poseStack,
            Font font,
            IceStatusClientStore.View ice,
            int y,
            int alpha
    ) {
        boolean coldVisible = ice.coldGauge() > 0.0
                || ice.coldStage() != ElementStatePayloadV1.ColdStage.NONE
                || ice.frozen();
        String immunity = ice.immunityRemainingMillis() > 0
                ? String.format(Locale.ROOT, "IMMUNE %.1fs",
                ice.immunityRemainingMillis() / 1_000.0)
                : "";
        String primary = coldVisible ? iceLabel(ice) : immunity;
        boolean secondLine = coldVisible && !immunity.isEmpty();
        int primaryWidth = font.width(primary);
        float left = -(8 + 3 + primaryWidth) / 2.0f;
        float iconLeft = left;
        float iconTop = y;
        int iconColor = iceColor(ice, alpha, !coldVisible);
        int accentColor = MonsterUiVisuals.withAlpha(
                ice.freezeFlash() ? 0xFFFFFFFF : 0xFFB8F7FF, alpha);

        geometry.submitCustomGeometry(poseStack, RenderTypes.debugQuads(),
                (pose, vertices) -> {
                    // ProjectS procedural snowflake; no vanilla freeze texture or overlay.
                    quad(pose, vertices, iconLeft + 3, iconTop,
                            iconLeft + 5, iconTop + 8, PRIMARY_Z, iconColor);
                    quad(pose, vertices, iconLeft, iconTop + 3,
                            iconLeft + 8, iconTop + 5, PRIMARY_Z, iconColor);
                    quad(pose, vertices, iconLeft + 1, iconTop + 1,
                            iconLeft + 3, iconTop + 3, PRIMARY_Z, iconColor);
                    quad(pose, vertices, iconLeft + 5, iconTop + 1,
                            iconLeft + 7, iconTop + 3, PRIMARY_Z, iconColor);
                    quad(pose, vertices, iconLeft + 1, iconTop + 5,
                            iconLeft + 3, iconTop + 7, PRIMARY_Z, iconColor);
                    quad(pose, vertices, iconLeft + 5, iconTop + 5,
                            iconLeft + 7, iconTop + 7, PRIMARY_Z, iconColor);
                    if (ice.stagePulse()) {
                        quad(pose, vertices, iconLeft + 3, iconTop - 1,
                                iconLeft + 5, iconTop, BORDER_Z, accentColor);
                        quad(pose, vertices, iconLeft + 3, iconTop + 8,
                                iconLeft + 5, iconTop + 9, BORDER_Z, accentColor);
                        quad(pose, vertices, iconLeft - 1, iconTop + 3,
                                iconLeft, iconTop + 5, BORDER_Z, accentColor);
                        quad(pose, vertices, iconLeft + 8, iconTop + 3,
                                iconLeft + 9, iconTop + 5, BORDER_Z, accentColor);
                    }
                    if (ice.freezeFlash()) {
                        quad(pose, vertices, iconLeft - 1, iconTop - 1,
                                iconLeft + 9, iconTop, BORDER_Z, accentColor);
                        quad(pose, vertices, iconLeft - 1, iconTop + 8,
                                iconLeft + 9, iconTop + 9, BORDER_Z, accentColor);
                        quad(pose, vertices, iconLeft - 1, iconTop,
                                iconLeft, iconTop + 9, BORDER_Z, accentColor);
                        quad(pose, vertices, iconLeft + 8, iconTop,
                                iconLeft + 9, iconTop + 9, BORDER_Z, accentColor);
                    }
                    if (ice.shatterFlash()) {
                        // Short procedural crack rays derived from Frozen -> immunity.
                        quad(pose, vertices, iconLeft + 8, iconTop + 2,
                                iconLeft + 11, iconTop + 3, BORDER_Z, accentColor);
                        quad(pose, vertices, iconLeft + 10, iconTop + 2,
                                iconLeft + 11, iconTop + 5, BORDER_Z, accentColor);
                        quad(pose, vertices, iconLeft - 3, iconTop + 6,
                                iconLeft, iconTop + 7, BORDER_Z, accentColor);
                        quad(pose, vertices, iconLeft - 3, iconTop + 4,
                                iconLeft - 2, iconTop + 7, BORDER_Z, accentColor);
                    }
                });
        drawText(text, poseStack, font, primary, left + 11, y + 1,
                iceTextColor(ice, alpha, !coldVisible));
        if (secondLine) {
            drawCenteredText(text, poseStack, font, immunity, y + 10,
                    MonsterUiVisuals.withAlpha(0xFF9BB5C9, alpha));
            return 20;
        }
        return 11;
    }

    private static String iceLabel(IceStatusClientStore.View ice) {
        String gauge = formatColdGauge(ice.coldGauge());
        if (ice.frozen() || ice.coldStage() == ElementStatePayloadV1.ColdStage.FROZEN) {
            return "FROZEN " + gauge;
        }
        return switch (ice.coldStage()) {
            case NONE -> "Cold " + gauge;
            case CHILLED -> "Cold I " + gauge;
            case DEEP_CHILL -> "Cold II " + gauge;
            case FROZEN -> "FROZEN " + gauge;
        };
    }

    private static String formatColdGauge(double gauge) {
        if (gauge <= Long.MAX_VALUE && gauge == Math.rint(gauge)) {
            return Long.toString((long) gauge);
        }
        return String.format(Locale.ROOT, "%.1f", gauge);
    }

    private static int iceColor(
            IceStatusClientStore.View ice, int alpha, boolean immunityOnly
    ) {
        int color;
        if (immunityOnly) color = 0xFF8199AD;
        else if (ice.freezeFlash()) color = 0xFFFFFFFF;
        else if (ice.frozen()) color = 0xFFE9FFFF;
        else if (ice.stagePulse()) color = 0xFFF4FFFF;
        else color = switch (ice.coldStage()) {
            case NONE -> 0xFFB8D3E8;
            case CHILLED -> 0xFFAADFFF;
            case DEEP_CHILL -> 0xFF51E6FF;
            case FROZEN -> 0xFFE9FFFF;
        };
        return MonsterUiVisuals.withAlpha(color, alpha);
    }

    private static int iceTextColor(
            IceStatusClientStore.View ice, int alpha, boolean immunityOnly
    ) {
        int color = immunityOnly ? 0xFF9BB5C9
                : ice.frozen() ? 0xFFF2FFFF
                : ice.coldStage() == ElementStatePayloadV1.ColdStage.DEEP_CHILL
                ? 0xFF8AF0FF : 0xFFC9E8FF;
        return MonsterUiVisuals.withAlpha(color, alpha);
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
