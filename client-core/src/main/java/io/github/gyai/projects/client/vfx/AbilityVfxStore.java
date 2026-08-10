package io.github.gyai.projects.client.vfx;

import java.util.*;

/** Bounded client-side cue lifetime, independent from Minecraft clocks. */
public final class AbilityVfxStore {
    public static final int MAX_ACTIVE=64, DEDUPE_SIZE=256;
    private UUID session; private long lastSequence=Long.MIN_VALUE; private String world,dimension;
    private final LinkedHashMap<UUID,Boolean> retired=new LinkedHashMap<>(17,.75f,true){protected boolean removeEldestEntry(Map.Entry<UUID,Boolean> e){return size()>16;}};
    private final LinkedHashMap<UUID,Active> active=new LinkedHashMap<>();
    private final LinkedHashMap<UUID,Boolean> seen=new LinkedHashMap<>(DEDUPE_SIZE+1,.75f,true){protected boolean removeEldestEntry(Map.Entry<UUID,Boolean> e){return size()>DEDUPE_SIZE;}};
    public record Active(AbilityVfx.Cue cue,long receivedTick) { public boolean alive(long tick){return tick<receivedTick+cue.duration();} public boolean isPrimitiveActive(long tick,AbilityVfx.Primitive primitive){long start=receivedTick+primitive.delay();return tick>=start&&tick<start+primitive.duration();} public double progress(long tick,AbilityVfx.Primitive p){long at=receivedTick+p.delay();return Math.clamp((tick-at)/(double)p.duration(),0,1);} }
    public boolean receive(AbilityVfx.Decoded decoded,String currentWorld,String currentDimension,long tick){if(!decoded.valid()||decoded.cue()==null||!Objects.equals(currentWorld,decoded.cue().worldId().toString())||!Objects.equals(currentDimension,decoded.cue().dimension()))return false; AbilityVfx.Cue c=decoded.cue();if(retired.containsKey(c.session()))return false;if(session!=null&&!session.equals(c.session())){retired.put(session,true);clearState();}if(session==null)session=c.session();if(c.sequence()<=lastSequence||seen.containsKey(c.cueId()))return false;lastSequence=c.sequence();seen.put(c.cueId(),true);world=currentWorld;dimension=currentDimension;if(c.hook()==AbilityVfx.Hook.CANCEL||c.hook()==AbilityVfx.Hook.EXPIRE)active.entrySet().removeIf(e->e.getValue().cue().castId().equals(c.castId())&&e.getValue().cue().hook()!=AbilityVfx.Hook.CANCEL&&e.getValue().cue().hook()!=AbilityVfx.Hook.EXPIRE);if(c.primitives().isEmpty())return true;if(active.size()>=MAX_ACTIVE)return false;active.put(c.cueId(),new Active(c,tick));return true;}
    public void tick(long tick,String currentWorld,String currentDimension){if(currentWorld==null||currentDimension==null||!Objects.equals(world,currentWorld)||!Objects.equals(dimension,currentDimension)){clearWorld();return;}active.entrySet().removeIf(e->!e.getValue().alive(tick));}
    public Collection<Active> active(){return List.copyOf(active.values());} public void clearWorld(){active.clear();world=null;dimension=null;} private void clearState(){active.clear();seen.clear();session=null;lastSequence=Long.MIN_VALUE;world=null;dimension=null;} public void resetConnection(){clearState();retired.clear();} public void clear(){resetConnection();} public int size(){return active.size();}
}
