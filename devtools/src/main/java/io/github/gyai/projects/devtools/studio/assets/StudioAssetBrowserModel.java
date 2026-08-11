package io.github.gyai.projects.devtools.studio.assets;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Mutable, deterministic browser chrome state backed only by frozen demo cards.
 * It deliberately has no catalog, document, or server dependency.
 */
public final class StudioAssetBrowserModel {
    private final List<StudioAssetCard> cards;
    private boolean open;
    private boolean searchFocused;
    private String searchQuery = "";
    private StudioAssetCategory category = StudioAssetCategory.SHAPE;
    private String selectedCardId;

    public StudioAssetBrowserModel() { this(StudioAssetCard.demo()); }

    public StudioAssetBrowserModel(List<StudioAssetCard> cards) {
        if (cards == null || cards.isEmpty()) throw new IllegalArgumentException("cards");
        this.cards = List.copyOf(cards.stream().map(Objects::requireNonNull).toList());
    }

    public static StudioAssetBrowserModel demo() { return new StudioAssetBrowserModel(); }

    public List<StudioAssetCard> cards() { return cards; }

    public boolean isOpen() { return open; }

    public boolean openState() { return open; }

    public boolean searchFocused() { return searchFocused; }

    public String searchQuery() { return searchQuery; }

    public StudioAssetSearchChrome searchChrome() {
        return new StudioAssetSearchChrome(searchQuery, searchFocused, "アセットを検索");
    }

    public StudioAssetCategory category() { return category; }

    public String selectedCardId() { return selectedCardId; }

    public Optional<StudioAssetCard> selectedCard() {
        return cards.stream().filter(card -> card.id().equals(selectedCardId)).findFirst();
    }

    public List<StudioAssetCategory> categories() { return List.of(StudioAssetCategory.values()); }

    public List<StudioAssetCard> filteredCards() {
        String needle = searchQuery.toLowerCase(Locale.ROOT);
        return cards.stream()
                .filter(card -> card.category() == category)
                .filter(card -> needle.isBlank() || matches(card, needle))
                .toList();
    }

    public StudioAssetBrowserModel open() {
        open = true;
        return this;
    }

    public StudioAssetBrowserModel close() {
        open = false;
        searchFocused = false;
        return this;
    }

    public StudioAssetBrowserModel toggle() {
        return open ? close() : open();
    }

    public StudioAssetBrowserModel setSearchFocused(boolean next) {
        searchFocused = next;
        return this;
    }

    public StudioAssetBrowserModel setSearchQuery(String next) {
        searchQuery = next == null ? "" : next.strip();
        if (selectedCardId != null && filteredCards().stream().noneMatch(card -> card.id().equals(selectedCardId))) {
            selectedCardId = null;
        }
        return this;
    }

    public StudioAssetBrowserModel clearSearch() { return setSearchQuery(""); }

    public StudioAssetBrowserModel selectCategory(StudioAssetCategory next) {
        category = Objects.requireNonNull(next, "category");
        if (selectedCardId != null && cards.stream()
                .noneMatch(card -> card.id().equals(selectedCardId) && card.category() == category)) {
            selectedCardId = null;
        }
        return this;
    }

    public boolean selectCard(String id) {
        if (id == null) return false;
        Optional<StudioAssetCard> visible = filteredCards().stream()
                .filter(card -> card.id().equals(id)).findFirst();
        if (visible.isEmpty()) return false;
        selectedCardId = id;
        return true;
    }

    public StudioAssetBrowserModel clearSelection() {
        selectedCardId = null;
        return this;
    }

    private static boolean matches(StudioAssetCard card, String needle) {
        return card.id().toLowerCase(Locale.ROOT).contains(needle)
                || card.label().toLowerCase(Locale.ROOT).contains(needle)
                || card.technicalLabel().toLowerCase(Locale.ROOT).contains(needle)
                || card.detail().toLowerCase(Locale.ROOT).contains(needle);
    }
}
