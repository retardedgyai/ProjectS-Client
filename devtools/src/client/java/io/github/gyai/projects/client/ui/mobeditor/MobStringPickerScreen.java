package io.github.gyai.projects.client.ui.mobeditor;

import io.github.gyai.projects.client.ui.icon.ProjectSIcon;
import io.github.gyai.projects.client.ui.render.ProjectSIconRenderer;
import io.github.gyai.projects.client.ui.render.ProjectSUiDraw;
import io.github.gyai.projects.client.ui.screen.ProjectSThemedScreen;
import io.github.gyai.projects.client.ui.theme.ProjectSThemeManager;
import io.github.gyai.projects.client.ui.widget.ProjectSButton;
import io.github.gyai.projects.client.ui.widget.ProjectSTextField;
import io.github.gyai.projects.client.ui.mobeditor.widget.MobPickerEntryButton;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

/** Searchable, paged picker for entity, item, glow, and variant choices. */
public final class MobStringPickerScreen extends ProjectSThemedScreen {
    private static final LinkedHashSet<String> RECENT = new LinkedHashSet<>();

    public record Entry(
            String id, String label, String category, ProjectSIcon icon,
            boolean enabled, String disabledReason, ItemStack itemStack
    ) {
        public Entry {
            id = id == null ? "" : id;
            label = label == null ? id : label;
            category = category == null ? "" : category;
            disabledReason = disabledReason == null ? "" : disabledReason;
            itemStack = itemStack == null ? ItemStack.EMPTY : itemStack.copy();
        }

        public Entry(String id, String label, String category, ProjectSIcon icon) {
            this(id, label, category, icon, true, "", ItemStack.EMPTY);
        }

        public Entry(String id, String label, String category, ProjectSIcon icon,
                     boolean enabled, String disabledReason) {
            this(id, label, category, icon, enabled, disabledReason, ItemStack.EMPTY);
        }
    }

    private final Screen parent;
    private final String heading;
    private final List<Entry> source;
    private final String selected;
    private final Consumer<String> selection;
    private String query = "";
    private String category = "すべて";
    private int page;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;

    public MobStringPickerScreen(Screen parent, String heading, List<Entry> entries,
                                 String selected, Consumer<String> selection) {
        super(Component.literal(heading));
        this.parent = parent;
        this.heading = heading;
        this.source = entries == null ? List.of() : List.copyOf(entries);
        this.selected = selected == null ? "" : selected;
        this.selection = selection == null ? ignored -> { } : selection;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(680, Math.max(1, width - 24));
        panelHeight = Math.min(450, Math.max(1, height - 24));
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        ProjectSTextField search = addRenderableWidget(new ProjectSTextField(font,
                panelX + 16, panelY + 54, Math.max(1, panelWidth - 250), 28,
                Component.literal("検索"), Component.literal("名前またはID")));
        search.setValue(query);
        search.setMaxLength(64);
        search.setResponder(value -> {
            query = value;
            page = 0;
        });
        addRenderableWidget(new ProjectSButton(panelX + panelWidth - 224, panelY + 54,
                84, 28, Component.literal("検索"), ProjectSButton.Kind.SECONDARY,
                ProjectSIcon.SEARCH, this::rebuild));
        addRenderableWidget(new ProjectSButton(panelX + panelWidth - 132, panelY + 54,
                116, 28, Component.literal(category), ProjectSButton.Kind.GHOST,
                ProjectSIcon.FILTER, this::nextCategory));

        List<Entry> filtered = filtered();
        int maximumRows = Math.max(1, (panelHeight - 160) / 46);
        int columns = panelWidth < 500 ? 2 : 3;
        int pageSize = Math.max(columns, maximumRows * columns);
        List<Entry> visible = MobEditorPickerLogic.page(filtered, page, pageSize);
        int cardWidth = Math.max(1, (panelWidth - 32 - (columns - 1) * 8) / columns);
        for (int index = 0; index < visible.size(); index++) {
            Entry entry = visible.get(index);
            int x = panelX + 16 + index % columns * (cardWidth + 8);
            int y = panelY + 96 + index / columns * 46;
            boolean colorEntry = entry.category().equals("Minecraftカラー");
            MobPickerEntryButton button = new MobPickerEntryButton(
                    x, y, cardWidth, 38,
                    Component.literal(entry.label() + "  [" + entry.id() + "]"),
                    entry.id().equalsIgnoreCase(selected)
                            ? ProjectSButton.Kind.SECONDARY : ProjectSButton.Kind.GHOST,
                    entry.itemStack().isEmpty() && !colorEntry ? entry.icon() : null,
                    () -> choose(entry), entry.itemStack(),
                    colorEntry ? minecraftColor(entry.id()) : -1);
            button.selected(entry.id().equalsIgnoreCase(selected));
            button.active = entry.enabled();
            String tooltip = entry.enabled()
                    ? entry.id() + (entry.category().isBlank()
                    ? "" : "\nカテゴリ: " + entry.category())
                    : entry.disabledReason();
            button.setTooltip(Tooltip.create(Component.literal(tooltip)));
            addRenderableWidget(button);
        }
        int bottom = panelY + panelHeight - 44;
        addRenderableWidget(new ProjectSButton(panelX + 16, bottom, 84, 28,
                Component.literal("前へ"), ProjectSButton.Kind.GHOST,
                ProjectSIcon.BACK, () -> {
                    page = Math.max(0, page - 1);
                    rebuild();
                }));
        addRenderableWidget(new ProjectSButton(panelX + 108, bottom, 84, 28,
                Component.literal("次へ"), ProjectSButton.Kind.GHOST,
                ProjectSIcon.NEXT, () -> {
                    if ((page + 1) * pageSize < filtered.size()) page++;
                    rebuild();
                }));
        addRenderableWidget(new ProjectSButton(panelX + panelWidth - 112, bottom, 96, 28,
                Component.literal("キャンセル"), ProjectSButton.Kind.GHOST,
                ProjectSIcon.CLOSE, this::onClose));
    }

