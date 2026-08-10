package io.github.gyai.projects.client.vfx;

/** Pure particle dispatch limits. Runtime code supplies the client setting and performs the actual spawn. */
public final class AbilityVfxParticlePolicy {
    public static final int PER_PRIMITIVE = 64, PER_CUE = 256, PER_TICK = 512;
    private AbilityVfxParticlePolicy() { }
    public static int density(int requested, AbilityVfx.Quality quality, int particleStatus) {
        if (requested <= 0 || quality == null) return 0;
        // Minecraft's ParticleStatus ordinal is ALL=0, DECREASED=1, MINIMAL=2 in 26.1.2.
        double status = particleStatus >= 2 ? .25 : particleStatus == 1 ? .5 : 1.0;
        return Math.max(0, Math.min(PER_PRIMITIVE, (int) Math.ceil(requested * quality.density * status)));
    }
    public static int allow(int requested, int cueUsed, int tickUsed) {
        return Math.max(0, Math.min(Math.min(requested, PER_PRIMITIVE), Math.min(PER_CUE-Math.max(0,cueUsed), PER_TICK-Math.max(0,tickUsed))));
    }
}
