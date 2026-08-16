package io.github.gyai.projects.client.shell;

import java.util.List;
import java.util.stream.Collectors;

/** Renderer-neutral narration snapshot for the shell's current page and state. */
public record ClientShellAccessibility(String label, String value, List<String> focusLabels) {
    public ClientShellAccessibility {
        if (label == null || label.isBlank() || value == null) throw new IllegalArgumentException("label/value");
        focusLabels = List.copyOf(focusLabels == null ? List.of() : focusLabels);
    }

    public static ClientShellAccessibility from(ClientShellSnapshot state, ClientShellDataSnapshot data) {
        return from(state, data, ClientShellLayout.Breakpoint.WIDE);
    }

    public static ClientShellAccessibility from(ClientShellSnapshot state,
                                                ClientShellDataSnapshot data,
                                                ClientShellLayout.Breakpoint breakpoint) {
        String page = switch (state.page()) {
            case HOME -> state.state() == ClientShellState.HOME ? "Hub home" : state.state().name();
            case LIBRARY -> "Profiles";
            case SETTINGS -> "Preferences";
        };
        String profile = data.profile(state.profileId()).name();
        return new ClientShellAccessibility(
                "ProjectS Client Hub",
                page + ". " + profile + ". " + state.announcement(),
                ClientShellLayout.focusOrder(state.page(), state.state(),
                                breakpoint == null ? ClientShellLayout.Breakpoint.WIDE : breakpoint).stream()
                        .map(ClientShellAccessibility::labelFor)
                        .collect(Collectors.toUnmodifiableList()));
    }

    private static String labelFor(String id) {
        return switch (id) {
            case "brand" -> "ProjectS Client Hub home";
            case "tab.home", "side.home", "error.home", "library.home", "settings.home" -> "Home";
            case "tab.library", "side.library" -> "Library";
            case "tab.settings", "side.settings", "action.settings", "action.account" -> "Settings";
            case "toolbar.review" -> "Choose a prototype state to review";
            case "home.profile" -> "Choose a sample server profile";
            case "home.play.action", "launch.cancel.action" -> id.equals("home.play.action") ? "Play preview" : "Cancel preview";
            case "home.open-client.action", "connecting.open-client.action", "connected.open-client.action", "error.open-client.action" -> "Open client preview";
            case "connecting.error.action" -> "Preview a recoverable error";
            case "connecting.cancel.action" -> "Cancel preview";
            case "connected.home.action" -> "Back to hub";
            case "error.retry.action" -> "Retry preview";
            case "error.home.action" -> "Back to hub";
            default -> id;
        };
    }
}