    private void choose(Entry entry) {
        if (!entry.enabled()) return;
        RECENT.remove(entry.id());
        RECENT.add(entry.id());
        while (RECENT.size() > 24) RECENT.remove(RECENT.getFirst());
        selection.accept(entry.id());
        minecraft.setScreen(parent);
    }

    private List<Entry> filtered() {
        return source.stream().filter(entry ->
                (category.equals("すべて") || category.equals(entry.category())
                        || category.equals("最近使用") && RECENT.contains(entry.id()))
                        && MobEditorPickerLogic.matches(entry.id(), entry.label(), query)).toList();
    }

    private void nextCategory() {
        List<String> values = Stream.concat(
                Stream.of("すべて", "最近使用"),
                source.stream().map(Entry::category)
                        .filter(value -> !value.isBlank()).distinct()).toList();
        category = values.get((values.indexOf(category) + 1) % values.size());
        page = 0;
        rebuild();
    }

    private void rebuild() {
        clearWidgets();
        init();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics,
            int mouseX, int mouseY, float tickProgress) {
        var theme = ProjectSThemeManager.get().activeTheme();
        var tokens = theme.tokens();
        graphics.fill(0, 0, width, height, tokens.background());
        ProjectSUiDraw.cutPanel(graphics, panelX, panelY, panelWidth, panelHeight,
                theme.metrics().modalCornerCut(), tokens.surfaceRaised(), tokens.borderStrong());
        ProjectSIconRenderer.draw(graphics, ProjectSIcon.SEARCH,
                panelX + 16, panelY + 18, 18, tokens, false, false);
        graphics.text(font, heading, panelX + 44, panelY + 20,
                tokens.textPrimary(), false);
        List<Entry> filtered = filtered();
        graphics.text(font, filtered.size() + "件 / ページ " + (page + 1),
                panelX + panelWidth - 130, panelY + 22, tokens.textMuted(), false);
        if (filtered.isEmpty()) {
            graphics.centeredText(font, "一致する候補がありません",
                    panelX + panelWidth / 2, panelY + panelHeight / 2,
                    tokens.textMuted());
        }
        super.extractRenderState(graphics, mouseX, mouseY, tickProgress);
    }

    private static int minecraftColor(String id) {
        return switch (id) {
            case "BLACK" -> 0x000000;
            case "DARK_BLUE" -> 0x0000AA;
            case "DARK_GREEN" -> 0x00AA00;
            case "DARK_AQUA" -> 0x00AAAA;
            case "DARK_RED" -> 0xAA0000;
            case "DARK_PURPLE" -> 0xAA00AA;
            case "GOLD" -> 0xFFAA00;
            case "GRAY" -> 0xAAAAAA;
            case "DARK_GRAY" -> 0x555555;
            case "BLUE" -> 0x5555FF;
            case "GREEN" -> 0x55FF55;
            case "AQUA" -> 0x55FFFF;
            case "RED" -> 0xFF5555;
            case "LIGHT_PURPLE" -> 0xFF55FF;
            case "YELLOW" -> 0xFFFF55;
            case "ORANGE" -> 0xF9801D;
            case "MAGENTA" -> 0xC74EBD;
            case "LIGHT_BLUE" -> 0x3AB3DA;
            case "LIME" -> 0x80C71F;
            case "PINK" -> 0xF38BAA;
            case "LIGHT_GRAY" -> 0x9D9D97;
            case "CYAN" -> 0x169C9C;
            case "PURPLE" -> 0x8932B8;
            case "BROWN" -> 0x835432;
            default -> 0xFFFFFF;
        };
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
