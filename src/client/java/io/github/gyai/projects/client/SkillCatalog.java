package io.github.gyai.projects.client;

import java.util.List;

public final class SkillCatalog {
    public static final Group SCOUT = new Group(
            "Scout",
            "機動力と連射に優れたレンジドクラス",
            0xFF48C9E8,
            List.of(
                    new Skill(
                            "Q", "Rapid Volley", "アクティブ",
                            "CD 12秒 / 持続 5秒",
                            "攻撃速度が40%上昇し、通常射撃ごとに追加の矢を1本放つ。",
                            false
                    ),
                    new Skill(
                            "E", "開発中のスキル", "アクティブ",
                            "未実装",
                            "このスキル枠は現在開発中です。",
                            true
                    ),
                    new Skill(
                            "R", "Fan Volley", "アクティブ",
                            "CD 8秒 / 10本",
                            "前方54°へ10本の矢を扇状に放つ。各矢は11＋攻撃力×1.8ダメージ。",
                            false
                    ),
                    new Skill(
                            "F", "開発中のアルティメット", "アルティメット",
                            "未実装",
                            "このアルティメット枠は現在開発中です。",
                            true
                    ),
                    new Skill(
                            "右クリック", "Blink", "移動",
                            "CD 3秒 / 最大約4ブロック",
                            "向いている方向と反対へ、障害物に当たるまで瞬間移動する。",
                            false
                    ),
                    new Skill(
                            "Jump×2", "Double Jump", "移動",
                            "クールダウンなし",
                            "空中でもう一度ジャンプし、前上方へ跳躍する。",
                            false
                    ),
                    new Skill(
                            "PASSIVE", "Scout Passive", "パッシブ",
                            "Scoutの矢 3ヒット",
                            "3発目の対象へ、その対象の最大HP10%分の追加ダメージを与える。",
                            false
                    )
            )
    );

    public static final Group PAINTER = new Group(
            "画術師",
            "画題を選び、二段階入力で術を描く魔法職 ・ マナ 400",
            0xFF9D7CFF,
            List.of(
                    new Skill(
                            "Q / E / R", "Subject Selection", "操作",
                            "選択時間 5秒",
                            "Disaster / Serenity / Tormentを選択後、Q・E・Rで術を発動。Fで解除。",
                            false
                    ),
                    new Skill(
                            "Q → Q", "Devastating Fire", "Disaster",
                            "マナ80 / CD10秒共有 / 射程16",
                            "炎弾が爆発し、範囲へ基礎8＋対象最大HP3%のダメージ。",
                            false
                    ),
                    new Skill(
                            "Q → E", "Severing Bolt", "Disaster",
                            "マナ80 / CD10秒共有 / 射程28",
                            "0.7秒後に落雷。孤立・行動妨害・減少HPに応じて威力が上昇。",
                            false
                    ),
                    new Skill(
                            "Q → R", "Molten Fissure", "Disaster",
                            "マナ80 / CD10秒共有 / 持続5秒",
                            "前方に灼熱帯を描き、毎秒3ダメージと移動速度低下IIを与える。",
                            false
                    ),
                    new Skill(
                            "E → Q", "Fleeting Current", "Serenity",
                            "マナ90 / CD12秒共有 / 射程12",
                            "水流の軌跡を描き、持続中の自身へ移動速度上昇IIを付与。",
                            false
                    ),
                    new Skill(
                            "E → E", "Pool of Reflection", "Serenity",
                            "マナ90 / CD12秒共有 / 半径4",
                            "照準地点にプールを作り、範囲内の自身へ衝撃吸収IIを付与。",
                            false
                    ),
                    new Skill(
                            "E → R", "Stirring Lights", "Serenity",
                            "マナ90 / CD12秒共有 / 持続9秒",
                            "光を3つ獲得。攻撃時に1つ消費し、3追加ダメージとマナ15回復。",
                            false
                    ),
                    new Skill(
                            "R → Q", "Grim Visage", "Torment",
                            "マナ50 / CD12秒共有 / 射程16",
                            "魂弾で6ダメージを与え、1.5秒間の恐怖と暗闇を付与。",
                            false
                    ),
                    new Skill(
                            "R → E", "Gaze of the Abyss", "Torment",
                            "マナ50 / CD12秒共有 / 半径5",
                            "深淵の視線に入った最初の敵へ5ダメージと1.5秒の拘束。",
                            false
                    ),
                    new Skill(
                            "R → R", "Crushing Maw", "Torment",
                            "マナ50 / CD12秒共有 / 半径4",
                            "範囲へ7ダメージを与え、中心へ引き寄せて移動速度を低下。",
                            false
                    ),
                    new Skill(
                            "F", "Spiraling Despair", "アルティメット",
                            "マナ100 / CD100秒 / 持続5秒",
                            "敵を中心に領域が拡大。毎秒3ダメージを与え、最後に12ダメージで爆発。",
                            false
                    ),
                    new Skill(
                            "PASSIVE", "Signature of the Visionary", "パッシブ",
                            "異なる術 2種 / 4秒以内",
                            "条件達成の0.6秒後、半径2.5の紋章が爆発して6追加ダメージ。",
                            false
                    )
            )
    );

    public static final Group COMMON = new Group(
            "共通・試作",
            "全クラス共通の操作と、クラス未登録の試作スキル",
            0xFF57D39B,
            List.of(
                    new Skill(
                            "左Shift", "回避", "共通",
                            "CD 4秒",
                            "向いている方向へ素早く踏み込む。",
                            false
                    ),
                    new Skill(
                            "Q / 右クリック", "回転斬り", "試作剣",
                            "闘気30 / 実CD 5.6秒 / 半径3",
                            "周囲の敵へ11＋攻撃力×1.2のダメージを与える。",
                            false
                    )
            )
    );

    public static final List<Group> GROUPS = List.of(SCOUT, PAINTER, COMMON);

    private SkillCatalog() {
    }

    public static Skill findHudSkill(String className, int slotIndex, String slotName) {
        if (SCOUT.name().equals(className) && slotIndex >= 0 && slotIndex < 4) {
            return SCOUT.skills().get(slotIndex);
        }
        if (!PAINTER.name().equals(className)) {
            return null;
        }

        Skill exactMatch = PAINTER.skills().stream()
                .filter(skill -> skill.name().equals(slotName))
                .findFirst()
                .orElse(null);
        if (exactMatch != null) {
            return exactMatch;
        }

        return switch (slotIndex) {
            case 0 -> new Skill(
                    "Q", "Subject: Disaster", "画題選択",
                    "選択時間5秒 / CD10秒共有",
                    "Disasterを選択する。続けてQ・E・Rを押すと破壊系の術を発動。",
                    false
            );
            case 1 -> new Skill(
                    "E", "Subject: Serenity", "画題選択",
                    "選択時間5秒 / CD12秒共有",
                    "Serenityを選択する。続けてQ・E・Rを押すと調和系の術を発動。",
                    false
            );
            case 2 -> new Skill(
                    "R", "Subject: Torment", "画題選択",
                    "選択時間5秒 / CD12秒共有",
                    "Tormentを選択する。続けてQ・E・Rを押すと束縛系の術を発動。",
                    false
            );
            case 3 -> PAINTER.skills().stream()
                    .filter(skill -> skill.name().equals("Spiraling Despair"))
                    .findFirst()
                    .orElse(null);
            default -> null;
        };
    }

    public record Group(
            String name,
            String description,
            int accentColor,
            List<Skill> skills
    ) {
    }

    public record Skill(
            String input,
            String name,
            String category,
            String stats,
            String description,
            boolean unavailable
    ) {
    }
}
