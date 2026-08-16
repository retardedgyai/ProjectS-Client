package io.github.gyai.projects.client;

import io.github.gyai.projects.client.ui.icon.ProjectSIcon;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import io.github.gyai.projects.client.ui.mobeditor.AbilityEditorModel;
import io.github.gyai.projects.client.ui.mobeditor.AbilityUndoBaseline;
import io.github.gyai.projects.client.ui.mobeditor.DuplicateRequestCorrelation;
import io.github.gyai.projects.client.ui.mobeditor.AbilityAssignmentPanel;
import io.github.gyai.projects.client.ui.mobeditor.MobEditorActionBar;
import io.github.gyai.projects.client.ui.mobeditor.MobEditorLayout;
import io.github.gyai.projects.client.ui.mobeditor.MobEditorInputLogic;
import io.github.gyai.projects.client.ui.mobeditor.MobEditorPickerLogic;
import io.github.gyai.projects.client.ui.mobeditor.MobEditorTabBar;
import io.github.gyai.projects.client.ui.mobeditor.MobEditorUiLogic;
import io.github.gyai.projects.client.ui.mobeditor.MobEditorVariantLogic;
import io.github.gyai.projects.client.ui.mobeditor.MobItemStackCache;
import io.github.gyai.projects.client.ui.mobeditor.MobStringPickerScreen;
import io.github.gyai.projects.client.ui.mobeditor.LeatherColorPickerScreen;
import io.github.gyai.projects.client.ui.mobeditor.MobListPanel;
import io.github.gyai.projects.client.ui.mobeditor.MobPreviewPanel;
import io.github.gyai.projects.client.ui.mobeditor.MobPropertyPanel;
import io.github.gyai.projects.client.ui.mobeditor.widget.MobEquipmentSlotCard;
import io.github.gyai.projects.client.ui.mobeditor.widget.MobHeadCard;
import io.github.gyai.projects.client.ui.mobeditor.widget.MobLibraryEntry;
import io.github.gyai.projects.client.ui.render.ProjectSUiDraw;
import io.github.gyai.projects.client.ui.screen.ProjectSThemedScreen;
import io.github.gyai.projects.client.ui.theme.ProjectSThemeManager;
import io.github.gyai.projects.client.ui.widget.ProjectSButton;
import io.github.gyai.projects.client.ui.widget.ProjectSCard;
import io.github.gyai.projects.client.ui.widget.ProjectSIconButton;
import io.github.gyai.projects.client.ui.widget.ProjectSModal;
import io.github.gyai.projects.client.ui.widget.ProjectSDropdown;
import io.github.gyai.projects.client.ui.widget.ProjectSTextField;
import io.github.gyai.projects.client.ui.widget.ProjectSToast;

public final class MobEditorScreen extends ProjectSThemedScreen {
    private enum Tab { BASIC, STATS, AI, ABILITIES, APPEARANCE, TEST }
    private enum View { FRONT, BACK, LEFT, RIGHT, FREE, HEAD }

    private static final int ABILITY_ASSIGNED_PAGE_SIZE = 5;
    private static final int ABILITY_AVAILABLE_PAGE_SIZE = 4;
    private static final String CONFLICT_ERROR =
            "サーバー側で変更されたため、最新状態を確認してから保存してください";

    private final Screen parent;
    private final MobPreviewEntity preview = new MobPreviewEntity();
    private final Map<String, EditBox> fields = new HashMap<>();
    private final Map<String, String> labels = new HashMap<>();
    private final EnumMap<MobEditorData.Slot, MobEditorData.EquipmentSource>
            equipmentSources = new EnumMap<>(MobEditorData.Slot.class);
    private final java.util.LinkedHashSet<String> recentHeads =
            new java.util.LinkedHashSet<>();
    private MobEditorData.Mob draft;
    private MobEditorData.Mob original;
    private MobEditorData.Mob duplicateTemplate;
    private final DuplicateRequestCorrelation duplicateCorrelation =
            new DuplicateRequestCorrelation();
    private Tab tab = Tab.BASIC;
    private View view = View.FREE;
    private MobPreviewEntity.Animation animation = MobPreviewEntity.Animation.IDLE;
    private EditBox search;
    private EditBox newId;
    private int knownRevision;
    private int syncedStateRevision = -1;
    private boolean dirty;
    private boolean grid = true;
    private boolean hitbox;
    private boolean eyeLine;
    private boolean darkBackground = true;
    private double yaw;
    private double pitch;
    private double zoom = 48;
    private double previewOffsetX;
    private double previewOffsetY;
    private String searchQuery = "";
    private String categoryFilter = "ALL";
    private int mobPage;
    private int mobListOffset;
    private String headQuery = "";
    private String headBrowserTab = "ローカル";
    private int headPage;
    private MobEditorData.Slot selectedEquipmentSlot = MobEditorData.Slot.HEAD;
    private String appliedEntityType = "";
    private String localError = "";
    private final AbilityEditorModel abilities = new AbilityEditorModel();
    private final AbilityUndoBaseline abilityUndoBaseline = new AbilityUndoBaseline();
    private int assignedAbilityOffset;
    private int availableAbilityOffset;
    private MobEditorLayout layout;
    private int propertyScroll;
    private String lastToastMessage = "";
    private MobPropertyPanel propertyPanel;
    private boolean buildingProperty;
    private boolean pendingSave;
    private boolean pendingReload;
    private boolean pendingApply;
    private final List<MobLibraryEntry> libraryEntries = new ArrayList<>();

    private static final List<String> GLOW_COLORS = List.of(
            "WHITE", "YELLOW", "GOLD", "RED", "DARK_RED", "LIGHT_PURPLE",
            "DARK_PURPLE", "BLUE", "DARK_BLUE", "AQUA", "DARK_AQUA",
            "GREEN", "DARK_GREEN", "GRAY", "DARK_GRAY", "BLACK");
    private static final List<String> LIVING_ENTITY_TYPES = List.of(
            "ZOMBIE", "HUSK", "DROWNED", "ZOMBIE_VILLAGER", "SKELETON",
            "STRAY", "BOGGED", "CREEPER", "SPIDER", "CAVE_SPIDER",
            "ENDERMAN", "BLAZE", "WITCH", "PILLAGER", "VINDICATOR",
            "EVOKER", "RAVAGER", "PIGLIN", "PIGLIN_BRUTE", "HOGLIN",
            "ZOGLIN", "SLIME", "MAGMA_CUBE", "PHANTOM", "GUARDIAN",
            "ELDER_GUARDIAN", "WITHER", "ENDER_DRAGON", "WARDEN",
            "VILLAGER", "WANDERING_TRADER", "IRON_GOLEM", "SNOW_GOLEM",
            "WOLF", "CAT", "HORSE", "SHEEP", "COW", "PIG", "CHICKEN",
            "RABBIT", "FOX", "GOAT", "CAMEL", "ARMADILLO", "ALLAY");

