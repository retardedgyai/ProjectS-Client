package io.github.gyai.projects.client.ui.icon;

/**
 * Canonical ProjectS icon registry.
 *
 * <p>Future atlases use transparent 16x16 and 32x32 cells, grayscale artwork,
 * one or two pixel strokes, one-to-two pixel padding, no text and no baked glow.
 * The 32px artwork should retain the same silhouette but may refine its detail
 * instead of being a mechanical 2x scale.</p>
 */
public enum ProjectSIcon {
    ADD(ProjectSIconCategory.COMMON, "追加", "add", ProjectSIconFallback.PLUS, "項目を追加"),
    REMOVE(ProjectSIconCategory.COMMON, "削除", "remove", ProjectSIconFallback.MINUS, "項目を減らす"),
    EDIT(ProjectSIconCategory.COMMON, "編集", "edit", ProjectSIconFallback.PENCIL, "内容を編集"),
    COPY(ProjectSIconCategory.COMMON, "複製", "copy", ProjectSIconFallback.COPY, "内容を複製"),
    DELETE(ProjectSIconCategory.COMMON, "削除", "delete", ProjectSIconFallback.TRASH, "完全に削除"),
    SAVE(ProjectSIconCategory.COMMON, "保存", "save", ProjectSIconFallback.FLOPPY, "変更を保存"),
    APPLY(ProjectSIconCategory.COMMON, "適用", "apply", ProjectSIconFallback.CHECK, "変更を適用"),
    RELOAD(ProjectSIconCategory.COMMON, "再読込", "reload", ProjectSIconFallback.RELOAD, "データを再読込"),
    UPLOAD(ProjectSIconCategory.COMMON, "アップロード", "upload", ProjectSIconFallback.UPLOAD, "データを送信"),
    DOWNLOAD(ProjectSIconCategory.COMMON, "ダウンロード", "download", ProjectSIconFallback.DOWNLOAD, "データを取得"),
    UNDO(ProjectSIconCategory.COMMON, "元に戻す", "undo", ProjectSIconFallback.UNDO, "直前の変更を戻す"),
    REDO(ProjectSIconCategory.COMMON, "やり直す", "redo", ProjectSIconFallback.REDO, "変更をやり直す"),

    AI(ProjectSIconCategory.EDITOR, "AI", "ai", ProjectSIconFallback.NODES, "AI支援設定"),
    STATS(ProjectSIconCategory.EDITOR, "数値", "stats", ProjectSIconFallback.BARS, "数値設定"),
    APPEARANCE(ProjectSIconCategory.EDITOR, "外見", "appearance", ProjectSIconFallback.PALETTE, "外見設定"),
    TEST(ProjectSIconCategory.EDITOR, "テスト", "test", ProjectSIconFallback.FLASK, "動作テスト"),
    SCRIPT(ProjectSIconCategory.EDITOR, "スクリプト", "script", ProjectSIconFallback.SCRIPT, "スクリプト設定"),
    VARIABLE(ProjectSIconCategory.EDITOR, "変数", "variable", ProjectSIconFallback.VARIABLE, "変数設定"),
    CONDITION(ProjectSIconCategory.EDITOR, "条件", "condition", ProjectSIconFallback.CONDITION, "条件設定"),
    EVENT(ProjectSIconCategory.EDITOR, "イベント", "event", ProjectSIconFallback.EVENT, "イベント設定"),
    BEHAVIOR(ProjectSIconCategory.EDITOR, "行動", "behavior", ProjectSIconFallback.BEHAVIOR, "行動設定"),
    GOAL(ProjectSIconCategory.EDITOR, "目標", "goal", ProjectSIconFallback.GOAL, "AI目標設定"),
    DROPS(ProjectSIconCategory.EDITOR, "ドロップ", "drops", ProjectSIconFallback.DROPS, "ドロップ設定"),
    SPAWN(ProjectSIconCategory.EDITOR, "スポーン", "spawn", ProjectSIconFallback.SPAWN, "スポーン設定"),
    EQUIPMENT(ProjectSIconCategory.EDITOR, "装備", "equipment", ProjectSIconFallback.EQUIPMENT, "装備設定"),
    HEAD(ProjectSIconCategory.EDITOR, "頭部", "head", ProjectSIconFallback.HEAD, "頭部設定"),

