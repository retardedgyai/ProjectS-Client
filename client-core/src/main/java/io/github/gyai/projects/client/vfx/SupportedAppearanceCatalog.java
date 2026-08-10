package io.github.gyai.projects.client.vfx;

import java.util.Set;

/** Exact, platform-neutral appearance allow-list shared by transport and dispatch policy. */
public final class SupportedAppearanceCatalog {
    public static final String DEBUG_QUAD = "projects:debug_quad";
    private static final Set<String> PARTICLES = Set.of(
            "minecraft:ash", "minecraft:cloud", "minecraft:crit", "minecraft:enchanted_hit",
            "minecraft:end_rod", "minecraft:firework", "minecraft:flame", "minecraft:soul",
            "minecraft:soul_fire_flame");
    private SupportedAppearanceCatalog() { }
    public static boolean supports(AbilityVfx.AppearanceKind kind, String id) {
        return kind != null && id != null && ((kind == AbilityVfx.AppearanceKind.DEBUG_QUAD && DEBUG_QUAD.equals(id)) || (kind == AbilityVfx.AppearanceKind.PARTICLE && PARTICLES.contains(id)));
    }
    public static boolean isParticle(String id) { return PARTICLES.contains(id); }
    public static Set<String> particleIds() { return PARTICLES; }
}
