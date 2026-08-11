package io.github.gyai.projects.devtools.studio.timeline;

import io.github.gyai.projects.ui.runtime.IconKey;

import java.util.List;
import java.util.Objects;

/** Frozen reference tracks used to make the Stage 3 shell feel like a real workspace. */
public record StudioTimelineTrack(
        TrackId id,
        String label,
        String technicalLabel,
        double durationSeconds,
        IconKey iconKey
) {
    public enum TrackId { SPIRAL, FLAME }

    public StudioTimelineTrack {
        Objects.requireNonNull(id, "id");
        if (label == null || label.isBlank()
                || technicalLabel == null || technicalLabel.isBlank()
                || !Double.isFinite(durationSeconds) || durationSeconds <= 0
                || iconKey == null) {
            throw new IllegalArgumentException("Invalid Studio timeline track");
        }
    }

    public String name() { return id.name(); }

    public static List<StudioTimelineTrack> demo() {
        return List.of(
                new StudioTimelineTrack(TrackId.SPIRAL, "螺旋", "SPIRAL", 4.0, IconKey.SHAPE),
                new StudioTimelineTrack(TrackId.FLAME, "炎", "FLAME", 3.0, IconKey.PARTICLE));
    }
}