    SEARCH(ProjectSIconCategory.UI, "検索", "search", ProjectSIconFallback.SEARCH, "項目を検索"),
    FILTER(ProjectSIconCategory.UI, "絞り込み", "filter", ProjectSIconFallback.FILTER, "項目を絞り込む"),
    SORT(ProjectSIconCategory.UI, "並べ替え", "sort", ProjectSIconFallback.SORT, "項目を並べ替える"),
    SETTINGS(ProjectSIconCategory.UI, "設定", "settings", ProjectSIconFallback.SETTINGS, "設定を開く"),
    CLOSE(ProjectSIconCategory.UI, "閉じる", "close", ProjectSIconFallback.CLOSE, "画面を閉じる"),
    BACK(ProjectSIconCategory.UI, "戻る", "back", ProjectSIconFallback.BACK, "前へ戻る"),
    NEXT(ProjectSIconCategory.UI, "次へ", "next", ProjectSIconFallback.NEXT, "次へ進む"),
    MENU(ProjectSIconCategory.UI, "メニュー", "menu", ProjectSIconFallback.MENU, "メニューを開く"),
    TAB(ProjectSIconCategory.UI, "タブ", "tab", ProjectSIconFallback.TAB, "タブ表示"),
    DROPDOWN(ProjectSIconCategory.UI, "選択肢", "dropdown", ProjectSIconFallback.DROPDOWN, "選択肢を開く"),
    MORE(ProjectSIconCategory.UI, "その他", "more", ProjectSIconFallback.MORE, "その他の操作"),
    EXPAND(ProjectSIconCategory.UI, "展開", "expand", ProjectSIconFallback.EXPAND, "内容を展開"),
    COLLAPSE(ProjectSIconCategory.UI, "折り畳み", "collapse", ProjectSIconFallback.COLLAPSE, "内容を折り畳む"),
    CHECKBOX_CHECKED(ProjectSIconCategory.UI, "選択済み", "checkbox_checked", ProjectSIconFallback.CHECKBOX_CHECKED, "チェック済み"),
    CHECKBOX_EMPTY(ProjectSIconCategory.UI, "未選択", "checkbox_empty", ProjectSIconFallback.CHECKBOX_EMPTY, "未チェック"),

    SUCCESS(ProjectSIconCategory.STATUS, "成功", "success", ProjectSIconFallback.SUCCESS, ProjectSIconColorRole.SUCCESS, "処理成功"),
    WARNING(ProjectSIconCategory.STATUS, "警告", "warning", ProjectSIconFallback.WARNING, ProjectSIconColorRole.WARNING, "注意が必要"),
    ERROR(ProjectSIconCategory.STATUS, "エラー", "error", ProjectSIconFallback.ERROR, ProjectSIconColorRole.DANGER, "処理エラー"),
    INFO(ProjectSIconCategory.STATUS, "情報", "info", ProjectSIconFallback.INFO, ProjectSIconColorRole.INFO, "補足情報"),
    LOCK(ProjectSIconCategory.STATUS, "ロック", "lock", ProjectSIconFallback.LOCK, "利用不可"),
    UNLOCK(ProjectSIconCategory.STATUS, "ロック解除", "unlock", ProjectSIconFallback.UNLOCK, "利用可能"),
    VISIBLE(ProjectSIconCategory.STATUS, "表示", "visible", ProjectSIconFallback.EYE, "表示中"),
    HIDDEN(ProjectSIconCategory.STATUS, "非表示", "hidden", ProjectSIconFallback.EYE_HIDDEN, "非表示中"),
    FAVORITE(ProjectSIconCategory.STATUS, "お気に入り", "favorite", ProjectSIconFallback.STAR, "お気に入り"),
    FAVORITE_FILLED(ProjectSIconCategory.STATUS, "お気に入り済み", "favorite_filled", ProjectSIconFallback.STAR_FILLED, "お気に入り登録済み"),
    PINNED(ProjectSIconCategory.STATUS, "固定", "pinned", ProjectSIconFallback.PIN, "項目を固定"),
    DISABLED(ProjectSIconCategory.STATUS, "無効", "disabled", ProjectSIconFallback.DISABLED, ProjectSIconColorRole.DISABLED, "無効状態"),
    LOADING(ProjectSIconCategory.STATUS, "読込中", "loading", ProjectSIconFallback.SPINNER, ProjectSIconColorRole.DISABLED, "処理中"),

