package io.github.gyai.projects.devtools.studio.timeline;

import io.github.gyai.projects.ui.runtime.IconKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Platform-neutral playback state for the Stage 3 reference timeline.
 *
 * <p>This intentionally owns no production document or server data. It is a
 * small deterministic state machine that a Studio host can wire to later.</p>
 */
public final class StudioPlaybackModel {
    private final List<StudioTimelineTrack> tracks;
    private StudioPlaybackState state = StudioPlaybackState.STOPPED;
    private StudioPlaybackSpeed speed = StudioPlaybackSpeed.NORMAL;
    private boolean looping;
    private double playheadSeconds;
    private StudioTimelineTrack.TrackId selectedTrack = StudioTimelineTrack.TrackId.SPIRAL;

    public StudioPlaybackModel() { this(StudioTimelineTrack.demo()); }

    public StudioPlaybackModel(List<StudioTimelineTrack> tracks) {
        if (tracks == null || tracks.isEmpty()) throw new IllegalArgumentException("tracks");
        ArrayList<StudioTimelineTrack> copy = new ArrayList<>(tracks.size());
        for (StudioTimelineTrack track : tracks) copy.add(Objects.requireNonNull(track, "track"));
        this.tracks = List.copyOf(copy);
        if (this.tracks.stream().noneMatch(track -> track.id() == StudioTimelineTrack.TrackId.SPIRAL)) {
            selectedTrack = this.tracks.getFirst().id();
        }
    }

    public static StudioPlaybackModel demo() { return new StudioPlaybackModel(); }

    public List<StudioTimelineTrack> tracks() { return tracks; }

    public StudioPlaybackState state() { return state; }

    public StudioPlaybackState playbackState() { return state; }

    public boolean isStopped() { return state.isStopped(); }

    public boolean isPlaying() { return state.isPlaying(); }

    public boolean isPaused() { return state.isPaused(); }

    public StudioPlaybackSpeed speed() { return speed; }

    public boolean looping() { return looping; }

    public boolean loop() { return looping; }

    public double playheadSeconds() { return playheadSeconds; }

    public double playhead() { return playheadSeconds; }

    public double durationSeconds() {
        double duration = 0;
        for (StudioTimelineTrack track : tracks) duration = Math.max(duration, track.durationSeconds());
        return duration;
    }

    public StudioTimelineTrack.TrackId selectedTrackId() { return selectedTrack; }

    public Optional<StudioTimelineTrack> selectedTrack() {
        return tracks.stream().filter(track -> track.id() == selectedTrack).findFirst();
    }

    public StudioPlaybackModel dispatch(StudioPlaybackAction action) {
        Objects.requireNonNull(action, "action");
        switch (action) {
            case RESTART -> restart();
            case PLAY_PAUSE -> playPause();
            case STOP -> stop();
            case LOOP -> looping = !looping;
            case SPEED -> speed = speed.next();
        }
        return this;
    }

    public StudioPlaybackModel play() {
        state = StudioPlaybackState.PLAYING;
        return this;
    }

    public StudioPlaybackModel pause() {
        if (state == StudioPlaybackState.PLAYING) state = StudioPlaybackState.PAUSED;
        return this;
    }

    public StudioPlaybackModel playPause() {
        state = state == StudioPlaybackState.PLAYING
                ? StudioPlaybackState.PAUSED : StudioPlaybackState.PLAYING;
        return this;
    }

    public StudioPlaybackModel stop() {
        state = StudioPlaybackState.STOPPED;
        playheadSeconds = 0;
        return this;
    }

    public StudioPlaybackModel restart() {
        playheadSeconds = 0;
        state = StudioPlaybackState.PLAYING;
        return this;
    }

    public StudioPlaybackModel setLooping(boolean next) {
        looping = next;
        return this;
    }

    public StudioPlaybackModel loop(boolean next) { return setLooping(next); }

    public StudioPlaybackModel setSpeed(StudioPlaybackSpeed next) {
        speed = Objects.requireNonNull(next, "speed");
        return this;
    }

    public StudioPlaybackModel speed(StudioPlaybackSpeed next) { return setSpeed(next); }

    public StudioPlaybackModel seek(double seconds) {
        if (!Double.isFinite(seconds) || seconds < 0) throw new IllegalArgumentException("seconds");
        playheadSeconds = Math.clamp(seconds, 0, durationSeconds());
        return this;
    }

    /** Advances only while PLAYING; elapsed time is expressed in seconds. */
    public StudioPlaybackModel advance(double elapsedSeconds) {
        if (!Double.isFinite(elapsedSeconds) || elapsedSeconds < 0) {
            throw new IllegalArgumentException("elapsedSeconds");
        }
        if (state != StudioPlaybackState.PLAYING || elapsedSeconds == 0) return this;
        double duration = durationSeconds();
        double next = playheadSeconds + elapsedSeconds * speed.multiplier();
        if (next < duration) {
            playheadSeconds = next;
            return this;
        }
        if (looping) {
            playheadSeconds = duration <= 0 ? 0 : next % duration;
        } else {
            playheadSeconds = duration;
            state = StudioPlaybackState.STOPPED;
        }
        return this;
    }

    public StudioPlaybackModel selectTrack(StudioTimelineTrack.TrackId next) {
        Objects.requireNonNull(next, "track");
        if (tracks.stream().noneMatch(track -> track.id() == next)) {
            throw new IllegalArgumentException("Unknown track: " + next);
        }
        selectedTrack = next;
        return this;
    }

    public IconKey playPauseIcon() {
        return state == StudioPlaybackState.PLAYING ? IconKey.PAUSE : IconKey.PLAY;
    }
}
