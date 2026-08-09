package io.github.gyai.projects.client.beta;

/** Compatibility facade for the shared Elements status route. */
public final class FireStatusRenderRoute {
    private FireStatusRenderRoute() {
    }

    public static Route decide(Input input) {
        if (input == null) return Route.HIDDEN;
        return switch (ElementStatusRenderRoute.decide(
                new ElementStatusRenderRoute.Input(
                        input.targetNetworkId(), input.fireStacks() > 0,
                        input.snapshotExpired(), input.entityNetworkId(),
                        input.entityPresent(), input.entityRemoved(), input.entityAlive(),
                        input.sameDimension(), input.withinDisplayRange(), input.guiHidden(),
                        input.selectedTarget(), input.tracked()))) {
            case TRACKED -> Route.TRACKED;
            case STANDALONE -> Route.STANDALONE;
            case HIDDEN -> Route.HIDDEN;
        };
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