    MOB_GENERIC(ProjectSIconCategory.MOB, "モブ", "mob_generic", ProjectSIconFallback.MOB, "汎用モブ"),
    ZOMBIE(ProjectSIconCategory.MOB, "ゾンビ", "zombie", ProjectSIconFallback.ZOMBIE, "ゾンビ系"),
    SKELETON(ProjectSIconCategory.MOB, "スケルトン", "skeleton", ProjectSIconFallback.SKELETON, "スケルトン系"),
    CREEPER(ProjectSIconCategory.MOB, "クリーパー", "creeper", ProjectSIconFallback.CREEPER, "クリーパー系"),
    SPIDER(ProjectSIconCategory.MOB, "クモ", "spider", ProjectSIconFallback.SPIDER, "クモ系"),
    ENDERMAN(ProjectSIconCategory.MOB, "エンダーマン", "enderman", ProjectSIconFallback.ENDERMAN, "エンダーマン系"),
    VILLAGER(ProjectSIconCategory.MOB, "村人", "villager", ProjectSIconFallback.VILLAGER, "村人系"),
    ANIMAL(ProjectSIconCategory.MOB, "動物", "animal", ProjectSIconFallback.ANIMAL, "動物系"),
    BOSS(ProjectSIconCategory.MOB, "ボス", "boss", ProjectSIconFallback.BOSS, "ボスモブ"),
    ELITE(ProjectSIconCategory.MOB, "エリート", "elite", ProjectSIconFallback.ELITE, "エリートモブ"),
    NORMAL_MOB(ProjectSIconCategory.MOB, "通常モブ", "normal_mob", ProjectSIconFallback.MOB, "通常モブ"),
    HUMANOID(ProjectSIconCategory.MOB, "人型", "humanoid", ProjectSIconFallback.HUMANOID, "人型モブ"),
    UNDEAD(ProjectSIconCategory.MOB, "アンデッド", "undead", ProjectSIconFallback.UNDEAD, "アンデッド系"),

    SWORD(ProjectSIconCategory.COMBAT, "剣", "sword", ProjectSIconFallback.SWORD, "近接武器"),
    SHIELD(ProjectSIconCategory.COMBAT, "盾", "shield", ProjectSIconFallback.SHIELD, "防御"),
    BOW(ProjectSIconCategory.COMBAT, "弓", "bow", ProjectSIconFallback.BOW, "弓武器"),
    ARROW(ProjectSIconCategory.COMBAT, "矢", "arrow", ProjectSIconFallback.ARROW, "矢弾"),
    MAGIC(ProjectSIconCategory.COMBAT, "魔法", "magic", ProjectSIconFallback.MAGIC, "魔法攻撃"),
    HEAL(ProjectSIconCategory.COMBAT, "回復", "heal", ProjectSIconFallback.HEAL, ProjectSIconColorRole.SUCCESS, "体力回復"),
    SPEED(ProjectSIconCategory.COMBAT, "速度", "speed", ProjectSIconFallback.SPEED, "速度効果"),
    ARMOR(ProjectSIconCategory.COMBAT, "防具", "armor", ProjectSIconFallback.ARMOR, "防具値"),
    CRITICAL(ProjectSIconCategory.COMBAT, "クリティカル", "critical", ProjectSIconFallback.CRITICAL, "クリティカル攻撃"),
    DAMAGE(ProjectSIconCategory.COMBAT, "ダメージ", "damage", ProjectSIconFallback.DAMAGE, ProjectSIconColorRole.DANGER, "与ダメージ"),
    PHYSICAL(ProjectSIconCategory.COMBAT, "物理", "physical", ProjectSIconFallback.PHYSICAL, "物理属性"),
    MAGICAL(ProjectSIconCategory.COMBAT, "魔法属性", "magical", ProjectSIconFallback.MAGICAL, "魔法属性"),
    TRUE_DAMAGE(ProjectSIconCategory.COMBAT, "確定ダメージ", "true_damage", ProjectSIconFallback.TRUE_DAMAGE, "確定ダメージ"),
    MELEE(ProjectSIconCategory.COMBAT, "近接", "melee", ProjectSIconFallback.MELEE, "近接攻撃"),
    RANGED(ProjectSIconCategory.COMBAT, "遠隔", "ranged", ProjectSIconFallback.RANGED, "遠隔攻撃"),

