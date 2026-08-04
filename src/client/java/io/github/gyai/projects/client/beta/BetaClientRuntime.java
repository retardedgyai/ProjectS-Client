package io.github.gyai.projects.client.beta;

import java.util.Optional;

public final class BetaClientRuntime {
    private static final BetaClientConnectionState CONNECTION =
            new BetaClientConnectionState();

    private BetaClientRuntime() {
    }

    public static Optional<BetaCapabilityAcknowledgementPayload> receive(
            BetaCapabilityAdvertisementPayload payload
    ) {
        if (payload == null || !payload.decoded().successful()) return Optional.empty();
        byte[] acknowledgement = CONNECTION.accept(payload.decoded().value());
        return Optional.of(new BetaCapabilityAcknowledgementPayload(acknowledgement));
    }

    public static boolean receive(BetaStatePayload payload) {
        return payload != null && payload.decoded().successful()
                && CONNECTION.receive(payload.decoded().value());
    }

    public static Optional<BetaCommandPayload> command(
            BetaProtocol.Capability capability,
            long targetRevision,
            byte[] payload
    ) {
        return CONNECTION.command(capability, targetRevision, payload)
                .map(BetaProtocol::encodeCommand)
                .map(BetaCommandPayload::new);
    }

    public static BetaClientSession session() {
        return CONNECTION.session();
    }

    public static BetaUiStateStores stores() {
        return CONNECTION.stores();
    }

    public static void clear() {
        CONNECTION.clear();
    }
}