    public MobEditorScreen(Screen parent) {
        super(Component.literal("Mob Editor"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        knownRevision = MobEditorClientState.localRevision();
        if (knownRevision != syncedStateRevision) syncState();
        fields.clear();
        labels.clear();
        libraryEntries.clear();
        layout = MobEditorLayout.of(width, height);
        if (!layout.usable()) {
            localError = "画面サイズが小さすぎます。ウィンドウを広げてください";
            return;
        }
        propertyPanel = new MobPropertyPanel(layout.property());
        propertyScroll = MobPropertyPanel.normalizeScroll(propertyScroll,
                layout.property().height(), propertyContentHeight());
        MobEditorLayout.Bounds mobList = layout.mobList();
        MobEditorLayout.Bounds actionBar = layout.actionBar();
        int sideX = mobList.x() + 4;
        search = addRenderableWidget(new ProjectSTextField(
                font, sideX, mobList.y() + 28, mobList.width() - 66, 20,
                Component.empty(), Component.literal("検索")));
        search.setValue(searchQuery);
        search.setMaxLength(64);
        search.setResponder(value -> {
            searchQuery = value;
            refreshLibraryEntries();
        });
        addThemedButton(mobList.x() + 4, mobList.y() + 4,
                Math.max(1, mobList.width() - 8), "カテゴリ: " + categoryFilter,
                this::cycleCategoryFilter);
        button(mobList.right() - 58, mobList.y() + 28, "検索", () -> {
            requestMobPage(0);
        }, 54);
        int sideActionY = MobListPanel.actionY(mobList);
        int createY = MobListPanel.createY(mobList);
        newId = addRenderableWidget(new ProjectSTextField(
                font, sideX, createY, mobList.width() - 66, 20,
                Component.empty(), Component.literal("新規ID")));
        newId.setMaxLength(64);
        addThemedButton(mobList.right() - 58, createY, 54, "作成", () -> {
            String id = newId.getValue().trim();
            if (!id.isBlank()) createDraft(id);
        });
        for (int index = 0; index < 3; index++) {
            MobEditorLayout.Bounds bounds = MobListPanel.actionBounds(mobList, index);
            if (index == 0) addThemedButton(bounds.x(), bounds.y(), bounds.width(), "複製", this::duplicate);
            if (index == 1) addThemedButton(bounds.x(), bounds.y(), bounds.width(), "再読込", this::reloadDefinitions);
            if (index == 2) addThemedButton(bounds.x(), bounds.y(), bounds.width(), "戻る", this::onClose);
        }
        int pagerY = MobListPanel.pagerY(mobList);
        MobEditorLayout.Bounds pager0 = MobListPanel.pagerBounds(mobList, pagerY, 0);
        button(pager0.x(), pager0.y(), "頁◀", () -> {
            requestMobPage(Math.max(0, mobPage - 1));
        }, pager0.width());
        MobEditorLayout.Bounds pager1 = MobListPanel.pagerBounds(mobList, pagerY, 1);
        button(pager1.x(), pager1.y(), "▲", () -> {
            mobListOffset = Math.max(0, mobListOffset - 1);
            refreshWidgets();
        }, pager1.width());
        MobEditorLayout.Bounds pager2 = MobListPanel.pagerBounds(mobList, pagerY, 2);
        button(pager2.x(), pager2.y(), "▼", () -> {
            int maximum = Math.max(0,
                    MobEditorClientState.state().mobs().size() - visibleMobCount());
            mobListOffset = Math.min(maximum, mobListOffset + 1);
            refreshWidgets();
        }, pager2.width());
        MobEditorLayout.Bounds pager3 = MobListPanel.pagerBounds(mobList, pagerY, 3);
        button(pager3.x(), pager3.y(), "頁▶", () -> {
            requestMobPage(mobPage + 1);
        }, pager3.width());

        addRenderableWidget(MobEditorTabBar.create(layout.tabs(),
                Arrays.stream(Tab.values()).map(MobEditorScreen::tabName).toList(),
                tab.ordinal(), selected -> switchTab(Tab.values()[selected])));
        if (draft != null) {
            buildingProperty = true;
            buildTab(layout.property().x(), layout.property().y() - propertyScroll);
            buildingProperty = false;
            propertyPanel.applyVisibility();
        }
        buildBottom(actionBar.x());
        buildPreviewButtons();
        buildMobButtons();
    }

    private void buildMobButtons() {
        String query = search == null ? "" : search.getValue().trim();
        List<MobEditorStatePayload.MobSummary> filtered =
                MobEditorClientState.state().mobs().stream()
                        .filter(value -> MobEditorUiLogic.matches(
                                value.id(), value.displayName(), value.tags(), query))
                        .filter(value -> MobEditorUiLogic.categoryMatches(
                                value.category().name(), categoryFilter))
                        .toList();
        int visible = visibleMobCount();
        mobListOffset = Math.min(mobListOffset,
                Math.max(0, filtered.size() - visible));
        List<MobEditorStatePayload.MobSummary> mobs = filtered.stream()
                .skip(mobListOffset).limit(visible).toList();
        int y = MobListPanel.listViewport(layout.mobList()).y();
        for (var mob : mobs) {
            MobLibraryEntry entry = new MobLibraryEntry(
                    layout.mobList().x() + 4, y, layout.mobList().width() - 8,
                    mob, draft != null && draft.id().equals(mob.id()),
                    dirty && draft != null && draft.id().equals(mob.id()),
                    () -> selectMob(mob.id()));
            libraryEntries.add(addRenderableWidget(entry));
            y += 48;
        }
    }

    private int visibleMobCount() {
        return Math.max(1, MobListPanel.listViewport(layout.mobList()).height() / 48);
    }

    private void refreshLibraryEntries() {
        for (MobLibraryEntry entry : libraryEntries) removeWidget(entry);
        libraryEntries.clear();
        if (layout != null) buildMobButtons();
    }

    private void requestMobPage(int requestedPage) {
        if (MobEditorClientState.requestMobs(searchQuery, requestedPage)) {
            mobPage = requestedPage;
            mobListOffset = 0;
        } else {
            localError = "通信中です。応答後にもう一度操作してください";
        }
    }

    private void cycleCategoryFilter() {
        List<String> values = List.of("ALL", "NORMAL", "ELITE", "BOSS");
        categoryFilter = values.get((values.indexOf(categoryFilter) + 1) % values.size());
        mobListOffset = 0;
        refreshLibraryEntries();
    }

    private void buildTab(int x, int y) {
        switch (tab) {
            case BASIC -> buildBasic(x, y);
            case STATS -> buildStats(x, y);
            case AI -> buildAi(x, y);
            case ABILITIES -> buildAbilities(x, y);
            case APPEARANCE -> buildAppearance(x, y);
            case TEST -> buildTest(x, y);
        }
    }

    private void buildAbilities(int x, int y) {
        if (!MobEditorClientState.abilityAuthoringAvailable()) {
            addRenderableWidget(new ProjectSCard(x, y, Math.min(360, layout.property().width()), 48,
                    Component.literal("Ability 編集"), AbilityAssignmentPanel.unavailableMessage(),
                    ProjectSCard.State.WARNING, null));
            return;
        }
        clampAbilityOffsets();
        List<String> assigned = abilities.assigned();
        int assignedEnd = Math.min(assigned.size(),
                assignedAbilityOffset + ABILITY_ASSIGNED_PAGE_SIZE);
        List<AbilityAssignmentPanel.AssignedRow> assignedRows = new ArrayList<>();
        for (String id : assigned.subList(assignedAbilityOffset, assignedEnd)) {
            MobEditorV2StatePayload.State authority = MobEditorClientState.authoritativeV2State();
            String label = authority.catalog().stream()
                    .filter(entry -> entry.id().equals(id)).findFirst()
                    .map(entry -> entry.displayName() + " (" + id + ")")
                    .orElse("利用不可: " + id);
            boolean stale = authority.catalog().stream().noneMatch(entry -> entry.id().equals(id));
            assignedRows.add(new AbilityAssignmentPanel.AssignedRow(id, label, stale, () -> {
                abilities.remove(id);
                dirty = true;
                clampAbilityOffsets();
                refreshWidgets();
            }, () -> {
                abilities.move(id, -1);
                dirty = true;
                refreshWidgets();
            }, () -> {
                abilities.move(id, 1);
                dirty = true;
                refreshWidgets();
            }));
        }
        List<AbilityEditorModel.CatalogItem> available = abilities.available();
        int availableEnd = Math.min(available.size(),
                availableAbilityOffset + ABILITY_AVAILABLE_PAGE_SIZE);
        List<AbilityAssignmentPanel.AvailableRow> availableRows = new ArrayList<>();
        for (AbilityEditorModel.CatalogItem entry : available.subList(
                availableAbilityOffset, availableEnd)) {
            availableRows.add(new AbilityAssignmentPanel.AvailableRow(entry.id(),
                    entry.displayName() + " (" + entry.id() + ")", () -> {
                if (abilities.add(entry.id())) {
                    dirty = true;
                    clampAbilityOffsets();
                    refreshWidgets();
                }
            }));
        }
        AbilityAssignmentPanel.build(new MobEditorLayout.Bounds(layout.property().x(), y,
                layout.property().width(), layout.property().height()), new AbilityAssignmentPanel.View(
                assignedRows, availableRows, () -> {
            assignedAbilityOffset = Math.max(0, assignedAbilityOffset - ABILITY_ASSIGNED_PAGE_SIZE);
            refreshWidgets();
        }, () -> {
            assignedAbilityOffset += ABILITY_ASSIGNED_PAGE_SIZE;
            clampAbilityOffsets();
            refreshWidgets();
        }, () -> {
            availableAbilityOffset = Math.max(0,
                    availableAbilityOffset - ABILITY_AVAILABLE_PAGE_SIZE);
            refreshWidgets();
        }, () -> {
            availableAbilityOffset += ABILITY_AVAILABLE_PAGE_SIZE;
            clampAbilityOffsets();
            refreshWidgets();
        }), this::addPropertyWidget);
    }

    private void clampAbilityOffsets() {
        assignedAbilityOffset = clampAbilityOffset(assignedAbilityOffset,
                abilities.assigned().size(), ABILITY_ASSIGNED_PAGE_SIZE);
        availableAbilityOffset = clampAbilityOffset(availableAbilityOffset,
                abilities.available().size(), ABILITY_AVAILABLE_PAGE_SIZE);
    }

    private static int clampAbilityOffset(int offset, int size, int pageSize) {
        return Math.max(0, Math.min(offset, Math.max(0, size - pageSize)));
    }

    private void buildBasic(int x, int y) {
        if (compactProperty()) {
            int w = layout.property().width();
            field("id", "内部ID（固定）", draft.id(), x, y, w).setEditable(false);
            field("display", "表示名", draft.displayName(), x, y + 44, w);
            addPropertyButton(x, y + 88, w,
                    "EntityType: " + draft.entityType(), this::openEntityPicker);
            field("level", "レベル", Integer.toString(draft.level()), x, y + 132, w);
            field("tags", "タグ（,区切り）", String.join(",", draft.tags()), x, y + 176, w);
            addPropertyButton(x, y + 220, w, "カテゴリ: " + draft.category(), this::cycleCategory);
            addPropertyButton(x, y + 244, w, "有効: " + (draft.enabled() ? "ON" : "OFF"), this::toggleEnabled);
            addPropertyButton(x, y + 268, w, "ネームプレート: " + draft.nameplate(), this::cycleNameplate);
            return;
        }
        field("id", "内部ID（固定）", draft.id(), x, y, 174).setEditable(false);
        field("display", "表示名", draft.displayName(), x + 190, y, 174);
        addPropertyButton(x, y + 58, 174,
                "EntityType: " + draft.entityType(), this::openEntityPicker);
        field("level", "レベル", Integer.toString(draft.level()), x + 190, y + 44, 174);
        field("tags", "タグ（,区切り）", String.join(",", draft.tags()), x, y + 88, 364);
        addPropertyButton(x, y + 132, 174, "カテゴリ: " + draft.category(), () -> {
                    collectCurrentTab();
                    draft = copyBasic(draft, draft.displayName(), draft.entityType(),
                            next(draft.category()), draft.enabled(), draft.level(),
                            draft.nameplate(), draft.tags());
                    dirty = true;
                    rebuild();
                });
        addPropertyButton(x + 190, y + 132, 174, "有効: " + (draft.enabled() ? "ON" : "OFF"), () -> {
                    collectCurrentTab();
                    draft = copyBasic(draft, draft.displayName(), draft.entityType(),
                            draft.category(), !draft.enabled(), draft.level(),
                            draft.nameplate(), draft.tags());
                    dirty = true;
                    rebuild();
                });
        addPropertyButton(x, y + 164, 364, "ネームプレート: " + draft.nameplate(), () -> {
                    collectCurrentTab();
                    draft = copyBasic(draft, draft.displayName(), draft.entityType(),
                            draft.category(), draft.enabled(), draft.level(),
                            next(draft.nameplate()), draft.tags());
                    dirty = true;
                    rebuild();
                });
    }

    private void buildStats(int x, int y) {
        MobEditorData.Stats s = draft.stats();
        String[] keys = {"hp", "patk", "matk", "pdef", "mdef",
                "move", "aspeed", "crit", "critdmg", "reduction"};
        String[] names = {"最大HP", "物理攻撃", "魔法攻撃", "物理防御", "魔法防御",
                "移動速度", "攻撃速度", "クリ率", "クリ倍率", "被ダメ軽減"};
        double[] values = {s.maxHealth(), s.physicalAttack(), s.magicalAttack(),
                s.physicalDefense(), s.magicalDefense(), s.movementSpeed(),
                s.attackSpeed(), s.criticalChance(), s.criticalDamage(),
                s.damageReduction()};
        for (int index = 0; index < keys.length; index++) {
            int column = compactProperty() ? 0 : index / 5;
            int row = compactProperty() ? index : index % 5;
            field(keys[index], names[index], number(values[index]),
                    x + column * 190, y + row * 38,
                    compactProperty() ? layout.property().width() : 174);
        }
        MobEditorData.BasicAttack a = draft.attack();
        int attackY = y + (compactProperty() ? 380 : 200);
        int smallWidth = compactProperty() ? layout.property().width() : 112;
        int compactStep = compactProperty() ? 38 : 0;
        field("fixed", "固定ダメージ", number(a.fixedDamage()), x, attackY, smallWidth);
        field("coef", "攻撃力係数", number(a.coefficient()), x + (compactProperty() ? 0 : 126), attackY + compactStep, smallWidth);
        field("interval", "間隔", number(a.intervalSeconds()), x + (compactProperty() ? 0 : 252), attackY + compactStep * 2, smallWidth);
        field("range", "距離", number(a.range()), x, attackY + (compactProperty() ? 114 : 38), smallWidth);
        field("knockback", "KB", number(a.knockback()), x + (compactProperty() ? 0 : 126), attackY + (compactProperty() ? 152 : 38), smallWidth);
        addPropertyButton(x + (compactProperty() ? 0 : 252), attackY + (compactProperty() ? 190 : 38), smallWidth, "種別: " + a.damageType(), () -> {
                    collectCurrentTab();
                    draft = withAttack(draft, new MobEditorData.BasicAttack(
                            next(a.damageType()), a.fixedDamage(), a.coefficient(),
                            a.intervalSeconds(), a.range(), a.knockback(),
                            a.criticalAllowed()));
                    dirty = true;
                    rebuild();
                });
        addPropertyButton(x + (compactProperty() ? 0 : 252), attackY + (compactProperty() ? 214 : 70), smallWidth, "クリティカル:"
                + (a.criticalAllowed() ? "ON" : "OFF"), () -> {
                    collectCurrentTab();
                    MobEditorData.BasicAttack current = draft.attack();
                    draft = withAttack(draft, new MobEditorData.BasicAttack(
                            current.damageType(), current.fixedDamage(), current.coefficient(),
                            current.intervalSeconds(), current.range(), current.knockback(),
                            !current.criticalAllowed()));
                    dirty = true;
                    rebuild();
                });
    }

    private void buildAi(int x, int y) {
        MobEditorData.Ai ai = draft.ai();
        int w = compactProperty() ? layout.property().width() : 174;
        int col = compactProperty() ? 0 : 190;
        field("aggro", "索敵距離", number(ai.aggroRange()), x, y, w);
        field("chase", "追跡距離", number(ai.chaseRange()), x + col, y + (compactProperty() ? 44 : 0), w);
        field("leash", "帰還距離", number(ai.leashRange()), x, y + (compactProperty() ? 88 : 44), w);
        field("airange", "攻撃距離", number(ai.attackRange()), x + col, y + (compactProperty() ? 132 : 44), w);
        field("refresh", "再検索秒", number(ai.refreshSeconds()), x, y + (compactProperty() ? 176 : 88), w);
        addPropertyButton(x + col, y + (compactProperty() ? 220 : 88), w, "プリセット: " + ai.preset(), () -> {
                    collectCurrentTab();
                    draft = withAi(draft, new MobEditorData.Ai(
                            next(ai.preset()), ai.priority(), ai.aggroRange(),
                            ai.chaseRange(), ai.leashRange(), ai.attackRange(),
                            ai.refreshSeconds(), ai.returnHome(), ai.resetHealth(),
                            ai.avoidFalls(), ai.avoidWater()));
                    dirty = true;
                    rebuild();
                });
        addPropertyButton(x, y + (compactProperty() ? 244 : 164), w, "優先: " + ai.priority(), () -> {
                    collectCurrentTab();
                    MobEditorData.Ai current = draft.ai();
                    draft = withAi(draft, new MobEditorData.Ai(
                            current.preset(), next(current.priority()), current.aggroRange(),
                            current.chaseRange(), current.leashRange(), current.attackRange(),
                            current.refreshSeconds(), current.returnHome(), current.resetHealth(),
                            current.avoidFalls(), current.avoidWater()));
                    dirty = true;
                    rebuild();
                });
        int toggleY = y + (compactProperty() ? 272 : 132);
        toggleAi(x, toggleY, "帰還", ai.returnHome(), 0);
        toggleAi(x + (compactProperty() ? 0 : 94), toggleY + (compactProperty() ? 24 : 0), "帰還回復", ai.resetHealth(), 1);
        toggleAi(x + (compactProperty() ? 0 : 188), toggleY + (compactProperty() ? 48 : 0), "落下回避", ai.avoidFalls(), 2);
        toggleAi(x + (compactProperty() ? 0 : 282), toggleY + (compactProperty() ? 72 : 0), "水回避", ai.avoidWater(), 3);
    }

    private void toggleAi(int x, int y, String name, boolean value, int flag) {
        addPropertyButton(x, y, compactProperty() ? layout.property().width() : 86, name + ":" + (value ? "ON" : "OFF"), () -> {
                    collectCurrentTab();
                    MobEditorData.Ai ai = draft.ai();
                    draft = withAi(draft, new MobEditorData.Ai(
                            ai.preset(), ai.priority(), ai.aggroRange(), ai.chaseRange(),
                            ai.leashRange(), ai.attackRange(), ai.refreshSeconds(),
                            flag == 0 ? !ai.returnHome() : ai.returnHome(),
                            flag == 1 ? !ai.resetHealth() : ai.resetHealth(),
                            flag == 2 ? !ai.avoidFalls() : ai.avoidFalls(),
                            flag == 3 ? !ai.avoidWater() : ai.avoidWater()));
                    dirty = true;
                    rebuild();
                });
    }

    private void buildAppearance(int x, int y) {
        if (compactProperty()) {
            buildAppearanceCompact();
            return;
        }
        MobEditorData.Appearance appearance = draft.appearance();
        field("scale", "スケール", number(appearance.scale()), x, y, 86);
        ProjectSButton ageButton = addPropertyButton(x + 92, y + 16, 86,
                "年齢: " + appearance.age(), () -> {
                    collectCurrentTab();
                    draft = withAppearance(draft, new MobEditorData.Appearance(
                            appearance.scale(), next(appearance.age()),
                            appearance.glowing(), appearance.glowingColor(),
                            appearance.variants(), appearance.equipment()));
                    dirty = true;
                    rebuild();
                });
        ageButton.active = supportsBaby(draft.entityType());
        addPropertyButton(x + 184, y + 16, 86,
                "発光:" + (appearance.glowing() ? "ON" : "OFF"), () -> {
                    collectCurrentTab();
                    draft = withAppearance(draft, new MobEditorData.Appearance(
                            appearance.scale(), appearance.age(),
                            !appearance.glowing(), appearance.glowingColor(),
                            appearance.variants(), appearance.equipment()));
                    dirty = true;
                    rebuild();
                });
        addPropertyButton(x + 276, y + 16, 88, appearance.glowingColor(),
                () -> openGlowPicker(this::updateAppearanceGlowColor));
        List<String> variantKeys = variantKeys(draft.entityType());
        for (int index = 0; index < variantKeys.size(); index++) {
            String key = variantKeys.get(index);
            variantControl(key,
                    appearance.variants().getOrDefault(key,
                            defaultVariant(draft.entityType(), key)), x + index * 126,
                    y + 44, 112);
        }
        boolean equipmentSupported = supportsEquipment(draft.entityType());
        int slotIndex = 0;
        for (MobEditorData.Slot slot : MobEditorData.Slot.values()) {
            MobEditorData.Equipment entry = appearance.equipment().get(slot);
            equipmentSources.put(slot, entry.source());
            int cardX = x + (slotIndex % 2) * 184;
            int cardY = y + 88 + (slotIndex / 2) * 54;
            MobEquipmentSlotCard card = new MobEquipmentSlotCard(
                    cardX, cardY, 178, slot, entry, slotIcon(slot),
                    slot == selectedEquipmentSlot, equipmentSupported, () -> {
                        collectCurrentTab(false);
                        selectedEquipmentSlot = slot;
                        refreshWidgets();
                    });
            addPropertyWidget(card);
            slotIndex++;
        }
        MobEditorData.Equipment selected = appearance.equipment().get(selectedEquipmentSlot);
        ProjectSButton source = addPropertyButton(x, y + 252, 150,
                shortSlot(selectedEquipmentSlot) + " / " + selected.source(),
                this::cycleSelectedEquipmentSource);
        source.active = equipmentSupported;
        String selectedValue = selected.source() == MobEditorData.EquipmentSource.VANILLA_ITEM
                ? selected.material() : selected.referenceId();
        EditBox selectedItem = field("eq_" + selectedEquipmentSlot.name(),
                "選択アイテム", selectedValue, x + 156, y + 252, 132);
        selectedItem.setEditable(false);
        ProjectSButton itemPicker = addPropertyButton(x + 294, y + 252, 70,
                "選択", this::openEquipmentPicker);
        itemPicker.active = equipmentSupported
                && selected.source() != MobEditorData.EquipmentSource.NONE;
        ProjectSButton glintButton = addPropertyButton(x, y + 286, 116,
                "Glint:" + (selected.glint() ? "ON" : "OFF"),
                () -> toggleSelectedEquipment(true));
        ProjectSButton visibleButton = addPropertyButton(x + 122, y + 286, 116,
                "表示:" + (selected.visible() ? "ON" : "OFF"),
                () -> toggleSelectedEquipment(false));
        glintButton.active = equipmentSupported;
        visibleButton.active = equipmentSupported;
        field("eq_color", "革防具色 #RRGGBB", selected.color(),
                x + 244, y + 286, 74).setEditable(false);
        ProjectSButton colorPicker = addPropertyButton(x + 322, y + 286, 42,
                "色", this::openLeatherColorPicker);
        colorPicker.active = equipmentSupported && isLeatherArmor(selectedValue);
        ProjectSButton clear = addPropertyButton(x + 322, y + 320, 42,
                "削除", this::clearSelectedEquipment);
        clear.active = equipmentSupported;

        EditBox headSearch = field("head_search", "ヘッド検索/タグ", headQuery,
                x, y + 354, 174);
        headSearch.setMaxLength(64);
        headSearch.setResponder(value -> headQuery = value);
        button(x + 180, y + 368, "検索", () -> requestHeadPage(0), 54);
        button(x + 240, y + 368, "前", () ->
                requestHeadPage(Math.max(0, headPage - 1)), 54);
        button(x + 300, y + 368, "次", () -> requestHeadPage(headPage + 1), 54);
        addPropertyButton(x, y + 400, 110, "ヘッド登録", () ->
                minecraft.setScreen(new HeadImportScreen(this)));
        ProjectSButton favoriteButton = addPropertyButton(x + 116, y + 400, 128,
                "お気に入り切替", () -> MobEditorClientState.updateHeadFavorite(
                        MobEditorClientState.state().headDetail()));
        favoriteButton.active = MobEditorClientState.state().headDetail() != null;
        addHeadTabButtons(x, y + 432, 364);
        int headIndex = 0;
        String selectedHeadId = appearance.equipment().get(MobEditorData.Slot.HEAD)
                .referenceId();
        for (var head : sortedHeads()) {
            MobHeadCard card = new MobHeadCard(x + (headIndex % 2) * 184,
                    y + 462 + (headIndex / 2) * 46, 178, head,
                    head.id().equals(selectedHeadId), () -> {
                        setHeadReference(head.id());
                        MobEditorClientState.requestHead(head.id(), headQuery, headPage);
                    });
            addPropertyWidget(card);
            headIndex++;
        }
        if (headBrowserTab.equals("外部カタログ")) {
            addExternalCatalogDisabledCard(x, y + 462, 364);
        }
    }

    private void requestHeadPage(int requestedPage) {
        if (MobEditorClientState.requestHeads(headQuery, requestedPage)) {
            headPage = requestedPage;
        } else {
            localError = "通信中です。応答後にもう一度操作してください";
        }
    }

    private void openEntityPicker() {
        List<MobStringPickerScreen.Entry> entries = LIVING_ENTITY_TYPES.stream()
                .map(id -> new MobStringPickerScreen.Entry(id, entityLabel(id),
                        entityCategory(id), MobEditorUiLogic.entityIcon(id, "NORMAL")))
                .toList();
        minecraft.setScreen(new MobStringPickerScreen(this, "EntityTypeを選択",
                entries, draft.entityType(), this::entityTypeChanged));
    }

    private void entityTypeChanged(String value) {
        collectCurrentTab(false);
        if (draft == null || value.equalsIgnoreCase(draft.entityType())) return;
        MobEditorData.Mob changed = copyBasic(draft, draft.displayName(),
                value.toUpperCase(Locale.ROOT), draft.category(), draft.enabled(),
                draft.level(), draft.nameplate(), draft.tags());
        MobEditorData.Mob normalized = normalizeAppearance(changed, value);
        Runnable apply = () -> {
            draft = normalized;
            dirty = true;
            refreshWidgets();
        };
        if (!changed.appearance().equals(normalized.appearance())) {
            confirm("EntityType変更を適用しますか？",
                    "互換性のない年齢・Variant・装備は安全な初期値へ変更されます。",
                    "変更", apply);
        } else {
            apply.run();
        }
    }

    private void openGlowPicker(java.util.function.Consumer<String> consumer) {
        List<MobStringPickerScreen.Entry> entries = GLOW_COLORS.stream()
                .map(color -> new MobStringPickerScreen.Entry(color, glowLabel(color),
                        "Minecraftカラー", ProjectSIcon.COLOR))
                .toList();
        minecraft.setScreen(new MobStringPickerScreen(this, "発光色を選択", entries,
                draft.appearance().glowingColor(), consumer));
    }

    private void updateAppearanceGlowColor(String value) {
        collectCurrentTab(false);
        MobEditorData.Appearance current = draft.appearance();
        draft = withAppearance(draft, new MobEditorData.Appearance(
                current.scale(), current.age(), current.glowing(), value,
                current.variants(), current.equipment()));
        dirty = true;
        refreshWidgets();
    }

    private void variantControl(String key, String current, int x, int y, int width) {
        List<String> options = MobEditorVariantLogic.options(draft.entityType(), key);
        if (key.equals("size") || options.isEmpty()) {
            field("variant_" + key, variantLabel(key), current, x, y, width);
            return;
        }
        List<String> values = options.contains(current)
                ? options : java.util.stream.Stream.concat(
                        java.util.stream.Stream.of(current), options.stream()).toList();
        addPropertyButton(x, y + 14, width,
                variantLabel(key) + ": " + variantValueLabel(key, current),
                () -> openVariantPicker(key, current, values));
    }

    private void openVariantPicker(String key, String current, List<String> values) {
        boolean dyePalette = key.equals("collar-color")
                || key.equals("color") && draft.entityType().equalsIgnoreCase("SHEEP");
        List<MobStringPickerScreen.Entry> entries = values.stream()
                .map(value -> new MobStringPickerScreen.Entry(value,
                        variantValueLabel(key, value),
                        dyePalette ? "Minecraftカラー" : variantLabel(key),
                        key.equals("profession") || key.equals("villager-type")
                                ? ProjectSIcon.VILLAGER : ProjectSIcon.FILTER))
                .toList();
        minecraft.setScreen(new MobStringPickerScreen(this,
                variantLabel(key) + "を選択", entries, current,
                value -> updateVariant(key, value)));
    }

    private void updateVariant(String key, String value) {
        collectCurrentTab(false);
        Map<String, String> variants = new HashMap<>(draft.appearance().variants());
        variants.put(key, value);
        MobEditorData.Appearance current = draft.appearance();
        draft = withAppearance(draft, new MobEditorData.Appearance(
                current.scale(), current.age(), current.glowing(), current.glowingColor(),
                variants, current.equipment()));
        dirty = true;
        refreshWidgets();
    }

    private void cycleSelectedEquipmentSource() {
        collectCurrentTab(false);
        MobEditorData.Equipment current = draft.appearance().equipment()
                .get(selectedEquipmentSlot);
        MobEditorData.EquipmentSource source = next(current.source());
        equipmentSources.put(selectedEquipmentSlot, source);
        EnumMap<MobEditorData.Slot, MobEditorData.Equipment> equipment =
                new EnumMap<>(draft.appearance().equipment());
        equipment.put(selectedEquipmentSlot, source == MobEditorData.EquipmentSource.NONE
                ? MobEditorData.Equipment.empty() : new MobEditorData.Equipment(
                        source, current.referenceId(), current.material(),
                        source == MobEditorData.EquipmentSource.VANILLA_ITEM
                                ? current.color() : "", current.glint(),
                        current.visible(), true));
        draft = withAppearance(draft, new MobEditorData.Appearance(
                draft.appearance().scale(), draft.appearance().age(),
                draft.appearance().glowing(), draft.appearance().glowingColor(),
                draft.appearance().variants(), equipment));
        dirty = true;
        refreshWidgets();
    }

    private void openEquipmentPicker() {
        MobEditorData.Equipment equipment = draft.appearance().equipment()
                .get(selectedEquipmentSlot);
        if (equipment.source() == MobEditorData.EquipmentSource.CUSTOM_HEAD) {
            headBrowserTab = "ローカル";
            localError = "下のHead BrowserからローカルHeadを選択してください";
            refreshWidgets();
            return;
        }
        List<MobStringPickerScreen.Entry> entries;
        if (equipment.source() == MobEditorData.EquipmentSource.PROJECTS_ITEM) {
            entries = List.of(
                    new MobStringPickerScreen.Entry("starter_sword", "スターターソード",
                            "武器", ProjectSIcon.SWORD, true, "",
                            MobItemStackCache.get("IRON_SWORD")),
                    new MobStringPickerScreen.Entry("painter_staff", "ペインターの杖",
                            "武器", ProjectSIcon.MAGIC, true, "",
                            MobItemStackCache.get("BLAZE_ROD")),
                    new MobStringPickerScreen.Entry("starter_bow", "スターターボウ",
                            "武器", ProjectSIcon.BOW, true, "",
                            MobItemStackCache.get("BOW")));
        } else {
            entries = vanillaMaterials().stream().map(material -> {
                boolean fits = MobEditorPickerLogic.materialFits(
                        material, selectedEquipmentSlot.name());
                return new MobStringPickerScreen.Entry(material,
                        material.replace('_', ' '),
                        MobEditorPickerLogic.materialCategory(material),
                        slotIcon(selectedEquipmentSlot), fits,
                        fits ? "" : shortSlot(selectedEquipmentSlot)
                                + "へ装備できないアイテムです",
                        MobItemStackCache.get(material));
            }).toList();
        }
        String selected = equipment.source() == MobEditorData.EquipmentSource.VANILLA_ITEM
                ? equipment.material() : equipment.referenceId();
        minecraft.setScreen(new MobStringPickerScreen(this,
                equipment.source() == MobEditorData.EquipmentSource.PROJECTS_ITEM
                        ? "ProjectS Itemを選択" : "Vanilla Itemを選択",
                entries, selected, this::setSelectedEquipmentItem));
    }

    private void setSelectedEquipmentItem(String value) {
        collectCurrentTab(false);
        MobEditorData.Equipment current = draft.appearance().equipment()
                .get(selectedEquipmentSlot);
        EnumMap<MobEditorData.Slot, MobEditorData.Equipment> equipment =
                new EnumMap<>(draft.appearance().equipment());
        boolean vanilla = current.source() == MobEditorData.EquipmentSource.VANILLA_ITEM;
        equipment.put(selectedEquipmentSlot, new MobEditorData.Equipment(
                current.source(), vanilla ? "" : value, vanilla ? value : "",
                isLeatherArmor(value) ? current.color() : "", current.glint(),
                current.visible(), true));
        MobEditorData.Appearance appearance = draft.appearance();
        draft = withAppearance(draft, new MobEditorData.Appearance(
                appearance.scale(), appearance.age(), appearance.glowing(),
                appearance.glowingColor(), appearance.variants(), equipment));
        dirty = true;
        refreshWidgets();
    }

    private void openLeatherColorPicker() {
        MobEditorData.Equipment selected = draft.appearance().equipment()
                .get(selectedEquipmentSlot);
        String item = selected.source() == MobEditorData.EquipmentSource.VANILLA_ITEM
                ? selected.material() : selected.referenceId();
        if (!isLeatherArmor(item)) {
            localError = "革防具を選択したときだけ色を変更できます";
            return;
        }
        String initial = selected.color().matches("#[0-9a-fA-F]{6}")
                ? selected.color() : "#A06540";
        MobEditorData.Mob before = draft;
        boolean dirtyBefore = dirty;
        minecraft.setScreen(new LeatherColorPickerScreen(this, initial,
                color -> updateSelectedEquipment(color, selected.glint(), selected.visible()),
                () -> {
                    draft = before;
                    dirty = dirtyBefore;
                    preview.update(draft, MobEditorClientState.state().headDetail());
                }));
    }

    private void clearSelectedEquipment() {
        collectCurrentTab(false);
        EnumMap<MobEditorData.Slot, MobEditorData.Equipment> equipment =
                new EnumMap<>(draft.appearance().equipment());
        equipment.put(selectedEquipmentSlot, MobEditorData.Equipment.empty());
        equipmentSources.put(selectedEquipmentSlot, MobEditorData.EquipmentSource.NONE);
        MobEditorData.Appearance current = draft.appearance();
        draft = withAppearance(draft, new MobEditorData.Appearance(
                current.scale(), current.age(), current.glowing(), current.glowingColor(),
                current.variants(), equipment));
        dirty = true;
        refreshWidgets();
    }

    private static List<String> vanillaMaterials() {
        return List.of("LEATHER_HELMET", "LEATHER_CHESTPLATE", "LEATHER_LEGGINGS",
                "LEATHER_BOOTS", "CHAINMAIL_HELMET", "CHAINMAIL_CHESTPLATE",
                "CHAINMAIL_LEGGINGS", "CHAINMAIL_BOOTS", "IRON_HELMET",
                "IRON_CHESTPLATE", "IRON_LEGGINGS", "IRON_BOOTS", "GOLDEN_HELMET",
                "GOLDEN_CHESTPLATE", "GOLDEN_LEGGINGS", "GOLDEN_BOOTS",
                "DIAMOND_HELMET", "DIAMOND_CHESTPLATE", "DIAMOND_LEGGINGS",
                "DIAMOND_BOOTS", "NETHERITE_HELMET", "NETHERITE_CHESTPLATE",
                "NETHERITE_LEGGINGS", "NETHERITE_BOOTS", "ELYTRA", "SHIELD",
                "WOODEN_SWORD", "STONE_SWORD", "IRON_SWORD", "GOLDEN_SWORD",
                "DIAMOND_SWORD", "NETHERITE_SWORD", "BOW", "CROSSBOW", "TRIDENT",
                "IRON_AXE", "DIAMOND_AXE", "NETHERITE_AXE", "CARVED_PUMPKIN",
                "PLAYER_HEAD", "SKELETON_SKULL", "WITHER_SKELETON_SKULL");
    }

    private static boolean isLeatherArmor(String value) {
        if (value == null) return false;
        String upper = value.toUpperCase(Locale.ROOT);
        return upper.startsWith("LEATHER_") && (upper.endsWith("HELMET")
                || upper.endsWith("CHESTPLATE") || upper.endsWith("LEGGINGS")
                || upper.endsWith("BOOTS"));
    }

    private List<MobEditorStatePayload.HeadSummary> sortedHeads() {
        return MobEditorClientState.state().heads().stream()
                .filter(value -> !headBrowserTab.equals("お気に入り") || value.favorite())
                .filter(value -> !headBrowserTab.equals("最近使用")
                        || recentHeads.contains(value.id()))
                .filter(value -> !headBrowserTab.equals("外部カタログ"))
                .filter(value -> MobEditorUiLogic.headMatches(value.id(), value.displayName(),
                        value.tags(), value.favorite(), headQuery,
                        headBrowserTab.equals("お気に入り")))
                .sorted(java.util.Comparator.comparing(
                        (MobEditorStatePayload.HeadSummary value) ->
                                recentHeads.contains(value.id())).reversed())
                .limit(4).toList();
    }

    private void addExternalCatalogDisabledCard(int x, int y, int width) {
        ProjectSButton status = addPropertyButton(x, y, width,
                "外部カタログは未設定です", () -> { });
        status.active = false;
        status.setTooltip(Tooltip.create(Component.literal(
                MobEditorPickerLogic.externalCatalogDisabledReason())));
    }

    private static ProjectSIcon slotIcon(MobEditorData.Slot slot) {
        return switch (slot) {
            case HEAD -> ProjectSIcon.HEAD;
            case CHEST, LEGS, FEET -> ProjectSIcon.ARMOR;
            case MAIN_HAND -> ProjectSIcon.SWORD;
            case OFF_HAND -> ProjectSIcon.SHIELD;
        };
    }

    private void addHeadTabButtons(int x, int y, int width) {
        List<String> tabs = List.of("ローカル", "外部", "お気に入り", "最近");
        List<String> values = List.of("ローカル", "外部カタログ", "お気に入り", "最近使用");
        int gap = 4;
        int buttonWidth = Math.max(42, (width - gap * 3) / 4);
        for (int index = 0; index < tabs.size(); index++) {
            String value = values.get(index);
            ProjectSButton button = addPropertyButton(x + index * (buttonWidth + gap), y,
                    buttonWidth, tabs.get(index), () -> {
                        headBrowserTab = value;
                        localError = "";
                        refreshWidgets();
                    });
            button.selected(value.equals(headBrowserTab));
        }
    }

    private static String entityCategory(String id) {
        return switch (id) {
            case "WOLF", "CAT", "HORSE", "SHEEP", "COW", "PIG", "CHICKEN",
                    "RABBIT", "FOX", "GOAT", "CAMEL", "ARMADILLO" -> "動物";
            case "VILLAGER", "WANDERING_TRADER", "IRON_GOLEM", "SNOW_GOLEM",
                    "ALLAY" -> "友好";
            case "WITHER", "ENDER_DRAGON", "WARDEN" -> "ボス";
            default -> "敵対";
        };
    }

    private static String entityLabel(String id) {
        return switch (id) {
            case "ZOMBIE" -> "ゾンビ";
            case "SKELETON" -> "スケルトン";
            case "CREEPER" -> "クリーパー";
            case "VILLAGER" -> "村人";
            case "WOLF" -> "オオカミ";
            case "CAT" -> "ネコ";
            case "HORSE" -> "ウマ";
            case "SHEEP" -> "ヒツジ";
            default -> id.replace('_', ' ');
        };
    }

    private static String glowLabel(String color) {
        return switch (color) {
            case "WHITE" -> "白";
            case "YELLOW" -> "黄";
            case "GOLD" -> "金";
            case "RED" -> "赤";
            case "BLUE" -> "青";
            case "AQUA" -> "水色";
            case "GREEN" -> "緑";
            case "BLACK" -> "黒";
            default -> color;
        };
    }

    private static String variantLabel(String key) {
        return switch (key) {
            case "size" -> "サイズ";
            case "color" -> "色";
            case "sheared" -> "毛刈り済み";
            case "variant" -> "種類";
            case "collar-color" -> "首輪の色";
            case "angry" -> "怒り状態";
            case "profession" -> "職業";
            case "villager-type" -> "村人タイプ";
            default -> key;
        };
    }

    private static String variantValueLabel(String key, String value) {
        if (value.equals("true")) return "オン";
        if (value.equals("false")) return "オフ";
        return value.replace('_', ' ');
    }

    private void buildTest(int x, int y) {
        if (compactProperty()) {
            String[] actions = {"足元へ召喚", "カーソルへ召喚", "テスト削除", "AI停止", "AI再開", "無敵", "無敵解除", "HP 25%", "HP 50%", "HP 100%", "外見再適用", "全員分削除"};
            for (int index = 0; index < actions.length; index++) {
                int code = index;
                MobEditorLayout.Bounds bounds = MobPropertyPanel.compactColumn(
                        new MobEditorLayout.Bounds(x, y, layout.property().width(), layout.property().height()),
                        index / 2, index % 2, 2);
                Runnable action = switch (index) {
                    case 0 -> () -> testSpawn(false);
                    case 1 -> () -> testSpawn(true);
                    case 2 -> MobEditorClientState::despawnTests;
                    case 11 -> MobEditorClientState::despawnAllTests;
                    default -> () -> MobEditorClientState.controlTests(code - 3);
                };
                addPropertyButton(bounds.x(), bounds.y(), bounds.width(), actions[index], action);
            }
            return;
        }
        button(x, y, "足元へ召喚", () -> testSpawn(false));
        button(x + 126, y, "カーソルへ召喚", () -> testSpawn(true));
        button(x + 252, y, "テスト削除", MobEditorClientState::despawnTests);
        String[] names = {"AI停止", "AI再開", "無敵", "無敵解除",
                "HP 25%", "HP 50%", "HP 100%", "外見再適用"};
        for (int index = 0; index < names.length; index++) {
            int code = index;
            button(x + (index % 3) * 126, y + 40 + (index / 3) * 34,
                    names[index], () -> MobEditorClientState.controlTests(code));
        }
        button(x + 252, y + 142, "全員分削除",
                MobEditorClientState::despawnAllTests);
    }

    private void buildBottom(int x) {
        MobEditorLayout.Bounds bounds = layout.actionBar();
        ProjectSButton undo = MobEditorActionBar.action(bounds, 0, 4,
                "元に戻す", ProjectSButton.Kind.GHOST, this::undo);
        ProjectSButton validate = MobEditorActionBar.action(bounds, 1, 4,
                "検証", ProjectSButton.Kind.SECONDARY, this::validateDraft);
        ProjectSButton save = MobEditorActionBar.action(bounds, 2, 4,
                "保存", ProjectSButton.Kind.PRIMARY, this::saveDraft);
        ProjectSButton apply = MobEditorActionBar.action(bounds, 3, 4,
                "適用", ProjectSButton.Kind.PRIMARY, this::applyDefinition);
        undo.active = original != null;
        validate.active = draft != null && !MobEditorClientState.communicating();
        validate.loading(MobEditorClientState.communicating());
        save.active = draft != null && !MobEditorClientState.communicating()
                && !MobEditorClientState.state().revisionConflict();
        save.loading(MobEditorClientState.communicating());
        apply.active = draft != null && !dirty && !MobEditorClientState.communicating()
                && !MobEditorClientState.state().revisionConflict();
        apply.loading(MobEditorClientState.communicating());
        addRenderableWidget(undo);
        addRenderableWidget(validate);
        addRenderableWidget(save);
        addRenderableWidget(apply);
    }

    private void buildAppearanceCompact() {
        MobEditorLayout.Bounds panel = new MobEditorLayout.Bounds(layout.property().x(),
                layout.property().y() - propertyScroll, layout.property().width(), layout.property().height());
        MobEditorData.Appearance appearance = draft.appearance();
        MobEditorLayout.Bounds scale = MobPropertyPanel.compactRow(panel, 0);
        field("scale", "スケール", number(appearance.scale()), scale.x(), scale.y(), scale.width());
        MobEditorLayout.Bounds age = MobPropertyPanel.compactColumn(panel, 1, 0, 3);
        MobEditorLayout.Bounds glow = MobPropertyPanel.compactColumn(panel, 1, 1, 3);
        MobEditorLayout.Bounds color = MobPropertyPanel.compactColumn(panel, 1, 2, 3);
        ProjectSButton ageButton = addPropertyButton(age.x(), age.y(), age.width(), "年齢: " + appearance.age(), () -> {
            collectCurrentTab();
            draft = withAppearance(draft, new MobEditorData.Appearance(appearance.scale(), next(appearance.age()),
                    appearance.glowing(), appearance.glowingColor(), appearance.variants(), appearance.equipment()));
            dirty = true; rebuild();
        });
        ageButton.active = supportsBaby(draft.entityType());
        addPropertyButton(glow.x(), glow.y(), glow.width(), "発光:" + (appearance.glowing() ? "ON" : "OFF"), () -> {
            collectCurrentTab();
            draft = withAppearance(draft, new MobEditorData.Appearance(appearance.scale(), appearance.age(),
                    !appearance.glowing(), appearance.glowingColor(), appearance.variants(), appearance.equipment()));
            dirty = true; rebuild();
        });
        addPropertyButton(color.x(), color.y(), color.width(), appearance.glowingColor(),
                () -> openGlowPicker(this::updateAppearanceGlowColor));
        int row = 2;
        for (String key : variantKeys(draft.entityType())) {
            MobEditorLayout.Bounds bounds = MobPropertyPanel.compactRow(panel, row++);
            variantControl(key, appearance.variants().getOrDefault(key,
                    defaultVariant(draft.entityType(), key)), bounds.x(), bounds.y(), bounds.width());
        }
        boolean equipmentSupported = supportsEquipment(draft.entityType());
        for (MobEditorData.Slot slot : MobEditorData.Slot.values()) {
            MobEditorData.Equipment entry = appearance.equipment().get(slot);
            equipmentSources.put(slot, entry.source());
            MobEditorLayout.Bounds bounds = MobPropertyPanel.compactRow(panel, row++);
            ProjectSButton slotButton = addPropertyButton(bounds.x(), bounds.y(), bounds.width(),
                    (slot == selectedEquipmentSlot ? "▶" : "") + shortSlot(slot) + ": "
                            + entry.source(), () -> {
                        collectCurrentTab(false);
                        selectedEquipmentSlot = slot;
                        refreshWidgets();
                    });
            slotButton.active = equipmentSupported;
        }
        MobEditorData.Equipment selected = appearance.equipment().get(selectedEquipmentSlot);
        MobEditorLayout.Bounds sourceBounds = MobPropertyPanel.compactRow(panel, row++);
        ProjectSButton source = addPropertyButton(sourceBounds.x(), sourceBounds.y(),
                sourceBounds.width(), shortSlot(selectedEquipmentSlot) + " / " + selected.source(),
                this::cycleSelectedEquipmentSource);
        source.active = equipmentSupported;
        MobEditorLayout.Bounds itemBounds = MobPropertyPanel.compactRow(panel, row++);
        String selectedValue = selected.source() == MobEditorData.EquipmentSource.VANILLA_ITEM
                ? selected.material() : selected.referenceId();
        EditBox itemField = field("eq_" + selectedEquipmentSlot.name(), "選択アイテム",
                selectedValue, itemBounds.x(), itemBounds.y(), itemBounds.width());
        itemField.setEditable(false);
        MobEditorLayout.Bounds chooseBounds = MobPropertyPanel.compactRow(panel, row++);
        ProjectSButton choose = addPropertyButton(chooseBounds.x(), chooseBounds.y(),
                chooseBounds.width(), "アイテムを選択", this::openEquipmentPicker);
        choose.active = equipmentSupported
                && selected.source() != MobEditorData.EquipmentSource.NONE;
        MobEditorLayout.Bounds glintBounds = MobPropertyPanel.compactColumn(panel, row, 0, 2);
        MobEditorLayout.Bounds visibleBounds = MobPropertyPanel.compactColumn(panel, row++, 1, 2);
        ProjectSButton glint = addPropertyButton(glintBounds.x(), glintBounds.y(),
                glintBounds.width(), "Glint:" + (selected.glint() ? "ON" : "OFF"),
                () -> toggleSelectedEquipment(true));
        ProjectSButton visible = addPropertyButton(visibleBounds.x(), visibleBounds.y(),
                visibleBounds.width(), "表示:" + (selected.visible() ? "ON" : "OFF"),
                () -> toggleSelectedEquipment(false));
        glint.active = equipmentSupported;
        visible.active = equipmentSupported;
        MobEditorLayout.Bounds equipmentColor = MobPropertyPanel.compactRow(panel, row++);
        field("eq_color", "革防具色 #RRGGBB", selected.color(), equipmentColor.x(),
                equipmentColor.y(), equipmentColor.width()).setEditable(false);
        MobEditorLayout.Bounds colorBounds = MobPropertyPanel.compactRow(panel, row++);
        ProjectSButton colorPicker = addPropertyButton(colorBounds.x(), colorBounds.y(),
                colorBounds.width(), "色を選択", this::openLeatherColorPicker);
        colorPicker.active = equipmentSupported && isLeatherArmor(selectedValue);
        MobEditorLayout.Bounds clearBounds = MobPropertyPanel.compactRow(panel, row++);
        ProjectSButton clear = addPropertyButton(clearBounds.x(), clearBounds.y(),
                clearBounds.width(), "選択中スロットをクリア", this::clearSelectedEquipment);
        clear.active = equipmentSupported;
        MobEditorLayout.Bounds searchBounds = MobPropertyPanel.compactRow(panel, ++row);
        EditBox headSearch = field("head_search", "ヘッド検索/タグ", headQuery, searchBounds.x(), searchBounds.y(), searchBounds.width());
        headSearch.setMaxLength(64);
        headSearch.setResponder(value -> headQuery = value);
        row++;
        for (int column = 0; column < 3; column++) {
            MobEditorLayout.Bounds bounds = MobPropertyPanel.compactColumn(panel, row, column, 3);
            if (column == 0) addPropertyButton(bounds.x(), bounds.y(), bounds.width(), "検索", () -> requestHeadPage(0));
            if (column == 1) addPropertyButton(bounds.x(), bounds.y(), bounds.width(), "前", () -> requestHeadPage(Math.max(0, headPage - 1)));
            if (column == 2) addPropertyButton(bounds.x(), bounds.y(), bounds.width(), "次", () -> requestHeadPage(headPage + 1));
        }
        MobEditorLayout.Bounds importBounds = MobPropertyPanel.compactColumn(panel, ++row, 0, 2);
        MobEditorLayout.Bounds favoriteBounds = MobPropertyPanel.compactColumn(panel, row, 1, 2);
        addPropertyButton(importBounds.x(), importBounds.y(), importBounds.width(), "ヘッド登録", () -> minecraft.setScreen(new HeadImportScreen(this)));
        ProjectSButton favorite = addPropertyButton(favoriteBounds.x(), favoriteBounds.y(), favoriteBounds.width(), "お気に入り切替", () -> MobEditorClientState.updateHeadFavorite(MobEditorClientState.state().headDetail()));
        favorite.active = MobEditorClientState.state().headDetail() != null;
        addHeadTabButtons(panel.x(), panel.y() + (++row) * 38, panel.width());
        int headRow = ++row;
        int column = 0;
        String selectedHeadId = appearance.equipment().get(MobEditorData.Slot.HEAD)
                .referenceId();
        for (var head : sortedHeads()) {
            int cardRow = headRow + column++;
            int cardY = panel.y() + cardRow * 46;
            MobHeadCard card = new MobHeadCard(panel.x(), cardY, panel.width(), head,
                    head.id().equals(selectedHeadId), () -> {
                        setHeadReference(head.id());
                        MobEditorClientState.requestHead(head.id(), headQuery, headPage);
                    });
            addPropertyWidget(card);
        }
        if (headBrowserTab.equals("外部カタログ")) {
            addExternalCatalogDisabledCard(panel.x(), panel.y() + (headRow + column) * 46,
                    panel.width());
        }
    }

    private boolean compactProperty() {
        return layout.property().width() < 380;
    }

    private void cycleCategory() {
        collectCurrentTab();
        draft = copyBasic(draft, draft.displayName(), draft.entityType(), next(draft.category()),
                draft.enabled(), draft.level(), draft.nameplate(), draft.tags());
        dirty = true;
        rebuild();
    }

    private void toggleEnabled() {
        collectCurrentTab();
        draft = copyBasic(draft, draft.displayName(), draft.entityType(), draft.category(),
                !draft.enabled(), draft.level(), draft.nameplate(), draft.tags());
        dirty = true;
        rebuild();
    }

    private void cycleNameplate() {
        collectCurrentTab();
        draft = copyBasic(draft, draft.displayName(), draft.entityType(), draft.category(),
                draft.enabled(), draft.level(), next(draft.nameplate()), draft.tags());
        dirty = true;
        rebuild();
    }

    private void buildPreviewButtons() {
        previewIconButton(0, ProjectSIcon.RESET, () -> "プレビューをリセット",
                this::resetPreview, () -> false);
        previewIconButton(1, ProjectSIcon.CAMERA, () -> "視点: " + view, () -> {
            view = next(view);
            applyView();
        }, () -> view != View.FREE);
        previewIconButton(2, ProjectSIcon.BACKGROUND,
                () -> "背景: " + (darkBackground ? "Dark" : "Light"),
                () -> darkBackground = !darkBackground, () -> darkBackground);
        previewIconButton(3, ProjectSIcon.GRID,
                () -> "グリッド: " + (grid ? "ON" : "OFF"),
                () -> grid = !grid, () -> grid);
        previewIconButton(4, ProjectSIcon.HITBOX,
                () -> "当たり判定: " + (hitbox ? "ON" : "OFF"),
                () -> hitbox = !hitbox, () -> hitbox);
        previewIconButton(5, ProjectSIcon.EYE_LINE,
                () -> "視線: " + (eyeLine ? "ON" : "OFF"),
                () -> eyeLine = !eyeLine, () -> eyeLine);
        previewIconButton(6, ProjectSIcon.PLAY, () -> "アニメーション: " + animation, () -> {
            animation = next(animation);
            preview.setAnimation(animation);
        }, () -> animation != MobPreviewEntity.Animation.IDLE);
    }

    private void previewIconButton(int index, ProjectSIcon icon,
                                   java.util.function.Supplier<String> label,
                                   Runnable action,
                                   java.util.function.BooleanSupplier selected) {
        MobEditorLayout.Bounds bounds = MobPreviewPanel.controlBounds(layout.preview(), index, 7);
        ProjectSIconButton[] holder = new ProjectSIconButton[1];
        holder[0] = new ProjectSIconButton(
                bounds.x(), bounds.y(), bounds.width(), bounds.height(), icon,
                Component.literal(label.get()), ProjectSButton.Kind.GHOST, () -> {
                    action.run();
                    holder[0].selected(selected.getAsBoolean());
                    holder[0].setMessage(Component.literal(label.get()));
                    holder[0].setTooltip(Tooltip.create(Component.literal(label.get())));
                });
        holder[0].selected(selected.getAsBoolean());
        addRenderableWidget(holder[0]);
    }

    private EditBox field(
            String key,
            String label,
            String value,
            int x,
            int y,
            int width
    ) {
        if (buildingProperty) {
            x = Math.max(layout.property().x(), Math.min(x, layout.property().right() - 48));
            width = Math.min(width, layout.property().right() - x);
        }
        EditBox box = addRenderableWidget(new ProjectSTextField(
                font, x, y + (label.isBlank() ? 0 : 14), width, 20,
                Component.literal(label), Component.empty()));
        box.setValue(value);
        box.setMaxLength(key.equals("tags") ? 512 : 128);
        fields.put(key, box);
        labels.put(key, label);
        box.setResponder(ignored -> {
            collectCurrentTab(false);
            if (draft != null) {
                preview.update(draft, MobEditorClientState.state().headDetail());
            }
        });
        if (buildingProperty) propertyPanel.track(box, label.isBlank() ? 0 : 12, 3);
        return box;
    }

    private void button(int x, int y, String label, Runnable action) {
        button(x, y, label, action, 112);
    }

    private void button(int x, int y, String label, Runnable action, int width) {
        addThemedButton(x, y, width, label, action);
    }

    private ProjectSButton addThemedButton(int x, int y, int width, String label, Runnable action) {
        if (buildingProperty) {
            x = Math.max(layout.property().x(), Math.min(x, layout.property().right() - 42));
            width = Math.min(width, layout.property().right() - x);
        }
        ProjectSButton result = addRenderableWidget(new ProjectSButton(x, y, width, 20,
                Component.literal(label), ProjectSButton.Kind.SECONDARY, action));
        if (buildingProperty) propertyPanel.track(result, 0, 0);
        return result;
    }

    private ProjectSButton addPropertyButton(int x, int y, int width, String label, Runnable action) {
        ProjectSButton result = addThemedButton(x, y, width, label, action);
        if (!buildingProperty) propertyPanel.track(result, 0, 0);
        return result;
    }

    private void addPropertyWidget(net.minecraft.client.gui.components.AbstractWidget widget) {
        addRenderableWidget(widget);
        propertyPanel.track(widget, 0, 0);
    }

    private void switchTab(Tab value) {
        collectCurrentTab();
        if (!localError.isBlank()) return;
        tab = value;
        propertyScroll = MobPropertyPanel.tabChangeScroll();
        refreshWidgets();
    }

    private void collectCurrentTab() {
        collectCurrentTab(true);
    }

    private void collectCurrentTab(boolean finalizeEntityType) {
        if (draft == null || fields.isEmpty()) return;
        MobEditorData.Mob before = draft;
        try {
            localError = "";
            switch (tab) {
                case BASIC -> {
                    String entityType = text("entity").toUpperCase(Locale.ROOT);
                    String display = text("display");
                    List<String> tags = Arrays.stream(text("tags").split(","))
                            .map(String::trim).filter(value -> !value.isBlank()).toList();
                    if (!MobEditorInputLogic.utf8Within(display, 128)
                            || !MobEditorInputLogic.utf8Within(entityType, 64)
                            || !validTags(tags)) {
                        throw new IllegalArgumentException("Mob text input exceeds its bounds");
                    }
                    draft = copyBasic(
                            draft, display, entityType,
                            draft.category(), draft.enabled(), integer("level"),
                            draft.nameplate(), tags);
                    if (finalizeEntityType) {
                        draft = normalizeAppearance(draft, entityType);
                    }
                }
                case STATS -> {
                    draft = withStats(draft, new MobEditorData.Stats(
                            decimal("hp"), decimal("patk"), decimal("matk"),
                            decimal("pdef"), decimal("mdef"), decimal("move"),
                            decimal("aspeed"), decimal("crit"), decimal("critdmg"),
                            decimal("reduction")));
                    MobEditorData.BasicAttack attack = draft.attack();
                    draft = withAttack(draft, new MobEditorData.BasicAttack(
                            attack.damageType(), decimal("fixed"), decimal("coef"),
                            decimal("interval"), decimal("range"),
                            decimal("knockback"), attack.criticalAllowed()));
                }
                case AI -> {
                    MobEditorData.Ai ai = draft.ai();
                    draft = withAi(draft, new MobEditorData.Ai(
                            ai.preset(), ai.priority(), decimal("aggro"),
                            decimal("chase"), decimal("leash"), decimal("airange"),
                            decimal("refresh"), ai.returnHome(), ai.resetHealth(),
                            ai.avoidFalls(), ai.avoidWater()));
                }
                case APPEARANCE -> {
                    EnumMap<MobEditorData.Slot, MobEditorData.Equipment> equipment =
                            new EnumMap<>(MobEditorData.Slot.class);
                    for (MobEditorData.Slot slot : MobEditorData.Slot.values()) {
                        MobEditorData.Equipment old = draft.appearance().equipment().get(slot);
                        MobEditorData.EquipmentSource source = equipmentSources.getOrDefault(
                                slot, old.source());
                        String value = fields.containsKey("eq_" + slot.name())
                                ? text("eq_" + slot.name())
                                : source == MobEditorData.EquipmentSource.VANILLA_ITEM
                                ? old.material() : old.referenceId();
                        String color = slot == selectedEquipmentSlot
                                ? text("eq_color") : old.color();
                        if (source == MobEditorData.EquipmentSource.NONE
                                || source == MobEditorData.EquipmentSource.CUSTOM_HEAD) {
                            color = "";
                        }
                        equipment.put(slot, new MobEditorData.Equipment(
                                source,
                                source == MobEditorData.EquipmentSource.PROJECTS_ITEM
                                        || source == MobEditorData.EquipmentSource.CUSTOM_HEAD
                                        ? value : "",
                                source == MobEditorData.EquipmentSource.VANILLA_ITEM
                                        ? value.toUpperCase(Locale.ROOT) : "",
                                color,
                                source != MobEditorData.EquipmentSource.NONE && old.glint(),
                                old.visible(), true));
                    }
                    MobEditorData.Appearance old = draft.appearance();
                    Map<String, String> variants = new HashMap<>();
                    for (String key : variantKeys(draft.entityType())) {
                        variants.put(key, fields.containsKey("variant_" + key)
                                ? text("variant_" + key)
                                : old.variants().getOrDefault(key,
                                defaultVariant(draft.entityType(), key)));
                    }
                    draft = withAppearance(draft, new MobEditorData.Appearance(
                            decimal("scale"), old.age(), old.glowing(),
                            old.glowingColor(), variants, equipment));
                }
                case ABILITIES, TEST -> { }
            }
            if (!draft.equals(before)) dirty = true;
            preview.update(draft, MobEditorClientState.state().headDetail());
        } catch (NumberFormatException ignored) {
            localError = "有限な数値または整数を入力してください";
        } catch (IllegalArgumentException ignored) {
            localError = "入力がUTF-8バイト上限または件数上限を超えています";
        }
    }

    private void syncState() {
        var state = MobEditorClientState.state();
        syncedStateRevision = MobEditorClientState.localRevision();
        var v2 = MobEditorClientState.v2State();
        boolean preserveConflictDraft = MobEditorConflictLogic.preserveDirtyWorkingDraft(
                dirty, draft, state.revisionConflict());
        if (v2 == null && !preserveConflictDraft) {
            // A v1 response has no v2 assignment authority.
            abilities.replace(List.of(), List.of());
            abilityUndoBaseline.clear();
            assignedAbilityOffset = 0;
            availableAbilityOffset = 0;
        }
        boolean duplicateWasPending = duplicateCorrelation.pending();
        boolean duplicateSucceeded = duplicateCorrelation.consumeSuccessful(state.supported(),
                state.permitted(), state.success(), state.revisionConflict(),
                MobEditorClientState.communicating() ? "中..." : "",
                state.detail() == null ? null : state.detail().id());
        if (!duplicateCorrelation.pending() && !duplicateSucceeded) duplicateTemplate = null;
        boolean saveFinished = pendingSave && !MobEditorClientState.communicating();
        boolean saveAccepted = saveFinished && state.success()
                && !state.revisionConflict() && state.detail() != null;
        if (saveFinished) pendingSave = false;
        boolean reloadWasPending = pendingReload;
        boolean reloadFinished = reloadWasPending && !MobEditorClientState.communicating();
        boolean reloadAccepted = reloadFinished && state.success()
                && !state.revisionConflict() && state.detail() != null;
        if (reloadFinished) pendingReload = false;
        if (reloadAccepted) {
            draft = null;
            original = null;
            dirty = false;
        }
        boolean ignoreIncomingDetail = duplicateWasPending && !duplicateSucceeded
                || reloadWasPending && !reloadAccepted;
        if (state.detail() != null && !ignoreIncomingDetail) {
            if (duplicateSucceeded && duplicateTemplate != null) {
                draft = copyId(duplicateTemplate, state.detail().id());
                original = state.detail();
                dirty = true;
                duplicateTemplate = null;
                if (v2 == null) {
                    abilities.replace(List.of(), List.of());
                    abilityUndoBaseline.clear();
                } else {
                    replaceAuthoritativeAbilities();
                }
                MobEditorClientState.validate(draft);
            } else if (saveAccepted) {
                draft = state.detail();
                original = draft;
                dirty = false;
                replaceAuthoritativeAbilities();
            } else {
                MobEditorData.Mob incoming = state.detail();
                boolean selectionChanged = draft == null
                        || !draft.id().equals(incoming.id());
                boolean sameDraft = draft != null && draft.id().equals(incoming.id());
                boolean preserveDirtyDraft = preserveConflictDraft
                        || dirty && sameDraft && !state.revisionConflict();
                if (!preserveDirtyDraft) {
                    draft = incoming;
                    if (original == null
                            || !original.id().equals(draft.id())
                            || original.revision() != draft.revision()) {
                        original = draft;
                        dirty = false;
                        replaceAuthoritativeAbilities();
                    } else if (v2 != null && v2.detail() != null) {
                        replaceAuthoritativeAbilities();
                    }
                    if (appliedEntityType.isBlank() || selectionChanged) {
                        appliedEntityType = draft.entityType();
                    }
                }
            }
            equipmentSources.clear();
            equipmentSources.putAll(draft.appearance().equipment().entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(
                            Map.Entry::getKey, value -> value.getValue().source(),
                            (left, right) -> left,
                            () -> new EnumMap<>(MobEditorData.Slot.class))));
            preview.update(draft, state.headDetail());
            requestDraftHeadDetail(state.headDetail());
        }
        if (pendingApply && !MobEditorClientState.communicating()) {
            if (state.success() && !state.revisionConflict() && draft != null) {
                appliedEntityType = draft.entityType();
            }
            pendingApply = false;
        }
        if (state.revisionConflict()) localError = CONFLICT_ERROR;
        else if (CONFLICT_ERROR.equals(localError)) localError = "";
    }

    private void replaceAuthoritativeAbilities() {
        MobEditorV2StatePayload.State authority = MobEditorClientState
                .authoritativeV2State();
        if (authority == null || draft == null || authority.detail() == null
                || !authority.detail().base().id().equals(draft.id())) return;
        abilityUndoBaseline.capture(authority, draft.id());
        abilities.replace(abilityUndoBaseline.assigned(), abilityUndoBaseline.catalog());
        assignedAbilityOffset = 0;
        availableAbilityOffset = 0;
    }

    private void requestDraftHeadDetail(MobEditorData.Head current) {
        MobEditorData.Equipment head = draft.appearance().equipment()
                .get(MobEditorData.Slot.HEAD);
        if (head.source() != MobEditorData.EquipmentSource.CUSTOM_HEAD
                || head.referenceId().isBlank()
                || current != null && current.id().equals(head.referenceId())
                || MobEditorClientState.communicating()) return;
        MobEditorClientState.requestHead(head.referenceId(), headQuery, headPage);
    }

    private void selectMob(String id) {
        if (dirty) {
            confirm("未保存の変更を破棄しますか？", "別のMobを選択します", "破棄して選択", () ->
                    discardAndSelect(id));
            return;
        }
        discardAndSelect(id);
    }

    private void discardAndSelect(String id) {
        clearPendingDuplicate();
        if (!MobEditorClientState.select(id)) {
            localError = "通信中です。応答後にもう一度操作してください";
            return;
        }
        pendingSave = false;
        pendingReload = false;
        localError = "";
        draft = null;
        original = null;
        dirty = false;
    }

    private void duplicate() {
        if (draft == null) return;
        collectCurrentTab();
        if (!localError.isBlank()) return;
        pendingSave = false;
        pendingReload = false;
        duplicateTemplate = draft;
        String duplicateId = draft.id() + "_copy";
        if (!MobEditorInputLogic.utf8Within(duplicateId, 64)) {
            clearPendingDuplicate();
            localError = "複製後のIDがUTF-8バイト上限を超えています";
            return;
        }
        duplicateCorrelation.begin(duplicateId);
        if (!MobEditorClientState.create(duplicateId)) {
            clearPendingDuplicate();
            localError = "通信中です。応答後にもう一度操作してください";
        }
    }

    private void createDraft(String id) {
        clearPendingDuplicate();
        pendingSave = false;
        pendingReload = false;
        if (!MobEditorInputLogic.utf8Within(id, 64)) {
            localError = "IDがUTF-8バイト上限を超えています";
            return;
        }
        if (dirty) {
            confirm("未保存の変更を破棄しますか？", "新しいMob Draftを作成します", "破棄して作成", () -> {
                if (MobEditorClientState.create(id)) dirty = false;
                else localError = "通信中です。応答後にもう一度操作してください";
            });
            return;
        }
        if (!MobEditorClientState.create(id)) {
            localError = "通信中です。応答後にもう一度操作してください";
        }
    }

    private void setHeadReference(String id) {
        if (draft == null) return;
        collectCurrentTab();
        EnumMap<MobEditorData.Slot, MobEditorData.Equipment> equipment =
                new EnumMap<>(MobEditorData.Slot.class);
        equipment.putAll(draft.appearance().equipment());
        equipment.put(MobEditorData.Slot.HEAD, new MobEditorData.Equipment(
                MobEditorData.EquipmentSource.CUSTOM_HEAD,
                id, "", "", false, true, true));
        draft = withAppearance(draft, new MobEditorData.Appearance(
                draft.appearance().scale(), draft.appearance().age(),
                draft.appearance().glowing(), draft.appearance().glowingColor(),
                draft.appearance().variants(), equipment));
        dirty = true;
        recentHeads.remove(id);
        recentHeads.add(id);
        equipmentSources.put(MobEditorData.Slot.HEAD,
                MobEditorData.EquipmentSource.CUSTOM_HEAD);
        preview.update(draft, MobEditorClientState.state().headDetail());
    }

    private void updateSelectedEquipment(String color, boolean glint, boolean visible) {
        collectCurrentTab(false);
        EnumMap<MobEditorData.Slot, MobEditorData.Equipment> equipment =
                new EnumMap<>(MobEditorData.Slot.class);
        equipment.putAll(draft.appearance().equipment());
        MobEditorData.Equipment current = equipment.get(selectedEquipmentSlot);
        equipment.put(selectedEquipmentSlot, new MobEditorData.Equipment(
                current.source(), current.referenceId(), current.material(), color,
                glint, visible, true));
        draft = withAppearance(draft, new MobEditorData.Appearance(
                draft.appearance().scale(), draft.appearance().age(),
                draft.appearance().glowing(), draft.appearance().glowingColor(),
                draft.appearance().variants(), equipment));
        dirty = true;
        rebuild();
    }

    private void toggleSelectedEquipment(boolean glintToggle) {
        collectCurrentTab();
        MobEditorData.Equipment current = draft.appearance().equipment()
                .get(selectedEquipmentSlot);
        updateSelectedEquipment(current.color(),
                glintToggle ? !current.glint() : current.glint(),
                glintToggle ? current.visible() : !current.visible());
    }

    private void validateDraft() {
        collectCurrentTab();
        if (!MobEditorConflictLogic.canMutate(
                MobEditorClientState.state().revisionConflict())) {
            localError = "競合を解決するまで検証できません。再読込またはMobの再選択を行ってください";
            return;
        }
        if (draft != null && localError.isBlank()) {
            if (MobEditorClientState.abilityAuthoringAvailable()) MobEditorClientState.validateAbilities(draft, abilities.assigned());
            else MobEditorClientState.validate(draft);
        }
    }

    private void saveDraft() {
        collectCurrentTab();
        if (MobEditorClientState.communicating()
                || MobEditorClientState.state().revisionConflict()) {
            localError = MobEditorClientState.state().revisionConflict()
                    ? "競合を解決するまで保存できません"
                    : "通信中です。応答後にもう一度操作してください";
            toasts.show(ProjectSToast.Kind.WARNING, Component.literal("保存できません"),
                    Component.literal(localError), 2600);
            return;
        }
        if (draft != null && localError.isBlank()) {
            pendingSave = MobEditorClientState.abilityAuthoringAvailable()
                    ? MobEditorClientState.saveAbilities(draft, abilities.assigned())
                    : saveV1Draft(draft);
        }
    }

    private boolean saveV1Draft(MobEditorData.Mob value) {
        MobEditorClientState.save(value);
        return MobEditorClientState.communicating();
    }

    private void applyDefinition() {
        collectCurrentTab();
        if (draft == null || !localError.isBlank()) return;
        if (!MobEditorConflictLogic.canMutate(
                MobEditorClientState.state().revisionConflict())) {
            localError = "競合を解決するまで適用できません。再読込またはMobの再選択を行ってください";
            return;
        }
        if (dirty) {
            localError = "適用前にDraftを保存してください";
            return;
        }
        if (!appliedEntityType.isBlank()
                && !appliedEntityType.equals(draft.entityType())) {
            confirm("EntityType変更を適用しますか？", "既存個体は同じ位置で再生成されます", "適用",
                    this::sendApply);
            return;
        }
        sendApply();
    }

    private void sendApply() {
        MobEditorClientState.apply();
        pendingApply = MobEditorClientState.communicating();
    }

    private void reloadDefinitions() {
        if (dirty) {
            confirm("未保存の変更を破棄しますか？", "サーバーの定義を再読み込みします", "破棄して再読込",
                    this::discardAndReload);
            return;
        }
        discardAndReload();
    }

    private void discardAndReload() {
        clearPendingDuplicate();
        if (!MobEditorClientState.reload()) {
            localError = "通信中です。応答後にもう一度操作してください";
            return;
        }
        pendingSave = false;
        pendingReload = true;
        localError = "";
    }

    private void testSpawn(boolean cursor) {
        collectCurrentTab();
        if (MobEditorClientState.state().revisionConflict()) {
            localError = "競合を解決するまでテスト召喚できません";
            return;
        }
        if (MobEditorClientState.communicating()) {
            localError = "通信中です。応答後にもう一度操作してください";
            return;
        }
        if (draft != null && localError.isBlank()) {
            if (MobEditorClientState.abilityAuthoringAvailable()) MobEditorClientState.testAbilities(draft, abilities.assigned(), cursor);
            else MobEditorClientState.testSpawn(draft, cursor);
        }
    }

    private void undo() {
        if (original == null) return;
        draft = original;
        abilityUndoBaseline.restoreInto(abilities);
        clampAbilityOffsets();
        dirty = false;
        localError = "";
        refreshWidgets();
    }

    @Override
    public void tick() {
        super.tick();
        preview.tick();
        String message = localError.isBlank()
                ? MobEditorClientState.state().message() : localError;
        if (!message.isBlank() && !message.equals(lastToastMessage)) {
            ProjectSToast.Kind kind = !localError.isBlank()
                    ? ProjectSToast.Kind.ERROR : MobEditorClientState.state().success()
                    ? ProjectSToast.Kind.SUCCESS : ProjectSToast.Kind.WARNING;
            toasts.show(kind, Component.literal(kind == ProjectSToast.Kind.SUCCESS ? "Mob Editor" : "確認"),
                    Component.literal(message), 2800);
            lastToastMessage = message;
        }
        if (knownRevision != MobEditorClientState.localRevision()) {
            rebuild();
        }
    }

    private void rebuild() {
        collectCurrentTab();
        refreshWidgets();
    }

    private void refreshWidgets() {
        ProjectSDropdown.closeAny();
        clearWidgets();
        init();
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float tickProgress
    ) {
        var theme = ProjectSThemeManager.get().activeTheme();
        var tokens = theme.tokens();
        graphics.fill(0, 0, width, height,
                darkBackground ? tokens.background() : tokens.backgroundAlt());
        if (layout == null || !layout.usable()) {
            graphics.centeredText(font, "画面サイズが小さすぎます。ウィンドウを広げてください",
                    width / 2, height / 2, tokens.warning());
            super.extractRenderState(graphics, mouseX, mouseY, tickProgress);
            return;
        }
        ProjectSUiDraw.cutPanel(graphics, layout.mobList().x(), layout.mobList().y(),
                layout.mobList().width(), layout.mobList().height(), theme.metrics().cornerCut(),
                tokens.surfaceAlt(), tokens.borderCard());
        ProjectSUiDraw.cutPanel(graphics, layout.property().x(), layout.property().y(),
                layout.property().width(), layout.property().height(), theme.metrics().cornerCut(),
                tokens.surface(), tokens.borderCard());
        graphics.text(font, "モブライブラリ", layout.header().x() + 4, layout.header().y() + 9,
                tokens.textPrimary(), false);
        graphics.text(font, title.getString() + (dirty ? "  ● 未保存" : ""),
                layout.tabs().x(), layout.header().y() + 9,
                dirty ? tokens.warning() : tokens.textPrimary(), false);
        renderPreview(graphics, mouseX, mouseY, tickProgress);
        super.extractRenderState(graphics, mouseX, mouseY, tickProgress);
    }

    @Override
    protected void extractThemedForeground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickProgress
    ) {
        if (layout == null || !layout.usable()) return;
        renderMessage(graphics);
        if (tab == Tab.AI && draft != null) renderAiSummary(graphics);
    }

