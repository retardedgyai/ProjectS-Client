package io.github.gyai.projects.devtools.skillvfx;

/** Pure playback and anchor math; Minecraft access is intentionally kept in the client adapter. */
public final class SkillVfxPreviewController {
    public enum Anchor { PLAYER, FORWARD_3M }
    public enum Quality { LOW, MEDIUM, HIGH }
    public record Vec(double x,double y,double z) { }
    private final SkillVfxTimeline timeline=new SkillVfxTimeline();
    private Anchor anchor=Anchor.PLAYER; private Quality quality=Quality.MEDIUM; private SkillVfxModel.Hook hook=SkillVfxModel.Hook.CAST;
    public SkillVfxTimeline timeline(){return timeline;} public Anchor anchor(){return anchor;} public Quality quality(){return quality;} public SkillVfxModel.Hook hook(){return hook;}
    public void anchor(Anchor value){anchor=value;} public void quality(Quality value){quality=value;} public void hook(SkillVfxModel.Hook value){if(value!=SkillVfxModel.Hook.TRAVEL)hook=value;}
    public void cycleSpeed(){var all=SkillVfxTimeline.Speed.values();timeline.speed(all[(timeline.speed().ordinal()+1)%all.length]);}
    public void cycleQuality(){var all=Quality.values();quality(all[(quality.ordinal()+1)%all.length]);}
    public void cycleAnchor(){anchor(anchor==Anchor.PLAYER?Anchor.FORWARD_3M:Anchor.PLAYER);}
    public Vec origin(Vec player,Vec look){if(anchor==Anchor.PLAYER)return player; double length=Math.sqrt(look.x*look.x+look.z*look.z); if(length<.00001)return player; return new Vec(player.x+look.x/length*3,player.y,player.z+look.z/length*3);}
    public void play(){timeline.play();} public void pause(){timeline.pause();} public void stop(){timeline.stop();} public void restart(){timeline.restart();} public void loop(){timeline.loop(!timeline.loop());}
}
