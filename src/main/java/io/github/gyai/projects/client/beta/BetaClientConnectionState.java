package io.github.gyai.projects.client.beta;

import java.util.Optional;

public final class BetaClientConnectionState {
    private final BetaClientSession session = new BetaClientSession();
    private final BetaUiStateStores stores = new BetaUiStateStores();

    public synchronized byte[] accept(BetaProtocol.Advertisement advertisement) {
        stores.clear();
        return session.accept(advertisement);
    }

    public synchronized boolean receive(BetaProtocol.Envelope envelope) {
        return stores.receive(envelope, session);
    }

    public synchronized Optional<BetaProtocol.Command> command(
            BetaProtocol.Capability capability,
            long targetRevision,
            byte[] payload
    ) {
        return session.command(capability, targetRevision, payload);
    }

    public BetaClientSession session() {
        return session;
    }

    public BetaUiStateStores stores() {
        return stores;
    }

    public synchronized void clear() {
        session.clear();
        stores.clear();
    }
}
