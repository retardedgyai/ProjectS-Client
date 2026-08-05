package io.github.gyai.projects.client.beta;

import java.util.Optional;

/** Shared lifecycle boundary for Fire and Ice projections of one Elements document. */
public final class ElementStatusClientStores {
    private final FireStatusClientStore fire = new FireStatusClientStore();
    private final IceStatusClientStore ice = new IceStatusClientStore();

    public void receive(BetaDisplayDocument document, long receivedAtMillis) {
        fire.receive(document, receivedAtMillis);
        ice.receive(document, receivedAtMillis);
    }

    public Optional<FireStatusClientStore.View> fireView(long nowMillis) {
        return fire.view(nowMillis);
    }

    public Optional<IceStatusClientStore.View> iceView(long nowMillis) {
        return ice.view(nowMillis);
    }

    public void clear() {
        fire.clear();
        ice.clear();
    }

    public void clearTarget(int targetNetworkId) {
        fire.clearTarget(targetNetworkId);
        ice.clearTarget(targetNetworkId);
    }
}
