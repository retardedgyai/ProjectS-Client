package io.github.gyai.projects.devtools.studio.assets;

import io.github.gyai.projects.ui.runtime.IconKey;
import io.github.gyai.projects.ui.runtime.UiMaterialTier;
import io.github.gyai.projects.ui.runtime.UiRect;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Pure contained geometry for the floating GLASS_SOLID asset drawer. */
public final class StudioAssetBrowserPresentation {
    public record SearchField(
            UiRect bounds,
            UiRect iconBounds,
            UiRect textBounds,
            IconKey iconKey,
            String value,
            String placeholder,
            boolean focused,
            UiMaterialTier materialTier
    ) {
        public SearchField {
            if (bounds == null || iconBounds == null || textBounds == null || iconKey == null
                    || value == null || placeholder == null || materialTier == null) {
                throw new IllegalArgumentException("search field");
            }
        }
    }

    public record CategoryTab(
            StudioAssetCategory category,
            UiRect bounds,
            IconKey iconKey,
            boolean selected,
            UiMaterialTier materialTier
    ) {
        public CategoryTab {
            if (category == null || bounds == null || iconKey == null || materialTier == null) {
                throw new IllegalArgumentException("category tab");
            }
        }
    }

    public record GridCell(
            StudioAssetCard card,
            UiRect bounds,
            boolean selected,
            UiMaterialTier materialTier
    ) {
        public GridCell {
            if (card == null || bounds == null || materialTier == null) throw new IllegalArgumentException("cell");
        }
    }

    public record Layout(
            UiRect popupBounds,
            UiMaterialTier materialTier,
            UiRect titleBounds,
            UiRect closeButton,
            SearchField searchField,
            UiRect categoryHeader,
            List<CategoryTab> categories,
            UiRect gridBounds,
            List<GridCell> cells
    ) {
        public Layout {
            if (popupBounds == null || materialTier == null || titleBounds == null || closeButton == null
                    || searchField == null || categoryHeader == null || categories == null
                    || gridBounds == null || cells == null) {
                throw new IllegalArgumentException("asset browser layout");
            }
            categories = List.copyOf(categories);
            cells = List.copyOf(cells);
        }

        public boolean containsAllChrome() {
            return contains(popupBounds, titleBounds)
                    && contains(popupBounds, closeButton)
                    && contains(popupBounds, searchField.bounds())
                    && contains(popupBounds, searchField.iconBounds())
                    && contains(popupBounds, searchField.textBounds())
                    && contains(popupBounds, categoryHeader)
                    && categories.stream().allMatch(tab -> contains(categoryHeader, tab.bounds()))
                    && contains(popupBounds, gridBounds)
                    && cells.stream().allMatch(cell -> contains(gridBounds, cell.bounds()));
        }

        public boolean popup() { return materialTier == UiMaterialTier.GLASS_SOLID; }
    }

    private StudioAssetBrowserPresentation() { }

    public static Layout layout(UiRect popupBounds, StudioAssetBrowserModel model) {
        Objects.requireNonNull(model, "model");
        return layout(popupBounds, model.searchChrome(), model.category(), model.filteredCards(), model.selectedCardId());
    }

