package io.github.gyai.projects.client.beta;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ClientWorldLifecycleGuardTest {
    public static void main(String[] args) {
        firstWorldIsRecordedWithoutClear();
        sameWorldDoesNotClear();
        worldChangeClearsOnce();
        worldToNullClearsOnce();
        reconnectDoesNotCarryPriorWorld();
        worldTransitionsClearFireAndIceStores();
        System.out.println("ClientWorldLifecycleGuardTest passed");
    }

    private static void firstWorldIsRecordedWithoutClear() {
        ClientWorldLifecycleGuard guard = new ClientWorldLifecycleGuard();
        assert !guard.observe(null);
        assert !guard.observe("world:A");
    }

    private static void sameWorldDoesNotClear() {
        ClientWorldLifecycleGuard guard = new ClientWorldLifecycleGuard();
        assert !guard.observe("world:A");
        assert !guard.observe("world:A");
    }

    private static void worldChangeClearsOnce() {
        ClientWorldLifecycleGuard guard = new ClientWorldLifecycleGuard();
        assert !guard.observe("world:A");
        assert guard.observe("world:B");
        assert !guard.observe("world:B");
    }

    private static void worldToNullClearsOnce() {
        ClientWorldLifecycleGuard guard = new ClientWorldLifecycleGuard();
        assert !guard.observe("world:B");
        assert guard.observe(null);
        assert !guard.observe(null);
    }

    private static void reconnectDoesNotCarryPriorWorld() {
        ClientWorldLifecycleGuard guard = new ClientWorldLifecycleGuard();
        assert !guard.observe("world:A");
        guard.reset();
        assert !guard.observe(null);
        assert !guard.observe("world:A");
    }

    private static void worldTransitionsClearFireAndIceStores() {
        ElementStatusClientStores stores = new ElementStatusClientStores();
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("target-network-id", "42");
        fields.put("state-revision", "1");
        fields.put("fire-stacks", "1");
        fields.put("fire-fractional-gauge", "0");
        fields.put("fire-threshold", "25");
        fields.put("fire-progress-ratio", "0");
        fields.put("fire-decay-active", "false");
        fields.put("fire-decay-starts-in-millis", "0");
        fields.put("fire-detonation-pulse-revision", "0");
        fields.put("snapshot-expires-at-millis", "10000");
        fields.put("cold-gauge", "10");
        fields.put("cold-stage", "chilled");
        fields.put("frozen", "false");
        fields.put("refreeze-immunity", "0");
        stores.receive(new BetaDisplayDocument(1, BetaDisplayDocument.Status.READY, "",
                fields, List.of()), 100);
        assert stores.fireView(100).isPresent();
        assert stores.iceView(100).isPresent();
        stores.clear();
        assert stores.fireView(100).isEmpty();
        assert stores.iceView(100).isEmpty();
    }
}