    private void renderPreview(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float tickProgress
    ) {
        var theme = ProjectSThemeManager.get().activeTheme();
        var tokens = theme.tokens();
        MobEditorLayout.Bounds previewBounds = layout.preview();
        int left = previewBounds.x();
        int top = previewBounds.y();
        int right = previewBounds.right();
        int bottom = previewBounds.bottom();
        MobEditorLayout.Bounds previewContent = MobPreviewPanel.contentBounds(previewBounds, 7);
        MobPreviewPanel.renderChrome(graphics, font, previewBounds,
                "ライブプレビュー  " + view + " / " + animation, darkBackground, grid, 7);
        LivingEntity entity = preview.update(draft, MobEditorClientState.state().headDetail());
        if (entity != null) {
            int centerX = (left + right) / 2 + (int) previewOffsetX;
            int centerY = (top + bottom) / 2 + 30 + (int) previewOffsetY;
            int size = (int) (view == View.HEAD ? zoom * 2.8 : zoom);
            float verticalOffset = view == View.HEAD
                    ? (float) (entity.getEyeHeight() * size * .45) : 0;
            InventoryScreen.extractEntityInInventoryFollowsMouse(
                    graphics, previewContent.x(), previewContent.y(), previewContent.right(), previewContent.bottom(),
                    size, verticalOffset,
                    (float) (centerX + yaw), (float) (centerY + pitch), entity);
            if (hitbox) {
                int boxWidth = Math.max(18, (int) (entity.getBbWidth() * zoom));
                int boxHeight = Math.max(24, (int) (entity.getBbHeight() * zoom));
                graphics.outline(centerX - boxWidth / 2,
                        centerY - boxHeight, boxWidth, boxHeight, tokens.danger());
            }
            if (eyeLine) {
                int eyeY = centerY - (int) (entity.getEyeHeight() * zoom);
                graphics.horizontalLine(previewContent.x(), previewContent.right(), eyeY, tokens.success());
            }
            if (draft != null) {
                String metrics = "Scale %.2f / Hit %.2f×%.2f / Eye %.2f / 攻撃距離 %.2f"
                        .formatted(draft.appearance().scale(), entity.getBbWidth(),
                                entity.getBbHeight(), entity.getEyeHeight(),
                                draft.attack().range());
                graphics.text(font, metrics, left + 8, previewContent.bottom() - 12,
                        tokens.textMuted(), false);
            }
        } else {
            graphics.centeredText(font, "プレビュー可能なLivingEntityを選択してください",
                    (left + right) / 2, (top + previewContent.bottom()) / 2, tokens.danger());
        }
    }

