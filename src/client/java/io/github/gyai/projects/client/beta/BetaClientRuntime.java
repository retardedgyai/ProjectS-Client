package io.github.gyai.projects.client.beta;

import java.util.Optional;

public final class BetaClientRuntime {
    private static final BetaClientSession SESSION = new BetaClientSession();
    private static final BetaUiStateStores STORES = new BetaUiStateStores();

    private BetaClientRuntime() {
    }

    public static Optional<BetaCapabilityAcknowledgementPayload> receive(
            BetaCapabilityAdvertisementPayload payload
    ) {
        if (payload == null || !payload.decoded().successful()) return Optional.empty();
        byte[] acknowledgement = SESSION.accept(payload.decoded().value());
        return Optional.of(new BetaCapabilityAcknowledgementPayload(acknowledgement));
    }

    public static boolean receive(BetaStatePayload payload) {
        return payload != null && payload.decoded().successful()
                && STORES.receive(payload.decoded().value(), SESSION);
    }

    public static Optional<BetaCommandPayload> command(
            BetaProtocol.Capability capability,
            long targetRevision,
            byte[] payload
    ) {
        return SESSION.command(capability, targetRevision, payload)
                .map(BetaProtocol::encodeCommand)
                .map(BetaCommandPayload::new);
    }

    public static BetaClientSession session() {
        return SESSION;
    }

    public static BetaUiStateStores stores() {
        return STORES;
    }

    public static void clear() {
        SESSION.clear();
        STORES.clear();
    }
}
