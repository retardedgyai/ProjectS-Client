package io.github.gyai.projects.client.ui;

import io.github.gyai.projects.client.ui.icon.ProjectSIcon;
import io.github.gyai.projects.client.ui.icon.ProjectSIconAtlasRegion;
import io.github.gyai.projects.client.ui.icon.ProjectSIconCatalog;
import io.github.gyai.projects.client.ui.icon.ProjectSIconCategory;
import io.github.gyai.projects.client.ui.icon.ProjectSIconColorRole;
import io.github.gyai.projects.client.ui.icon.ProjectSIconFallback;
import io.github.gyai.projects.client.ui.icon.ProjectSIconGalleryLayout;
import io.github.gyai.projects.client.ui.icon.ProjectSIconRenderMode;
import io.github.gyai.projects.client.ui.icon.ProjectSIconRenderPolicy;
import io.github.gyai.projects.client.ui.icon.ProjectSIconState;
import io.github.gyai.projects.client.ui.icon.ProjectSIconTint;
import io.github.gyai.projects.client.ui.icon.ProjectSStandardIcons;
import io.github.gyai.projects.client.ui.render.ProjectSColorMath;
import io.github.gyai.projects.client.ui.render.ProjectSEasing;
import io.github.gyai.projects.client.ui.render.ProjectSOverlayPlacement;
import io.github.gyai.projects.client.ui.render.ProjectSUiLayout;
import io.github.gyai.projects.devtools.ui.DevToolsUiKitLayout;
import io.github.gyai.projects.client.ui.theme.ProjectSThemeConfig;
import io.github.gyai.projects.client.ui.theme.ProjectSThemeId;
import io.github.gyai.projects.client.ui.theme.ProjectSThemeManager;
import io.github.gyai.projects.client.ui.theme.ProjectSThemeRegistry;
import io.github.gyai.projects.client.ui.widget.ProjectSInteractionGate;
import io.github.gyai.projects.client.ui.widget.ProjectSNumberLogic;
import io.github.gyai.projects.client.ui.widget.ProjectSToastState;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public final class ProjectSUiLogicTest {
    private ProjectSUiLogicTest() { }

    public static void main(String[] args) throws Exception {
        themeRegistryAndActivation();
        configFallbackAndRoundTrip();
        listenerLifecycle();
        colorAndAnimation();
        visualTokensAndMetrics();
        iconRegistryAtlasTintAndSearch();
        numberLogic();
        placementAndResponsiveLayout();
        modalGateAndToast();
    }

    private static void themeRegistryAndActivation() throws Exception {
        ProjectSThemeRegistry registry = new ProjectSThemeRegistry();
        assert registry.all().size() == 4;
        assert registry.all().stream().filter(value -> value.enabled()).count() == 1;
        assert registry.get(ProjectSThemeId.OBSIDIAN).productionReady();
        assert !registry.get(ProjectSThemeId.FOREST).enabled();
        Path directory = Files.createTempDirectory("projects-ui-theme-");
        ProjectSThemeManager manager = new ProjectSThemeManager(
                registry, new ProjectSThemeConfig(directory.resolve("ui.json")));
        assert !manager.activate(ProjectSThemeId.FOREST).success();
        assert !manager.activate(ProjectSThemeId.SANDSTONE).success();
        assert !manager.activate(ProjectSThemeId.ARCTIC).success();
        assert manager.activeThemeId() == ProjectSThemeId.OBSIDIAN;
        deleteTree(directory);
    }

    private static void configFallbackAndRoundTrip() throws Exception {
        Path directory = Files.createTempDirectory("projects-ui-config-");
        Path path = directory.resolve("projects-client-ui.json");
        ProjectSThemeConfig config = new ProjectSThemeConfig(path);
        assert config.load().themeId() == ProjectSThemeId.OBSIDIAN;
        assert !config.load().valid();
        Files.writeString(path, "{broken");
        assert config.load().themeId() == ProjectSThemeId.OBSIDIAN;
        Files.writeString(path,
                "{\"schemaVersion\":1,\"activeTheme\":\"OBSIDIAN\" garbage}");
        assert !config.load().valid();
        Files.writeString(path,
                "{/*comment*/\"schemaVersion\":1,\"activeTheme\":\"OBSIDIAN\"}");
        assert !config.load().valid();
        Files.writeString(path, "{\"schemaVersion\":1,"
                + "\"activeTheme\":\"OBSIDIAN\",\"future\":true}");
        assert config.load().valid();
        assert config.load().themeId() == ProjectSThemeId.OBSIDIAN;
        Files.writeString(path, "{\"schemaVersion\":1,"
                + "\"activeTheme\":\"UNKNOWN\",\"future\":true}");
        assert config.load().themeId() == ProjectSThemeId.OBSIDIAN;
        Files.writeString(path, "x".repeat(17 * 1024));
        assert !config.load().valid();
        assert config.save(ProjectSThemeId.OBSIDIAN);
        assert config.load().valid();
        assert config.load().themeId() == ProjectSThemeId.OBSIDIAN;
        ProjectSThemeManager manager = new ProjectSThemeManager(
                new ProjectSThemeRegistry(), config);
        assert manager.activate(ProjectSThemeId.OBSIDIAN).success();
        ProjectSThemeManager reloaded = new ProjectSThemeManager(
                new ProjectSThemeRegistry(), config);
        assert reloaded.activeThemeId() == ProjectSThemeId.OBSIDIAN;
        Files.writeString(path,
                "{\"schemaVersion\":1,\"activeTheme\":\"FOREST\"}");
        reloaded.reload();
        assert reloaded.activeThemeId() == ProjectSThemeId.OBSIDIAN;
        deleteTree(directory);
    }

    private static void listenerLifecycle() throws Exception {
        Path directory = Files.createTempDirectory("projects-ui-listener-");
        ProjectSThemeManager manager = new ProjectSThemeManager(
                new ProjectSThemeRegistry(),
                new ProjectSThemeConfig(directory.resolve("ui.json")));
        AtomicInteger calls = new AtomicInteger();
        var listener = (io.github.gyai.projects.client.ui.theme.ThemeChangeListener)
                (previous, current) -> calls.incrementAndGet();
        manager.addListener(listener);
        assert manager.listenerCount() == 1;
        assert manager.activate(ProjectSThemeId.OBSIDIAN).success();
        assert calls.get() == 1;
        manager.removeListener(listener);
        assert manager.listenerCount() == 0;
        manager.activate(ProjectSThemeId.OBSIDIAN);
        assert calls.get() == 1;
        deleteTree(directory);
    }

    private static void colorAndAnimation() {
        assert ProjectSColorMath.lerpArgb(0xFF000000, 0xFFFFFFFF, .5)
                == 0xFF808080;
        assert ProjectSColorMath.lerpArgb(0xFF000000, 0xFFFFFFFF, -5)
                == 0xFF000000;
        assert ProjectSEasing.progress(50, 100, 100) == 0;
        assert ProjectSEasing.progress(250, 100, 100) == 1;
        assert ProjectSEasing.progress(150, 100, 100) == .5;
        assert ProjectSEasing.smooth(-1) == 0;
        assert ProjectSEasing.smooth(2) == 1;
    }

    private static void visualTokensAndMetrics() {
        var theme = new ProjectSThemeRegistry().fallback();
        var tokens = theme.tokens();
        var metrics = theme.metrics();
        assert ProjectSColorMath.brightness(tokens.accentPrimary())
                < ProjectSColorMath.brightness(tokens.accentPrimaryHover());
        assert ProjectSColorMath.brightness(tokens.accentPrimary()) >= 180;
        assert ProjectSColorMath.brightness(tokens.accentPrimary()) <= 240;
        assert ProjectSColorMath.brightness(tokens.accentPrimaryPressed())
                < ProjectSColorMath.brightness(tokens.accentPrimary());
        assert ProjectSColorMath.brightness(tokens.border())
                < ProjectSColorMath.brightness(tokens.borderSelected());
        assert ProjectSColorMath.brightness(tokens.borderCard())
                < ProjectSColorMath.brightness(tokens.borderSubtle());
        assert ProjectSColorMath.brightness(tokens.warningSurface()) < 40;
        assert ProjectSColorMath.brightness(tokens.dangerSurface()) < 40;
        assert ProjectSColorMath.brightness(tokens.dangerSurface())
                < ProjectSColorMath.brightness(tokens.danger()) / 3;
        assert ProjectSColorMath.brightness(tokens.textDisabled())
                < ProjectSColorMath.brightness(tokens.textPrimary());
        assert metrics.inputCornerCut() <= 2;
        assert metrics.iconCornerCut() > metrics.inputCornerCut();
        assert ProjectSColorMath.brightness(tokens.background())
                < ProjectSColorMath.brightness(tokens.surfaceAlt());
        assert ProjectSColorMath.brightness(tokens.surfaceAlt())
                < ProjectSColorMath.brightness(tokens.surface());
        assert ProjectSColorMath.brightness(tokens.surface())
                < ProjectSColorMath.brightness(tokens.surfaceRaised());
    }

    private static void numberLogic() {
        assert ProjectSNumberLogic.isIntermediate("");
        assert ProjectSNumberLogic.isIntermediate("-");
        assert ProjectSNumberLogic.isIntermediate(".");
        assert ProjectSNumberLogic.parseFinite("NaN") == null;
        assert ProjectSNumberLogic.parseFinite("Infinity") == null;
        assert ProjectSNumberLogic.parseFinite("12.5") == 12.5;
        assert ProjectSNumberLogic.clamp(-1, 0, 100) == 0;
        assert ProjectSNumberLogic.clamp(101, 0, 100) == 100;
        assert ProjectSNumberLogic.step(10, .5, false, 0, 100) == 10.5;
        assert ProjectSNumberLogic.step(10, .5, true, 0, 100) == 15;
    }

    @SuppressWarnings("deprecation")
    private static void iconRegistryAtlasTintAndSearch() {
        List<ProjectSIcon> icons = ProjectSIconCatalog.all();
        assert icons.size() == ProjectSIcon.registeredCount();
        assert new HashSet<>(icons.stream().map(ProjectSIcon::id).toList()).size()
                == icons.size();
        assert new HashSet<>(icons.stream().map(ProjectSIcon::debugName).toList()).size()
                == icons.size();
        assert ProjectSIconCatalog.validate(icons).isEmpty();
        for (ProjectSIcon icon : icons) {
            assert icon.category() != null;
            assert !icon.displayName().isBlank();
            assert !icon.debugName().isBlank();
            assert icon.fallback() != null;
            assert icon.fallback() != ProjectSIconFallback.MISSING;
            assert icon.atlas16().u() >= 0 && icon.atlas16().v() >= 0;
            assert icon.atlas32().u() >= 0 && icon.atlas32().v() >= 0;
            assert icon.atlas16().matchesCellSize(16);
            assert icon.atlas32().matchesCellSize(32);
            assert icon.atlas32().u() == icon.atlas16().u() * 2;
            assert icon.atlas32().v() == icon.atlas16().v() * 2;
        }
        ProjectSIconCategory[] categories = ProjectSIconCategory.values();
        for (int index = 0; index < categories.length; index++) {
            assert categories[index].displayOrder() == index;
            assert !categories[index].displayName().isBlank();
            assert !ProjectSIconCatalog.category(categories[index]).isEmpty();
        }
        assert ProjectSIcon.REFRESH == ProjectSIcon.RELOAD;
        assert ProjectSIcon.MOB == ProjectSIcon.MOB_GENERIC;
        assert ProjectSIcon.SKULL == ProjectSIcon.UNDEAD;
        assert ProjectSIcon.atlasWidth16() == 256;
        assert ProjectSIcon.atlasHeight16() == 112;
        assert ProjectSIcon.atlasWidth32() == 512;
        assert ProjectSIcon.atlasHeight32() == 224;

        ProjectSIconAtlasRegion invalid = new ProjectSIconAtlasRegion(
                -1, 0, 16, 16, 256, 128);
        ProjectSIconAtlasRegion overflow = new ProjectSIconAtlasRegion(
                250, 120, 16, 16, 256, 128);
        assert !ProjectSIconRenderPolicy.usable(invalid, 16);
        assert !ProjectSIconRenderPolicy.usable(overflow, 16);
        assert ProjectSIconRenderPolicy.select(ProjectSIcon.SAVE, 16, true, true)
                == ProjectSIconRenderMode.ATLAS_16;
        assert ProjectSIconRenderPolicy.select(ProjectSIcon.SAVE, 20, true, true)
                == ProjectSIconRenderMode.ATLAS_32;
        assert ProjectSIconRenderPolicy.select(ProjectSIcon.SAVE, 32, false, false)
                == ProjectSIconRenderMode.CODE_FALLBACK;
        assert ProjectSIconRenderPolicy.select(null, 16, true, true)
                == ProjectSIconRenderMode.CODE_FALLBACK;
        assert !ProjectSIconCatalog.hasImplementedFallback(ProjectSIconFallback.MISSING);

        var tokens = new ProjectSThemeRegistry().fallback().tokens();
        assert ProjectSIconTint.resolve(ProjectSIconColorRole.DEFAULT,
                ProjectSIconState.NORMAL, tokens) == tokens.textSecondary();
        assert ProjectSIconTint.resolve(ProjectSIconColorRole.DEFAULT,
                ProjectSIconState.HOVERED, tokens) == tokens.textPrimary();
        assert ProjectSIconTint.resolve(ProjectSIconColorRole.DEFAULT,
                ProjectSIconState.SELECTED, tokens) == tokens.accentPrimary();
        assert ProjectSIconTint.resolve(ProjectSIconColorRole.DEFAULT,
                ProjectSIconState.FOCUSED, tokens) == tokens.accentPrimaryHover();
        assert ProjectSIconTint.resolve(ProjectSIconColorRole.DEFAULT,
                ProjectSIconState.DISABLED, tokens) == tokens.textDisabled();
        assert ProjectSIconTint.resolve(ProjectSIconColorRole.SUCCESS,
                ProjectSIconState.NORMAL, tokens) == tokens.success();
        assert ProjectSIconTint.resolve(ProjectSIconColorRole.WARNING,
                ProjectSIconState.NORMAL, tokens) == tokens.warning();
        assert ProjectSIconTint.resolve(ProjectSIconColorRole.DANGER,
                ProjectSIconState.NORMAL, tokens) == tokens.danger();
        assert ProjectSIconTint.resolve(ProjectSIconColorRole.INFO,
                ProjectSIconState.NORMAL, tokens) == tokens.info();

        assert ProjectSIconCatalog.search(null, "save").contains(ProjectSIcon.SAVE);
        assert ProjectSIconCatalog.search(null, "保存").contains(ProjectSIcon.SAVE);
        assert ProjectSIconCatalog.search(null, "").size() == icons.size();
        assert ProjectSIconCatalog.search(ProjectSIconCategory.COMBAT, "").stream()
                .allMatch(icon -> icon.category() == ProjectSIconCategory.COMBAT);
        assert ProjectSIconCatalog.validate(List.of(ProjectSIcon.SAVE, ProjectSIcon.SAVE))
                .stream().anyMatch(error -> error.startsWith("duplicate id"));

        assert ProjectSStandardIcons.INCREMENT == ProjectSIcon.ADD;
        assert ProjectSStandardIcons.DECREMENT == ProjectSIcon.REMOVE;
        assert ProjectSStandardIcons.DROPDOWN == ProjectSIcon.DROPDOWN;
        assert ProjectSStandardIcons.LOCKED == ProjectSIcon.LOCK;
        assert ProjectSStandardIcons.LOADING == ProjectSIcon.LOADING;

        int maxCategorySize = Arrays.stream(ProjectSIconCategory.values())
                .mapToInt(category -> ProjectSIconCatalog.category(category).size())
                .max().orElseThrow();
        int galleryWidth = (ProjectSUiLayout.contentWidth(960) - 12) / 2 - 20;
        int galleryHeight = ProjectSIconGalleryLayout.contentHeight(
                maxCategorySize, galleryWidth);
        assert galleryHeight + 42
                <= DevToolsUiKitLayout.sectionHeight(
                DevToolsUiKitLayout.Section.ICON_GALLERY, false);
        int narrowGalleryWidth = 248;
        int narrowControlsHeight = ProjectSIconGalleryLayout.controlsHeight(
                narrowGalleryWidth);
        assert narrowControlsHeight == 168;
        assert 88 + 36 + 28 < narrowControlsHeight;
        assert ProjectSIconGalleryLayout.contentHeight(
                maxCategorySize, narrowGalleryWidth) + 42
                <= DevToolsUiKitLayout.sectionHeight(
                DevToolsUiKitLayout.Section.ICON_GALLERY, true);
        assert ProjectSIconGalleryLayout.visible(100, 52, 58, 487);
        assert !ProjectSIconGalleryLayout.visible(500, 52, 58, 487);
    }

    private static void placementAndResponsiveLayout() {
        var dropdown = ProjectSOverlayPlacement.dropdown(
                300, 190, 120, 100, 320, 240, 6);
        assert dropdown.x() + dropdown.width() <= 314;
        assert dropdown.y() + dropdown.height() <= 234;
        var tooltip = ProjectSOverlayPlacement.tooltip(
                315, 235, 140, 60, 320, 240, 6);
        assert tooltip.x() >= 6 && tooltip.x() + tooltip.width() <= 314;
        assert tooltip.y() >= 6 && tooltip.y() + tooltip.height() <= 234;
        assert ProjectSUiLayout.columns(640) == 1;
        assert ProjectSUiLayout.columns(800) == 1;
        assert ProjectSUiLayout.columns(819) == 1;
        assert ProjectSUiLayout.columns(820) == 2;
        assert ProjectSUiLayout.columns(900) == 2;
        assert ProjectSUiLayout.contentWidth(300) == 268;
        assert ProjectSUiLayout.contentWidth(320) == 288;
        assert ProjectSUiLayout.contentWidth(960) == 928;
        assert ProjectSUiLayout.columns(960) == 2;
        int viewport = 529 - DevToolsUiKitLayout.headerHeight() - DevToolsUiKitLayout.footerHeight();
        assert viewport == 429;
        assert DevToolsUiKitLayout.headerHeight() + viewport == 529 - DevToolsUiKitLayout.footerHeight();
        int uiKitContentHeight = DevToolsUiKitLayout.contentHeight(960);
        assert uiKitContentHeight == 1120;
        int uiKitMaxScroll = ProjectSUiLayout.maxScroll(
                uiKitContentHeight, viewport);
        int finalSectionBottom = DevToolsUiKitLayout.headerHeight() + 6
                + (uiKitContentHeight - 12) - uiKitMaxScroll;
        assert finalSectionBottom <= 529 - DevToolsUiKitLayout.footerHeight();
        assert ProjectSUiLayout.themeViewportHeight(180)
                >= ProjectSUiLayout.themeCardHeight(180);
        assert ProjectSUiLayout.themeViewportHeight(240)
                >= ProjectSUiLayout.themeCardHeight(240);
        int compactMaxScroll = ProjectSUiLayout.maxScroll(
                ProjectSUiLayout.themeContentHeight(4, 180),
                ProjectSUiLayout.themeViewportHeight(180));
        int lastCardY = ProjectSUiLayout.themeHeaderHeight(180) + 6
                + 3 * (ProjectSUiLayout.themeCardHeight(180) + 12)
                - compactMaxScroll;
        assert lastCardY >= ProjectSUiLayout.themeHeaderHeight(180);
        assert lastCardY + ProjectSUiLayout.themeCardHeight(180)
                <= 180 - ProjectSUiLayout.themeFooterHeight(180);
        assert ProjectSUiLayout.includeVisibilityStop(
                1080, 1110, 1098, 1100) == 1098;
        assert ProjectSUiLayout.fullyVisible(60, 78, 58, 138);
        assert !ProjectSUiLayout.fullyVisible(59, 80, 58, 138);
        assert ProjectSUiLayout.maxScroll(900, 400) == 500;
        assert ProjectSUiLayout.clampScroll(800, 900, 400) == 500;
    }

    private static void modalGateAndToast() {
        ProjectSInteractionGate gate = new ProjectSInteractionGate();
        assert gate.canUseBackground();
        gate.openModal();
        assert !gate.canUseBackground();
        gate.closeModal();
        assert gate.canUseBackground();
        long start = 1_000;
        assert ProjectSToastState.phase(start, start, 100, 500)
                == ProjectSToastState.Phase.FADE_IN;
        assert ProjectSToastState.phase(start + 100, start, 100, 500)
                == ProjectSToastState.Phase.VISIBLE;
        assert ProjectSToastState.phase(start + 650, start, 100, 500)
                == ProjectSToastState.Phase.FADE_OUT;
        assert ProjectSToastState.phase(start + 700, start, 100, 500)
                == ProjectSToastState.Phase.EXPIRED;
    }

    private static void deleteTree(Path directory) throws Exception {
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}
