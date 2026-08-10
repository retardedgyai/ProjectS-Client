package io.github.gyai.projects.client.ui.widget;

import io.github.gyai.projects.client.ui.icon.ProjectSIcon;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public final class ProjectSIconButton extends ProjectSButton {
    private final Component tooltipText;
    private final Component disabledReason;

    public ProjectSIconButton(
            int x, int y, int width, int height,
            ProjectSIcon icon,
            Component tooltip,
            Kind kind,
            Runnable action
    ) {
        this(x, y, width, height, icon, tooltip, null, kind, action);
    }

    public ProjectSIconButton(
            int x, int y, int width, int height,
            ProjectSIcon icon,
            Component tooltip,
            Component disabledReason,
            Kind kind,
            Runnable action
    ) {
        super(x, y, width, height, Component.empty(), kind, icon, action);
        if (tooltip == null) throw new IllegalArgumentException("Tooltip is required");
        this.tooltipText = tooltip;
        this.disabledReason = disabledReason;
        setMessage(tooltip);
        setTooltip(Tooltip.create(tooltip));
    }

    /** @deprecated Use {@link ProjectSIcon}. */
    @Deprecated
    public ProjectSIconButton(
            int x, int y, int width, int height,
            io.github.gyai.projects.client.ui.render.ProjectSIconRenderer.Icon icon,
            Component tooltip, Component disabledReason,
            Kind kind, Runnable action
    ) {
        this(x, y, width, height, icon.canonical(), tooltip,
                disabledReason, kind, action);
    }

    /** @deprecated Use {@link ProjectSIcon}. */
    @Deprecated
    public ProjectSIconButton(
            int x, int y, int width, int height,
            io.github.gyai.projects.client.ui.render.ProjectSIconRenderer.Icon icon,
            Component tooltip, Kind kind, Runnable action
    ) {
        this(x, y, width, height, icon.canonical(), tooltip, kind, action);
    }

    public ProjectSIconButton(
            int x, int y, int width, int height,
            ProjectSIcon icon,
            Component tooltip,
            Kind kind,
            Consumer<InputWithModifiers> action
    ) {
        super(x, y, width, height, Component.empty(), kind, icon, action);
        if (tooltip == null) throw new IllegalArgumentException("Tooltip is required");
        this.tooltipText = tooltip;
        disabledReason = null;
        setMessage(tooltip);
        setTooltip(Tooltip.create(tooltip));
    }

    /** @deprecated Use {@link ProjectSIcon}. */
    @Deprecated
    public ProjectSIconButton(
            int x, int y, int width, int height,
            io.github.gyai.projects.client.ui.render.ProjectSIconRenderer.Icon icon,
            Component tooltip, Kind kind, Consumer<InputWithModifiers> action
    ) {
        this(x, y, width, height, icon.canonical(), tooltip, kind, action);
    }

    public ProjectSIconButton enabled(boolean enabled) {
        active = enabled;
        setTooltip(Tooltip.create(!enabled && disabledReason != null
                ? disabledReason : tooltipText));
        return this;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        super.updateWidgetNarration(output);
        if (!active && disabledReason != null) {
            output.add(NarratedElementType.HINT, disabledReason);
        }
    }
}
