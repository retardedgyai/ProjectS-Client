package io.github.gyai.projects.client.vfx;

/** Executes the single shared planner output with per-source and per-spawn fault isolation. */
public final class AbilityVfxParticleDispatchRunner {
    private AbilityVfxParticleDispatchRunner() { }
    public interface Source { AbilityVfxParticlePlanner.Plan plan(int tickUsed); }
    public interface Sink { void spawn(AbilityVfxParticlePlanner.Spawn spawn); }
    /** Failed spawns still reserve their attempted budget, preventing a faulty sink from bypassing caps. */
    public static int run(Iterable<? extends Source> sources,Sink sink,int tickUsed) {
        int used=Math.max(0,tickUsed);if(sources==null||sink==null)return used;
        for(Source source:sources){if(used>=AbilityVfxParticlePolicy.PER_TICK)break;AbilityVfxParticlePlanner.Plan plan;try{plan=source.plan(used);}catch(RuntimeException ignored){continue;}if(plan==null)continue;for(AbilityVfxParticlePlanner.Spawn spawn:plan.spawns()){if(used>=AbilityVfxParticlePolicy.PER_TICK)return used;try{sink.spawn(spawn);}catch(RuntimeException ignored){}used++;}}
        return used;
    }
}