    private void renderMessage(GuiGraphicsExtractor graphics) {
        var tokens = ProjectSThemeManager.get().activeTheme().tokens();
        int x = layout.property().x() + 4;
        int y = layout.actionBar().y() - 12;
        if (!localError.isBlank()) {
            graphics.text(font, font.plainSubstrByWidth(localError,
                            Math.max(1, layout.property().width() - 8)),
                    x, y, tokens.danger(), false);
            return;
        }
        String message = MobEditorClientState.state().message();
        if (message.isBlank()) return;
        graphics.text(font, font.plainSubstrByWidth(message,
                        Math.max(1, layout.property().width() - 8)),
                x, y,
                MobEditorClientState.state().success()
                        ? tokens.success() : tokens.danger(), false);
    }

    private void renderAiSummary(GuiGraphicsExtractor graphics) {
        MobEditorData.Ai ai = draft.ai();
        var tokens = ProjectSThemeManager.get().activeTheme().tokens();
        int x = layout.property().x();
        int y = layout.property().y() + (compactProperty() ? 364 : 250) - propertyScroll;
        if (!propertyPanel.summaryVisible(y, 78)) return;
        graphics.text(font, "挙動サマリー", x, y, tokens.warning(), false);
        graphics.text(font, "%.1fブロック以内のプレイヤーを検出します。"
                .formatted(ai.aggroRange()), x, y + 16, tokens.textMuted(), false);
        graphics.text(font, "最大%.1fブロックまで追跡します。"
                .formatted(ai.chaseRange()), x, y + 30, tokens.textMuted(), false);
        graphics.text(font, "初期位置から%.1fブロックで帰還します。"
                .formatted(ai.leashRange()), x, y + 44, tokens.textMuted(), false);
        graphics.text(font, "HIGHEST_THREATは脅威値基盤未実装のため選択不可です。",
                x, y + 62, tokens.warning(), false);
    }