    CAMERA(ProjectSIconCategory.PREVIEW, "カメラ", "camera", ProjectSIconFallback.CAMERA, "カメラ操作"),
    GRID(ProjectSIconCategory.PREVIEW, "グリッド", "grid", ProjectSIconFallback.GRID, "グリッド表示"),
    HITBOX(ProjectSIconCategory.PREVIEW, "当たり判定", "hitbox", ProjectSIconFallback.HITBOX, "当たり判定表示"),
    EYE_LINE(ProjectSIconCategory.PREVIEW, "視線", "eye_line", ProjectSIconFallback.EYE_LINE, "視線表示"),
    ROTATE(ProjectSIconCategory.PREVIEW, "回転", "rotate", ProjectSIconFallback.ROTATE, "表示を回転"),
    RESET(ProjectSIconCategory.PREVIEW, "リセット", "reset", ProjectSIconFallback.RESET, "表示を初期化"),
    PLAY(ProjectSIconCategory.PREVIEW, "再生", "play", ProjectSIconFallback.PLAY, "プレビュー再生"),
    PAUSE(ProjectSIconCategory.PREVIEW, "一時停止", "pause", ProjectSIconFallback.PAUSE, "プレビューを一時停止"),
    STOP(ProjectSIconCategory.PREVIEW, "停止", "stop", ProjectSIconFallback.STOP, "プレビュー停止"),
    FRONT_VIEW(ProjectSIconCategory.PREVIEW, "正面", "front_view", ProjectSIconFallback.FRONT_VIEW, "正面表示"),
    BACK_VIEW(ProjectSIconCategory.PREVIEW, "背面", "back_view", ProjectSIconFallback.BACK_VIEW, "背面表示"),
    LEFT_VIEW(ProjectSIconCategory.PREVIEW, "左面", "left_view", ProjectSIconFallback.LEFT_VIEW, "左面表示"),
    RIGHT_VIEW(ProjectSIconCategory.PREVIEW, "右面", "right_view", ProjectSIconFallback.RIGHT_VIEW, "右面表示"),
    HEAD_VIEW(ProjectSIconCategory.PREVIEW, "頭部表示", "head_view", ProjectSIconFallback.HEAD_VIEW, "頭部を表示"),
    LIGHT(ProjectSIconCategory.PREVIEW, "照明", "light", ProjectSIconFallback.LIGHT, "照明設定"),
    BACKGROUND(ProjectSIconCategory.PREVIEW, "背景", "background", ProjectSIconFallback.BACKGROUND, "背景設定"),

