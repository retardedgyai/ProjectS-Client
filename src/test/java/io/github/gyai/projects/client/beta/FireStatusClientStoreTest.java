package io.github.gyai.projects.client.beta;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FireStatusClientStoreTest {
    public static void main(String[] args) {
        zeroIsHiddenAndOneThroughNineAreDisplayed();
        stackPulseAndDetonationFlashAreServerDrivenAndNotDuplicated();
        staleTargetAndExpiryAreCleared();
        oldServerAndUnsupportedStateClearDisplay();
        malformedAndNonFiniteSnapshotsFailClosed();
        System.out.println("FireStatusClientStoreTest passed");
    }

    private static void zeroIsHiddenAndOneThroughNineAreDisplayed() {
        FireStatusClientStore store = new FireStatusClientStore();
        assert store.receive(document(1, 42, 0, 0, 0, false, 10_000), 100);
        assert store.view(100).isEmpty();
        for (int stack = 1; stack <= 9; stack++) {
            assert store.receive(document(stack + 1L, 42, stack, 12.5,
                    0, false, 10_000), 100 + stack);
            var view = store.view(100 + stack).orElseThrow();
            assert view.fireStacks() == stack;
            assert view.fractionalProgress() == .5;
            assert view.warning() == (stack == 9);
        }
    }

    private static void stackPulseAndDetonationFlashAreServerDrivenAndNotDuplicated() {
        FireStatusClientStore store = new FireStatusClientStore();
        assert store.receive(document(1, 42, 8, 0, 0, false, 10_000), 1_000);
        assert store.receive(document(2, 42, 9, 0, 0, false, 10_000), 1_010);
        assert store.view(1_010).orElseThrow().stackPulse();
        // Server reports post-detonation residual 3 and advances the pulse revision.
        assert store.receive(document(3, 42, 3, 0, 1, false, 10_000), 1_020);
        var detonated = store.view(1_020).orElseThrow();
        assert detonated.fireStacks() == 3;
        assert detonated.detonationFlash();
        assert !store.receive(document(3, 42, 3, 0, 1, false, 10_000), 1_030);
        assert store.view(1_030).orElseThrow().detonationFlash();
        assert !store.view(1_281).orElseThrow().detonationFlash();
    }

    private static void staleTargetAndExpiryAreCleared() {
        FireStatusClientStore store = new FireStatusClientStore();
        assert store.receive(document(5, 42, 3, 0, 1, true, 2_000), 1_000);
        assert !store.receive(document(4, 42, 9, 0, 1, false, 2_000), 1_001);
        assert store.view(1_001).orElseThrow().fireStacks() == 3;
        assert store.view(1_001).orElseThrow().decayActive();
        assert store.receive(document(6, 77, 2, 0, 0, false, 3_000), 1_002);
        assert store.view(1_002).orElseThrow().targetNetworkId() == 77;
        store.clearTarget(42);
        assert store.view(1_002).isPresent();
        store.clearTarget(77);
        assert store.view(1_002).isEmpty();
        assert store.receive(document(7, 77, 2, 0, 0, false, 3_000), 1_003);
        assert !store.view(3_000).isPresent();
        store.clear();
        assert store.view(3_001).isEmpty();
    }

    private static void malformedAndNonFiniteSnapshotsFailClosed() {
        FireStatusClientStore store = new FireStatusClientStore();
        Map<String, String> missing = new LinkedHashMap<>(document(1, 42, 1,
                0, 0, false, 10_000).fields());
        missing.remove("fire-threshold");
        assert !store.receive(new BetaDisplayDocument(1, BetaDisplayDocument.Status.READY,
                "", missing, List.of()), 0);
        assertThrows(() -> new BetaDisplayDocument(1, BetaDisplayDocument.Status.READY,
                "", fields(1, 42, 1, Double.NaN, 0, false, 10_000), List.of()));
    }

    private static void oldServerAndUnsupportedStateClearDisplay() {
        FireStatusClientStore store = new FireStatusClientStore();
        assert store.receive(document(1, 42, 3, 0, 0, false, 10_000), 100);
        BetaDisplayDocument oldServer = new BetaDisplayDocument(2,
                BetaDisplayDocument.Status.READY, "legacy elements",
                Map.of("fire-gauge", "0.5"), List.of());
        assert !store.receive(oldServer, 101);
        assert store.view(101).isEmpty();
        assert store.receive(document(3, 42, 3, 0, 0, false, 10_000), 102);
        assert !store.receive(BetaDisplayDocument.unsupported("old server"), 103);
        assert store.view(103).isEmpty();
    }

    private static BetaDisplayDocument document(long revision, int target, int stacks,
                                                double gauge, long pulse, boolean decay,
                                                long expiry) {
        return new BetaDisplayDocument(revision, BetaDisplayDocument.Status.READY, "",
                fields(revision, target, stacks, gauge, pulse, decay, expiry), List.of());
    }

    private static Map<String, String> fields(long revision, int target, int stacks,
                                              double gauge, long pulse, boolean decay,
                                              long expiry) {
        double threshold = 25.0;
        return Map.of("target-network-id", Integer.toString(target),
                "state-revision", Long.toString(revision),
                "fire-stacks", Integer.toString(stacks),
                "fire-fractional-gauge", Double.toString(gauge),
                "fire-threshold", Double.toString(threshold),
                "fire-progress-ratio", Double.toString(gauge / threshold),
                "fire-decay-active", Boolean.toString(decay),
                "fire-decay-starts-in-millis", "0",
                "fire-detonation-pulse-revision", Long.toString(pulse),
                "snapshot-expires-at-millis", Long.toString(expiry));
    }

    private static void assertThrows(Runnable action) {
        try { action.run(); throw new AssertionError("expected exception"); }
        catch (IllegalArgumentException expected) { }
    }
}
