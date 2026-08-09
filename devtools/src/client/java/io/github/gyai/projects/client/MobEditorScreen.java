package io.github.gyai.projects.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
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
import io.github.gyai.projects.client.ui.mobeditor.MobEditorTabBar;
import io.github.gyai.projects.client.ui.mobeditor.MobListPanel;
import io.github.gyai.projects.client.ui.mobeditor.MobPreviewPanel;
import io.github.gyai.projects.client.ui.mobeditor.MobPropertyPanel;
import io.github.gyai.projects.client.ui.render.ProjectSColorMath;
import io.github.gyai.projects.client.ui.render.ProjectSUiDraw;
import io.github.gyai.projects.client.ui.screen.ProjectSThemedScreen;
import io.github.gyai.projects.client.ui.theme.ProjectSThemeManager;
import io.github.gyai.projects.client.ui.widget.ProjectSButton;
import io.github.gyai.projects.client.ui.widget.ProjectSCard;
import io.github.gyai.projects.client.ui.widget.ProjectSTextField;
import io.github.gyai.projects.client.ui.widget.ProjectSToast;

public final class MobEditorScreen extends ProjectSThemedScreen {
    private enum Tab { BASIC, STATS, AI, ABILITIES, APPEARANCE, TEST }
    private enum View { FRONT, BACK, LEFT, RIGHT, FREE, HEAD }

