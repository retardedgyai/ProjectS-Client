package io.github.gyai.projects.client.beta;

import java.util.Optional;

public final class BetaClientRuntime {
    private static final BetaClientLifecycleAdapter LIFECYCLE =
            new BetaClientLifecycleAdapter(new BetaClientConnectionState());
    private static final FireStatusClientStore FIRE = new FireStatusClientStore();

    private BetaClientRuntime() {
    }

    public static Optional<BetaCapabilityAcknowledgementPayload> receive(
            BetaCapabilityAdvertisementPayload payload
    ) {
        if (payload == null || !payload.decoded().successful()) return Optional.empty();
        FIRE.clear();
        return LIFECYCLE.accept(payload.decoded().value())
                .map(BetaCapabilityAcknowledgementPayload::new);
    }

    public static boolean receive(BetaStatePayload payload) {
        if (payload == null || !payload.decoded().successful()) return false;
        BetaProtocol.Envelope envelope = payload.decoded().value();
        if (!LIFECYCLE.receive(envelope)) return false;
        if (envelope.kind() == BetaProtocol.Kind.STATE
                && envelope.capability() == BetaProtocol.Capability.ELEMENTS) {
            FIRE.receive(LIFECYCLE.stores().elements(), System.currentTimeMillis());
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

    public static void beginConnection() { FIRE.clear(); LIFECYCLE.beginConnection(); }

    public static void disconnect() { FIRE.clear(); LIFECYCLE.disconnect(); }

    public static Optional<FireStatusClientStore.View> fireStatus(long nowMillis) {
        return FIRE.view(nowMillis);
    }

    public static void clearElementTarget() { FIRE.clear(); }

    public static void clearElementTarget(int targetNetworkId) {
        FIRE.clearTarget(targetNetworkId);
    }

    public static BetaClientLifecycleAdapter.State lifecycleState() {
        return LIFECYCLE.state();
    }

    public static void clear() {
        disconnect();
    }
}
