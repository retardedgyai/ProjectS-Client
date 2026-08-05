package io.github.gyai.projects.client.beta;

import java.util.ArrayList;
import java.util.List;

public final class BetaUiViewModels {
    private BetaUiViewModels() {
    }

    public static Panel hud(BetaDisplayDocument state) {
        return panel("HUD", state, List.of("level", "xp", "class-id", "resources"));
    }

    public static Panel party(BetaDisplayDocument state) {
        return panel("Party", state, List.of("party-id", "leader", "members"));
    }

    public static Panel elementTargetOverlay(BetaDisplayDocument state) {
        return panel("Elements", state,
                List.of("target-network-id", "cold-gauge",
                        "cold-stage", "frozen", "refreeze-immunity"));
    }

    public static Panel equipmentDetail(BetaDisplayDocument state) {
        return panel("Equipment", state,
                List.of("schema-status", "tier", "item-level", "rarity", "quality",
                        "base-rolls", "mods", "binding", "trade-policy", "enhancement", "broken"));
    }

    public static Panel craftingScreen(BetaDisplayDocument state) {
        return panel("Crafting", state,
                List.of("recipe-id", "recipe-revision", "inputs", "output", "request-id", "terminal"));
    }

    public static Panel enhancementScreen(BetaDisplayDocument state) {
        return panel("Enhancement", state,
                List.of("item-id", "item-revision", "level", "broken", "preview-status",
                        "costs", "outcomes", "request-id", "terminal"));
    }

    public static Panel mobEditorV2Screen(BetaDisplayDocument state) {
        return panel("Mob Editor v2", state,
                List.of("schema-version", "base-revision", "page", "validation",
                        "conflict", "save-result", "rollback-result", "test-spawn"));
    }

    private static Panel panel(String title, BetaDisplayDocument state, List<String> preferredFields) {
        ArrayList<String> lines = new ArrayList<>();
        for (String key : preferredFields) {
            if (state.fields().containsKey(key)) lines.add(key + ": " + state.fields().get(key));
        }
        lines.addAll(state.entries());
        boolean retryAllowed = state.status() != BetaDisplayDocument.Status.RETRY_FORBIDDEN
                && state.status() != BetaDisplayDocument.Status.TERMINAL;
        return new Panel(title, state.status(), state.message(), lines, retryAllowed,
                state.status() == BetaDisplayDocument.Status.CONFLICT);
    }

    public record Panel(
            String title,
            BetaDisplayDocument.Status status,
            String message,
            List<String> lines,
            boolean retryAllowed,
            boolean revisionConflict
    ) {
        public Panel {
            lines = List.copyOf(lines);
        }
    }
}
