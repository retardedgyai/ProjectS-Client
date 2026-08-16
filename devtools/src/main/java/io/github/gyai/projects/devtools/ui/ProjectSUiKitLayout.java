package io.github.gyai.projects.devtools.ui;

import io.github.gyai.projects.ui.runtime.UiRect;

import java.util.ArrayList;
import java.util.List;

/** Responsive logical layout model for the Stage 1 UI Kit pilot. */
public record ProjectSUiKitLayout(
        int width,
        int height,
        UiRect viewport,
        UiRect titleBounds,
        UiRect contentClip,
        List<UiRect> materialCards,
        List<UiRect> buttonCards,
        List<UiRect> themeControls,
        List<UiRect> accentControls
) {
    public ProjectSUiKitLayout {
        if (width < 0 || height < 0 || viewport == null || titleBounds == null || contentClip == null
                || materialCards == null || buttonCards == null
                || themeControls == null || accentControls == null) {
            throw new IllegalArgumentException("layout");
        }
        materialCards = List.copyOf(materialCards);
        buttonCards = List.copyOf(buttonCards);
        themeControls = List.copyOf(themeControls);
        accentControls = List.copyOf(accentControls);
    }

    public static ProjectSUiKitLayout at(int requestedWidth, int requestedHeight) {
        int width = Math.max(0, requestedWidth);
        int height = Math.max(0, requestedHeight);
        UiRect viewport = new UiRect(0, 0, width, height);
        int margin = Math.max(8, Math.min(24, width / 30));
        int header = Math.min(48, Math.max(30, height / 6));
        int footer = Math.min(28, Math.max(18, height / 12));
        int titleHeight = Math.min(18, Math.max(1, header / 3));
        UiRect titleBounds = new UiRect(margin, 4,
                Math.max(1, Math.min(260, width - margin * 2)), titleHeight);
        UiRect contentClip = new UiRect(margin, header + 4,
                Math.max(0, width - margin * 2), Math.max(0, height - header - footer - 8));

        List<UiRect> theme = new ArrayList<>();
        List<UiRect> accents = new ArrayList<>();
        int controlHeight = Math.min(20, Math.max(1, header - (int) titleBounds.bottom() - 2));
        int controlY = Math.max((int) titleBounds.bottom() + 2, header - controlHeight - 2);
        int controlWidth = Math.max(1, Math.min(76, (width - margin * 2 - 12) / 4));
        int x = margin;
        theme.add(new UiRect(x, controlY, controlWidth, controlHeight));
        theme.add(new UiRect(x + controlWidth + 4, controlY, controlWidth, controlHeight));
        int accentX = Math.min(Math.max(0, width - margin - controlWidth * 2 - 4), x + controlWidth * 2 + 12);
        accents.add(new UiRect(accentX, controlY, controlWidth, controlHeight));
        accents.add(new UiRect(accentX + controlWidth + 4, controlY, controlWidth, controlHeight));

        int gap = Math.max(4, Math.min(12, width / 80));
        int surfaceColumns = width >= 1200 ? 4 : 2;
        int surfaceWidth = Math.max(1, (int) Math.floor(
                (contentClip.width() - gap * (surfaceColumns - 1)) / surfaceColumns));
        int surfaceHeight = Math.max(1, Math.min(74, Math.max(1, (int) Math.floor(contentClip.height() / 4))));
        List<UiRect> materials = grid(contentClip.x(), contentClip.y(), surfaceWidth, surfaceHeight,
                gap, surfaceColumns, 4);
        int buttonY = (int) contentClip.y() + (int) Math.ceil(surfaceHeight * 2.0) + gap * 2;
        int buttonColumns = width >= 1200 ? 5 : width >= 760 ? 3 : 2;
        int buttonWidth = Math.max(1, (int) Math.floor(
                (contentClip.width() - gap * (buttonColumns - 1)) / buttonColumns));
        int buttonHeight = Math.max(1, Math.min(34, Math.max(1, (int) Math.floor(contentClip.height() / 8))));
        List<UiRect> buttons = grid(contentClip.x(), buttonY, buttonWidth, buttonHeight,
                gap, buttonColumns, 5);
        return new ProjectSUiKitLayout(width, height, viewport, titleBounds, contentClip,
                materials, buttons, theme, accents);
    }

    private static List<UiRect> grid(double x, double y, int itemWidth, int itemHeight,
                                     int gap, int columns, int count) {
        List<UiRect> result = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            int column = index % Math.max(1, columns);
            int row = index / Math.max(1, columns);
            result.add(new UiRect(x + column * (itemWidth + gap),
                    y + row * (itemHeight + gap), itemWidth, itemHeight));
        }
        return result;
    }

    public UiRect clipped(UiRect bounds) { return bounds.intersection(contentClip); }

    public boolean hasNonNegativeBounds() {
        return all().stream().allMatch(bounds -> bounds.width() >= 0 && bounds.height() >= 0);
    }

    public boolean controlsReachable() {
        return themeControls.stream().allMatch(this::withinViewport)
                && accentControls.stream().allMatch(this::withinViewport);
    }

    private boolean withinViewport(UiRect rect) {
        return rect.width() > 0 && rect.height() > 0
                && rect.x() >= viewport.x() && rect.y() >= viewport.y()
                && rect.right() <= viewport.right() && rect.bottom() <= viewport.bottom();
    }

    public List<UiRect> all() {
        List<UiRect> result = new ArrayList<>();
        result.add(titleBounds); result.addAll(materialCards); result.addAll(buttonCards);
        result.addAll(themeControls); result.addAll(accentControls);
        return List.copyOf(result);
    }
}
