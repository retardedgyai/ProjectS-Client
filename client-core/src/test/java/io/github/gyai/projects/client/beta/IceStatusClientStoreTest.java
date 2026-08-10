package io.github.gyai.projects.client.beta;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class IceStatusClientStoreTest {
    public static void main(String[] args) throws Exception {
        noneAndZeroAreHiddenWhileAuthoritativeStagesRender();
        transitionsPulseAndFlashOnlyOnce();
        staleExpiryAndTargetChangeFailClosed();
        immunityCountsDownAndStaleStateCannotReviveIt();
        stateDisappearanceClearsOldTransitions();
        elementStoresClearFireAndIceTogether();
        malformedAndUnsupportedDocumentsClearDisplay();
        vanillaIceApisAndClientAuthorityAreAbsent();
        System.out.println("IceStatusClientStoreTest passed");
    }

    private static void noneAndZeroAreHiddenWhileAuthoritativeStagesRender() {
        IceStatusClientStore store = new IceStatusClientStore();
        assert store.receive(document(1, 42, 0, "none", false, 0, 10_000), 100);
        assert store.view(100).isEmpty();

        assert store.receive(document(2, 42, 12.5, "chilled", false, 0, 10_000), 110);
        var chilled = store.view(110).orElseThrow();
        assert chilled.coldGauge() == 12.5;
        assert chilled.coldStage() == ElementStatePayloadV1.ColdStage.CHILLED;
        assert !chilled.frozen();

        assert store.receive(document(3, 42, 40, "deep_chill", false, 0, 10_000), 120);
        assert store.view(120).orElseThrow().coldStage()
                == ElementStatePayloadV1.ColdStage.DEEP_CHILL;

        assert store.receive(document(4, 42, 65, "frozen", true, 0, 10_000), 130);
        var frozen = store.view(130).orElseThrow();
        assert frozen.coldStage() == ElementStatePayloadV1.ColdStage.FROZEN;
        assert frozen.frozen();
    }

    private static void transitionsPulseAndFlashOnlyOnce() {
        IceStatusClientStore store = new IceStatusClientStore();
        assert store.receive(document(1, 42, 0, "none", false, 0, 10_000), 1_000);
        assert store.receive(document(2, 42, 10, "chilled", false, 0, 10_000), 1_010);
        assert store.view(1_010).orElseThrow().stagePulse();
        assert !store.receive(document(2, 42, 10, "chilled", false, 0, 10_000), 1_011);

        assert store.receive(document(3, 42, 20, "deep_chill", false, 0, 10_000), 1_020);
        assert store.view(1_020).orElseThrow().stagePulse();

        assert store.receive(document(4, 42, 30, "frozen", true, 0, 10_000), 1_030);
        var frozen = store.view(1_030).orElseThrow();
        assert frozen.freezeFlash();
        assert !frozen.shatterFlash();
        assert !store.receive(document(4, 42, 30, "frozen", true, 0, 10_000), 1_031);

        assert store.receive(document(5, 42, 18, "chilled", false, 5_000, 10_000), 1_040);
        var shattered = store.view(1_040).orElseThrow();
        assert shattered.shatterFlash();
        assert !shattered.freezeFlash();
        assert shattered.immunityRemainingMillis() == 3_960;
        assert !store.receive(document(5, 42, 18, "chilled", false, 5_000, 10_000), 1_041);
        assert !store.view(1_301).orElseThrow().shatterFlash();
    }

    private static void staleExpiryAndTargetChangeFailClosed() {
        IceStatusClientStore store = new IceStatusClientStore();
        assert store.receive(document(10, 42, 20, "deep_chill", false, 0, 3_000), 1_000);
        assert store.receive(document(11, 42, 30, "frozen", true, 0, 3_000), 1_010);
        assert store.view(1_010).orElseThrow().freezeFlash();

        assert store.receive(document(12, 77, 35, "frozen", true, 0, 3_000), 1_020);
        var replacement = store.view(1_020).orElseThrow();
        assert replacement.targetNetworkId() == 77;
        assert !replacement.stagePulse();
        assert !replacement.freezeFlash();
        assert !replacement.shatterFlash();

        assert !store.receive(document(11, 42, 50, "frozen", true, 9_000, 3_000), 1_021);
        assert store.view(1_021).orElseThrow().targetNetworkId() == 77;
        store.clearTarget(42);
        assert store.view(1_021).isPresent();
        store.clearTarget(77);
        assert store.view(1_021).isEmpty();

        assert store.receive(document(13, 77, 10, "chilled", false, 0, 3_000), 1_030);
        assert store.view(3_000).isEmpty();
    }

    private static void immunityCountsDownAndStaleStateCannotReviveIt() {
        IceStatusClientStore store = new IceStatusClientStore();
        assert store.receive(document(1, 42, 5, "chilled", false, 2_000, 10_000), 1_000);
        assert store.view(1_000).orElseThrow().immunityRemainingMillis() == 1_000;
        assert store.view(1_500).orElseThrow().immunityRemainingMillis() == 500;
        assert store.view(2_000).orElseThrow().immunityRemainingMillis() == 0;

        assert store.receive(document(2, 42, 5, "chilled", false, 0, 10_000), 2_010);
        assert !store.receive(document(1, 42, 5, "chilled", false, 9_000, 10_000), 2_020);
        assert store.view(2_020).orElseThrow().immunityRemainingMillis() == 0;
    }

    private static void stateDisappearanceClearsOldTransitions() {
        IceStatusClientStore store = new IceStatusClientStore();
        assert store.receive(document(1, 42, 0, "none", false, 0, 10_000), 1_000);
        assert store.receive(document(2, 42, 30, "frozen", true, 0, 10_000), 1_010);
        assert store.view(1_010).orElseThrow().freezeFlash();
        assert store.receive(document(3, 42, 0, "none", false, 0, 10_000), 1_020);
        assert store.view(1_020).isEmpty();
        assert store.receive(document(4, 42, 5, "chilled", false, 0, 10_000), 1_030);
        var cold = store.view(1_030).orElseThrow();
        assert cold.stagePulse();
        assert !cold.freezeFlash();
        assert !cold.shatterFlash();
    }

    private static void elementStoresClearFireAndIceTogether() {
        ElementStatusClientStores stores = new ElementStatusClientStores();
        stores.receive(fullDocument(1, 42, 1, 10, "chilled", false, 0, 10_000), 1_000);
        assert stores.fireView(1_000).isPresent();
        assert stores.iceView(1_000).isPresent();
        stores.clearTarget(77);
        assert stores.fireView(1_000).isPresent();
        assert stores.iceView(1_000).isPresent();
        stores.clearTarget(42);
        assert stores.fireView(1_000).isEmpty();
        assert stores.iceView(1_000).isEmpty();

        stores.receive(fullDocument(2, 42, 1, 10, "chilled", false, 0, 10_000), 1_010);
        stores.clear();
        assert stores.fireView(1_010).isEmpty();
        assert stores.iceView(1_010).isEmpty();
    }

    private static void malformedAndUnsupportedDocumentsClearDisplay() {
        IceStatusClientStore store = new IceStatusClientStore();
        assert store.receive(document(1, 42, 10, "chilled", false, 0, 10_000), 1_000);
        Map<String, String> missing = new LinkedHashMap<>(
                document(2, 42, 10, "chilled", false, 0, 10_000).fields());
        missing.remove("cold-stage");
        assert !store.receive(new BetaDisplayDocument(2,
                BetaDisplayDocument.Status.READY, "", missing, List.of()), 1_010);
        assert store.view(1_010).isEmpty();

        assert store.receive(document(3, 42, 10, "chilled", false, 0, 10_000), 1_020);
        assert !store.receive(BetaDisplayDocument.unsupported("old server"), 1_030);
        assert store.view(1_030).isEmpty();
    }

    private static void vanillaIceApisAndClientAuthorityAreAbsent() throws Exception {
        String sources = Files.readString(Path.of(
                "client-core/src/main/java/io/github/gyai/projects/client/beta/IceStatusClientStore.java"))
                + Files.readString(Path.of(
                "client-core/src/client/java/io/github/gyai/projects/client/MonsterUiRenderer.java"));
        assert !sources.contains("setFreezeTicks");
        assert !sources.contains("getFreezeTicks");
        assert !sources.contains("powder_snow");
        assert !sources.contains("PowderSnow");
        assert !sources.contains("coldGauge++");
        assert !sources.contains("100.0");
    }

    private static BetaDisplayDocument document(
            long revision, int target, double gauge, String stage, boolean frozen,
            long immuneUntilMillis, long expiry
    ) {
        return new BetaDisplayDocument(revision, BetaDisplayDocument.Status.READY, "",
                Map.of("target-network-id", Integer.toString(target),
                        "state-revision", Long.toString(revision),
                        "snapshot-expires-at-millis", Long.toString(expiry),
                        "cold-gauge", Double.toString(gauge),
                        "cold-stage", stage,
                        "frozen", Boolean.toString(frozen),
                        "refreeze-immunity", Long.toString(immuneUntilMillis)),
                List.of());
    }

    private static BetaDisplayDocument fullDocument(
            long revision, int target, int fireStacks, double coldGauge,
            String coldStage, boolean frozen, long immuneUntilMillis, long expiry
    ) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("target-network-id", Integer.toString(target));
        fields.put("state-revision", Long.toString(revision));
        fields.put("fire-stacks", Integer.toString(fireStacks));
        fields.put("fire-fractional-gauge", "0.0");
        fields.put("fire-threshold", "25.0");
        fields.put("fire-progress-ratio", "0.0");
        fields.put("fire-decay-active", "false");
        fields.put("fire-decay-starts-in-millis", "0");
        fields.put("fire-detonation-pulse-revision", "0");
        fields.put("snapshot-expires-at-millis", Long.toString(expiry));
        fields.put("cold-gauge", Double.toString(coldGauge));
        fields.put("cold-stage", coldStage);
        fields.put("frozen", Boolean.toString(frozen));
        fields.put("refreeze-immunity", Long.toString(immuneUntilMillis));
        return new BetaDisplayDocument(revision, BetaDisplayDocument.Status.READY,
                "", fields, List.of());
    }
}
