package io.github.gyai.projects.client;

import io.github.gyai.projects.client.vfx.*;
import java.util.*;

/** Client composition point for any local presentation producer; DevTools is one such producer. */
public final class AbilityVfxLocalPreview {
    private static final AbilityVfxLocalPreviewStore STORE=new AbilityVfxLocalPreviewStore();
    private AbilityVfxLocalPreview() { }
    public static boolean begin(AbilityVfx.Cue cue,long tick){return STORE.begin(cue,tick);} public static List<AbilityVfxStore.Active> active(){return STORE.active();} public static Optional<AbilityVfxLocalPreviewStore.Preview> preview(){return STORE.preview();} public static void play(){STORE.play();} public static void pause(){STORE.pause();} public static void restart(){STORE.restart();} public static void seek(double tick){STORE.seek(tick);} public static void advance(double ticks){STORE.advance(ticks);} public static void quality(AbilityVfx.Quality quality){STORE.quality(quality);} public static void tick(long tick){STORE.tick(tick);} public static void stop(){STORE.stop();} public static void closeEditor(){STORE.closeEditor();} public static void worldChanged(){STORE.worldChanged();} public static void disconnect(){STORE.disconnect();} public static void connectionReset(){STORE.connectionReset();}
}
