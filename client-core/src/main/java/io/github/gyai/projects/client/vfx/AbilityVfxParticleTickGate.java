package io.github.gyai.projects.client.vfx;

/** Small pure guard so multiple client systems cannot execute particle cues twice in one tick. */
public final class AbilityVfxParticleTickGate {
    private long last=Long.MIN_VALUE;
    public boolean first(long tick) { if(last==tick)return false;last=tick;return true; }
    public void reset() { last=Long.MIN_VALUE; }
}
