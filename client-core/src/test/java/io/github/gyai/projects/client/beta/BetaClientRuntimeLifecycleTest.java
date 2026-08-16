package io.github.gyai.projects.client.beta;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.List;
import java.util.UUID;

public final class BetaClientRuntimeLifecycleTest {
    public static void main(String[] args) throws Exception {
        oldServerFallbackRemainsHidden();
        newServerSessionIsActiveOnlyAfterAdvertisement();
        sessionSwitchAndDisconnectClearState();
        System.out.println("BetaClientRuntimeLifecycleTest passed");
    }

    private static void oldServerFallbackRemainsHidden() {
        BetaClientLifecycleAdapter adapter = adapter();
        adapter.beginConnection();
        assert adapter.state() == BetaClientLifecycleAdapter.State.WAITING_FOR_ADVERTISEMENT;
        assert adapter.session().oldServerFallback();
        assert adapter.command(BetaProtocol.Capability.HUD, 0, new byte[0]).isEmpty();
        assert adapter.stores().hud().status() == BetaDisplayDocument.Status.LOADING;
    }

    private static void newServerSessionIsActiveOnlyAfterAdvertisement() throws Exception {
        BetaClientLifecycleAdapter adapter = adapter();
        UUID session = UUID.randomUUID();
        assert adapter.accept(advertisement(session, 1)).isEmpty();
        adapter.beginConnection();
        assert adapter.accept(advertisement(session, 1)).isPresent();
        assert adapter.state() == BetaClientLifecycleAdapter.State.ACTIVE;
        assert adapter.session().supports(BetaProtocol.Capability.HUD);
        assert adapter.command(BetaProtocol.Capability.HUD, 0, new byte[0]).isPresent();
    }

    private static void sessionSwitchAndDisconnectClearState() throws Exception {
        BetaClientLifecycleAdapter adapter = adapter(); adapter.beginConnection();
        adapter.accept(advertisement(UUID.randomUUID(), 1));
        adapter.command(BetaProtocol.Capability.HUD, 0, new byte[0]).orElseThrow();
        assert adapter.session().pendingRequestCount() == 1;
        adapter.accept(advertisement(UUID.randomUUID(), 2));
        assert adapter.session().pendingRequestCount() == 0;
        assert adapter.session().revision() == 2;
        adapter.disconnect(); adapter.disconnect();
        assert adapter.state() == BetaClientLifecycleAdapter.State.DISCONNECTED;
        assert adapter.session().oldServerFallback();
        assert adapter.session().pendingRequestCount() == 0;
        assert adapter.command(BetaProtocol.Capability.HUD, 0, new byte[0]).isEmpty();
    }

    private static BetaClientLifecycleAdapter adapter() {
        return new BetaClientLifecycleAdapter(new BetaClientConnectionState());
    }

    private static BetaProtocol.Advertisement advertisement(UUID session, long revision)
            throws Exception {
        return new BetaProtocol.Advertisement(session, revision, List.of(
                new BetaProtocol.Descriptor(BetaProtocol.Capability.HUD, 1)));
    }

    private static void writeString(DataOutputStream out, String value) throws Exception {
        byte[] encoded = value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        out.writeInt(encoded.length); out.write(encoded);
    }
}
