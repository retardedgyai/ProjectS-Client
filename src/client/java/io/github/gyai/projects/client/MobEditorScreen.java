package io.github.gyai.projects.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
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

public final class MobEditorScreen extends Screen {
    private enum Tab { BASIC, STATS, AI, ABILITIES, APPEARANCE, TEST }
    private enum View { FRONT, BACK, LEFT, RIGHT, FREE, HEAD }

    private static final int LEFT_WIDTH = 190;
    private static final int CENTER_WIDTH = 390;
    private static final int TOP = 34;
    private static final int BOTTOM = 38;
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
        int centerX = LEFT_WIDTH + 14;
        search = addRenderableWidget(new EditBox(
                font, 10, 48, 112, 20,
                Component.literal("検索")));
        search.setValue(searchQuery);
        search.setResponder(value -> {
            searchQuery = value;
            refreshWidgets();
        });
        button(126, 48, "検索", () -> {
            requestMobPage(0);
        }, 54);
        newId = addRenderableWidget(new EditBox(
                font, 10, height - 66, 112, 20,
                Component.literal("新規ID")));
        addRenderableWidget(Button.builder(Component.literal("作成"), button -> {
            String id = newId.getValue().trim();
            if (!id.isBlank()) createDraft(id);
        }).bounds(126, height - 66, 54, 20).build());
        addRenderableWidget(Button.builder(Component.literal("複製"), button -> duplicate())
                .bounds(10, height - 42, 54, 20).build());
        addRenderableWidget(Button.builder(Component.literal("再読込"), button ->
                reloadDefinitions()).bounds(68, height - 42, 54, 20).build());
        addRenderableWidget(Button.builder(Component.literal("戻る"), button -> onClose())
                .bounds(126, height - 42, 54, 20).build());
        button(10, height - 90, "頁◀", () -> {
            requestMobPage(Math.max(0, mobPage - 1));
        }, 38);
        button(50, height - 90, "▲", () -> {
            mobListOffset = Math.max(0, mobListOffset - 1);
            refreshWidgets();
        }, 38);
        button(92, height - 90, "▼", () -> {
            int maximum = Math.max(0,
                    MobEditorClientState.state().mobs().size() - visibleMobCount());
            mobListOffset = Math.min(maximum, mobListOffset + 1);
            refreshWidgets();
        }, 38);
        button(132, height - 90, "頁▶", () -> {
            requestMobPage(mobPage + 1);
        }, 48);

