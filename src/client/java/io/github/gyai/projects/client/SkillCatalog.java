package io.github.gyai.projects.client;

import java.util.List;

public final class SkillCatalog {
    public static final Group WARRIOR = new Group(
            "ウォーリアー",
            "闘気を高めて戦う近接クラス ・ 闘気 100",
            0xFFE0A33A,
            List.of(
                    new Skill(
                            "spin_slash", "Q / 右クリック", "回転斬り", "Q候補",
                            "闘気消費なし / CD 8秒 / 半径3",
                            "周囲の敵へ11＋攻撃力×1.2のダメージ。"
                                    + "異なる敵1体につき闘気を1獲得する。",
                            false
                    ),
                    new Skill(
                            "sweeping_slash", "Q", "薙ぎ払い", "Q候補",
                            "闘気消費なし / CD 6秒 / 射程4.5 / 角度100°",
                            "前方の扇状範囲へ14＋攻撃力×1.4のダメージ。",
                            false
                    ),
                    new Skill(
                            "warrior_charge", "E", "猛進", "E候補",
                            "闘気消費なし / CD 10秒 / 最大7ブロック",
                            "地上・空中を問わず視線方向へ軌跡を描いてダッシュし、通過した敵へ"
                                    + "8＋攻撃力×0.8のダメージ。",
                            false
                    ),
                    new Skill(
                            "execution_leap", "E", "処刑跳躍", "E候補",
                            "闘気消費なし / CD 9秒 / 射程10",
                            "照準中の敵の近くへ跳躍し12＋攻撃力のダメージ。"
                                    + "撃破時はクールダウンを解消。",
                            false
                    ),
                    new Skill(
                            "earth_shatter", "E", "大地砕き", "E候補",
                            "闘気消費なし / CD 12秒 / 半径4",
                            "周囲へ10＋攻撃力×0.9のダメージを与え、"
                                    + "Mobを減速・打ち上げする。",
                            false
                    ),
                    new Skill(
                            "indomitable_spirit", "R", "不屈の闘志", "R候補",
                            "闘気消費なし / CD 20秒 / 持続5秒",
                            "被ダメージを25%軽減し、攻撃速度を25%上昇。"
                                    + "命中時の闘気獲得が2になる。",
                            false
                    ),
                    new Skill(
                            "battlefield_aura", "R", "戦場の覇気", "R候補",
                            "闘気消費なし / CD 18秒 / 半径5",
                            "周囲のMobを減速し、敵1体につき吸収体力2、"
                                    + "最大12を6秒間得る。",
                            false
                    ),
                    new Skill(
                            "endure", "R", "耐え抜く", "R候補",
                            "闘気消費なし / CD 24秒 / 持続5秒",
                            "受けるダメージの40%を終了時まで保留。"
                                    + "与ダメージの50%分だけ保留量を減らす。",
                            false
                    ),
                    new Skill(
                            "fighting_spirit_release", "F", "闘気解放", "F候補",
                            "闘気20以上 / 全闘気消費 / CD 35秒 / 半径5",
                            "周囲へ10＋攻撃力＋消費闘気×0.25のダメージ。",
                            false
                    ),
                    new Skill(
                            "blood_battle", "F", "血戦", "F候補",
                            "闘気20以上 / 全闘気消費 / CD 40秒",
                            "攻撃速度上昇、通常攻撃の50%範囲追撃、"
                                    + "命中ごとに装備中EのCDを0.5秒短縮。",
                            false
                    ),
                    new Skill(
                            "end_war_strike", "F", "終戦の一撃", "F候補",
                            "闘気20以上 / 全闘気消費 / CD 45秒 / 詠唱0.8秒",
                            "前方広範囲へ20＋攻撃力×2＋消費闘気×0.35。"
                                    + "闘気100時は自身の減少HP率で威力上昇。",
                            false
                    ),
                    new Skill(
                            "warrior_passive", "PASSIVE", "闘気", "パッシブ",
                            "最大100 / 戦闘後10秒維持 / 毎秒5減少",
                            "闘気1につき与ダメージが0.1%増加。"
                                    + "闘気100中は有効な敵への1ヒットごとに体力を1.0回復する。",
                            false
                    )
            )
    );

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
                    )
            )
    );

    public static final List<Group> GROUPS = List.of(WARRIOR, SCOUT, COMMON);

    private SkillCatalog() {
    }

    public static Skill findHudSkill(
            String className,
            String skillId,
            int slotIndex,
            String slotName
    ) {
        Skill identified = findById(skillId);
        if (identified != null) return identified;
        if (WARRIOR.name().equals(className) && slotIndex >= 0 && slotIndex < 4) {
            return null;
        }
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

    public static Skill findById(String skillId) {
        if (skillId == null || skillId.isBlank()) return null;
        return GROUPS.stream()
                .flatMap(group -> group.skills().stream())
                .filter(skill -> skill.id().equals(skillId))
                .findFirst()
                .orElse(null);
    }

    public record Group(
            String name,
            String description,
            int accentColor,
            List<Skill> skills
    ) {
    }

    public record Skill(
            String id,
            String input,
            String name,
            String category,
            String stats,
            String description,
            boolean unavailable
    ) {
        public Skill(
                String input,
                String name,
                String category,
                String stats,
                String description,
                boolean unavailable
        ) {
            this("", input, name, category, stats, description, unavailable);
        }
    }
}
