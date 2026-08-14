package io.github.gyai.projects.client.shell;

/** Reviewable home-flow states. None of these states starts a process or contacts a service. */
public enum ClientShellState {
    HOME,
    LAUNCHING,
    CONNECTING,
    CONNECTED,
    RECOVERABLE_ERROR
}
