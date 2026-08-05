package io.github.gyai.projects.client.beta;

/** Pure selection between the tracked Monster UI and standalone Fire routes. */
public final class FireStatusRenderRoute {
    private FireStatusRenderRoute() {
    }

    public static Route decide(Input input) {
        if (input == null
                || input.guiHidden()
                || input.snapshotExpired()
                || input.fireStacks() <= 0
                || !input.entityPresent()
                || input.entityRemoved()
                || !input.entityAlive()
                || !input.sameDimension()
                || !input.withinDisplayRange()
                || input.targetNetworkId() != input.entityNetworkId()) {
            return Route.HIDDEN;
        }
        if (input.tracked()) {
            return Route.TRACKED;
        }
        return input.selectedTarget()
                ? Route.STANDALONE
                : Route.HIDDEN;
    }

    public enum Route {
        TRACKED,
        STANDALONE,
        HIDDEN
    }

    public record Input(
            int targetNetworkId,
            int fireStacks,
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
