package io.github.gyai.projects.client.vfx;

/** Pure renderer limits so visual work remains testable without Minecraft. */
public final class AbilityVfxRenderPolicy {
    public static final int FRAME_BUDGET=8192; public static final double RANGE=96.0;
    private AbilityVfxRenderPolicy(){}
    public static boolean withinDistance(double dx,double dy,double dz){return Double.isFinite(dx)&&Double.isFinite(dy)&&Double.isFinite(dz)&&dx*dx+dy*dy+dz*dz<=RANGE*RANGE;}
    public static int allow(int requested,int cueUsed,int frameUsed){return Math.max(0,Math.min(Math.min(requested,AbilityVfx.MAX_SAMPLES_PER_PRIMITIVE),Math.min(AbilityVfx.MAX_SAMPLES_PER_CUE-Math.max(0,cueUsed),FRAME_BUDGET-Math.max(0,frameUsed))));}
}