    @Override
    public boolean mouseDragged(
            MouseButtonEvent event,
            double deltaX,
            double deltaY
    ) {
        if (super.mouseDragged(event, deltaX, deltaY)) return true;
        if (modal.isOpen()) return true;
        if (insidePreview(event.x(), event.y())) {
            if (event.button() == 0) {
                yaw += deltaX * 2;
                pitch += deltaY * 2;
                view = View.FREE;
                return true;
            }
            if (event.button() == 1) {
                previewOffsetX += deltaX;
                previewOffsetY += deltaY;
                return true;
            }
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double horizontal,
            double vertical
    ) {
        if (super.mouseScrolled(mouseX, mouseY, horizontal, vertical)) return true;
        if (modal.isOpen()) return true;
        if (layout == null || !layout.usable()) return false;
        if (layout.property().contains(mouseX, mouseY) && draft != null
                && propertyContentHeight() > layout.property().height()) {
            propertyScroll = layout.clampPropertyScroll(propertyScroll - (int) (vertical * 20),
                    propertyContentHeight());
            refreshWidgets();
            return true;
        }
        if (insidePreview(mouseX, mouseY)) {
            zoom = Math.clamp(zoom + vertical * 4, 16, 120);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    private boolean insidePreview(double x, double y) {
        return layout != null && layout.usable()
                && MobPreviewPanel.contentBounds(layout.preview(), 7).contains(x, y);
    }

    private void resetPreview() {
        yaw = 0;
        pitch = 0;
        zoom = 48;
        previewOffsetX = 0;
        previewOffsetY = 0;
        view = View.FRONT;
        applyView();
    }

    private void applyView() {
        switch (view) {
            case FRONT, FREE, HEAD -> yaw = 0;
            case BACK -> yaw = 180;
            case LEFT -> yaw = -90;
            case RIGHT -> yaw = 90;
        }
    }

    private int propertyContentHeight() {
        return switch (tab) {
            case APPEARANCE -> Math.max(
                    MobPropertyPanel.contentHeight(tab.name(), compactProperty()),
                    compactProperty() ? 1000 : 600);
            case BASIC, STATS, AI -> MobPropertyPanel.contentHeight(tab.name(), compactProperty());
            case TEST -> compactProperty() ? 228 : MobPropertyPanel.contentHeight(tab.name(), false);
            case ABILITIES -> AbilityAssignmentPanel.contentHeight(layout.property().width(),
                    Math.min(ABILITY_ASSIGNED_PAGE_SIZE, abilities.assigned().size()),
                    Math.min(ABILITY_AVAILABLE_PAGE_SIZE, abilities.available().size()));
        };
    }

    private String text(String key) {
        EditBox value = fields.get(key);
        if (value != null) return value.getValue().trim();
        if (key.equals("entity") && draft != null) return draft.entityType();
        return "";
    }

    private double decimal(String key) {
        double value = Double.parseDouble(text(key));
        if (!Double.isFinite(value)) throw new NumberFormatException();
        return value;
    }

    private int integer(String key) {
        int value = Integer.parseInt(text(key));
        if (key.equals("level") && (value < 0 || value > 65_535)) {
            throw new NumberFormatException();
        }
        return value;
    }

    private static boolean validTags(List<String> tags) {
        if (tags.size() > 32 || tags.stream().distinct().count() != tags.size()) return false;
        return tags.stream().allMatch(value -> MobEditorInputLogic.utf8Within(value, 32));
    }

    private static String number(double value) {
        return String.format(Locale.ROOT, "%.4f", value).replaceAll("\\.?0+$", "");
    }

    private static String tabName(Tab tab) {
        return switch (tab) {
            case BASIC -> "基本";
            case STATS -> "Stats";
            case AI -> "AI";
            case ABILITIES -> "Abilities";
            case APPEARANCE -> "外見";
            case TEST -> "テスト";
        };
    }

    private static String shortSlot(MobEditorData.Slot slot) {
        return switch (slot) {
            case HEAD -> "頭";
            case CHEST -> "胴";
            case LEGS -> "脚";
            case FEET -> "足";
            case MAIN_HAND -> "主手";
            case OFF_HAND -> "副手";
        };
    }

    private static boolean supportsBaby(String entityType) {
        return java.util.Set.of(
                "ZOMBIE", "HUSK", "DROWNED", "ZOMBIE_VILLAGER", "PIGLIN",
                "PIGLIN_BRUTE", "ZOGLIN", "SHEEP", "WOLF", "CAT", "HORSE",
                "VILLAGER").contains(entityType.toUpperCase(Locale.ROOT));
    }

    private static boolean supportsEquipment(String entityType) {
        return MobEditorUiLogic.equipmentSupported(entityType);
    }

    private static List<String> variantKeys(String entityType) {
        return MobEditorVariantLogic.keys(entityType);
    }

    private static String defaultVariant(String entityType, String key) {
        String type = entityType.toUpperCase(Locale.ROOT);
        return switch (key) {
            case "size" -> "1";
            case "sheared", "angry" -> "false";
            case "variant" -> type.equals("CAT") ? "TABBY" : "PALE";
            case "collar-color" -> "RED";
            case "color" -> type.equals("HORSE") ? "BROWN" : "WHITE";
            case "style" -> "NONE";
            case "profession" -> "NONE";
            case "villager-type" -> "PLAINS";
            default -> "";
        };
    }

    private static String nextGlowColor(String current) {
        List<String> colors = List.of(
                "WHITE", "YELLOW", "GOLD", "RED", "DARK_RED", "LIGHT_PURPLE",
                "DARK_PURPLE", "BLUE", "DARK_BLUE", "AQUA", "DARK_AQUA",
                "GREEN", "DARK_GREEN", "GRAY", "DARK_GRAY", "BLACK");
        int index = colors.indexOf(current.toUpperCase(Locale.ROOT));
        return colors.get((index + 1) % colors.size());
    }

    private static <T extends Enum<T>> T next(T value) {
        T[] values = value.getDeclaringClass().getEnumConstants();
        return values[(value.ordinal() + 1) % values.length];
    }

    private static MobEditorData.Mob copyId(MobEditorData.Mob source, String id) {
        return new MobEditorData.Mob(
                1, 0, id, source.displayName() + " コピー", source.entityType(),
                source.category(), source.enabled(), source.level(),
                source.nameplate(), source.tags(), source.stats(), source.attack(),
                source.ai(), source.appearance());
    }

    private static MobEditorData.Mob copyBasic(
            MobEditorData.Mob source,
            String display,
            String entityType,
            MobEditorData.Category category,
            boolean enabled,
            int level,
            MobEditorData.NameplateMode nameplate,
            List<String> tags
    ) {
        return new MobEditorData.Mob(
                source.schemaVersion(), source.revision(), source.id(), display,
                entityType, category, enabled, level, nameplate, tags,
                source.stats(), source.attack(), source.ai(), source.appearance());
    }

    private static MobEditorData.Mob withStats(
            MobEditorData.Mob source,
            MobEditorData.Stats stats
    ) {
        return new MobEditorData.Mob(
                source.schemaVersion(), source.revision(), source.id(),
                source.displayName(), source.entityType(), source.category(),
                source.enabled(), source.level(), source.nameplate(), source.tags(),
                stats, source.attack(), source.ai(), source.appearance());
    }

    private static MobEditorData.Mob withAttack(
            MobEditorData.Mob source,
            MobEditorData.BasicAttack attack
    ) {
        return new MobEditorData.Mob(
                source.schemaVersion(), source.revision(), source.id(),
                source.displayName(), source.entityType(), source.category(),
                source.enabled(), source.level(), source.nameplate(), source.tags(),
                source.stats(), attack, source.ai(), source.appearance());
    }

    private static MobEditorData.Mob withAi(
            MobEditorData.Mob source,
            MobEditorData.Ai ai
    ) {
        return new MobEditorData.Mob(
                source.schemaVersion(), source.revision(), source.id(),
                source.displayName(), source.entityType(), source.category(),
                source.enabled(), source.level(), source.nameplate(), source.tags(),
                source.stats(), source.attack(), ai, source.appearance());
    }

    private static MobEditorData.Mob withAppearance(
            MobEditorData.Mob source,
            MobEditorData.Appearance appearance
    ) {
        return new MobEditorData.Mob(
                source.schemaVersion(), source.revision(), source.id(),
                source.displayName(), source.entityType(), source.category(),
                source.enabled(), source.level(), source.nameplate(), source.tags(),
                source.stats(), source.attack(), source.ai(), appearance);
    }

    private static MobEditorData.Mob normalizeAppearance(
            MobEditorData.Mob source,
            String entityType
    ) {
        if (!MobPreviewEntity.knownEntityType(entityType)) return source;
        MobEditorData.Appearance old = source.appearance();
        MobEditorData.Age age = supportsBaby(entityType)
                ? old.age() : MobEditorData.Age.ADULT;
        Map<String, String> variants = new HashMap<>();
        for (String key : variantKeys(entityType)) {
            if (old.variants().containsKey(key)) variants.put(key, old.variants().get(key));
        }
        Map<MobEditorData.Slot, MobEditorData.Equipment> equipment = old.equipment();
        if (!supportsEquipment(entityType)) {
            EnumMap<MobEditorData.Slot, MobEditorData.Equipment> empty =
                    new EnumMap<>(MobEditorData.Slot.class);
            for (MobEditorData.Slot slot : MobEditorData.Slot.values()) {
                empty.put(slot, MobEditorData.Equipment.empty());
            }
            equipment = empty;
        }
        return withAppearance(source, new MobEditorData.Appearance(
                old.scale(), age, old.glowing(), old.glowingColor(), variants, equipment));
    }

    @Override
    public void removed() {
        preview.close();
        MobItemStackCache.clear();
        super.removed();
    }

    @Override
    public void onClose() {
        if (dirty) {
            confirm("未保存の変更を破棄しますか？", "Mob Editorを閉じます", "破棄して閉じる",
                    this::closeEditor);
            return;
        }
        closeEditor();
    }

    private void closeEditor() {
        clearPendingDuplicate();
        MobEditorClientState.close();
        minecraft.setScreen(parent);
    }

    private void confirm(String title, String body, String confirmLabel, Runnable confirmed) {
        modal.open(Component.literal(title), Component.literal(body), Component.literal(confirmLabel),
                confirmed, Component.literal("キャンセル"), () -> { },
                io.github.gyai.projects.client.ui.widget.ProjectSModal.PrimaryKind.DANGER, true);
    }

    private void clearPendingDuplicate() {
        duplicateCorrelation.clear();
        duplicateTemplate = null;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
