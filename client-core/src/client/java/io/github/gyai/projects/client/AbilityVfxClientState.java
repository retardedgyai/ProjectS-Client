package io.github.gyai.projects.client;

import io.github.gyai.projects.client.vfx.AbilityVfx;
import io.github.gyai.projects.client.vfx.AbilityVfxStore;
import net.minecraft.client.Minecraft;

public final class AbilityVfxClientState {
    private static final AbilityVfxStore STORE=new AbilityVfxStore(); private static Object level; private static String dimension; private static String world; private static long ticks;
    private AbilityVfxClientState(){}
    public static void receive(AbilityVfx.Decoded d){if(!d.valid())return; AbilityVfx.Cue c=d.cue(); if(world!=null&&(!world.equals(c.worldId().toString())||dimension!=null&&!dimension.equals(c.dimension())))return; world=c.worldId().toString(); STORE.receive(d,world,c.dimension(),ticks);}
    public static void tick(Minecraft client){if(client.level==null){clearWorld();return;}String now=client.level.dimension().identifier().toString();if(level!=client.level||dimension!=null&&!dimension.equals(now)){clearWorld();level=client.level;world=null;}dimension=now;ticks++;STORE.tick(ticks,world,now);}
    public static long ticks(){return ticks;} public static java.util.Collection<AbilityVfxStore.Active> active(){return STORE.active();} public static void clearWorld(){STORE.clearWorld();level=null;dimension=null;world=null;} public static void resetConnection(){STORE.resetConnection();level=null;dimension=null;world=null;ticks=0;} public static void clear(){resetConnection();}
}
