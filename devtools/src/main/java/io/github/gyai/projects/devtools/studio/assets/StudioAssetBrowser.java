package io.github.gyai.projects.devtools.studio.assets;

import io.github.gyai.projects.ui.runtime.IconKey;
import io.github.gyai.projects.ui.runtime.TextStyle;
import io.github.gyai.projects.ui.runtime.UiAccessibilityMetadata;
import io.github.gyai.projects.ui.runtime.UiAccessibilityRole;
import io.github.gyai.projects.ui.runtime.UiColorRole;
import io.github.gyai.projects.ui.runtime.UiDrawList;
import io.github.gyai.projects.ui.runtime.UiInsets;
import io.github.gyai.projects.ui.runtime.UiLayer;
import io.github.gyai.projects.ui.runtime.UiMaterialTier;
import io.github.gyai.projects.ui.runtime.UiNode;
import io.github.gyai.projects.ui.runtime.UiPoint;
import io.github.gyai.projects.ui.runtime.UiRect;
import io.github.gyai.projects.ui.runtime.UiSurface;
import io.github.gyai.projects.ui.runtime.UiTheme;
import io.github.gyai.projects.ui.runtime.component.GlassPanel;
import io.github.gyai.projects.ui.runtime.component.IconButton;
import io.github.gyai.projects.ui.runtime.icon.IconCatalog;

import java.util.Objects;

/** Floating GLASS_SOLID browser shell with bounded demo search/filter/selection state. */
public final class StudioAssetBrowser extends GlassPanel {
    private final StudioAssetBrowserModel model;
    private Runnable closeRequest = this::close;
    private boolean closeRequestInFlight;
    private StudioAssetBrowserPresentation.Layout presentation;

    public StudioAssetBrowser(UiRect popupBounds) {
        this("studio-asset-browser", popupBounds, new StudioAssetBrowserModel());
    }

    public StudioAssetBrowser(String id, UiRect popupBounds) {
        this(id, popupBounds, new StudioAssetBrowserModel());
    }

    public StudioAssetBrowser(String id, UiRect popupBounds, StudioAssetBrowserModel model) {
        super(id, popupBounds, UiMaterialTier.GLASS_SOLID, 12, new UiInsets(12));
        this.model = Objects.requireNonNull(model, "model");
        setClipToBounds(true);
        setLayer(UiLayer.OVERLAY);
        setAccessibility(UiAccessibilityMetadata.of(UiAccessibilityRole.DIALOG, id));
        setVisible(model.isOpen());
        rebuild();
    }

    public StudioAssetBrowserModel model() { return model; }

    public StudioAssetBrowserPresentation.Layout presentation() { return presentation; }

    public StudioAssetBrowserPresentation.Layout layout() { return presentation; }

    public boolean isOpen() { return model.isOpen(); }

    /**
     * Binds the close affordance to an authoritative host/workspace transition.
     * A null callback restores the component-local close behavior.
     */
    public StudioAssetBrowser setOnCloseRequest(Runnable next) {
        closeRequest = next == null ? this::close : next;
        return this;
    }

    /** Alias used by hosts that name the action directly rather than as a listener. */
    public StudioAssetBrowser setCloseRequest(Runnable next) {
        return setOnCloseRequest(next);
    }

    /**
     * Requests the authoritative close transition. The callback is fired once
     * per open request; direct {@link #close()} remains available for local use.
     */
    public boolean requestClose() {
        if (!model.isOpen() || closeRequestInFlight) return false;
        closeRequestInFlight = true;
        try {
            closeRequest.run();
            return true;
        } finally {
            closeRequestInFlight = false;
        }
    }

    public StudioAssetBrowser open() {
        model.open();
        setVisible(true);
        rebuild();
        return this;
    }

    public StudioAssetBrowser close() {
        model.close();
        setVisible(false);
        rebuild();
        return this;
    }

    public StudioAssetBrowser toggle() {
        return model.isOpen() ? close() : open();
    }

    public StudioAssetBrowser setSearchQuery(String query) {
        model.setSearchQuery(query);
        rebuild();
        return this;
    }

    public StudioAssetBrowser setSearchFocused(boolean focused) {
        model.setSearchFocused(focused);
        rebuild();
        return this;
    }

    public StudioAssetBrowser selectCategory(StudioAssetCategory category) {
        model.selectCategory(category);
        rebuild();
        return this;
    }

    public boolean selectCard(String id) {
        boolean selected = model.selectCard(id);
        if (selected) rebuild();
        return selected;
    }

    @Override
    public StudioAssetBrowser setBounds(UiRect nextBounds) {
        super.setBounds(nextBounds);
        rebuild();
        return this;
    }

    private void rebuild() {
        for (UiNode child : children()) removeChild(child);
        presentation = StudioAssetBrowserPresentation.layout(bounds(), model);
        if (!model.isOpen()) return;

        addChild(new AssetLabel("studio-asset-browser-title", local(presentation.titleBounds()),
                "アセットブラウザ", TextStyle.panel()));

        IconButton close = new IconButton("studio-asset-browser-close", local(presentation.closeButton()),
                IconCatalog.spec(IconKey.CLOSE), "閉じる", this::requestClose);
        close.setMaterialTier(UiMaterialTier.GLASS_THIN);
        addChild(close);

        SearchChromeNode search = new SearchChromeNode("studio-asset-browser-search",
                local(presentation.searchField().bounds()), model.searchChrome(),
                () -> setSearchFocused(true));
        addChild(search);

        for (StudioAssetBrowserPresentation.CategoryTab tab : presentation.categories()) {
            CategoryTabNode node = new CategoryTabNode("studio-asset-category-" + tab.category().name().toLowerCase(),
                    local(tab.bounds()), tab, () -> selectCategory(tab.category()));
            addChild(node);
        }

        for (StudioAssetBrowserPresentation.GridCell cell : presentation.cells()) {
            AssetCardNode node = new AssetCardNode("studio-asset-card-" + cell.card().id(),
                    local(cell.bounds()), cell, () -> selectCard(cell.card().id()));
            addChild(node);
        }
    }

