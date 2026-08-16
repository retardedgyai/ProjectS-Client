package io.github.gyai.projects.client.vfx;

import java.util.*;

/** Shared pure plan for remote and local particle cues; Minecraft only consumes its output. */
public final class AbilityVfxParticlePlanner {
    private AbilityVfxParticlePlanner() { }
    public interface Active { boolean test(AbilityVfx.Primitive primitive); }
    public interface Progress { double value(AbilityVfx.Primitive primitive); }
    public record Spawn(AbilityVfx.Appearance appearance, AbilityVfx.Vec at) { }
    public record Plan(List<Spawn> spawns, int cueUsed) { public Plan { spawns=List.copyOf(spawns); } }
    public static Plan plan(AbilityVfx.Cue cue, Active active, Progress progress, AbilityVfx.Quality quality, int particleStatus, int tickUsed) {
        if(cue==null||active==null||progress==null||quality==null)return new Plan(List.of(),0);
        List<Spawn> result=new ArrayList<>();int cueUsed=0;
        for(AbilityVfx.Primitive primitive:cue.primitives()) {
            if(primitive.appearance().kind()!=AbilityVfx.AppearanceKind.PARTICLE||!active.test(primitive))continue;
            int requested=AbilityVfxParticlePolicy.density(primitive.type()==AbilityVfx.Type.BURST?primitive.count():primitive.density(),quality,particleStatus);
            int allowed=AbilityVfxParticlePolicy.allow(requested,cueUsed,tickUsed+result.size());if(allowed<=0)break;
            AbilityVfxMotionPlanner.Plan motion =
                    AbilityVfxMotionPlanner.plan(primitive.motion(), progress.value(primitive));
            List<AbilityVfx.Command> commands=AbilityVfx.sample(primitive,cue.frame(),motion,quality);
            for(int index=0;index<Math.min(allowed,commands.size());index++){AbilityVfx.Command command=commands.get(index);result.add(new Spawn(primitive.appearance(),command.b()==null?command.a():command.b()));}
            cueUsed+=Math.min(allowed,commands.size());if(tickUsed+result.size()>=AbilityVfxParticlePolicy.PER_TICK)break;
        }
        return new Plan(result,cueUsed);
    }
}
