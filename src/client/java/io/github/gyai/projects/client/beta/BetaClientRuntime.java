package io.github.gyai.projects.client.beta;

import java.util.Optional;

public final class BetaClientRuntime {
    private static final BetaClientLifecycleAdapter LIFECYCLE =
            new BetaClientLifecycleAdapter(new BetaClientConnectionState());

    private BetaClientRuntime() {
    }

    public static Optional<BetaCapabilityAcknowledgementPayload> receive(
            BetaCapabilityAdvertisementPayload payload
    ) {
        if (payload == null || !payload.decoded().successful()) return Optional.empty();
        return LIFECYCLE.accept(payload.decoded().value())
                .map(BetaCapabilityAcknowledgementPayload::new);
    }

    public static boolean receive(BetaStatePayload payload) {
        return payload != null && payload.decoded().successful()
                && LIFECYCLE.receive(payload.decoded().value());
    }

    public static Optional<BetaCommandPayload> command(
            BetaProtocol.Capability capability,
            long targetRevision,
            byte[] payload
    ) {
        return LIFECYCLE.command(capability, targetRevision, payload)
                .map(BetaProtocol::encodeCommand)
                .map(BetaCommandPayload::new);
    }

    public static BetaClientSession session() {
        return LIFECYCLE.session();
    }

    public static BetaUiStateStores stores() {
        return LIFECYCLE.stores();
    }

    public static void beginConnection() { LIFECYCLE.beginConnection(); }

    public static void disconnect() { LIFECYCLE.disconnect(); }

    public static BetaClientLifecycleAdapter.State lifecycleState() {
        return LIFECYCLE.state();
    }

    public static void clear() {
        LIFECYCLE.disconnect();
    }
}
