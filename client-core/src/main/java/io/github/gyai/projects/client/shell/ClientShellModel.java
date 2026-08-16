package io.github.gyai.projects.client.shell;

import java.util.Objects;

/** Small mutable controller around immutable reducer state and immutable source snapshots. */
public final class ClientShellModel {
    private final ClientShellDataSource source;
    private ClientShellSnapshot state;
    private long clockNowMillis;
    private long transitionStartedAtMillis;

    public ClientShellModel(ClientShellDataSource source) {
        this.source = Objects.requireNonNull(source, "source");
        state = ClientShellSnapshot.initial();
    }

    public ClientShellSnapshot state() { return state; }

    /** Returns a source snapshot, failing closed to the offline sample rather than crashing UI. */
    public ClientShellDataSnapshot data() {
        try {
            ClientShellDataSnapshot next = source.snapshot();
            return next == null ? new SampleClientShellDataSource().snapshot() : next;
        } catch (RuntimeException ignored) {
            return new SampleClientShellDataSource().snapshot();
        }
    }

    public ClientShellSnapshot dispatch(ClientShellAction action) {
        state = ClientShellReducer.reduce(state, action);
        transitionStartedAtMillis = clockNowMillis;
        return state;
    }

    /**
     * Samples the frozen local transition clock without sleeping or scheduling a platform timer.
     * The model owns the origin so a newly dispatched automatic action always starts at zero.
     */
    public ClientShellSnapshot tick(long nowMillis) {
        if (nowMillis < clockNowMillis) throw new IllegalArgumentException("clock cannot move backwards");
        clockNowMillis = nowMillis;
        ClientShellSnapshot previous = state;
        state = ClientShellReducer.advance(state, nowMillis - transitionStartedAtMillis);
        if (state.state() != previous.state()) {
            transitionStartedAtMillis = clockNowMillis;
        }
        return state;
    }

    /** Descriptive alias for callers that use the injected-time vocabulary. */
    public ClientShellSnapshot advanceTo(long nowMillis) { return tick(nowMillis); }

    public long clockNowMillis() { return clockNowMillis; }

    public void replaceState(ClientShellSnapshot nextState) {
        state = Objects.requireNonNull(nextState, "nextState");
        transitionStartedAtMillis = clockNowMillis;
    }
}