        int tabWidth = 61;
        for (Tab value : Tab.values()) {
            addRenderableWidget(Button.builder(
                    Component.literal(tabName(value)), button -> switchTab(value))
                    .bounds(centerX + value.ordinal() * (tabWidth + 3), 38,
                            tabWidth, 20).build());
        }
        if (draft != null) buildTab(centerX, 70);
        buildBottom(centerX);
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
        int y = 76;
        for (var mob : mobs) {
            String marker = mob.enabled() ? "" : "[無効] ";
            addRenderableWidget(Button.builder(
                    Component.literal(marker + "[" + mob.category() + "] "
                            + mob.displayName() + " (" + mob.id() + ")"), button ->
                            selectMob(mob.id()))
                    .bounds(10, y, LEFT_WIDTH - 20, 22).build());
            y += 25;
        }
    }

    private int visibleMobCount() {
        return Math.max(1, (height - 182) / 25);
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
            addRenderableWidget(Button.builder(Component.literal("v2非対応: Ability編集は利用できません"), b -> { })
                    .bounds(x, y, 360, 20).build()).active = false;
            return;
        }
        clampAbilityOffsets();
        List<String> assigned = abilities.assigned();
        int assignedEnd = Math.min(assigned.size(),
                assignedAbilityOffset + ABILITY_ASSIGNED_PAGE_SIZE);
        int row = 0;
        for (String id : assigned.subList(assignedAbilityOffset, assignedEnd)) {
            MobEditorV2StatePayload.State authority = MobEditorClientState.authoritativeV2State();
            String label = authority.catalog().stream()
                    .filter(entry -> entry.id().equals(id)).findFirst()
                    .map(entry -> entry.displayName() + " (" + id + ")")
                    .orElse("利用不可: " + id);
            int current = row++;
            addRenderableWidget(Button.builder(Component.literal(label), b -> { })
                    .bounds(x, y + current * 25, 250, 20).build()).active = false;
            button(x + 254, y + current * 25, "削除", () -> {
                abilities.remove(id);
                dirty = true;
                clampAbilityOffsets();
                refreshWidgets();
            }, 48);
            button(x + 306, y + current * 25, "↑", () -> {
                abilities.move(id, -1);
                dirty = true;
                refreshWidgets();
            }, 24);
            button(x + 334, y + current * 25, "↓", () -> {
                abilities.move(id, 1);
                dirty = true;
                refreshWidgets();
            }, 24);
        }
        button(x, y + 128, "割当◀", () -> {
            assignedAbilityOffset = Math.max(0,
                    assignedAbilityOffset - ABILITY_ASSIGNED_PAGE_SIZE);
            refreshWidgets();
        }, 56);
        button(x + 60, y + 128, "割当▶", () -> {
            assignedAbilityOffset += ABILITY_ASSIGNED_PAGE_SIZE;
            clampAbilityOffsets();
            refreshWidgets();
        }, 56);
        List<AbilityEditorModel.CatalogItem> available = abilities.available();
        int availableEnd = Math.min(available.size(),
                availableAbilityOffset + ABILITY_AVAILABLE_PAGE_SIZE);
        int addY = y + 155;
        for (AbilityEditorModel.CatalogItem entry : available.subList(
                availableAbilityOffset, availableEnd)) {
            addRenderableWidget(Button.builder(Component.literal("追加: " + entry.displayName() + " (" + entry.id() + ")"), b -> {
                if (abilities.add(entry.id())) {
                    dirty = true;
                    clampAbilityOffsets();
                    refreshWidgets();
                }
            }).bounds(x, addY, 360, 20).build());
            addY += 24;
        }
        button(x, y + 255, "追加◀", () -> {
            availableAbilityOffset = Math.max(0,
                    availableAbilityOffset - ABILITY_AVAILABLE_PAGE_SIZE);
            refreshWidgets();
        }, 56);
        button(x + 60, y + 255, "追加▶", () -> {
            availableAbilityOffset += ABILITY_AVAILABLE_PAGE_SIZE;
            clampAbilityOffsets();
            refreshWidgets();
        }, 56);
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
        field("id", "内部ID（固定）", draft.id(), x, y, 174).setEditable(false);
        field("display", "表示名", draft.displayName(), x + 190, y, 174);
        field("entity", "EntityType", draft.entityType(), x, y + 44, 174);
        field("level", "レベル", Integer.toString(draft.level()), x + 190, y + 44, 174);
        field("tags", "タグ（,区切り）", String.join(",", draft.tags()), x, y + 88, 364);
        addRenderableWidget(Button.builder(
                Component.literal("カテゴリ: " + draft.category()), button -> {
                    collectCurrentTab();
                    draft = copyBasic(draft, draft.displayName(), draft.entityType(),
                            next(draft.category()), draft.enabled(), draft.level(),
                            draft.nameplate(), draft.tags());
                    dirty = true;
                    rebuild();
                }).bounds(x, y + 132, 174, 20).build());
        addRenderableWidget(Button.builder(
                Component.literal("有効: " + (draft.enabled() ? "ON" : "OFF")), button -> {
                    collectCurrentTab();
                    draft = copyBasic(draft, draft.displayName(), draft.entityType(),
                            draft.category(), !draft.enabled(), draft.level(),
                            draft.nameplate(), draft.tags());
                    dirty = true;
                    rebuild();
                }).bounds(x + 190, y + 132, 174, 20).build());
        addRenderableWidget(Button.builder(
                Component.literal("ネームプレート: " + draft.nameplate()), button -> {
                    collectCurrentTab();
                    draft = copyBasic(draft, draft.displayName(), draft.entityType(),
                            draft.category(), draft.enabled(), draft.level(),
                            next(draft.nameplate()), draft.tags());
                    dirty = true;
                    rebuild();
                }).bounds(x, y + 164, 364, 20).build());
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
            int column = index / 5;
            int row = index % 5;
            field(keys[index], names[index], number(values[index]),
                    x + column * 190, y + row * 38, 174);
        }
        MobEditorData.BasicAttack a = draft.attack();
        field("fixed", "固定ダメージ", number(a.fixedDamage()), x, y + 200, 112);
        field("coef", "攻撃力係数", number(a.coefficient()), x + 126, y + 200, 112);
        field("interval", "間隔", number(a.intervalSeconds()), x + 252, y + 200, 112);
        field("range", "距離", number(a.range()), x, y + 238, 112);
        field("knockback", "KB", number(a.knockback()), x + 126, y + 238, 112);
        addRenderableWidget(Button.builder(
                Component.literal("種別: " + a.damageType()), button -> {
                    collectCurrentTab();
                    draft = withAttack(draft, new MobEditorData.BasicAttack(
                            next(a.damageType()), a.fixedDamage(), a.coefficient(),
                            a.intervalSeconds(), a.range(), a.knockback(),
                            a.criticalAllowed()));
                    dirty = true;
                    rebuild();
                }).bounds(x + 252, y + 238, 112, 20).build());
        addRenderableWidget(Button.builder(
                Component.literal("クリティカル:"
                        + (a.criticalAllowed() ? "ON" : "OFF")), button -> {
                    collectCurrentTab();
                    MobEditorData.BasicAttack current = draft.attack();
                    draft = withAttack(draft, new MobEditorData.BasicAttack(
                            current.damageType(), current.fixedDamage(), current.coefficient(),
                            current.intervalSeconds(), current.range(), current.knockback(),
                            !current.criticalAllowed()));
                    dirty = true;
                    rebuild();
                }).bounds(x + 252, y + 270, 112, 20).build());
    }

    private void buildAi(int x, int y) {
        MobEditorData.Ai ai = draft.ai();
        field("aggro", "索敵距離", number(ai.aggroRange()), x, y, 174);
        field("chase", "追跡距離", number(ai.chaseRange()), x + 190, y, 174);
        field("leash", "帰還距離", number(ai.leashRange()), x, y + 44, 174);
        field("airange", "攻撃距離", number(ai.attackRange()), x + 190, y + 44, 174);
        field("refresh", "再検索秒", number(ai.refreshSeconds()), x, y + 88, 174);
        addRenderableWidget(Button.builder(
                Component.literal("プリセット: " + ai.preset()), button -> {
                    collectCurrentTab();
                    draft = withAi(draft, new MobEditorData.Ai(
                            next(ai.preset()), ai.priority(), ai.aggroRange(),
                            ai.chaseRange(), ai.leashRange(), ai.attackRange(),
                            ai.refreshSeconds(), ai.returnHome(), ai.resetHealth(),
                            ai.avoidFalls(), ai.avoidWater()));
                    dirty = true;
                    rebuild();
                }).bounds(x + 190, y + 88, 174, 20).build());
        addRenderableWidget(Button.builder(
                Component.literal("優先: " + ai.priority()), button -> {
                    collectCurrentTab();
                    MobEditorData.Ai current = draft.ai();
                    draft = withAi(draft, new MobEditorData.Ai(
                            current.preset(), next(current.priority()), current.aggroRange(),
                            current.chaseRange(), current.leashRange(), current.attackRange(),
                            current.refreshSeconds(), current.returnHome(), current.resetHealth(),
                            current.avoidFalls(), current.avoidWater()));
                    dirty = true;
                    rebuild();
                }).bounds(x, y + 164, 174, 20).build());
        toggleAi(x, y + 132, "帰還", ai.returnHome(), 0);
        toggleAi(x + 94, y + 132, "帰還回復", ai.resetHealth(), 1);
        toggleAi(x + 188, y + 132, "落下回避", ai.avoidFalls(), 2);
        toggleAi(x + 282, y + 132, "水回避", ai.avoidWater(), 3);
    }

    private void toggleAi(int x, int y, String name, boolean value, int flag) {
        addRenderableWidget(Button.builder(
                Component.literal(name + ":" + (value ? "ON" : "OFF")), button -> {
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
                }).bounds(x, y, 86, 20).build());
    }

    private void buildAppearance(int x, int y) {
        MobEditorData.Appearance appearance = draft.appearance();
        field("scale", "スケール", number(appearance.scale()), x, y, 86);
        Button ageButton = addRenderableWidget(Button.builder(
                Component.literal("年齢: " + appearance.age()), button -> {
                    collectCurrentTab();
                    draft = withAppearance(draft, new MobEditorData.Appearance(
                            appearance.scale(), next(appearance.age()),
                            appearance.glowing(), appearance.glowingColor(),
                            appearance.variants(), appearance.equipment()));
                    dirty = true;
                    rebuild();
                }).bounds(x + 92, y + 16, 86, 20).build());
        ageButton.active = supportsBaby(draft.entityType());
        addRenderableWidget(Button.builder(
                Component.literal("発光:" + (appearance.glowing() ? "ON" : "OFF")), button -> {
                    collectCurrentTab();
                    draft = withAppearance(draft, new MobEditorData.Appearance(
                            appearance.scale(), appearance.age(),
                            !appearance.glowing(), appearance.glowingColor(),
                            appearance.variants(), appearance.equipment()));
                    dirty = true;
                    rebuild();
                }).bounds(x + 184, y + 16, 86, 20).build());
        addRenderableWidget(Button.builder(
                Component.literal(appearance.glowingColor()), button -> {
                    collectCurrentTab();
                    MobEditorData.Appearance current = draft.appearance();
                    draft = withAppearance(draft, new MobEditorData.Appearance(
                            current.scale(), current.age(), current.glowing(),
                            nextGlowColor(current.glowingColor()), current.variants(),
                            current.equipment()));
                    dirty = true;
                    rebuild();
                }).bounds(x + 276, y + 16, 88, 20).build());
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
            Button sourceButton = addRenderableWidget(Button.builder(
                    Component.literal((slot == selectedEquipmentSlot ? "▶" : "")
                            + shortSlot(slot) + ":" + entry.source()), button -> {
                        collectCurrentTab();
                        selectedEquipmentSlot = slot;
                        equipmentSources.put(slot, next(equipmentSources.get(slot)));
                        dirty = true;
                        rebuild();
                    }).bounds(x, rowY, 132, 20).build());
            sourceButton.active = equipmentSupported;
            String value = entry.source() == MobEditorData.EquipmentSource.VANILLA_ITEM
                    ? entry.material() : entry.referenceId();
            field("eq_" + slot.name(), "", value, x + 138, rowY, 226)
                    .setEditable(equipmentSupported
                            && entry.source() != MobEditorData.EquipmentSource.NONE);
            row++;
        }
        MobEditorData.Equipment selected = appearance.equipment().get(selectedEquipmentSlot);
        addRenderableWidget(Button.builder(
                Component.literal("選択:" + shortSlot(selectedEquipmentSlot)), button -> {
                    selectedEquipmentSlot = next(selectedEquipmentSlot);
                    rebuild();
                }).bounds(x, y + 238, 86, 20).build());
        Button glintButton = addRenderableWidget(Button.builder(
                Component.literal("Glint:" + (selected.glint() ? "ON" : "OFF")), button -> {
                    toggleSelectedEquipment(true);
                }).bounds(x + 92, y + 238, 82, 20).build());
        Button visibleButton = addRenderableWidget(Button.builder(
                Component.literal("表示:" + (selected.visible() ? "ON" : "OFF")), button -> {
                    toggleSelectedEquipment(false);
                }).bounds(x + 180, y + 238, 82, 20).build());
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
        addRenderableWidget(Button.builder(
                Component.literal("ヘッド登録"), button ->
                        minecraft.setScreen(new HeadImportScreen(this)))
                .bounds(x, y + 334, 86, 20).build());
        Button favoriteButton = addRenderableWidget(Button.builder(
                Component.literal("お気に入り切替"), button ->
                        MobEditorClientState.updateHeadFavorite(
                                MobEditorClientState.state().headDetail()))
                .bounds(x + 92, y + 334, 104, 20).build());
        favoriteButton.active = MobEditorClientState.state().headDetail() != null;
        int headX = x;
        for (var head : MobEditorClientState.state().heads().stream()
                .sorted(java.util.Comparator.comparing(
                        (MobEditorStatePayload.HeadSummary value) ->
                                recentHeads.contains(value.id())).reversed())
                .limit(4).toList()) {
            addRenderableWidget(Button.builder(
                    Component.literal(head.favorite() ? "★" + head.displayName()
                            : head.displayName()), button -> {
                        setHeadReference(head.id());
                        MobEditorClientState.requestHead(head.id(), headQuery, headPage);
                    }).bounds(headX, y + 308, 88, 20).build());
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
        int y = height - 30;
        button(x, y, "元に戻す", this::undo);
        button(x + 96, y, "検証", this::validateDraft);
        button(x + 192, y, "保存", this::saveDraft);
        button(x + 288, y, "適用", this::applyDefinition);
    }

    private void buildPreviewButtons() {
        int x = previewLeft() + 8;
        int y = height - 30;
        button(x, y, "リセット", this::resetPreview, 72);
        button(x + 76, y, "視点", () -> {
            view = next(view);
            applyView();
        }, 58);
        button(x + 138, y, "背景", () -> darkBackground = !darkBackground, 58);
        button(x + 200, y, "Grid", () -> grid = !grid, 48);
        button(x + 252, y, "Hit", () -> hitbox = !hitbox, 44);
        button(x + 300, y, "Eye", () -> eyeLine = !eyeLine, 44);
        button(x + 348, y, "Anim", () -> {
            animation = next(animation);
            preview.setAnimation(animation);
        }, 52);
    }

    private EditBox field(
            String key,
            String label,
            String value,
            int x,
            int y,
            int width
    ) {
        EditBox box = addRenderableWidget(new EditBox(
                font, x, y + (label.isBlank() ? 0 : 14), width, 20,
                Component.literal(label)));
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
        return box;
    }

    private void button(int x, int y, String label, Runnable action) {
        button(x, y, label, action, 112);
    }

    private void button(int x, int y, String label, Runnable action, int width) {
        addRenderableWidget(Button.builder(Component.literal(label), button -> action.run())
                .bounds(x, y, width, 20).build());
    }

    private void switchTab(Tab value) {
        collectCurrentTab();
        if (!localError.isBlank()) return;
        tab = value;
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
            minecraft.setScreen(new ConfirmScreen(
                    confirmed -> {
                        minecraft.setScreen(this);
                        if (confirmed) discardAndSelect(id);
                    }, Component.literal("未保存の変更を破棄しますか？"),
                    Component.literal("別のMobを選択します")));
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
            minecraft.setScreen(new ConfirmScreen(
                    confirmed -> {
                        minecraft.setScreen(this);
                        if (confirmed) {
                            if (MobEditorClientState.create(id)) {
                                dirty = false;
                            } else {
                                localError = "通信中です。応答後にもう一度操作してください";
                            }
                        }
                    }, Component.literal("未保存の変更を破棄しますか？"),
                    Component.literal("新しいMob Draftを作成します")));
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
            minecraft.setScreen(new ConfirmScreen(
                    confirmed -> {
                        minecraft.setScreen(this);
                        if (confirmed) MobEditorClientState.apply();
                    },
                    Component.literal("EntityType変更を適用しますか？"),
                    Component.literal("既存個体は同じ位置で再生成されます")));
            return;
        }
        MobEditorClientState.apply();
    }

    private void reloadDefinitions() {
        if (dirty) {
            minecraft.setScreen(new ConfirmScreen(
                    confirmed -> {
                        minecraft.setScreen(this);
                        if (confirmed) discardAndReload();
                    }, Component.literal("未保存の変更を破棄しますか？"),
                    Component.literal("サーバーの定義を再読み込みします")));
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
        graphics.fill(0, 0, width, height,
                darkBackground ? 0xF2070A0F : 0xF2232A31);
        graphics.fill(0, 0, LEFT_WIDTH, height, 0xE9151B23);
        graphics.fill(LEFT_WIDTH, 0, LEFT_WIDTH + CENTER_WIDTH, height, 0xE90D131A);
        graphics.outline(LEFT_WIDTH, TOP, CENTER_WIDTH, height - TOP - BOTTOM,
                0xFF405160);
        graphics.text(font, "モブライブラリ", 10, 14, 0xFFF3F7FA, false);
        graphics.text(font, title.getString() + (dirty ? "  ● 未保存" : ""),
                LEFT_WIDTH + 14, 14, dirty ? 0xFFFFA94D : 0xFFF3F7FA, false);
        renderLabels(graphics);
        renderPreview(graphics, mouseX, mouseY, tickProgress);
        renderMessage(graphics);
        if (tab == Tab.AI && draft != null) renderAiSummary(graphics);
        super.extractRenderState(graphics, mouseX, mouseY, tickProgress);
    }

    private void renderLabels(GuiGraphicsExtractor graphics) {
        for (var entry : fields.entrySet()) {
            String label = labels.getOrDefault(entry.getKey(), "");
            if (!label.isBlank()) {
                graphics.text(font, label, entry.getValue().getX(),
                        entry.getValue().getY() - 12, 0xFF91A1AF, false);
            }
        }
    }

    private void renderPreview(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float tickProgress
    ) {
        int left = previewLeft();
        int top = TOP;
        int right = width - 8;
        int bottom = height - BOTTOM;
        graphics.fill(left, top, right, bottom,
                darkBackground ? 0xFF111820 : 0xFF607483);
        graphics.outline(left, top, right - left, bottom - top, 0xFF506273);
        graphics.text(font, "ライブプレビュー  " + view + " / " + animation,
                left + 8, top + 8, 0xFFF3F7FA, false);
        if (grid) {
            for (int x = left + 20; x < right; x += 20) {
                graphics.verticalLine(x, top + 28, bottom - 8, 0x334A6070);
            }
            for (int y = top + 28; y < bottom; y += 20) {
                graphics.horizontalLine(left + 8, right - 8, y, 0x334A6070);
            }
        }
        LivingEntity entity = draft == null ? null
                : preview.update(draft, MobEditorClientState.state().headDetail());
        if (entity != null) {
            int centerX = (left + right) / 2 + (int) previewOffsetX;
            int centerY = (top + bottom) / 2 + 30 + (int) previewOffsetY;
            int size = (int) (view == View.HEAD ? zoom * 2.8 : zoom);
            float verticalOffset = view == View.HEAD
                    ? (float) (entity.getEyeHeight() * size * .45) : 0;
            InventoryScreen.extractEntityInInventoryFollowsMouse(
                    graphics, left + 8, top + 26, right - 8, bottom - 8,
                    size, verticalOffset,
                    (float) (centerX + yaw), (float) (centerY + pitch), entity);
            if (hitbox) {
                int boxWidth = Math.max(18, (int) (entity.getBbWidth() * zoom));
                int boxHeight = Math.max(24, (int) (entity.getBbHeight() * zoom));
                graphics.outline(centerX - boxWidth / 2,
                        centerY - boxHeight, boxWidth, boxHeight, 0xFFFF6B6B);
            }
            if (eyeLine) {
                int eyeY = centerY - (int) (entity.getEyeHeight() * zoom);
                graphics.horizontalLine(left + 8, right - 8, eyeY, 0xFF69D6A5);
            }
            if (draft != null) {
                String metrics = "Scale %.2f / Hit %.2f×%.2f / Eye %.2f / 攻撃距離 %.2f"
                        .formatted(draft.appearance().scale(), entity.getBbWidth(),
                                entity.getBbHeight(), entity.getEyeHeight(),
                                draft.attack().range());
                graphics.text(font, metrics, left + 8, bottom - 18,
                        0xFFB8C4CE, false);
            }
        } else {
            graphics.centeredText(font, "プレビュー可能なLivingEntityを選択してください",
                    (left + right) / 2, (top + bottom) / 2, 0xFFFF6B6B);
        }
    }

    private void renderMessage(GuiGraphicsExtractor graphics) {
        if (!localError.isBlank()) {
            graphics.text(font, font.plainSubstrByWidth(localError, CENTER_WIDTH - 28),
                    LEFT_WIDTH + 14, height - 48, 0xFFFF6B6B, false);
            return;
        }
        String message = MobEditorClientState.state().message();
        if (message.isBlank()) return;
        graphics.text(font, font.plainSubstrByWidth(message, CENTER_WIDTH - 28),
                LEFT_WIDTH + 14, height - 48,
                MobEditorClientState.state().success()
                        ? 0xFF69D6A5 : 0xFFFF6B6B, false);
    }

    private void renderAiSummary(GuiGraphicsExtractor graphics) {
        MobEditorData.Ai ai = draft.ai();
        int x = LEFT_WIDTH + 14;
        int y = 250;
        graphics.text(font, "挙動サマリー", x, y, 0xFFE8B95C, false);
        graphics.text(font, "%.1fブロック以内のプレイヤーを検出します。"
                .formatted(ai.aggroRange()), x, y + 16, 0xFFB8C4CE, false);
        graphics.text(font, "最大%.1fブロックまで追跡します。"
                .formatted(ai.chaseRange()), x, y + 30, 0xFFB8C4CE, false);
        graphics.text(font, "初期位置から%.1fブロックで帰還します。"
                .formatted(ai.leashRange()), x, y + 44, 0xFFB8C4CE, false);
        graphics.text(font, "HIGHEST_THREATは脅威値基盤未実装のため選択不可です。",
                x, y + 62, 0xFFFFA94D, false);
    }

    @Override
    public boolean mouseDragged(
            MouseButtonEvent event,
            double deltaX,
            double deltaY
    ) {
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
        if (insidePreview(mouseX, mouseY)) {
            zoom = Math.clamp(zoom + vertical * 4, 16, 120);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    private boolean insidePreview(double x, double y) {
        return x >= previewLeft() && x <= width - 8
                && y >= TOP && y <= height - BOTTOM;
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

    private int previewLeft() {
        return LEFT_WIDTH + CENTER_WIDTH + 8;
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
            minecraft.setScreen(new ConfirmScreen(
                    confirmed -> {
                        if (confirmed) closeEditor();
                        else minecraft.setScreen(this);
                    }, Component.literal("未保存の変更を破棄しますか？"),
                    Component.literal("Mob Editorを閉じます")));
            return;
        }
        closeEditor();
    }

    private void closeEditor() {
        clearPendingDuplicate();
        MobEditorClientState.close();
        minecraft.setScreen(parent);
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
