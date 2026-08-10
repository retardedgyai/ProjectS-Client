package io.github.gyai.projects.client;

import net.minecraft.ChatFormatting;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

public final class ProjectSSkillHud {
    private static final int SLOT_SIZE = 22;
    private static final int SLOT_GAP = 2;
    private static final int SLOT_COUNT = 4;
    private static final int BAR_WIDTH = 94;
    private static final int ACCENT = 0xFF47D7FF;
    private static final int READY = 0xFF52C7A5;
    private static final int LOCKED = 0xFF3A414C;
    private static final Identifier SCOUT_ABILITIES = Identifier.fromNamespaceAndPath(
            ProjectSClient.MOD_ID, "textures/gui/scout_abilities.png");

    private static HudStatePayload.HudState state = HudStatePayload.HudState.hidden();
    private static long stateReceivedAtMillis;

    private ProjectSSkillHud() {
    }

    public static void register() {
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(ProjectSClient.MOD_ID, "skill_hud"),
                ProjectSSkillHud::render
        );
    }

    public static void update(HudStatePayload.HudState updatedState) {
        state = updatedState;
        stateReceivedAtMillis = System.currentTimeMillis();
    }

    public static void showHoveredTooltip(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY
    ) {
        Minecraft client = Minecraft.getInstance();
        if (!state.visible()
                || client.player == null
                || client.options.hideGui
                || state.slots().isEmpty()) {
            return;
        }

        int totalWidth = SLOT_SIZE * SLOT_COUNT + SLOT_GAP * (SLOT_COUNT - 1);
        int startX = (graphics.guiWidth() - totalWidth) / 2;
        int startY = graphics.guiHeight() - 62;
        if (mouseX < startX
                || mouseX >= startX + totalWidth
                || mouseY < startY
                || mouseY >= startY + SLOT_SIZE) {
            return;
        }

        int offset = mouseX - startX;
        int slotIndex = offset / (SLOT_SIZE + SLOT_GAP);
        if (slotIndex < 0
                || slotIndex >= state.slots().size()
                || offset % (SLOT_SIZE + SLOT_GAP) >= SLOT_SIZE) {
            return;
        }

        HudStatePayload.HudState.SkillSlot slot = state.slots().get(slotIndex);
        SkillCatalog.Skill skill = SkillCatalog.findHudSkill(
                state.className(), slot.skillId(),
                slotIndex, slot.name());
        graphics.setTooltipForNextFrame(
                client.font,
                buildTooltip(client.font, slot, skill),
                mouseX,
                mouseY
        );
    }

    public static HudStatePayload.HudState state() {
        return state;
    }

    private static void render(GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (!state.visible() || client.player == null || client.options.hideGui) {
            return;
        }

        Font font = client.font;
        int totalWidth = SLOT_SIZE * SLOT_COUNT + SLOT_GAP * (SLOT_COUNT - 1);
        int startX = (graphics.guiWidth() - totalWidth) / 2;
        int startY = graphics.guiHeight() - 62;

        if (!state.resourceName().isBlank() && state.resourceMaximum() > 0) {
            drawHeader(graphics, font, startX, startY - 24);
        }
        graphics.fill(
                startX - 3, startY - 3,
                startX + totalWidth + 3, startY + SLOT_SIZE + 3,
                0xA60A0D12
        );
        graphics.outline(
                startX - 3, startY - 3,
                totalWidth + 6, SLOT_SIZE + 6,
                0x70343D49
        );
        boolean scoutIcons = state.className().equals("Scout");
        for (int index = 0; index < SLOT_COUNT; index++) {
            HudStatePayload.HudState.SkillSlot slot = index < state.slots().size()
                    ? state.slots().get(index)
                    : null;
            if (slot != null) {
                drawSlot(graphics, font, slot,
                        startX + index * (SLOT_SIZE + SLOT_GAP), startY,
                        index, scoutIcons);
            }
        }
    }

    private static void drawHeader(
            GuiGraphicsExtractor graphics, Font font, int startX, int y
    ) {
        graphics.fill(startX, y, startX + BAR_WIDTH, y + 19, 0xC510141D);
        graphics.outline(startX, y, BAR_WIDTH, 19, 0xCC334052);
        graphics.text(font, state.className(), startX + 5, y + 5, 0xFFF1F5FA, true);

        if (!state.resourceName().isBlank() && state.resourceMaximum() > 0) {
            int barX = startX + 55;
            int barY = y + 6;
            int width = 92;
            float ratio = Math.clamp(state.resourceCurrent() / state.resourceMaximum(), 0, 1);
            graphics.fill(barX, barY, barX + width, barY + 7, 0xFF202733);
            graphics.fill(barX, barY, barX + Math.round(width * ratio), barY + 7, 0xFF3E8DFF);
            graphics.outline(barX, barY, width, 7, 0xFF6BAEFF);
            String value = "%s %.0f/%.0f".formatted(
                    state.resourceName(), state.resourceCurrent(), state.resourceMaximum());
            graphics.text(font, value, barX + (width - font.width(value)) / 2, y + 5,
                    0xFFFFFFFF, true);
        }
    }

    private static void drawSlot(
            GuiGraphicsExtractor graphics,
            Font font,
            HudStatePayload.HudState.SkillSlot slot,
            int x,
            int y,
            int iconIndex,
            boolean useScoutIcon
    ) {
        int pulse = (int) ((Math.sin(System.currentTimeMillis() / 180.0) + 1.0) * 22.0);
        float cooldownSeconds = currentCooldown(slot);
        int statusColor = !slot.enabled()
                ? LOCKED
                : slot.active() ? (0xFF5EE8FF + (pulse << 8)) : cooldownSeconds > 0 ? 0xFF677180 : READY;

        drawMinimalSlot(graphics, x, y, statusColor, slot.active());
        if (useScoutIcon) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    SCOUT_ABILITIES,
                    x + 2, y + 2,
                    iconIndex * 64.0f, 0.0f,
                    SLOT_SIZE - 4, SLOT_SIZE - 4,
                    64, 64,
                    256, 64
            );
        } else {
            String shortName = abbreviate(slot.name(), font, SLOT_SIZE - 8);
            drawScaledCenteredText(graphics, font, shortName,
                    x + SLOT_SIZE / 2, y + 13, 0.65f,
                    slot.enabled() ? 0xFFE8EDF4 : 0xFF777D87);
            drawScaledCenteredText(graphics, font, slot.key(),
                    x + SLOT_SIZE / 2, y + 3, 0.7f, 0xFFFFFFFF);
        }

        if (!slot.enabled()) {
            graphics.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, 0xA8303540);
            return;
        }

        if (cooldownSeconds > 0.05f) {
            graphics.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 2, 0xA8000000);
            String cooldown = "%.1f".formatted(cooldownSeconds);
            drawScaledCenteredText(
                    graphics, font, cooldown,
                    x + SLOT_SIZE / 2, y + 7, 0.75f, 0xFFFFFFFF);
        }

        if (slot.charges() > 0) {
            drawBadge(graphics, font, Integer.toString(slot.charges()),
                    x + SLOT_SIZE - 8, y - 2, 0xFF3E8DFF);
        }
        if (slot.stacks() > 0) {
            drawBadge(graphics, font, slot.stacks() + "/3",
                    x + SLOT_SIZE - 12, y + SLOT_SIZE - 7, 0xFFFFB347);
        }
    }

    private static void drawMinimalSlot(
            GuiGraphicsExtractor graphics, int x, int y, int statusColor, boolean active
    ) {
        graphics.fill(x + 1, y + 2, x + SLOT_SIZE + 1, y + SLOT_SIZE + 2, 0x70000000);
        graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0xE10D1219);
        graphics.outline(
                x, y, SLOT_SIZE, SLOT_SIZE,
                active ? 0xCC55DFF5 : 0xA0454F5D
        );
        graphics.fill(
                x + 1, y + SLOT_SIZE - 2,
                x + SLOT_SIZE - 1, y + SLOT_SIZE - 1,
                statusColor
        );
    }

    private static float currentCooldown(HudStatePayload.HudState.SkillSlot slot) {
        if (slot.cooldownSeconds() <= 0) {
            return 0;
        }
        float elapsedSeconds = (System.currentTimeMillis() - stateReceivedAtMillis) / 1_000.0f;
        return Math.max(0, slot.cooldownSeconds() - elapsedSeconds);
    }

    private static List<FormattedCharSequence> buildTooltip(
            Font font,
            HudStatePayload.HudState.SkillSlot slot,
            SkillCatalog.Skill skill
    ) {
        String name = skill == null ? slot.name() : skill.name();
        String input = ProjectSClient.resolveInputLabel(
                skill == null ? slot.key() : skill.input());
        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.add(Component.literal(name)
                .withStyle(slot.enabled() ? ChatFormatting.AQUA : ChatFormatting.GRAY)
                .getVisualOrderText());

        String category = skill == null ? "スキル" : skill.category();
        lines.add(Component.literal(input + "  |  " + category)
                .withStyle(ChatFormatting.WHITE)
                .getVisualOrderText());
        if (skill != null) {
            lines.addAll(font.split(
                    Component.literal(skill.stats()).withStyle(ChatFormatting.GRAY),
                    250
            ));
            lines.addAll(font.split(
                    Component.literal(SkillDescriptionStore.description(skill))
                            .withStyle(ChatFormatting.YELLOW),
                    250
            ));
        }

        if (!slot.enabled()) {
            lines.add(Component.literal("現在は使用できません")
                    .withStyle(ChatFormatting.RED)
                    .getVisualOrderText());
            return lines;
        }

        if (slot.active()) {
            lines.add(Component.literal("発動中")
                    .withStyle(ChatFormatting.AQUA)
                    .getVisualOrderText());
        }
        float cooldown = currentCooldown(slot);
        if (cooldown > 0.05f) {
            lines.add(Component.literal("クールダウン: 残り %.1f秒".formatted(cooldown))
                    .withStyle(ChatFormatting.RED)
                    .getVisualOrderText());
        } else if (!slot.active()) {
            lines.add(Component.literal("READY")
                    .withStyle(ChatFormatting.GREEN)
                    .getVisualOrderText());
        }
        if (slot.charges() > 0) {
            lines.add(Component.literal("チャージ: " + slot.charges())
                    .withStyle(ChatFormatting.BLUE)
                    .getVisualOrderText());
        }
        if (slot.stacks() > 0) {
            lines.add(Component.literal("パッシブ進行: " + slot.stacks() + "/3")
                    .withStyle(ChatFormatting.GOLD)
                    .getVisualOrderText());
        }
        return lines;
    }

    private static void drawBadge(
            GuiGraphicsExtractor graphics, Font font, String text, int x, int y, int color
    ) {
        int width = Math.max(8, Math.round(font.width(text) * 0.65f) + 3);
        graphics.fill(x, y, x + width, y + 7, 0xF00A0D12);
        graphics.outline(x, y, width, 7, color);
        drawScaledCenteredText(
                graphics, font, text, x + width / 2, y + 1, 0.65f, 0xFFFFFFFF);
    }

    private static void drawScaledCenteredText(
            GuiGraphicsExtractor graphics,
            Font font,
            String text,
            int centerX,
            int y,
            float scale,
            int color
    ) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX, y);
        graphics.pose().scale(scale, scale);
        graphics.centeredText(font, text, 0, 0, color);
        graphics.pose().popMatrix();
    }

    private static String abbreviate(String value, Font font, int maximumWidth) {
        if (font.width(value) <= maximumWidth) {
            return value;
        }
        String shortened = value;
        while (shortened.length() > 1
                && font.width(shortened + "…") > maximumWidth) {
            shortened = shortened.substring(0, shortened.length() - 1);
        }
        return shortened + "…";
    }
}
