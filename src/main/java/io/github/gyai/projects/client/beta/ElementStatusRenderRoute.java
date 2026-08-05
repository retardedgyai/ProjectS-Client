package io.github.gyai.projects.client.beta;

import java.util.Optional;

/** Pure shared selection for tracked and combined standalone Elements status routes. */
public final class ElementStatusRenderRoute {
    private ElementStatusRenderRoute() {
    }

    public static Route decide(Input input) {
        if (input == null
                || !input.statusVisible()
                || input.guiHidden()
                || input.snapshotExpired()
                || !input.entityPresent()
                || input.entityRemoved()
                || !input.entityAlive()
                || !input.sameDimension()
                || !input.withinDisplayRange()
                || input.targetNetworkId() != input.entityNetworkId()) {
            return Route.HIDDEN;
        }
        if (input.tracked()) return Route.TRACKED;
        return input.selectedTarget() ? Route.STANDALONE : Route.HIDDEN;
    }

    /** Selects at most one entity billboard even when Fire and Ice are both visible. */
    public static Optional<Selection> select(Status fire, Status ice) {
        if (fire == null && ice == null) return Optional.empty();
        if (fire == null) return Optional.of(new Selection(
                ice.targetNetworkId(), false, true));
        if (ice == null) return Optional.of(new Selection(
                fire.targetNetworkId(), true, false));
        if (fire.targetNetworkId() == ice.targetNetworkId()) {
            return Optional.of(new Selection(fire.targetNetworkId(), true, true));
        }
        if (fire.stateRevision() == ice.stateRevision()) {
            return Optional.empty();
        }
        return fire.stateRevision() > ice.stateRevision()
                ? Optional.of(new Selection(fire.targetNetworkId(), true, false))
                : Optional.of(new Selection(ice.targetNetworkId(), false, true));
    }

    public enum Route {
        TRACKED,
        STANDALONE,
        HIDDEN
    }

    public record Status(int targetNetworkId, long stateRevision) {
        public Status {
            if (targetNetworkId < 0 || stateRevision < 0) {
                throw new IllegalArgumentException("invalid element status");
            }
        }
    }

    public record Selection(int targetNetworkId, boolean fireVisible, boolean iceVisible) {
        public Selection {
            if (targetNetworkId < 0 || !fireVisible && !iceVisible) {
                throw new IllegalArgumentException("invalid element selection");
            }
        }
    }

    public record Input(
            int targetNetworkId,
            boolean statusVisible,
            boolean snapshotExpired,
            int entityNetworkId,
            boolean entityPresent,
            boolean entityRemoved,
            boolean entityAlive,
            boolean sameDimension,
            boolean withinDisplayRange,
            boolean guiHidden,
            boolean selectedTarget,
            boolean tracked
    ) {
    }
}