    private UiRect local(UiRect global) {
        return new UiRect(global.x() - bounds().x(), global.y() - bounds().y(),
                global.width(), global.height());
    }

    private static final class AssetLabel extends UiNode {
        private final String value;
        private final TextStyle style;

        private AssetLabel(String id, UiRect bounds, String value, TextStyle style) {
            super(id, bounds);
            this.value = Objects.requireNonNull(value, "value");
            this.style = Objects.requireNonNull(style, "style");
            setHitTestable(false);
            setAccessibility(UiAccessibilityMetadata.of(UiAccessibilityRole.LABEL, value));
        }

        @Override
        protected void appendSelf(UiDrawList drawList, UiTheme theme, UiRect globalBounds, UiRect clip) {
            drawList.text(new UiPoint(globalBounds.x(), globalBounds.y()), value, style,
                    theme.color(style.colorRole()));
        }
    }

    private static final class SearchChromeNode extends UiSurface {
        private final StudioAssetSearchChrome search;
        private final Runnable focusAction;

        private SearchChromeNode(String id, UiRect bounds, StudioAssetSearchChrome search, Runnable focusAction) {
            super(id, bounds, UiMaterialTier.GLASS_THIN, 8, new UiInsets(0));
            this.search = Objects.requireNonNull(search, "search");
            this.focusAction = Objects.requireNonNull(focusAction, "focusAction");
            setFocusable(true);
            setAccessibility(UiAccessibilityMetadata.of(UiAccessibilityRole.TEXT_FIELD, "アセットを検索"));
            addChild(new IconNode("search-icon", new UiRect(8, Math.max(0, (bounds.height() - 18) / 2),
                    Math.min(18, bounds.height()), Math.min(18, bounds.height())), IconKey.SEARCH));
            String value = search.hasQuery() ? search.query() : search.placeholder();
            addChild(new AssetLabel("search-value", new UiRect(34, 0, Math.max(0, bounds.width() - 42), bounds.height()),
                    value, search.hasQuery() ? TextStyle.body() : TextStyle.secondary()));
        }

        @Override
        public boolean handleEvent(io.github.gyai.projects.ui.runtime.UiEvent event) {
            focusAction.run();
            return super.handleEvent(event);
        }
    }

    private static final class IconNode extends UiNode {
        private final IconKey iconKey;

        private IconNode(String id, UiRect bounds, IconKey iconKey) {
            super(id, bounds);
            this.iconKey = Objects.requireNonNull(iconKey, "iconKey");
            setHitTestable(false);
        }

        @Override
        protected void appendSelf(UiDrawList drawList, UiTheme theme, UiRect globalBounds, UiRect clip) {
            drawList.icon(globalBounds, IconCatalog.spec(iconKey), theme.color(UiColorRole.TEXT_SECONDARY));
        }
    }

    private static final class CategoryTabNode extends UiSurface {
        private final StudioAssetBrowserPresentation.CategoryTab tab;

        private CategoryTabNode(String id, UiRect bounds,
                                StudioAssetBrowserPresentation.CategoryTab tab, Runnable action) {
            super(id, bounds, tab.materialTier(), 8, new UiInsets(0));
            this.tab = tab;
            setFocusable(true);
            setAccessibility(UiAccessibilityMetadata.of(UiAccessibilityRole.BUTTON, tab.category().label()));
            IconButton icon = new IconButton("category-icon", new UiRect(4, 4,
                    Math.min(20, Math.max(1, bounds.height() - 8)), Math.min(20, Math.max(1, bounds.height() - 8))),
                    IconCatalog.spec(tab.iconKey()), tab.category().label(), action);
            icon.setMaterialTier(UiMaterialTier.GLASS_THIN);
            icon.setSelected(tab.selected());
            addChild(icon);
            addChild(new AssetLabel("category-label", new UiRect(30, 0,
                    Math.max(0, bounds.width() - 34), bounds.height()), tab.category().label(), TextStyle.small()));
        }
    }

    private static final class AssetCardNode extends UiSurface {
        private AssetCardNode(String id, UiRect bounds,
                              StudioAssetBrowserPresentation.GridCell cell, Runnable action) {
            super(id, bounds, cell.materialTier(), 9, new UiInsets(0));
            setFocusable(true);
            setAccessibility(UiAccessibilityMetadata.of(UiAccessibilityRole.BUTTON, cell.card().label()));
            double iconSize = Math.min(32, Math.max(1, bounds.height() - 40));
            IconButton preview = new IconButton("preview-icon", new UiRect(10, 10, iconSize, iconSize),
                    IconCatalog.spec(cell.card().iconKey()), cell.card().label(), action);
            preview.setMaterialTier(UiMaterialTier.GLASS_THIN);
            preview.setSelected(cell.selected());
            addChild(preview);
            addChild(new AssetLabel("card-label", new UiRect(52, 10,
                    Math.max(1, bounds.width() - 60), 17), cell.card().label(), TextStyle.body()));
            addChild(new AssetLabel("card-technical", new UiRect(52, 28,
                    Math.max(1, bounds.width() - 60), 14), cell.card().technicalLabel(), TextStyle.technical()));
        }
    }
}
