package io.github.gyai.projects.devtools.ui;

import io.github.gyai.projects.ui.runtime.UiRect;

import java.util.ArrayList;
import java.util.List;

/** Deterministic, scroll-aware layout model for the dev-only Stage 2 gallery. */
public record ProjectSUiKitV02Layout(
        int width,
        int height,
        UiRect viewport,
        UiRect titleBounds,
        UiRect contentClip,
        double contentHeight,
        List<Section> sections,
        List<UiRect> themeControls,
        List<UiRect> accentControls,
        List<UiRect> iconCells
) {
    public record Section(String id, UiRect bounds) {
        public Section {
            if (id == null || id.isBlank() || bounds == null) throw new IllegalArgumentException("section");
        }
    }

    public ProjectSUiKitV02Layout {
        if (width <= 0 || height <= 0 || viewport == null || titleBounds == null || contentClip == null
                || !Double.isFinite(contentHeight) || contentHeight <= 0 || sections == null
                || themeControls == null || accentControls == null || iconCells == null) {
            throw new IllegalArgumentException("layout");
        }
        sections = List.copyOf(sections);
        themeControls = List.copyOf(themeControls);
        accentControls = List.copyOf(accentControls);
        iconCells = List.copyOf(iconCells);
    }

    public static ProjectSUiKitV02Layout at(int requestedWidth, int requestedHeight) {
        int width = Math.max(1, requestedWidth);
        int height = Math.max(1, requestedHeight);
        UiRect viewport = new UiRect(0, 0, width, height);
        double margin = Math.clamp(width / 40.0, 10, 28);
        double headerHeight = Math.clamp(height * .14, 48, 64);
        UiRect title = new UiRect(margin, 12, Math.max(1, width - margin * 2), 24);
        UiRect content = new UiRect(margin, headerHeight,
                Math.max(1, width - margin * 2), Math.max(1, height - headerHeight - margin));
        double gap = 12;
        boolean twoColumns = content.width() >= 900;
        double columnWidth = twoColumns ? (content.width() - gap) / 2 : content.width();
        double typographyHeight = 252;
        double themeHeight = 190;
        double materialsHeight = 138;
        double componentsHeight = 322;
        double iconCell = Math.clamp((columnWidth - 7 * 6) / 8, 24, 36);
        int iconColumns = Math.max(4, Math.min(8, (int) Math.floor((columnWidth + 6) / (iconCell + 6))));
        double iconCellWidth = Math.min(36,
                (columnWidth - (iconColumns - 1) * 6) / iconColumns);
        double iconRows = Math.ceil(27.0 / iconColumns);
        double iconsHeight = 50 + iconRows * (Math.max(24, iconCellWidth) + 6) + 12;

        List<Section> sections = new ArrayList<>();
        double leftY = 0;
        double rightY = 0;
        if (twoColumns) {
            sections.add(section("typography", content.x(), content.y() + leftY,
                    columnWidth, typographyHeight));
            leftY += typographyHeight + gap;
            sections.add(section("theme", content.x(), content.y() + leftY,
                    columnWidth, themeHeight));
            leftY += themeHeight + gap;
            sections.add(section("components", content.x(), content.y() + leftY,
                    columnWidth, componentsHeight));
            leftY += componentsHeight;

            double rightX = content.x() + columnWidth + gap;
            sections.add(section("materials", rightX, content.y() + rightY,
                    columnWidth, materialsHeight));
            rightY += materialsHeight + gap;
            sections.add(section("icons", rightX, content.y() + rightY,
                    columnWidth, iconsHeight));
            rightY += iconsHeight;
        } else {
            double y = content.y();
            sections.add(section("typography", content.x(), y, columnWidth, typographyHeight));
            y += typographyHeight + gap;
            sections.add(section("theme", content.x(), y, columnWidth, themeHeight));
            y += themeHeight + gap;
            sections.add(section("materials", content.x(), y, columnWidth, materialsHeight));
            y += materialsHeight + gap;
            sections.add(section("components", content.x(), y, columnWidth, componentsHeight));
            y += componentsHeight + gap;
            sections.add(section("icons", content.x(), y, columnWidth, iconsHeight));
            rightY = y + iconsHeight - content.y();
        }
        double maxSectionBottom = sections.stream()
                .mapToDouble(section -> section.bounds().bottom())
                .max()
                .orElse(content.y());
        double contentHeight = Math.max(content.height(), maxSectionBottom - content.y());
        Section theme = find(sections, "theme");
        Section icons = find(sections, "icons");
        List<UiRect> themeControls = controlGrid(theme.bounds(), 9, 4, 100, 28, 6, 6, 48);
        List<UiRect> accentControls = themeControls.subList(2, themeControls.size());
        List<UiRect> iconCells = iconGrid(icons.bounds(), iconColumns, iconCellWidth, 6, 50);
        return new ProjectSUiKitV02Layout(width, height, viewport, title, content, contentHeight,
                sections, themeControls, accentControls, iconCells);
    }

    public static ProjectSUiKitV02Layout atLogical(int physicalWidth, int physicalHeight, double guiScale) {
        if (!Double.isFinite(guiScale) || guiScale <= 0) throw new IllegalArgumentException("guiScale");
        return at((int) Math.round(physicalWidth / guiScale), (int) Math.round(physicalHeight / guiScale));
    }

    public Section section(String id) { return find(sections, id); }

    public boolean sectionsDoNotOverlap() {
        for (int left = 0; left < sections.size(); left++) {
            for (int right = left + 1; right < sections.size(); right++) {
                if (!sections.get(left).bounds().intersection(sections.get(right).bounds()).isEmpty()) return false;
            }
        }
        return true;
    }

    public boolean controlsInsideTheme() {
        UiRect theme = section("theme").bounds();
        return themeControls.stream().allMatch(control -> theme.contains(new io.github.gyai.projects.ui.runtime.UiPoint(
                control.x() + control.width() / 2, control.y() + control.height() / 2)));
    }

    public boolean iconsInsideGallery() {
        UiRect icons = section("icons").bounds();
        return iconCells.stream().allMatch(cell -> icons.contains(new io.github.gyai.projects.ui.runtime.UiPoint(
                cell.x() + cell.width() / 2, cell.y() + cell.height() / 2)));
    }

    /** Every section bottom is reachable through the scroll area's canonical content extent. */
    public boolean sectionsReachable() {
        double contentBottom = contentClip.y() + contentHeight;
        return sections.stream().allMatch(section ->
                section.bounds().y() >= contentClip.y() - 1e-9
                        && section.bounds().bottom() <= contentBottom + 1e-9);
    }

    public double maxSectionBottomRelativeToContentOrigin() {
        return sections.stream()
                .mapToDouble(section -> section.bounds().bottom() - contentClip.y())
                .max()
                .orElse(0);
    }

    private static Section section(String id, double x, double y, double width, double height) {
        return new Section(id, new UiRect(x, y, Math.max(1, width), Math.max(1, height)));
    }

    private static Section find(List<Section> sections, String id) {
        return sections.stream().filter(section -> section.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown section: " + id));
    }

    private static List<UiRect> controlGrid(UiRect section, int count, int columns,
                                            double cellWidth, double cellHeight, double xGap,
                                            double yGap, double topOffset) {
        List<UiRect> result = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            int column = index % columns;
            int row = index / columns;
            result.add(new UiRect(section.x() + 12 + column * (cellWidth + xGap),
                    section.y() + topOffset + row * (cellHeight + yGap), cellWidth, cellHeight));
        }
        return List.copyOf(result);
    }

    private static List<UiRect> iconGrid(UiRect section, int columns, double width,
                                        double gap, int topOffset) {
        List<UiRect> result = new ArrayList<>();
        double height = Math.max(24, width);
        for (int index = 0; index < 27; index++) {
            int column = index % columns;
            int row = index / columns;
            result.add(new UiRect(section.x() + column * (width + gap),
                    section.y() + topOffset + row * (height + gap), width, height));
        }
        return List.copyOf(result);
    }
}