    public static Layout layout(UiRect popupBounds, StudioAssetSearchChrome search,
                                StudioAssetCategory category, List<StudioAssetCard> cards,
                                String selectedCardId) {
        Objects.requireNonNull(popupBounds, "popupBounds");
        Objects.requireNonNull(search, "search");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(cards, "cards");
        double minimum = Math.min(popupBounds.width(), popupBounds.height());
        double margin = Math.min(12, Math.max(4, minimum / 6));
        double x = popupBounds.x() + margin;
        double y = popupBounds.y() + margin;
        double width = Math.max(0, popupBounds.width() - margin * 2);
        double closeSize = Math.min(24, Math.max(1, Math.min(width / 5, Math.max(1, popupBounds.height() / 5))));
        UiRect close = new UiRect(Math.max(popupBounds.x(), popupBounds.right() - margin - closeSize), y,
                closeSize, closeSize);
        UiRect title = new UiRect(x, y, Math.max(0, width - closeSize - 8), Math.min(20, closeSize));

        double searchY = Math.min(popupBounds.bottom(), y + Math.max(closeSize, 20) + 8);
        double searchHeight = Math.min(30, Math.max(0, popupBounds.bottom() - searchY));
        UiRect searchBounds = new UiRect(x, searchY, width, searchHeight);
        double iconSize = Math.min(18, Math.max(0, searchHeight - 8));
        UiRect searchIcon = new UiRect(searchBounds.x() + 8,
                searchBounds.y() + Math.max(0, (searchBounds.height() - iconSize) / 2), iconSize, iconSize);
        UiRect searchText = new UiRect(searchIcon.right() + 7, searchBounds.y(),
                Math.max(0, searchBounds.right() - searchIcon.right() - 15), searchBounds.height());
        SearchField searchField = new SearchField(searchBounds, searchIcon, searchText, IconKey.SEARCH,
                search.query(), search.placeholder(), search.focused(), UiMaterialTier.GLASS_THIN);

        double categoryY = Math.min(popupBounds.bottom(), searchBounds.bottom() + 8);
        double categoryHeight = Math.min(28, Math.max(0, popupBounds.bottom() - categoryY));
        UiRect categoryHeader = new UiRect(x, categoryY, width, categoryHeight);
        List<CategoryTab> categoryTabs = categoryTabs(categoryHeader, category);

        double gridY = Math.min(popupBounds.bottom(), categoryHeader.bottom() + 8);
        UiRect grid = new UiRect(x, gridY, width, Math.max(0, popupBounds.bottom() - margin - gridY));
        List<GridCell> cells = gridCells(grid, cards, selectedCardId);
        return new Layout(popupBounds, UiMaterialTier.GLASS_SOLID, title, close, searchField,
                categoryHeader, categoryTabs, grid, cells);
    }

    private static List<CategoryTab> categoryTabs(UiRect header, StudioAssetCategory selected) {
        if (header.isEmpty()) return List.of();
        double gap = Math.min(6, header.width() / 18);
        double tabWidth = Math.max(0, (header.width() - gap * 2) / 3);
        ArrayList<CategoryTab> result = new ArrayList<>();
        StudioAssetCategory[] categories = StudioAssetCategory.values();
        for (int index = 0; index < categories.length; index++) {
            StudioAssetCategory category = categories[index];
            UiRect tab = new UiRect(header.x() + index * (tabWidth + gap), header.y(), tabWidth, header.height());
            boolean active = category == selected;
            result.add(new CategoryTab(category, tab, category.iconKey(), active,
                    active ? UiMaterialTier.ACCENT_GLASS : UiMaterialTier.GLASS_THIN));
        }
        return List.copyOf(result);
    }

    private static List<GridCell> gridCells(UiRect grid, List<StudioAssetCard> cards, String selectedCardId) {
        if (cards.isEmpty()) return List.of();
        int columns = grid.width() >= 260 ? 2 : 1;
        double gap = Math.min(8, Math.max(0, grid.width() / 30));
        double cellWidth = Math.max(0, (grid.width() - gap * (columns - 1)) / columns);
        int rows = (cards.size() + columns - 1) / columns;
        double cellHeight = grid.height() <= 0 ? 0
                : Math.max(1, (grid.height() - gap * Math.max(0, rows - 1)) / rows);
        ArrayList<GridCell> result = new ArrayList<>();
        for (int index = 0; index < cards.size(); index++) {
            int column = index % columns;
            int row = index / columns;
            UiRect cell = new UiRect(grid.x() + column * (cellWidth + gap),
                    grid.y() + row * (cellHeight + gap), cellWidth, cellHeight);
            boolean selected = Objects.equals(selectedCardId, cards.get(index).id());
            result.add(new GridCell(cards.get(index), cell, selected,
                    selected ? UiMaterialTier.ACCENT_GLASS : UiMaterialTier.GLASS_PANEL));
        }
        return List.copyOf(result);
    }

    private static boolean contains(UiRect outer, UiRect inner) {
        return inner.isEmpty() || inner.x() >= outer.x() && inner.y() >= outer.y()
                && inner.right() <= outer.right() && inner.bottom() <= outer.bottom();
    }
}
