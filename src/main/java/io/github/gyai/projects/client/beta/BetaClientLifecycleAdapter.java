package io.github.gyai.projects.client.beta;

import java.util.Optional;

/** Pure Minecraft connection lifecycle boundary for the existing Beta stores. */
public final class BetaClientLifecycleAdapter {
    private final BetaClientConnectionState connection;
    private State state = State.DISCONNECTED;

    public BetaClientLifecycleAdapter(BetaClientConnectionState connection) {
        this.connection = java.util.Objects.requireNonNull(connection);
    }

    public synchronized void beginConnection() {
        connection.clear();
        state = State.WAITING_FOR_ADVERTISEMENT;
    }

    public synchronized Optional<byte[]> accept(BetaProtocol.Advertisement advertisement) {
        if (state == State.DISCONNECTED || advertisement == null) return Optional.empty();
        byte[] acknowledgement = connection.accept(advertisement);
        state = State.ACTIVE;
        return Optional.of(acknowledgement);
    }

    public synchronized boolean receive(BetaProtocol.Envelope envelope) {
        return state == State.ACTIVE && connection.receive(envelope);
    }

    public synchronized Optional<BetaProtocol.Command> command(
            BetaProtocol.Capability capability, long targetRevision, byte[] payload) {
        return state == State.ACTIVE
                ? connection.command(capability, targetRevision, payload) : Optional.empty();
    }

    public synchronized void disconnect() {
        connection.clear();
        state = State.DISCONNECTED;
    }

    public synchronized State state() { return state; }
    public BetaClientSession session() { return connection.session(); }
    public BetaUiStateStores stores() { return connection.stores(); }

    public enum State { DISCONNECTED, WAITING_FOR_ADVERTISEMENT, ACTIVE }
}
