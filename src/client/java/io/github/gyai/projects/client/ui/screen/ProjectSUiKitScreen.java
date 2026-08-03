package io.github.gyai.projects.client.ui.screen;

import io.github.gyai.projects.client.ui.render.ProjectSIconRenderer;
import io.github.gyai.projects.client.ui.icon.ProjectSIcon;
import io.github.gyai.projects.client.ui.icon.ProjectSIconCatalog;
import io.github.gyai.projects.client.ui.icon.ProjectSIconCategory;
import io.github.gyai.projects.client.ui.icon.ProjectSIconGalleryLayout;
import io.github.gyai.projects.client.ui.render.ProjectSIconAtlas;
import io.github.gyai.projects.client.ui.render.ProjectSUiDraw;
import io.github.gyai.projects.client.ui.render.ProjectSUiLayout;
import io.github.gyai.projects.client.ui.theme.ProjectSThemeId;
import io.github.gyai.projects.client.ui.theme.ProjectSThemeManager;
import io.github.gyai.projects.client.ui.widget.ProjectSButton;
import io.github.gyai.projects.client.ui.widget.ProjectSCard;
import io.github.gyai.projects.client.ui.widget.ProjectSDropdown;
import io.github.gyai.projects.client.ui.widget.ProjectSIconButton;
import io.github.gyai.projects.client.ui.widget.ProjectSIconGalleryEntry;
import io.github.gyai.projects.client.ui.widget.ProjectSModal;
import io.github.gyai.projects.client.ui.widget.ProjectSNumberField;
import io.github.gyai.projects.client.ui.widget.ProjectSTabBar;
import io.github.gyai.projects.client.ui.widget.ProjectSTextField;
import io.github.gyai.projects.client.ui.widget.ProjectSToast;
import io.github.gyai.projects.client.ui.widget.ProjectSToggle;
import io.github.gyai.projects.client.ui.widget.ProjectSTooltip;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class ProjectSUiKitScreen extends ProjectSThemedScreen {
    private static final int HEADER_HEIGHT = ProjectSUiLayout.uiKitHeaderHeight();
    private static final int FOOTER_HEIGHT = ProjectSUiLayout.uiKitFooterHeight();
    private static final int GALLERY_ENTRY_COUNT = java.util.Arrays.stream(
                    ProjectSIconCategory.values())
            .mapToInt(category -> ProjectSIconCatalog.category(category).size())
            .max().orElse(0);
    private final Screen parent;
    private final List<Section> sections = new ArrayList<>();
    private final List<ScrolledWidget> contentWidgets = new ArrayList<>();
    private final List<ProjectSIconGalleryEntry> galleryEntries = new ArrayList<>();
    private final ProjectSTooltip customTooltip = new ProjectSTooltip(
            Component.literal("ProjectS Tooltip"),
            Component.literal("画面端では自動的に位置を補正します"),
            ProjectSTooltip.Tone.NORMAL);
    private int scroll;
    private int contentHeight;
    private int panelX;
    private int panelWidth;
    private String sampleText = "pirate_swordsman";
    private double sampleNumber = 12.5;
    private boolean toggleValue = true;
    private int tabIndex;
    private String dropdownValue = "NORMAL";
    private String themeLabel;
    private String guiSizeLabel;
    private String iconRegistryLabel;
    private String iconAtlasLabel;
    private String galleryBackendLabel;
    private long iconAtlasRevision = -1;
    private String galleryQuery = "";
    private ProjectSIconCategory galleryCategory = ProjectSIconCategory.COMMON;
    private List<ProjectSIcon> galleryIcons = List.of();
    private ProjectSTextField gallerySearch;
    private ProjectSButton tooltipButton;

    public ProjectSUiKitScreen(Screen parent) {
        super(Component.literal("ProjectS UI Kit"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ProjectSDropdown.closeAny();
        sections.clear();
        contentWidgets.clear();
        galleryEntries.clear();
        panelWidth = ProjectSUiLayout.contentWidth(width);
        panelX = (width - panelWidth) / 2;
        themeLabel = "Theme: " + ProjectSThemeManager.get().activeThemeId();
        guiSizeLabel = "GUI " + width + " x " + height;
        iconRegistryLabel = "Icons " + ProjectSIcon.registeredCount();
        refreshAtlasDiagnostics();
        updateGalleryResults();
        int columns = ProjectSUiLayout.columns(width);
        int gap = 12;
        int columnWidth = columns == 2 ? (panelWidth - gap) / 2 : panelWidth;
        if (columns == 1) {
            boolean narrow = columnWidth < 380;
            int y = 0;
            y = section("Buttons", panelX, y, columnWidth,
                    sectionHeight(ProjectSUiLayout.UiKitSection.BUTTONS, narrow));
            y = section("Icon Gallery", panelX, y, columnWidth,
                    sectionHeight(ProjectSUiLayout.UiKitSection.ICON_GALLERY, narrow));
            y = section("Inputs", panelX, y, columnWidth,
                    sectionHeight(ProjectSUiLayout.UiKitSection.INPUTS, narrow));
            y = section("Navigation", panelX, y, columnWidth,
                    sectionHeight(ProjectSUiLayout.UiKitSection.NAVIGATION, narrow));
            y = section("Cards & Badges", panelX, y, columnWidth,
                    sectionHeight(ProjectSUiLayout.UiKitSection.CARDS, narrow));
            y = section("Feedback", panelX, y, columnWidth,
                    sectionHeight(ProjectSUiLayout.UiKitSection.FEEDBACK, narrow));
            y = section("Theme Cards", panelX, y, columnWidth,
                    sectionHeight(ProjectSUiLayout.UiKitSection.THEMES, narrow));
            section("Color Tokens / Spacing", panelX, y, columnWidth,
                    sectionHeight(ProjectSUiLayout.UiKitSection.TOKENS, narrow));
        } else {
            int leftX = panelX;
            int rightX = panelX + columnWidth + gap;
            int leftY = 0;
            leftY = section("Buttons", leftX, leftY, columnWidth,
                    sectionHeight(ProjectSUiLayout.UiKitSection.BUTTONS, false));
            leftY = section("Inputs", leftX, leftY, columnWidth,
                    sectionHeight(ProjectSUiLayout.UiKitSection.INPUTS, false));
            leftY = section("Feedback", leftX, leftY, columnWidth,
                    sectionHeight(ProjectSUiLayout.UiKitSection.FEEDBACK, false));
            section("Theme Cards", leftX, leftY, columnWidth,
                    sectionHeight(ProjectSUiLayout.UiKitSection.THEMES, false));
            int rightY = 0;
            rightY = section("Icon Gallery", rightX, rightY, columnWidth,
                    sectionHeight(ProjectSUiLayout.UiKitSection.ICON_GALLERY, false));
            rightY = section("Navigation", rightX, rightY, columnWidth,
                    sectionHeight(ProjectSUiLayout.UiKitSection.NAVIGATION, false));
            rightY = section("Cards & Badges", rightX, rightY, columnWidth,
                    sectionHeight(ProjectSUiLayout.UiKitSection.CARDS, false));
            section("Color Tokens / Spacing", rightX, rightY, columnWidth,
                    sectionHeight(ProjectSUiLayout.UiKitSection.TOKENS, false));
        }
        contentHeight = ProjectSUiLayout.uiKitContentHeight(width);
        scroll = ProjectSUiLayout.clampScroll(
                scroll, contentHeight, height - HEADER_HEIGHT - FOOTER_HEIGHT);
        for (Section section : sections) buildSection(section);
        addRenderableWidget(new ProjectSIconButton(
                panelX, height - 34, 32, 24,
                ProjectSIcon.CLOSE,
                Component.literal("前の画面へ戻る"),
                ProjectSButton.Kind.GHOST, this::onClose));
        addRenderableWidget(new ProjectSButton(
                panelX + 40, height - 34, 124, 24,
                Component.literal("テーマ画面"), ProjectSButton.Kind.SECONDARY,
                () -> minecraft.setScreen(new ProjectSThemeScreen(this))));
    }

    private int section(String title, int x, int y, int width, int height) {
        sections.add(new Section(title, x, y, width, height));
        return y + height + 12;
    }

    private static int sectionHeight(
            ProjectSUiLayout.UiKitSection section, boolean narrow
    ) {
        return ProjectSUiLayout.uiKitSectionHeight(section, narrow);
    }

    private void buildSection(Section section) {
        int y = contentY(section) + 32;
        if (section.title.equals("Buttons")) buildButtons(section.x + 10, y, section.width - 20);
        if (section.title.equals("Icon Gallery")) buildIconGallery(
                section.x + 10, y, section.width - 20);
        if (section.title.equals("Inputs")) buildInputs(section.x + 10, y, section.width - 20);
        if (section.title.equals("Navigation")) buildNavigation(section.x + 10, y, section.width - 20);
        if (section.title.equals("Cards & Badges")) buildCards(section.x + 10, y, section.width - 20);
        if (section.title.equals("Feedback")) buildFeedback(section.x + 10, y, section.width - 20);
        if (section.title.equals("Theme Cards")) buildThemeCards(section.x + 10, y, section.width - 20);
    }

    private void buildButtons(int x, int y, int available) {
        boolean narrow = available < 360;
        int buttonWidth = narrow ? (available - 6) / 2 : (available - 18) / 4;
        int secondX = x + buttonWidth + 6;
        int thirdX = narrow ? x : x + (buttonWidth + 6) * 2;
        int fourthX = narrow ? secondX : x + (buttonWidth + 6) * 3;
        int thirdY = narrow ? y + 34 : y;
        addIfVisible(new ProjectSButton(x, y, buttonWidth, 26,
                Component.literal("Primary"), ProjectSButton.Kind.PRIMARY,
                () -> toast("Primary", ProjectSToast.Kind.SUCCESS)));
        addIfVisible(new ProjectSButton(secondX, y, buttonWidth, 26,
                Component.literal("Secondary"), ProjectSButton.Kind.SECONDARY,
                () -> toast("Secondary", ProjectSToast.Kind.INFO)));
        addIfVisible(new ProjectSButton(thirdX, thirdY, buttonWidth, 26,
                Component.literal("Ghost"), ProjectSButton.Kind.GHOST,
                () -> toast("Ghost", ProjectSToast.Kind.INFO)));
        addIfVisible(new ProjectSButton(fourthX, thirdY, buttonWidth, 26,
                Component.literal("Danger"), ProjectSButton.Kind.DANGER,
                () -> openDangerModal()));
        int statesY = y + (narrow ? 70 : 38);
        ProjectSButton selected = new ProjectSButton(x, statesY, buttonWidth, 26,
                Component.literal("Selected"), ProjectSButton.Kind.SECONDARY, () -> { });
        selected.selected(true);
        addIfVisible(selected);
        ProjectSButton disabled = new ProjectSButton(secondX, statesY,
                buttonWidth, 26, Component.literal("Disabled"),
                ProjectSButton.Kind.SECONDARY, () -> { });
        disabled.active = false;
        addIfVisible(disabled);
        ProjectSButton loading = new ProjectSButton(
                narrow ? x : x + (buttonWidth + 6) * 2,
                statesY + (narrow ? 34 : 0),
                buttonWidth, 26, Component.literal("Loading"),
                ProjectSButton.Kind.PRIMARY, () -> { });
        loading.loading(true);
        addIfVisible(loading);
    }

    private void buildIconGallery(int x, int y, int available) {
        int fieldWidth = available >= 360 ? (available - 10) / 2 : available;
        gallerySearch = new ProjectSTextField(
                font, x, y + 12, fieldWidth, 28,
                Component.literal("Icon Search"), Component.literal("ID / 日本語名"));
        gallerySearch.setValue(galleryQuery);
        gallerySearch.setResponder(value -> {
            galleryQuery = value;
            updateGalleryResults();
        });
        addIfVisible(gallerySearch);
        List<ProjectSDropdown.Option<ProjectSIconCategory>> categoryOptions =
                java.util.Arrays.stream(ProjectSIconCategory.values())
                        .map(category -> new ProjectSDropdown.Option<>(category,
                                Component.literal(category.displayName()), true, null))
                        .toList();
        int dropdownY = y + (available >= 360 ? 12 : 48);
        addIfVisible(new ProjectSDropdown<>(
                available >= 360 ? x + fieldWidth + 10 : x,
                dropdownY, fieldWidth, 28,
                categoryOptions, galleryCategory.ordinal(), modal::isOpen,
                category -> {
                    galleryCategory = category;
                    updateGalleryResults();
                }));
        ProjectSIcon[] samples = {
                ProjectSIcon.SAVE, ProjectSIcon.EDIT, ProjectSIcon.APPLY,
                ProjectSIcon.DISABLED, ProjectSIcon.SUCCESS, ProjectSIcon.WARNING,
                ProjectSIcon.ERROR, ProjectSIcon.INFO
        };
        int samplesY = y + (available >= 360 ? 52 : 88);
        int perRow = Math.max(1, Math.min(samples.length, available / 40));
        for (int index = 0; index < samples.length; index++) {
            ProjectSIcon icon = samples[index];
            ProjectSIconButton button = new ProjectSIconButton(
                    x + index % perRow * 40,
                    samplesY + index / perRow * 36, 32, 28, icon,
                    Component.literal(icon.displayName() + " / " + icon.id()),
                    icon == ProjectSIcon.DISABLED
                            ? Component.literal("Disabled Tintのサンプル") : null,
                    icon == ProjectSIcon.ERROR
                            ? ProjectSButton.Kind.DANGER : ProjectSButton.Kind.GHOST,
                    () -> toast(icon.id(), ProjectSToast.Kind.INFO));
            if (icon == ProjectSIcon.APPLY) button.selected(true);
            if (icon == ProjectSIcon.DISABLED) button.enabled(false);
            addIfVisible(button);
        }
        int controlsHeight = ProjectSIconGalleryLayout.controlsHeight(available);
        int columns = ProjectSIconGalleryLayout.columns(available);
        int gap = 8;
        int entryWidth = (available - gap * (columns - 1)) / columns;
        int entriesY = y + controlsHeight;
        for (int index = 0; index < GALLERY_ENTRY_COUNT; index++) {
            int column = index % columns;
            int row = index / columns;
            ProjectSIconGalleryEntry entry = new ProjectSIconGalleryEntry(
                    x + column * (entryWidth + gap),
                    entriesY + row * ProjectSIconGalleryLayout.ENTRY_HEIGHT,
                    entryWidth, ProjectSIconGalleryLayout.ENTRY_HEIGHT - 6,
                    galleryBackendLabel);
            entry.setIcon(index < galleryIcons.size() ? galleryIcons.get(index) : null);
            galleryEntries.add(entry);
            addIfVisible(entry);
        }
    }

    private void updateGalleryResults() {
        galleryIcons = ProjectSIconCatalog.search(galleryCategory, galleryQuery);
        for (int index = 0; index < galleryEntries.size(); index++) {
            galleryEntries.get(index).setIcon(
                    index < galleryIcons.size() ? galleryIcons.get(index) : null);
        }
        updateScrolledWidgets();
    }

    private void refreshAtlasDiagnostics() {
        boolean atlas16 = ProjectSIconAtlas.is16Available();
        boolean atlas32 = ProjectSIconAtlas.is32Available();
        iconAtlasLabel = ProjectSIconAtlas.statusLabel();
        galleryBackendLabel = atlas16 == atlas32
                ? atlas16 ? "atlas" : "fallback" : "mixed";
        iconAtlasRevision = ProjectSIconAtlas.revision();
        for (ProjectSIconGalleryEntry entry : galleryEntries) {
            entry.setBackendLabel(galleryBackendLabel);
        }
    }

    private void buildInputs(int x, int y, int available) {
        if (available < 360) {
            buildNarrowInputs(x, y, available);
            return;
        }
        int fieldWidth = Math.max(130, Math.min(220, available / 2 - 8));
        ProjectSTextField normal = new ProjectSTextField(
                font, x, y + 12, fieldWidth, 28,
                Component.literal("Text Field"), Component.literal("内部ID"));
        normal.setValue(sampleText);
        normal.setResponder(value -> sampleText = value);
        addIfVisible(normal);
        ProjectSTextField error = new ProjectSTextField(
                font, x + fieldWidth + 12, y + 12, fieldWidth, 28,
                Component.literal("Error"), Component.literal("required"));
        error.setValue("");
        error.error(Component.literal("入力が必要です"));
        addIfVisible(error);
        ProjectSTextField disabled = new ProjectSTextField(
                font, x, y + 76, fieldWidth, 28,
                Component.literal("Disabled"), Component.empty());
        disabled.setValue("locked_value");
        disabled.enabled(false);
        addIfVisible(disabled);
        ProjectSNumberField number = new ProjectSNumberField(
                font, x + fieldWidth + 44, y + 76,
                Math.max(60, fieldWidth - 64), 28,
                Component.literal("Number Field"), sampleNumber,
                0, 100, .5, 1, "%", value -> sampleNumber = value);
        addIfVisible(number);
        addIfVisible(new ProjectSIconButton(
                x + fieldWidth + 12, y + 76, 28, 28,
                ProjectSNumberField.DECREMENT_ICON,
                Component.literal("0.5減らす / Shiftで5減らす"),
                ProjectSButton.Kind.GHOST,
                input -> number.decrement(input.hasShiftDown())));
        addIfVisible(new ProjectSIconButton(
                x + available - 28, y + 76, 28, 28,
                ProjectSNumberField.INCREMENT_ICON,
                Component.literal("0.5増やす / Shiftで5増やす"),
                ProjectSButton.Kind.GHOST,
                input -> number.increment(input.hasShiftDown())));
    }

    private void buildNarrowInputs(int x, int y, int available) {
        ProjectSTextField normal = new ProjectSTextField(
                font, x, y + 12, available, 28,
                Component.literal("Text Field"), Component.literal("内部ID"));
        normal.setValue(sampleText);
        normal.setResponder(value -> sampleText = value);
        addIfVisible(normal);
        ProjectSTextField error = new ProjectSTextField(
                font, x, y + 76, available, 28,
                Component.literal("Error"), Component.literal("required"));
        error.error(Component.literal("入力が必要です"));
        addIfVisible(error);
        ProjectSTextField disabled = new ProjectSTextField(
                font, x, y + 140, available, 28,
                Component.literal("Disabled"), Component.empty());
        disabled.setValue("locked_value");
        disabled.enabled(false);
        addIfVisible(disabled);
        ProjectSNumberField number = new ProjectSNumberField(
                font, x + 32, y + 204, available - 64, 28,
                Component.literal("Number Field"), sampleNumber,
                0, 100, .5, 1, "%", value -> sampleNumber = value);
        addIfVisible(number);
        addIfVisible(new ProjectSIconButton(
                x, y + 204, 28, 28, ProjectSNumberField.DECREMENT_ICON,
                Component.literal("0.5減らす / Shiftで5減らす"),
                ProjectSButton.Kind.GHOST,
                input -> number.decrement(input.hasShiftDown())));
        addIfVisible(new ProjectSIconButton(
                x + available - 28, y + 204, 28, 28,
                ProjectSNumberField.INCREMENT_ICON,
                Component.literal("0.5増やす / Shiftで5増やす"),
                ProjectSButton.Kind.GHOST,
                input -> number.increment(input.hasShiftDown())));
    }

    private void buildNavigation(int x, int y, int available) {
        List<ProjectSTabBar.Tab> tabs = List.of(
                new ProjectSTabBar.Tab("basic", Component.literal("基本"),
                        ProjectSIcon.MOB_GENERIC, true, Component.literal("基本設定")),
                new ProjectSTabBar.Tab("stats", Component.literal("Stats"),
                        ProjectSIcon.STATS, true, Component.literal("数値設定")),
                new ProjectSTabBar.Tab("appearance", Component.literal("外見"),
                        ProjectSIcon.APPEARANCE, true, Component.literal("外見設定")),
                new ProjectSTabBar.Tab("future", Component.literal("将来"),
                        ProjectSIcon.LOCK, false, Component.literal("未実装タブ")));
        addIfVisible(new ProjectSTabBar(x, y, available, 30,
                tabs, tabIndex, index -> tabIndex = index));
        boolean narrow = available < 360;
        addIfVisible(new ProjectSToggle(
                x, y + 44, Math.min(210, available),
                Component.literal("ライブプレビュー"),
                Component.literal("色だけでなくON/OFFも表示"),
                toggleValue, value -> toggleValue = value));
        List<ProjectSDropdown.Option<String>> options = List.of(
                new ProjectSDropdown.Option<>("NORMAL", Component.literal("NORMAL"), true, null),
                new ProjectSDropdown.Option<>("ELITE", Component.literal("ELITE"), true, null),
                new ProjectSDropdown.Option<>("BOSS", Component.literal("BOSS"), true, null),
                new ProjectSDropdown.Option<>("MYTHIC", Component.literal("MYTHIC"), false,
                        Component.literal("今後追加予定の項目です")));
        int selected = Math.max(0, List.of("NORMAL", "ELITE", "BOSS", "MYTHIC")
                .indexOf(dropdownValue));
        addIfVisible(new ProjectSDropdown<>(
                narrow ? x : x + Math.min(220, available / 2),
                y + (narrow ? 102 : 44),
                narrow ? available : Math.min(180, available / 2), 28,
                options, selected, modal::isOpen, value -> dropdownValue = value));
    }

    private void buildCards(int x, int y, int available) {
        int cardWidth = Math.max(48, (available - 16) / 3);
        addIfVisible(new ProjectSCard(x, y, cardWidth, 70,
                Component.literal("Base"), Component.literal("標準カード"),
                ProjectSCard.State.BASE, () -> { }));
        addIfVisible(new ProjectSCard(x + cardWidth + 8, y, cardWidth, 70,
                Component.literal("Selected"), Component.literal("選択中の状態"),
                ProjectSCard.State.SELECTED, () -> { }));
        addIfVisible(new ProjectSCard(x + (cardWidth + 8) * 2, y, cardWidth, 70,
                Component.literal("Warning"), Component.literal("注意状態"),
                ProjectSCard.State.WARNING, () -> { }));
    }

    private void buildFeedback(int x, int y, int available) {
        boolean narrow = available < 360;
        int buttonWidth = narrow ? (available - 8) / 2 : (available - 16) / 3;
        addIfVisible(new ProjectSButton(x, y, buttonWidth, 28,
                Component.literal("Toast"), ProjectSButton.Kind.PRIMARY,
                () -> toasts.show(ProjectSToast.Kind.SUCCESS,
                        Component.literal("保存しました"),
                        Component.literal("Toastは画面参照を保持しません"), 2500)));
        addIfVisible(new ProjectSButton(x + buttonWidth + 8, y, buttonWidth, 28,
                Component.literal("Modal"), ProjectSButton.Kind.SECONDARY,
                this::openSampleModal));
        tooltipButton = new ProjectSButton(
                narrow ? x : x + (buttonWidth + 8) * 2,
                y + (narrow ? 38 : 0),
                buttonWidth, 28, Component.literal("Tooltip"),
                ProjectSButton.Kind.GHOST, () -> { });
        addIfVisible(tooltipButton);
    }

    private void buildThemeCards(int x, int y, int available) {
        boolean narrow = available < 360;
        int cardWidth = narrow ? (available - 6) / 2 : (available - 18) / 4;
        for (ProjectSThemeId id : ProjectSThemeId.values()) {
            boolean active = id == ProjectSThemeId.OBSIDIAN;
            int index = id.ordinal();
            ProjectSCard card = new ProjectSCard(
                    x + (narrow ? index % 2 : index) * (cardWidth + 6),
                    y + (narrow ? index / 2 * 88 : 0), cardWidth, 78,
                    Component.literal(id.name()),
                    Component.literal(active ? "正式" : "予定"),
                    active ? ProjectSCard.State.SELECTED : ProjectSCard.State.DISABLED,
                    active ? () -> { } : null);
            addIfVisible(card);
        }
    }

    private void addIfVisible(net.minecraft.client.gui.components.AbstractWidget widget) {
        int topMargin = widget instanceof ProjectSTextField ? 12 : 0;
        contentWidgets.add(new ScrolledWidget(
                widget, widget.getY() + scroll, topMargin));
        addRenderableWidget(widget);
        updateWidgetVisibility(contentWidgets.getLast());
    }

    private void updateScrolledWidgets() {
        for (ScrolledWidget entry : contentWidgets) {
            entry.widget.setY(entry.baseY - scroll);
            updateWidgetVisibility(entry);
            if (!entry.widget.visible && getFocused() == entry.widget) {
                setFocused(null);
            }
        }
    }

    private void updateWidgetVisibility(ScrolledWidget entry) {
        boolean hasContent = !(entry.widget instanceof ProjectSIconGalleryEntry galleryEntry)
                || galleryEntry.hasIcon();
        entry.widget.visible = hasContent && ProjectSUiLayout.fullyVisible(
                entry.widget.getY() - entry.topMargin,
                entry.widget.getHeight() + entry.topMargin,
                HEADER_HEIGHT, height - FOOTER_HEIGHT);
    }

    private void toast(String label, ProjectSToast.Kind kind) {
        toasts.show(kind, Component.literal(label),
                Component.literal("ProjectSButtonの操作を確認しました"), 1800);
    }

    private void openSampleModal() {
        modal.open(Component.literal("変更を適用しますか？"),
                Component.literal("Modal表示中は背面WidgetとDropdownを操作できません。"),
                Component.literal("適用"), () -> toast("適用", ProjectSToast.Kind.SUCCESS),
                Component.literal("キャンセル"), () -> { },
                ProjectSModal.PrimaryKind.PRIMARY, true);
    }

    private void openDangerModal() {
        modal.open(Component.literal("危険操作の確認"),
                Component.literal("赤色は破壊的または危険な操作にだけ使用します。"),
                Component.literal("実行"), () -> toast("危険操作", ProjectSToast.Kind.WARNING),
                Component.literal("戻る"), () -> { },
                ProjectSModal.PrimaryKind.DANGER, true);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickProgress
    ) {
        if (iconAtlasRevision != ProjectSIconAtlas.revision()) {
            refreshAtlasDiagnostics();
        }
        var theme = ProjectSThemeManager.get().activeTheme();
        var tokens = theme.tokens();
        graphics.fill(0, 0, width, height, tokens.background());
        graphics.fill(0, 0, width, HEADER_HEIGHT, tokens.backgroundAlt());
        graphics.fill(0, height - FOOTER_HEIGHT, width, height, tokens.backgroundAlt());
        ProjectSUiDraw.separator(graphics, 0, HEADER_HEIGHT - 1,
                width, tokens.borderSubtle());
        ProjectSUiDraw.separator(graphics, 0, height - FOOTER_HEIGHT,
                width, tokens.borderSubtle());
        graphics.text(font, "ProjectS UI Kit", panelX, 15, tokens.textPrimary(), false);
        graphics.text(font, themeLabel,
                panelX, 32, tokens.accentPrimary(), false);
        graphics.text(font, guiSizeLabel,
                panelX + panelWidth - 90, 23, tokens.textMuted(), false);
        graphics.enableScissor(0, HEADER_HEIGHT, width, height - FOOTER_HEIGHT);
        renderSections(graphics, mouseX, mouseY);
        graphics.disableScissor();
        super.extractRenderState(graphics, mouseX, mouseY, tickProgress);
    }

    private void renderSections(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        var theme = ProjectSThemeManager.get().activeTheme();
        var tokens = theme.tokens();
        for (Section section : sections) {
            int y = contentY(section);
            if (y + section.height < HEADER_HEIGHT || y > height - FOOTER_HEIGHT) continue;
            ProjectSUiDraw.cutPanel(graphics, section.x, y, section.width, section.height,
                    theme.metrics().cornerCut(), tokens.surface(), tokens.borderSubtle());
            graphics.text(font, section.title, section.x + 10, y + 10,
                    tokens.textPrimary(), false);
            if (section.title.equals("Color Tokens / Spacing")) {
                renderTokens(graphics, section.x + 10, y + 32, section.width - 20);
            }
            if (section.title.equals("Cards & Badges")) {
                renderBadges(graphics, section.x + 10, y + section.height - 26);
            }
            if (section.title.equals("Icon Gallery")) {
                renderIconGallery(graphics, section, y, mouseX, mouseY);
            }
        }
    }

    private void renderIconGallery(
            GuiGraphicsExtractor graphics, Section section, int sectionY,
            int mouseX, int mouseY
    ) {
        var tokens = ProjectSThemeManager.get().activeTheme().tokens();
        int available = section.width - 20;
        int contentX = section.x + 10;
        int contentStart = sectionY + 32;
        if (section.width >= 420) {
            graphics.text(font, iconRegistryLabel, section.x + 110, sectionY + 10,
                    tokens.textSecondary(), false);
            graphics.text(font, iconAtlasLabel,
                    section.x + section.width - 196, sectionY + 10,
                    tokens.textMuted(), false);
        } else {
            graphics.text(font, iconRegistryLabel,
                    section.x + section.width - 72, sectionY + 10,
                    tokens.textSecondary(), false);
            graphics.text(font, iconAtlasLabel, section.x + 10, sectionY + 22,
                    tokens.textMuted(), false);
        }
        if (galleryIcons.isEmpty()) {
            int entriesY = contentStart
                    + ProjectSIconGalleryLayout.controlsHeight(available);
            ProjectSIconRenderer.draw(graphics, ProjectSIcon.SEARCH,
                    contentX + 6, entriesY + 5, 20, tokens, false);
            graphics.text(font, "該当する登録アイコンはありません",
                    contentX + 34, entriesY + 11, tokens.textMuted(), false);
        }
    }

    private void renderTokens(GuiGraphicsExtractor graphics, int x, int y, int available) {
        var tokens = ProjectSThemeManager.get().activeTheme().tokens();
        int cell = Math.max(42, available / 5);
        tokenSwatch(graphics, x, y, cell, 0, "BG", tokens.background());
        tokenSwatch(graphics, x, y, cell, 1, "Panel", tokens.surface());
        tokenSwatch(graphics, x, y, cell, 2, "Card", tokens.surfaceAlt());
        tokenSwatch(graphics, x, y, cell, 3, "Input", tokens.surfaceInput());
        tokenSwatch(graphics, x, y, cell, 4, "Primary", tokens.accentPrimary());
        tokenSwatch(graphics, x, y, cell, 5, "Hover", tokens.surfaceHover());
        tokenSwatch(graphics, x, y, cell, 6, "Success", tokens.success());
        tokenSwatch(graphics, x, y, cell, 7, "Warning", tokens.warning());
        tokenSwatch(graphics, x, y, cell, 8, "Danger", tokens.danger());
        tokenSwatch(graphics, x, y, cell, 9, "Info", tokens.info());
        graphics.text(font, "Spacing  4 / 8 / 12 / 16 / 24",
                x, y + 74, tokens.textSecondary(), false);
        graphics.text(font, "Button  22 / 28 / 32   Field 28   Tab 30",
                x, y + 92, tokens.textMuted(), false);
    }

    private void tokenSwatch(
            GuiGraphicsExtractor graphics, int x, int y, int cell,
            int index, String label, int color
    ) {
        var tokens = ProjectSThemeManager.get().activeTheme().tokens();
        int drawX = x + index % 5 * cell;
        int drawY = y + index / 5 * 34;
        graphics.fill(drawX, drawY, drawX + 28, drawY + 14, color);
        graphics.outline(drawX, drawY, 28, 14, tokens.border());
        graphics.text(font, label, drawX, drawY + 17, tokens.textMuted(), false);
    }

    private void renderBadges(GuiGraphicsExtractor graphics, int x, int y) {
        var tokens = ProjectSThemeManager.get().activeTheme().tokens();
        badge(graphics, x, y, "SUCCESS", tokens.success(),
                ProjectSIcon.SUCCESS);
        badge(graphics, x + 78, y, "WARNING", tokens.warning(),
                ProjectSIcon.WARNING);
        badge(graphics, x + 156, y, "ERROR", tokens.danger(),
                ProjectSIcon.ERROR);
    }

    private void badge(
            GuiGraphicsExtractor graphics, int x, int y,
            String text, int color, ProjectSIcon icon
    ) {
        graphics.fill(x, y, x + 68, y + 16,
                ProjectSThemeManager.get().activeTheme().tokens().surfaceInput());
        graphics.fill(x, y, x + 2, y + 16, color);
        ProjectSIconRenderer.drawTinted(graphics, icon, x + 6, y + 3, 10, color);
        graphics.text(font, text, x + 20, y + 5, color, false);
    }

    @Override
    protected void extractThemedForeground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickProgress
    ) {
        if (!modal.isOpen() && tooltipButton != null && tooltipButton.visible) {
            customTooltip.render(graphics, font, mouseX, mouseY,
                    tooltipButton.isHovered(), tooltipButton.isFocused());
        }
    }

    private int contentY(Section section) {
        return HEADER_HEIGHT + 6 + section.contentY - scroll;
    }

    @Override
    public boolean mouseScrolled(
            double mouseX, double mouseY, double horizontal, double vertical
    ) {
        if (modal.isOpen()) return true;
        if (super.mouseScrolled(mouseX, mouseY, horizontal, vertical)) return true;
        if (mouseY >= HEADER_HEIGHT && mouseY < height - FOOTER_HEIGHT) {
            int next = ProjectSUiLayout.clampScroll(
                    scroll - (int) Math.round(vertical * 30),
                    contentHeight, height - HEADER_HEIGHT - FOOTER_HEIGHT);
            for (ScrolledWidget entry : contentWidgets) {
                if (entry.widget instanceof ProjectSIconGalleryEntry galleryEntry
                        && !galleryEntry.hasIcon()) continue;
                int visibilityStart = entry.baseY + entry.widget.getHeight()
                        - (height - FOOTER_HEIGHT);
                int visibilityEnd = entry.baseY - entry.topMargin - HEADER_HEIGHT;
                next = ProjectSUiLayout.includeVisibilityStop(
                        scroll, next, visibilityStart, visibilityEnd);
            }
            next = ProjectSUiLayout.clampScroll(
                    next, contentHeight, height - HEADER_HEIGHT - FOOTER_HEIGHT);
            if (next != scroll) {
                ProjectSDropdown.closeAny();
                scroll = next;
                updateScrolledWidgets();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (super.keyPressed(event)) return true;
        int viewport = height - HEADER_HEIGHT - FOOTER_HEIGHT;
        return switch (event.key()) {
            case GLFW.GLFW_KEY_PAGE_DOWN -> setScroll(scroll + Math.max(30,
                    viewport - ProjectSIconGalleryLayout.ENTRY_HEIGHT));
            case GLFW.GLFW_KEY_PAGE_UP -> setScroll(scroll - Math.max(30,
                    viewport - ProjectSIconGalleryLayout.ENTRY_HEIGHT));
            case GLFW.GLFW_KEY_HOME -> setScroll(0);
            case GLFW.GLFW_KEY_END -> setScroll(ProjectSUiLayout.maxScroll(
                    contentHeight, viewport));
            default -> false;
        };
    }

    private boolean setScroll(int requested) {
        int next = ProjectSUiLayout.clampScroll(requested, contentHeight,
                height - HEADER_HEIGHT - FOOTER_HEIGHT);
        if (next == scroll) return false;
        ProjectSDropdown.closeAny();
        scroll = next;
        updateScrolledWidgets();
        return true;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public void removed() {
        gallerySearch = null;
        galleryIcons = List.of();
        galleryEntries.clear();
        super.removed();
    }

    private record Section(String title, int x, int contentY, int width, int height) { }

    private record ScrolledWidget(
            net.minecraft.client.gui.components.AbstractWidget widget,
            int baseY,
            int topMargin
    ) { }
}
