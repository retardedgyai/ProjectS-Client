package io.github.gyai.projects.devtools.skillvfx;

import io.github.gyai.projects.client.vfx.SupportedAppearanceCatalog;
import java.util.*;

/** Frontend-independent authoring defaults and the catalog projection used by DevTools. */
public final class SkillVfxAuthoring {
    public record PrimitiveChoice(SkillVfxModel.PrimitiveType type, String label, String description) { }
    public record AppearanceChoice(SkillVfxModel.Appearance appearance, String label) { }
    private static final Map<SkillVfxModel.PrimitiveType, PrimitiveChoice> PRIMITIVES = Map.ofEntries(
            Map.entry(SkillVfxModel.PrimitiveType.POINT, new PrimitiveChoice(SkillVfxModel.PrimitiveType.POINT,"点","一点から表示する VFX")),
            Map.entry(SkillVfxModel.PrimitiveType.LINE, new PrimitiveChoice(SkillVfxModel.PrimitiveType.LINE,"直線","2点を結ぶ直線状の VFX")),
            Map.entry(SkillVfxModel.PrimitiveType.ARC, new PrimitiveChoice(SkillVfxModel.PrimitiveType.ARC,"円弧","円周の一部に沿う VFX")),
            Map.entry(SkillVfxModel.PrimitiveType.CIRCLE, new PrimitiveChoice(SkillVfxModel.PrimitiveType.CIRCLE,"円","円形に広がる VFX")),
            Map.entry(SkillVfxModel.PrimitiveType.CONE, new PrimitiveChoice(SkillVfxModel.PrimitiveType.CONE,"円錐","前方へ広がる円錐状の VFX")),
            Map.entry(SkillVfxModel.PrimitiveType.SPIRAL, new PrimitiveChoice(SkillVfxModel.PrimitiveType.SPIRAL,"螺旋","回転しながら伸びる螺旋状の VFX")),
            Map.entry(SkillVfxModel.PrimitiveType.SPHERE, new PrimitiveChoice(SkillVfxModel.PrimitiveType.SPHERE,"球","球面状に広がる VFX")),
            Map.entry(SkillVfxModel.PrimitiveType.WAVE, new PrimitiveChoice(SkillVfxModel.PrimitiveType.WAVE,"波","前方へ進む波形の VFX")),
            Map.entry(SkillVfxModel.PrimitiveType.BEZIER, new PrimitiveChoice(SkillVfxModel.PrimitiveType.BEZIER,"ベジェ曲線","制御点を通る曲線状の VFX")),
            Map.entry(SkillVfxModel.PrimitiveType.BURST, new PrimitiveChoice(SkillVfxModel.PrimitiveType.BURST,"放射","中心から放射する VFX")));
    private SkillVfxAuthoring() { }
    public static List<PrimitiveChoice> primitiveChoices() { return Arrays.stream(SkillVfxModel.PrimitiveType.values()).map(PRIMITIVES::get).toList(); }
    public static SkillVfxModel.Primitive safeDefault(String id, SkillVfxModel.PrimitiveType type) { return SkillVfxModel.defaults(id, type); }
    public static List<AppearanceChoice> appearances() {
        ArrayList<AppearanceChoice> out=new ArrayList<>(); out.add(new AppearanceChoice(SkillVfxModel.Appearance.DEBUG_QUAD,"デバッグ表示"));
        SupportedAppearanceCatalog.particleIds().stream().sorted().forEach(id->out.add(new AppearanceChoice(SkillVfxModel.Appearance.particle(id),particleLabel(id))));
        return List.copyOf(out);
    }
    public static String particleLabel(String id) { return switch(id) {
        case "minecraft:ash" -> "灰"; case "minecraft:cloud" -> "雲"; case "minecraft:crit" -> "クリティカル";
        case "minecraft:enchanted_hit" -> "エンチャント攻撃"; case "minecraft:end_rod" -> "エンドロッド";
        case "minecraft:firework" -> "花火"; case "minecraft:flame" -> "炎"; case "minecraft:soul" -> "魂";
        case "minecraft:soul_fire_flame" -> "魂の炎"; default -> id; }; }
    public static Set<String> primitiveIds(SkillVfxModel.Visual visual) { Set<String> ids=new HashSet<>(); for(var h:visual.hooks()) for(var e:h.emissions()) for(var p:e.primitives()) ids.add(p.id()); return ids; }
    public static Set<String> emissionIds(SkillVfxModel.Visual visual) { Set<String> ids=new HashSet<>(); for(var h:visual.hooks()) for(var e:h.emissions()) ids.add(e.id()); return ids; }
}