    private static final int ABILITY_ASSIGNED_PAGE_SIZE = 5;
    private static final int ABILITY_AVAILABLE_PAGE_SIZE = 4;

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
    private int mobPage;
    private int mobListOffset;
    private String headQuery = "";
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
        layout = MobEditorLayout.of(width, height);
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
        search.setResponder(value -> {
            searchQuery = value;
            refreshWidgets();
        });
        button(mobList.right() - 58, mobList.y() + 28, "検索", () -> {
            requestMobPage(0);
        }, 54);
        int sideActionY = MobListPanel.actionY(mobList);
        int createY = MobListPanel.createY(mobList);
        newId = addRenderableWidget(new ProjectSTextField(
                font, sideX, createY, mobList.width() - 66, 20,
                Component.empty(), Component.literal("新規ID")));
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
        String query = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        List<MobEditorStatePayload.MobSummary> filtered =
                MobEditorClientState.state().mobs().stream()
                        .filter(value -> query.isBlank()
                                || value.id().toLowerCase(Locale.ROOT).contains(query)
                                || value.displayName().toLowerCase(Locale.ROOT).contains(query)
                                || value.tags().stream().anyMatch(tag ->
                                tag.toLowerCase(Locale.ROOT).contains(query)))
                        .toList();
        int visible = visibleMobCount();
        mobListOffset = Math.min(mobListOffset,
                Math.max(0, filtered.size() - visible));
        List<MobEditorStatePayload.MobSummary> mobs = filtered.stream()
                .skip(mobListOffset).limit(visible).toList();
        int y = MobListPanel.listViewport(layout.mobList()).y();
        for (var mob : mobs) {
            String marker = mob.enabled() ? "" : "[無効] ";
            MobEditorLayout.Bounds rowBounds = MobListPanel.rowBounds(layout.mobList(), y);
            addRenderableWidget(MobListPanel.row(rowBounds,
                    marker + "[" + mob.category() + "] " + mob.displayName() + " (" + mob.id() + ")",
                    true, draft != null && draft.id().equals(mob.id()), () -> selectMob(mob.id())));
            y += MobListPanel.rowStride();
        }
    }

    private int visibleMobCount() {
        return Math.max(1, MobListPanel.listViewport(layout.mobList()).height() / MobListPanel.rowStride());
    }

    private void requestMobPage(int requestedPage) {
        if (MobEditorClientState.requestMobs(searchQuery, requestedPage)) {
            mobPage = requestedPage;
            mobListOffset = 0;
        } else {
            localError = "通信中です。応答後にもう一度操作してください";
        }
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
            field("entity", "EntityType", draft.entityType(), x, y + 88, w);
            field("level", "レベル", Integer.toString(draft.level()), x, y + 132, w);
            field("tags", "タグ（,区切り）", String.join(",", draft.tags()), x, y + 176, w);
            addPropertyButton(x, y + 220, w, "カテゴリ: " + draft.category(), this::cycleCategory);
            addPropertyButton(x, y + 244, w, "有効: " + (draft.enabled() ? "ON" : "OFF"), this::toggleEnabled);
            addPropertyButton(x, y + 268, w, "ネームプレート: " + draft.nameplate(), this::cycleNameplate);
            return;
        }
        field("id", "内部ID（固定）", draft.id(), x, y, 174).setEditable(false);
        field("display", "表示名", draft.displayName(), x + 190, y, 174);
        field("entity", "EntityType", draft.entityType(), x, y + 44, 174);
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
        addPropertyButton(x + 276, y + 16, 88, appearance.glowingColor(), () -> {
                    collectCurrentTab();
                    MobEditorData.Appearance current = draft.appearance();
                    draft = withAppearance(draft, new MobEditorData.Appearance(
                            current.scale(), current.age(), current.glowing(),
                            nextGlowColor(current.glowingColor()), current.variants(),
                            current.equipment()));
                    dirty = true;
                    rebuild();
                });
        List<String> variantKeys = variantKeys(draft.entityType());
        for (int index = 0; index < variantKeys.size(); index++) {
            String key = variantKeys.get(index);
            field("variant_" + key, key,
                    appearance.variants().getOrDefault(key,
                            defaultVariant(draft.entityType(), key)),
                    x + index * 126, y + 44, 112);
        }
        boolean equipmentSupported = supportsEquipment(draft.entityType());
        int row = 0;
        for (MobEditorData.Slot slot : MobEditorData.Slot.values()) {
            MobEditorData.Equipment entry = appearance.equipment().get(slot);
            equipmentSources.put(slot, entry.source());
            int rowY = y + 88 + row * 24;
            ProjectSButton sourceButton = addPropertyButton(x, rowY, 132,
                    (slot == selectedEquipmentSlot ? "▶" : "") + shortSlot(slot) + ":" + entry.source(), () -> {
                        collectCurrentTab();
                        selectedEquipmentSlot = slot;
                        equipmentSources.put(slot, next(equipmentSources.get(slot)));
                        dirty = true;
                        rebuild();
                    });
            sourceButton.active = equipmentSupported;
            String value = entry.source() == MobEditorData.EquipmentSource.VANILLA_ITEM
                    ? entry.material() : entry.referenceId();
            field("eq_" + slot.name(), "", value, x + 138, rowY, 226)
                    .setEditable(equipmentSupported
                            && entry.source() != MobEditorData.EquipmentSource.NONE);
            row++;
        }
        MobEditorData.Equipment selected = appearance.equipment().get(selectedEquipmentSlot);
        addPropertyButton(x, y + 238, 86, "選択:" + shortSlot(selectedEquipmentSlot), () -> {
                    selectedEquipmentSlot = next(selectedEquipmentSlot);
                    rebuild();
                });
        ProjectSButton glintButton = addPropertyButton(x + 92, y + 238, 82,
                "Glint:" + (selected.glint() ? "ON" : "OFF"), () -> {
                    toggleSelectedEquipment(true);
                });
        ProjectSButton visibleButton = addPropertyButton(x + 180, y + 238, 82,
                "表示:" + (selected.visible() ? "ON" : "OFF"), () -> {
                    toggleSelectedEquipment(false);
                });
        glintButton.active = equipmentSupported;
        visibleButton.active = equipmentSupported;
        field("eq_color", "革防具色 #RRGGBB", selected.color(),
                x + 268, y + 224, 96).setEditable(equipmentSupported);

        EditBox headSearch = field("head_search", "ヘッド検索/タグ", headQuery,
                x, y + 268, 174);
        headSearch.setResponder(value -> headQuery = value);
        button(x + 180, y + 282, "検索", () -> {
            requestHeadPage(0);
        }, 54);
        button(x + 240, y + 282, "前", () -> {
            requestHeadPage(Math.max(0, headPage - 1));
        }, 54);
        button(x + 300, y + 282, "次", () -> {
            requestHeadPage(headPage + 1);
        }, 54);
        addPropertyButton(x, y + 334, 86, "ヘッド登録", () ->
                minecraft.setScreen(new HeadImportScreen(this)));
        ProjectSButton favoriteButton = addPropertyButton(x + 92, y + 334, 104,
                "お気に入り切替", () -> MobEditorClientState.updateHeadFavorite(
                        MobEditorClientState.state().headDetail()));
        favoriteButton.active = MobEditorClientState.state().headDetail() != null;
        int headX = x;
        for (var head : MobEditorClientState.state().heads().stream()
                .sorted(java.util.Comparator.comparing(
                        (MobEditorStatePayload.HeadSummary value) ->
                                recentHeads.contains(value.id())).reversed())
                .limit(4).toList()) {
            addPropertyButton(headX, y + 308, 88,
                    head.favorite() ? "★" + head.displayName() : head.displayName(), () -> {
                        setHeadReference(head.id());
                        MobEditorClientState.requestHead(head.id(), headQuery, headPage);
                    });
            headX += 92;
        }
    }

    private void requestHeadPage(int requestedPage) {
        if (MobEditorClientState.requestHeads(headQuery, requestedPage)) {
            headPage = requestedPage;
        } else {
            localError = "通信中です。応答後にもう一度操作してください";
        }
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
        addRenderableWidget(MobEditorActionBar.action(bounds, 0, 4,
                "元に戻す", ProjectSButton.Kind.GHOST, this::undo));
        addRenderableWidget(MobEditorActionBar.action(bounds, 1, 4,
                "検証", ProjectSButton.Kind.SECONDARY, this::validateDraft));
        addRenderableWidget(MobEditorActionBar.action(bounds, 2, 4,
                "保存", ProjectSButton.Kind.PRIMARY, this::saveDraft));
        addRenderableWidget(MobEditorActionBar.action(bounds, 3, 4,
                "適用", ProjectSButton.Kind.PRIMARY, this::applyDefinition));
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
        addPropertyButton(color.x(), color.y(), color.width(), appearance.glowingColor(), () -> {
            collectCurrentTab();
            MobEditorData.Appearance current = draft.appearance();
            draft = withAppearance(draft, new MobEditorData.Appearance(current.scale(), current.age(), current.glowing(),
                    nextGlowColor(current.glowingColor()), current.variants(), current.equipment()));
            dirty = true; rebuild();
        });
        int row = 2;
        for (String key : variantKeys(draft.entityType())) {
            MobEditorLayout.Bounds bounds = MobPropertyPanel.compactRow(panel, row++);
            field("variant_" + key, key, appearance.variants().getOrDefault(key,
                    defaultVariant(draft.entityType(), key)), bounds.x(), bounds.y(), bounds.width());
        }
        boolean equipmentSupported = supportsEquipment(draft.entityType());
        for (MobEditorData.Slot slot : MobEditorData.Slot.values()) {
            MobEditorData.Equipment entry = appearance.equipment().get(slot);
            equipmentSources.put(slot, entry.source());
            MobEditorLayout.Bounds source = MobPropertyPanel.compactColumn(panel, row, 0, 2);
            MobEditorLayout.Bounds value = MobPropertyPanel.compactColumn(panel, row, 1, 2);
            ProjectSButton sourceButton = addPropertyButton(source.x(), source.y(), source.width(),
                    (slot == selectedEquipmentSlot ? "▶" : "") + shortSlot(slot) + ":" + entry.source(), () -> {
                collectCurrentTab(); selectedEquipmentSlot = slot;
                equipmentSources.put(slot, next(equipmentSources.get(slot))); dirty = true; rebuild();
            });
            sourceButton.active = equipmentSupported;
            String equipmentValue = entry.source() == MobEditorData.EquipmentSource.VANILLA_ITEM
                    ? entry.material() : entry.referenceId();
            field("eq_" + slot.name(), "", equipmentValue, value.x(), value.y(), value.width())
                    .setEditable(equipmentSupported && entry.source() != MobEditorData.EquipmentSource.NONE);
            row++;
        }
        MobEditorData.Equipment selected = appearance.equipment().get(selectedEquipmentSlot);
        for (int column = 0; column < 3; column++) {
            MobEditorLayout.Bounds bounds = MobPropertyPanel.compactColumn(panel, row, column, 3);
            if (column == 0) addPropertyButton(bounds.x(), bounds.y(), bounds.width(), "選択:" + shortSlot(selectedEquipmentSlot), () -> { selectedEquipmentSlot = next(selectedEquipmentSlot); rebuild(); });
            if (column == 1) { ProjectSButton button = addPropertyButton(bounds.x(), bounds.y(), bounds.width(), "Glint:" + (selected.glint() ? "ON" : "OFF"), () -> toggleSelectedEquipment(true)); button.active = equipmentSupported; }
            if (column == 2) { ProjectSButton button = addPropertyButton(bounds.x(), bounds.y(), bounds.width(), "表示:" + (selected.visible() ? "ON" : "OFF"), () -> toggleSelectedEquipment(false)); button.active = equipmentSupported; }
        }
        MobEditorLayout.Bounds equipmentColor = MobPropertyPanel.compactRow(panel, ++row);
        field("eq_color", "革防具色 #RRGGBB", selected.color(), equipmentColor.x(), equipmentColor.y(), equipmentColor.width()).setEditable(equipmentSupported);
        MobEditorLayout.Bounds searchBounds = MobPropertyPanel.compactRow(panel, ++row);
        EditBox headSearch = field("head_search", "ヘッド検索/タグ", headQuery, searchBounds.x(), searchBounds.y(), searchBounds.width());
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
        int headRow = ++row;
        int column = 0;
        for (var head : MobEditorClientState.state().heads().stream().limit(4).toList()) {
            MobEditorLayout.Bounds bounds = MobPropertyPanel.compactColumn(panel, headRow + column / 2, column % 2, 2);
            addPropertyButton(bounds.x(), bounds.y(), bounds.width(), head.favorite() ? "★" + head.displayName() : head.displayName(), () -> {
                setHeadReference(head.id()); MobEditorClientState.requestHead(head.id(), headQuery, headPage);
            });
            column++;
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
        previewButton(0, "リセット", this::resetPreview);
        previewButton(1, "視点", () -> {
            view = next(view);
            applyView();
        });
        previewButton(2, "背景", () -> darkBackground = !darkBackground);
        previewButton(3, "Grid", () -> grid = !grid);
        previewButton(4, "Hit", () -> hitbox = !hitbox);
        previewButton(5, "Eye", () -> eyeLine = !eyeLine);
        previewButton(6, "Anim", () -> {
            animation = next(animation);
            preview.setAnimation(animation);
        });
    }

    private void previewButton(int index, String label, Runnable action) {
        MobEditorLayout.Bounds bounds = MobPreviewPanel.controlBounds(layout.preview(), index, 7);
        button(bounds.x(), bounds.y(), label, action, bounds.width());
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
                    draft = copyBasic(
                            draft, text("display"), entityType,
                            draft.category(), draft.enabled(), integer("level"),
                            draft.nameplate(), Arrays.stream(text("tags").split(","))
                            .map(String::trim).filter(value -> !value.isBlank()).toList());
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
                        MobEditorData.EquipmentSource source = equipmentSources.get(slot);
                        String value = text("eq_" + slot.name());
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
                        variants.put(key, text("variant_" + key));
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
        }
    }

    private void syncState() {
        var state = MobEditorClientState.state();
        syncedStateRevision = MobEditorClientState.localRevision();
        var v2 = MobEditorClientState.v2State();
        if (v2 == null) {
            // A v1 response has no v2 assignment authority.
            abilities.replace(List.of(), List.of());
            abilityUndoBaseline.clear();
            assignedAbilityOffset = 0;
            availableAbilityOffset = 0;
        }
        boolean duplicateSucceeded = duplicateCorrelation.consumeSuccessful(state.supported(),
                state.permitted(), state.success(), state.revisionConflict(), state.message(),
                state.detail() == null ? null : state.detail().id());
        if (!duplicateCorrelation.pending() && !duplicateSucceeded) duplicateTemplate = null;
        if (state.detail() != null) {
            if (duplicateSucceeded && duplicateTemplate != null) {
                draft = copyId(duplicateTemplate, state.detail().id());
                original = state.detail();
                abilityUndoBaseline.capture(v2, original.id());
                dirty = true;
                duplicateTemplate = null;
                MobEditorClientState.validate(draft);
            } else {
                MobEditorData.Mob incoming = state.detail();
                boolean selectionChanged = draft == null
                        || !draft.id().equals(incoming.id());
                boolean sameDraft = draft != null
                        && draft.id().equals(incoming.id())
                        && draft.revision() == incoming.revision();
                boolean preserveDirtyDraft = dirty && sameDraft && !state.revisionConflict();
                if (!preserveDirtyDraft) {
                    draft = incoming;
                    if (state.message().contains("保存しました") || original == null
                            || !original.id().equals(draft.id())
                            || original.revision() != draft.revision()) {
                        original = draft;
                        dirty = false;
                        replaceAuthoritativeAbilities(v2);
                    } else if (v2 != null && v2.detail() != null) {
                        replaceAuthoritativeAbilities(v2);
                    }
                    if (appliedEntityType.isBlank() || selectionChanged) {
                        appliedEntityType = draft.entityType();
                    }
                }
            }
            if (state.success() && state.message().contains("体へ適用しました")) {
                appliedEntityType = draft.entityType();
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
    }

    private void replaceAuthoritativeAbilities(MobEditorV2StatePayload.State v2) {
        if (v2 == null || !v2.supported() || !v2.permitted() || v2.detail() == null || draft == null
                || !v2.detail().base().id().equals(draft.id())) return;
        abilityUndoBaseline.capture(v2, draft.id());
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
        draft = null;
        original = null;
        dirty = false;
    }

    private void duplicate() {
        if (draft == null) return;
        collectCurrentTab();
        duplicateTemplate = draft;
        String duplicateId = draft.id() + "_copy";
        duplicateCorrelation.begin(duplicateId);
        if (!MobEditorClientState.create(duplicateId)) {
            clearPendingDuplicate();
            localError = "通信中です。応答後にもう一度操作してください";
        }
    }

    private void createDraft(String id) {
        clearPendingDuplicate();
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
        collectCurrentTab();
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
            if (MobEditorClientState.abilityAuthoringAvailable()) MobEditorClientState.saveAbilities(draft, abilities.assigned());
            else MobEditorClientState.save(draft);
        }
    }

    private void applyDefinition() {
        collectCurrentTab();
        if (draft == null || !localError.isBlank()) return;
        if (dirty) {
            localError = "適用前にDraftを保存してください";
            return;
        }
        if (!appliedEntityType.isBlank()
                && !appliedEntityType.equals(draft.entityType())) {
            confirm("EntityType変更を適用しますか？", "既存個体は同じ位置で再生成されます", "適用",
                    MobEditorClientState::apply);
            return;
        }
        MobEditorClientState.apply();
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
        if (!MobEditorClientState.reload()) {
            localError = "通信中です。応答後にもう一度操作してください";
            return;
        }
        draft = null;
        original = null;
        dirty = false;
    }

    private void testSpawn(boolean cursor) {
        collectCurrentTab();
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
        LivingEntity entity = draft == null ? null
                : preview.update(draft, MobEditorClientState.state().headDetail());
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
            graphics.text(font, font.plainSubstrByWidth(localError, layout.property().width() - 8),
                    x, y, tokens.danger(), false);
            return;
        }
        String message = MobEditorClientState.state().message();
        if (message.isBlank()) return;
        graphics.text(font, font.plainSubstrByWidth(message, layout.property().width() - 8),
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
        return MobPreviewPanel.contains(layout.preview(), x, y);
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
            case APPEARANCE, BASIC, STATS, AI -> MobPropertyPanel.contentHeight(tab.name(), compactProperty());
            case TEST -> compactProperty() ? 228 : MobPropertyPanel.contentHeight(tab.name(), false);
            case ABILITIES -> AbilityAssignmentPanel.contentHeight(layout.property().width(),
                    Math.min(ABILITY_ASSIGNED_PAGE_SIZE, abilities.assigned().size()),
                    Math.min(ABILITY_AVAILABLE_PAGE_SIZE, abilities.available().size()));
        };
    }

    private String text(String key) {
        EditBox value = fields.get(key);
        return value == null ? "" : value.getValue().trim();
    }

    private double decimal(String key) {
        double value = Double.parseDouble(text(key));
        if (!Double.isFinite(value)) throw new NumberFormatException();
        return value;
    }

    private int integer(String key) {
        return Integer.parseInt(text(key));
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
        String value = entityType.toUpperCase(Locale.ROOT);
        return value.contains("ZOMBIE") || value.contains("SKELETON")
                || java.util.Set.of("HUSK", "DROWNED", "PIGLIN", "PIGLIN_BRUTE",
                "ZOMBIFIED_PIGLIN", "STRAY", "BOGGED", "VILLAGER",
                "VINDICATOR", "PILLAGER", "WITCH").contains(value);
    }

    private static List<String> variantKeys(String entityType) {
        return switch (entityType.toUpperCase(Locale.ROOT)) {
            case "SLIME" -> List.of("size");
            case "SHEEP" -> List.of("color", "sheared");
            case "WOLF" -> List.of("variant", "collar-color", "angry");
            case "CAT" -> List.of("variant", "collar-color");
            case "HORSE" -> List.of("color");
            case "VILLAGER" -> List.of("profession", "villager-type");
            default -> List.of();
        };
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
