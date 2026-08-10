package io.github.gyai.projects.client.vfx;

import java.util.*;

/** Presentation-only local cue owner.  It intentionally has no network session, sequence, or dedupe semantics. */
public final class AbilityVfxLocalPreviewStore {
    public record Preview(AbilityVfx.Cue cue, double tick, boolean playing, AbilityVfx.Quality quality) {
        public boolean isPrimitiveActive(AbilityVfx.Primitive primitive) { return primitive != null && tick >= primitive.delay() && tick < primitive.delay()+primitive.duration(); }
        public double progress(AbilityVfx.Primitive primitive) { return primitive == null ? 0 : Math.clamp((tick-primitive.delay())/(double)Math.max(1,primitive.duration()),0,1); }
    }
    private AbilityVfx.Cue cue; private double playhead; private boolean playing; private AbilityVfx.Quality quality=AbilityVfx.Quality.MEDIUM;
    public boolean begin(AbilityVfx.Cue cue, long tick) {
        if (cue == null || cue.primitives() == null || cue.primitives().isEmpty()) return false;
        this.cue=cue; playhead=0; playing=true; quality=AbilityVfx.Quality.MEDIUM;
        return true;
    }
    /** Network ticks are not the local preview clock; DevTools owns this virtual playhead. */
    public void tick(long ignored) { }
    public Optional<Preview> preview() { return cue==null ? Optional.empty() : Optional.of(new Preview(cue,playhead,playing,quality)); }
    public List<AbilityVfxStore.Active> active() { return List.of(); }
    public boolean activePreview() { return cue != null; }
    public void play(){if(cue!=null)playing=true;} public void pause(){playing=false;} public void restart(){if(cue!=null){playhead=0;playing=true;}} public void seek(double value){if(cue!=null)playhead=Math.clamp(value,0,cue.duration());}
    public void advance(double ticks){if(cue==null||!playing)return;playhead=Math.max(0,playhead+Math.max(0,ticks));if(playhead>=cue.duration()){playhead=cue.duration();playing=false;}}
    public void quality(AbilityVfx.Quality value){quality=Objects.requireNonNull(value);}
    public void stop() { cue=null; playhead=0; playing=false; }
    public void closeEditor() { stop(); }
    public void worldChanged() { stop(); }
    public void disconnect() { stop(); }
    public void connectionReset() { stop(); }
}
