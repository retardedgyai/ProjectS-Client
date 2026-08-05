package io.github.gyai.projects.client.beta;

import java.util.Optional;

public final class BetaClientRuntime {
    private static final BetaClientLifecycleAdapter LIFECYCLE =
            new BetaClientLifecycleAdapter(new BetaClientConnectionState());
    private static final ElementStatusClientStores ELEMENTS =
            new ElementStatusClientStores();

    private BetaClientRuntime() {
    }

    public static Optional<BetaCapabilityAcknowledgementPayload> receive(
            BetaCapabilityAdvertisementPayload payload
    ) {
        if (payload == null || !payload.decoded().successful()) return Optional.empty();
        ELEMENTS.clear();
        return LIFECYCLE.accept(payload.decoded().value())
                .map(BetaCapabilityAcknowledgementPayload::new);
    }

    public static boolean receive(BetaStatePayload payload) {
        if (payload == null || !payload.decoded().successful()) return false;
        BetaProtocol.Envelope envelope = payload.decoded().value();
        if (!LIFECYCLE.receive(envelope)) return false;
        if (envelope.kind() == BetaProtocol.Kind.STATE
                && envelope.capability() == BetaProtocol.Capability.ELEMENTS) {
            ELEMENTS.receive(LIFECYCLE.stores().elements(), System.currentTimeMillis());
        }
        return true;
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

    public static void beginConnection() { ELEMENTS.clear(); LIFECYCLE.beginConnection(); }

    public static void disconnect() { ELEMENTS.clear(); LIFECYCLE.disconnect(); }

    public static Optional<FireStatusClientStore.View> fireStatus(long nowMillis) {
        return ELEMENTS.fireView(nowMillis);
    }

    public static Optional<IceStatusClientStore.View> iceStatus(long nowMillis) {
        return ELEMENTS.iceView(nowMillis);
    }

    public static void clearElementTarget() { ELEMENTS.clear(); }

    public static void clearElementTarget(int targetNetworkId) {
        ELEMENTS.clearTarget(targetNetworkId);
    }

    public static BetaClientLifecycleAdapter.State lifecycleState() {
        return LIFECYCLE.state();
    }

    public static void clear() {
        disconnect();
    }
}
