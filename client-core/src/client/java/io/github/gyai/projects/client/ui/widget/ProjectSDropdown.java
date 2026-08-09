package io.github.gyai.projects.client.ui.widget;

import io.github.gyai.projects.client.ui.render.ProjectSIconRenderer;
import io.github.gyai.projects.client.ui.render.ProjectSOverlayPlacement;
import io.github.gyai.projects.client.ui.render.ProjectSUiDraw;
import io.github.gyai.projects.client.ui.icon.ProjectSIcon;
import io.github.gyai.projects.client.ui.icon.ProjectSStandardIcons;
import io.github.gyai.projects.client.ui.render.ProjectSEasing;
import io.github.gyai.projects.client.ui.theme.ProjectSThemeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class ProjectSDropdown<T> extends AbstractWidget {
    public record Option<T>(T value, Component label, boolean enabled, Component disabledReason) { }

    private static WeakReference<ProjectSDropdown<?>> openDropdown = new WeakReference<>(null);
    private final List<Option<T>> options;
    private final String[] optionLabels;
    private final String[] fieldLabels;
    private final String[] menuLabels;
    private final Consumer<T> changed;
    private final BooleanSupplier modalOpen;
    private int selected;
    private int highlighted;
    private int scroll;
    private boolean open;
    private long openedAt;
    private int viewportWidth;
    private int viewportHeight;
    private int anchorX = Integer.MIN_VALUE;
    private int anchorBottom = Integer.MIN_VALUE;
    private int visibleMenuY;
    private int visibleMenuHeight;
    private ProjectSOverlayPlacement.Rect menuRect =
            new ProjectSOverlayPlacement.Rect(0, 0, 0, 0);

    public ProjectSDropdown(
            int x, int y, int width, int height,
            List<Option<T>> options, int selected,
            BooleanSupplier modalOpen, Consumer<T> changed
    ) {
        super(x, y, width, height, Component.literal("Dropdown"));
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("options required");
        }
        this.options = List.copyOf(options);
        optionLabels = new String[options.size()];
        fieldLabels = new String[options.size()];
        menuLabels = new String[options.size()];
        for (int index = 0; index < options.size(); index++) {
            optionLabels[index] = options.get(index).label().getString();
            fieldLabels[index] = Minecraft.getInstance().font.plainSubstrByWidth(
                    optionLabels[index], width - 28);
            menuLabels[index] = Minecraft.getInstance().font.plainSubstrByWidth(
                    optionLabels[index], width - 24);
        }
        this.selected = Math.clamp(selected, 0, options.size() - 1);
        highlighted = this.selected;
        this.modalOpen = modalOpen == null ? () -> false : modalOpen;
        this.changed = changed == null ? ignored -> { } : changed;
    }

    @Override
    protected void extractWidgetRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickProgress
    ) {
        var theme = ProjectSThemeManager.get().activeTheme();
        var tokens = theme.tokens();
        ProjectSUiDraw.cutPanel(graphics, getX(), getY(), width, height,
                theme.metrics().inputCornerCut(), tokens.surfaceInput(),
                isFocused() || open ? tokens.borderSelected()
                        : isHovered() ? tokens.border() : tokens.borderSubtle());
        Option<T> current = options.get(selected);
        graphics.text(Minecraft.getInstance().font,
                fieldLabels[selected],
                getX() + 8, getY() + (height - 8) / 2,
                current.enabled() ? tokens.textPrimary() : tokens.textDisabled(), false);
        ProjectSIconRenderer.draw(graphics, ProjectSStandardIcons.DROPDOWN,
                getRight() - 17, getY() + (height - 10) / 2,
                10, tokens, !active);
        setViewport(graphics.guiWidth(), graphics.guiHeight());
        handleCursor(graphics);
    }

    public static void renderOpenOverlay(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY
    ) {
        ProjectSDropdown<?> current = openDropdown.get();
        if (current == null || !current.open) return;
        current.setViewport(graphics.guiWidth(), graphics.guiHeight());
        current.renderMenu(graphics, mouseX, mouseY);
    }

    public static boolean mouseClickedOpen(
            MouseButtonEvent event, boolean doubleClick
    ) {
        ProjectSDropdown<?> current = openDropdown.get();
        return current != null && current.open
                && current.mouseClicked(event, doubleClick);
    }

    public static boolean mouseScrolledOpen(
            double mouseX, double mouseY, double horizontal, double vertical
    ) {
        ProjectSDropdown<?> current = openDropdown.get();
        return current != null && current.open
                && current.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    public static boolean keyPressedOpen(KeyEvent event) {
        ProjectSDropdown<?> current = openDropdown.get();
        return current != null && current.open && current.keyPressed(event);
    }

    private void renderMenu(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        var theme = ProjectSThemeManager.get().activeTheme();
        var tokens = theme.tokens();
        double progress = ProjectSEasing.smooth(ProjectSEasing.progress(
                System.nanoTime(), openedAt, 120_000_000L));
        int animatedHeight = Math.max(1,
                (int) Math.round(menuRect.height() * progress));
        boolean opensUp = menuRect.y() < getY();
        int clipY = opensUp
                ? menuRect.y() + menuRect.height() - animatedHeight
                : menuRect.y();
        visibleMenuY = clipY;
        visibleMenuHeight = animatedHeight;
        graphics.enableScissor(menuRect.x(), clipY,
                menuRect.x() + menuRect.width(), clipY + animatedHeight);
        ProjectSUiDraw.cutPanel(graphics, menuRect.x(), menuRect.y(),
                menuRect.width(), menuRect.height(), theme.metrics().cornerCut(),
                tokens.surfaceRaised(), tokens.border());
        int visible = Math.min(6, options.size());
        for (int row = 0; row < visible; row++) {
            int index = scroll + row;
            if (index >= options.size()) break;
            Option<T> option = options.get(index);
            int y = menuRect.y() + 3 + row * 22;
            boolean hovered = mouseX >= menuRect.x() && mouseX < menuRect.x() + width
                    && mouseY >= y && mouseY < y + 21
                    && mouseY >= visibleMenuY
                    && mouseY < visibleMenuY + visibleMenuHeight;
            if (hovered || index == highlighted) {
                graphics.fill(menuRect.x() + 2, y,
                        menuRect.x() + width - 2, y + 21, tokens.surfaceHover());
            }
            graphics.text(Minecraft.getInstance().font,
                    menuLabels[index],
                    menuRect.x() + 18, y + 6,
                    option.enabled() ? tokens.textPrimary() : tokens.textDisabled(), false);
            if (index == selected) {
                ProjectSIconRenderer.draw(graphics, ProjectSIcon.SUCCESS,
                        menuRect.x() + 5, y + 5, 10,
                        tokens, !option.enabled(), true);
            }
            if (hovered && !option.enabled() && option.disabledReason() != null) {
                graphics.setTooltipForNextFrame(Minecraft.getInstance().font,
                        option.disabledReason(), mouseX, mouseY);
            }
        }
        graphics.disableScissor();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!active) return false;
        if (open && menuRect.contains(event.x(), event.y())
                && event.y() >= visibleMenuY
                && event.y() < visibleMenuY + visibleMenuHeight) {
            int row = (int) ((event.y() - menuRect.y() - 3) / 22);
            int index = scroll + Math.clamp(row, 0, 5);
            if (index < options.size()) select(index);
            return true;
        }
        if (isMouseOver(event.x(), event.y())) {
            toggleOpen();
            return true;
        }
        if (open) {
            close();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(
            double mouseX, double mouseY, double horizontal, double vertical
    ) {
        if (!open || !menuRect.contains(mouseX, mouseY)
                || mouseY < visibleMenuY
                || mouseY >= visibleMenuY + visibleMenuHeight) return false;
        scroll = Math.clamp(scroll - (int) Math.signum(vertical),
                0, Math.max(0, options.size() - 6));
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isEscape() && open) {
            close();
            return true;
        }
        if (event.isConfirmation()) {
            if (!open) toggleOpen();
            else select(highlighted);
            return true;
        }
        if (open && (event.isUp() || event.isDown())) {
            int direction = event.isUp() ? -1 : 1;
            highlighted = Math.clamp(highlighted + direction, 0, options.size() - 1);
            if (highlighted < scroll) scroll = highlighted;
            if (highlighted >= scroll + 6) scroll = highlighted - 5;
            return true;
        }
        return false;
    }

    private void toggleOpen() {
        if (modalOpen.getAsBoolean()) return;
        if (open) {
            close();
            return;
        }
        ProjectSDropdown<?> previous = openDropdown.get();
        if (previous != null && previous != this) previous.close();
        open = true;
        openedAt = System.nanoTime();
        visibleMenuHeight = 0;
        highlighted = selected;
        openDropdown = new WeakReference<>(this);
    }

    private void select(int index) {
        Option<T> option = options.get(index);
        if (!option.enabled()) return;
        selected = index;
        changed.accept(option.value());
        close();
    }

    public void close() {
        open = false;
        if (openDropdown.get() == this) openDropdown.clear();
    }

    public static void closeAny() {
        ProjectSDropdown<?> current = openDropdown.get();
        if (current != null) current.close();
        openDropdown.clear();
    }

    private void setViewport(int width, int height) {
        if (viewportWidth == width && viewportHeight == height
                && anchorX == getX() && anchorBottom == getBottom()) return;
        viewportWidth = width;
        viewportHeight = height;
        anchorX = getX();
        anchorBottom = getBottom();
        int menuHeight = Math.min(6, options.size()) * 22 + 6;
        menuRect = ProjectSOverlayPlacement.dropdown(
                getX(), getBottom(), this.width, menuHeight,
                width, height, 6);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        Option<T> narrated = options.get(open ? highlighted : selected);
        output.add(NarratedElementType.TITLE, narrated.label());
        String hint = open
                ? "矢印キーで選択、Enterで確定、Escで閉じます"
                : "Enterで一覧を開きます";
        if (!narrated.enabled() && narrated.disabledReason() != null) {
            hint += ". 選択不可: " + narrated.disabledReason().getString();
        }
        output.add(NarratedElementType.HINT, hint);
    }

    public boolean isOpen() {
        return open;
    }

    public T value() {
        return options.get(selected).value();
    }
}