    PALETTE(ProjectSIconCategory.MISC, "パレット", "palette", ProjectSIconFallback.PALETTE, "色パレット"),
    CUBE(ProjectSIconCategory.MISC, "立方体", "cube", ProjectSIconFallback.CUBE, "立体表示"),
    LAYERS(ProjectSIconCategory.MISC, "レイヤー", "layers", ProjectSIconFallback.LAYERS, "レイヤー表示"),
    COLOR(ProjectSIconCategory.MISC, "色", "color", ProjectSIconFallback.COLOR, "色設定"),
    TIME(ProjectSIconCategory.MISC, "時間", "time", ProjectSIconFallback.TIME, "時間設定"),
    BOOK(ProjectSIconCategory.MISC, "ブック", "book", ProjectSIconFallback.BOOK, "ドキュメント"),
    HELP(ProjectSIconCategory.MISC, "ヘルプ", "help", ProjectSIconFallback.HELP, "ヘルプ表示"),
    INFO_CIRCLE(ProjectSIconCategory.MISC, "情報表示", "info_circle", ProjectSIconFallback.INFO, ProjectSIconColorRole.INFO, "詳細情報"),
    TAG(ProjectSIconCategory.MISC, "タグ", "tag", ProjectSIconFallback.TAG, "タグ設定"),
    LINK(ProjectSIconCategory.MISC, "リンク", "link", ProjectSIconFallback.LINK, "リンク設定"),
    FOLDER(ProjectSIconCategory.MISC, "フォルダー", "folder", ProjectSIconFallback.FOLDER, "フォルダー"),
    FILE(ProjectSIconCategory.MISC, "ファイル", "file", ProjectSIconFallback.FILE, "ファイル");

    public static final int ATLAS_COLUMNS = 16;
    private static final ProjectSIcon[] REGISTERED = values();
    private static final int ATLAS_ROWS = (REGISTERED.length + ATLAS_COLUMNS - 1)
            / ATLAS_COLUMNS;
    private static final ProjectSIconAtlasRegion[] ATLAS_16 = regions(16);
    private static final ProjectSIconAtlasRegion[] ATLAS_32 = regions(32);

    /** Compatibility aliases; aliases are not duplicate registry entries. */
    @Deprecated public static final ProjectSIcon REFRESH = RELOAD;
    @Deprecated public static final ProjectSIcon MOB = MOB_GENERIC;
    @Deprecated public static final ProjectSIcon SKULL = UNDEAD;

    private final ProjectSIconCategory category;
    private final String displayName;
    private final String debugName;
    private final ProjectSIconFallback fallback;
    private final ProjectSIconColorRole defaultColorRole;
    private final String description;

    ProjectSIcon(
            ProjectSIconCategory category, String displayName, String debugName,
            ProjectSIconFallback fallback, String description
    ) {
        this(category, displayName, debugName, fallback,
                ProjectSIconColorRole.DEFAULT, description);
    }

    ProjectSIcon(
            ProjectSIconCategory category, String displayName, String debugName,
            ProjectSIconFallback fallback, ProjectSIconColorRole defaultColorRole,
            String description
    ) {
        this.category = category;
        this.displayName = displayName;
        this.debugName = debugName;
        this.fallback = fallback;
        this.defaultColorRole = defaultColorRole;
        this.description = description;
    }

    public String id() { return name(); }
    public ProjectSIconCategory category() { return category; }
    public String displayName() { return displayName; }
    public String debugName() { return debugName; }
    public ProjectSIconFallback fallback() { return fallback; }
    public ProjectSIconColorRole defaultColorRole() { return defaultColorRole; }
    public String description() { return description; }
    public boolean atlasSupported() { return true; }
    public ProjectSIconAtlasRegion atlas16() { return ATLAS_16[ordinal()]; }
    public ProjectSIconAtlasRegion atlas32() { return ATLAS_32[ordinal()]; }
    public static int registeredCount() { return REGISTERED.length; }
    public static ProjectSIcon[] registeredValues() { return REGISTERED.clone(); }
    public static int atlasWidth16() { return ATLAS_COLUMNS * 16; }
    public static int atlasHeight16() { return ATLAS_ROWS * 16; }
    public static int atlasWidth32() { return ATLAS_COLUMNS * 32; }
    public static int atlasHeight32() { return ATLAS_ROWS * 32; }

    private static ProjectSIconAtlasRegion[] regions(int cellSize) {
        ProjectSIconAtlasRegion[] regions = new ProjectSIconAtlasRegion[REGISTERED.length];
        for (int index = 0; index < regions.length; index++) {
            regions[index] = ProjectSIconAtlasRegion.cell(
                    index, cellSize, ATLAS_COLUMNS, ATLAS_ROWS);
        }
        return regions;
    }
}
