package io.github.gyai.projects.client.beta;

import java.util.Optional;

public final class BetaClientConnectionState {
    private final BetaClientSession session = new BetaClientSession();
    private final BetaUiStateStores stores = new BetaUiStateStores();

    public synchronized byte[] accept(BetaProtocol.Advertisement advertisement) {
        return accept(advertisement, capability -> true);
    }

    public synchronized byte[] accept(BetaProtocol.Advertisement advertisement,
            java.util.function.Predicate<BetaProtocol.Capability> acknowledgementPolicy) {
        stores.clear();
        return session.accept(advertisement, acknowledgementPolicy);
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
