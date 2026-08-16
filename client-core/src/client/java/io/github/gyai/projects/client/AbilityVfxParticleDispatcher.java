package io.github.gyai.projects.client;

import io.github.gyai.projects.client.vfx.AbilityVfx;
import io.github.gyai.projects.client.vfx.AbilityVfxParticlePolicy;
import io.github.gyai.projects.client.vfx.AbilityVfxParticleTickGate;
import io.github.gyai.projects.client.vfx.AbilityVfxParticlePlanner;
import io.github.gyai.projects.client.vfx.AbilityVfxParticleDispatchRunner;
import io.github.gyai.projects.client.vfx.AbilityVfxStore;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import java.util.*;

/**
 * Tick-owned particle sink for both network cues and DevTools local previews.
 * It intentionally has no render-event registration: a frame can render more than once.
 */
public final class AbilityVfxParticleDispatcher {
    private static final AbilityVfxParticleTickGate GATE=new AbilityVfxParticleTickGate();
    private AbilityVfxParticleDispatcher() { }

    public static void dispatch(Minecraft client, long tick) {
        if (client == null || client.level == null || !GATE.first(tick)) return;
        try {
            List<AbilityVfxParticleDispatchRunner.Source> sources=new ArrayList<>();
            for (AbilityVfxStore.Active active : AbilityVfxClientState.active()) {
                sources.add(used -> AbilityVfxParticlePlanner.plan(active.cue(), primitive -> active.isPrimitiveActive(tick, primitive), primitive -> active.progress(tick, primitive), AbilityVfx.Quality.MEDIUM, client.options.particles().get().ordinal(), used));
            }
            AbilityVfxLocalPreview.preview().ifPresent(preview -> {
                if (preview.playing())sources.add(used -> AbilityVfxParticlePlanner.plan(preview.cue(),preview::isPrimitiveActive,preview::progress,preview.quality(),client.options.particles().get().ordinal(),used));
            });
            AbilityVfxParticleDispatchRunner.run(sources,spawn->{ParticleOptions particle=particle(spawn.appearance().id());if(particle!=null)client.level.addParticle(particle,spawn.at().x(),spawn.at().y(),spawn.at().z(),0,0,0);},0);
        } catch (RuntimeException ignored) {
            // Cosmetic effects must never interfere with the client tick.
        }
    }

    static void reset() { GATE.reset(); }
    /** Explicit switch is the audited 26.1.2 mapping; never resolve arbitrary registry ids. */
    private static ParticleOptions particle(String id) {
        return switch (id) {
            case "minecraft:ash" -> ParticleTypes.ASH;
            case "minecraft:cloud" -> ParticleTypes.CLOUD;
            case "minecraft:crit" -> ParticleTypes.CRIT;
            case "minecraft:enchanted_hit" -> ParticleTypes.ENCHANTED_HIT;
            case "minecraft:end_rod" -> ParticleTypes.END_ROD;
            case "minecraft:firework" -> ParticleTypes.FIREWORK;
            case "minecraft:flame" -> ParticleTypes.FLAME;
            case "minecraft:soul" -> ParticleTypes.SOUL;
            case "minecraft:soul_fire_flame" -> ParticleTypes.SOUL_FIRE_FLAME;
            default -> null;
        };
    }
}
