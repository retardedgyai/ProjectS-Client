package io.github.gyai.projects.devtools.skillvfx;

import java.util.*;

/** Deterministic visual timing model; it deliberately has no gameplay cast scheduler semantics. */
public final class SkillVfxTimeline {
    public enum Speed { QUARTER(.25), HALF(.5), NORMAL(1), DOUBLE(2); final double multiplier; Speed(double multiplier){this.multiplier=multiplier;} }
    public record Bar(String id,int start,int end) { public Bar { if(start<0||end<=start)throw new IllegalArgumentException("bar"); } }
    private boolean playing,loop; private Speed speed=Speed.NORMAL; private double ticks;
    public List<Bar> bars(SkillVfxModel.Visual visual,SkillVfxModel.Hook hook){ArrayList<Bar> out=new ArrayList<>();for(SkillVfxModel.Emission e:visual.emissions(hook))for(SkillVfxModel.Primitive p:e.primitives())out.add(new Bar(e.id()+"/"+p.id(),p.delayTicks(),p.delayTicks()+p.durationTicks()));return List.copyOf(out);}
    public void play(){playing=true;} public void pause(){playing=false;} public void stop(){playing=false;ticks=0;} public void restart(){ticks=0;playing=true;} public void seek(double value){ticks=Math.max(0,value);} public void advance(double elapsed,int duration){if(!playing)return;ticks+=Math.max(0,elapsed)*speed.multiplier;if(ticks>=duration){if(loop&&duration>0)ticks%=duration;else{ticks=duration;playing=false;}}} public double ticks(){return ticks;} public boolean playing(){return playing;} public void loop(boolean value){loop=value;} public boolean loop(){return loop;} public void speed(Speed value){speed=Objects.requireNonNull(value);} public Speed speed(){return speed;} public double multiplier(){return speed.multiplier;}
}
