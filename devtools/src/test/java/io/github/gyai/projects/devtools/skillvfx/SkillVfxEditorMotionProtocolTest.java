package io.github.gyai.projects.devtools.skillvfx;

import io.github.gyai.projects.client.vfx.AbilityVfx;
import io.github.gyai.projects.client.vfx.MotionDirection;
import io.github.gyai.projects.client.vfx.MotionEasing;
import io.github.gyai.projects.client.vfx.MotionMode;
import io.github.gyai.projects.client.vfx.MotionSpec;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Editor v3 Motion table, strict framing, fallback selection and preview propagation. */
public final class SkillVfxEditorMotionProtocolTest {
    public static void main(String[] args) throws Exception {
        visualAndRequestRoundTrip();
        stateRoundTripAndStrictness();
        selectionAndPreview();
    }

    private static void visualAndRequestRoundTrip() {
        SkillVfxModel.Visual visual = visual("projects:vfx/motion", List.of(
                SkillVfxModel.defaults("spiral", SkillVfxModel.PrimitiveType.SPIRAL)
                        .withAppearance(SkillVfxModel.Appearance.particle("minecraft:flame"))
                        .withMotion(new MotionSpec(MotionMode.TRAVEL, MotionDirection.REVERSE,
                                MotionEasing.EASE_OUT, .25, .5))));
        byte[] wire = SkillVfxEditorProtocolV3.encodeVisual(visual);
        assert SkillVfxEditorProtocolV3.decodeVisual(wire).equals(visual);
        SkillVfxEditorProtocol.Request request = new SkillVfxEditorProtocol.Request(
                SkillVfxEditorProtocol.Operation.APPLY_VISUAL_SESSION, 7, UUID.randomUUID(),
                "projects:test", 1, "base", "effective", SkillVfxEditorProtocol.encodeVisual(visual));
        byte[] requestWire = SkillVfxEditorProtocolV3.encodeRequest(request, visual);
        assert SkillVfxEditorProtocolV3.decodeRequest(requestWire).operation() == request.operation();
        assert SkillVfxEditorProtocolV3.decodeRequestVisual(requestWire).equals(visual);
    }

    private static void stateRoundTripAndStrictness() throws Exception {
        byte[] fixture = java.util.HexFormat.of().parseHex(Files.readString(
                Path.of("devtools/src/test/resources/protocol/skill-vfx-editor-v1-golden.hex")).replaceAll("\\s", ""));
        SkillVfxEditorProtocol.State original = SkillVfxEditorProtocol.decodeState(fixture);
        SkillVfxEditorProtocol.Snapshot old = original.snapshot();
        MotionSpec stateMotion = new MotionSpec(MotionMode.REVEAL, MotionDirection.FORWARD,
                MotionEasing.EASE_IN_OUT, .5, 0);
        SkillVfxModel.Visual authoredBase = withMotion(old.base(), stateMotion);
        SkillVfxModel.Visual authoredEffective = withMotion(old.effective(), stateMotion);
        SkillVfxEditorProtocol.Snapshot changed = new SkillVfxEditorProtocol.Snapshot(
                old.abilityId(), old.displayName(), old.gameplay(), old.visualId(), authoredBase, authoredEffective,
                old.revision(), old.baseFingerprint(), old.effectiveFingerprint(), old.sessionOverride());
        SkillVfxEditorProtocol.State source = new SkillVfxEditorProtocol.State(
                original.status(), original.correlation(), original.session(), original.catalog(), changed,
                original.previewAllowed(), original.message(), original.canonical());
        byte[] wire = SkillVfxEditorProtocolV3.encodeState(source);
        SkillVfxEditorProtocol.State decoded = SkillVfxEditorProtocolV3.decodeState(wire);
        assert decoded.snapshot().base().equals(authoredBase);
        assert decoded.snapshot().effective().equals(authoredEffective);

        byte[] trailing = Arrays.copyOf(wire, wire.length + 1);
        assertReject(() -> SkillVfxEditorProtocolV3.decodeState(trailing));
        byte[] wrongVersion = wire.clone(); wrongVersion[0] = 2;
        assertReject(() -> SkillVfxEditorProtocolV3.decodeState(wrongVersion));
        SkillVfxModel.Visual authored = visual("projects:vfx/motion-strict", List.of(
                SkillVfxModel.defaults("line", SkillVfxModel.PrimitiveType.LINE).withMotion(
                        new MotionSpec(MotionMode.REVEAL, MotionDirection.REVERSE, MotionEasing.LINEAR, .25, 0))));
        byte[] wrongId = SkillVfxEditorProtocolV3.encodeVisual(authored);
        int idOffset = firstIdOffset(wrongId); wrongId[idOffset] = 'x';
        assertReject(() -> SkillVfxEditorProtocolV3.decodeVisual(wrongId));
        byte[] unknownMode = SkillVfxEditorProtocolV3.encodeVisual(authored);
        unknownMode[motionModeOffset(unknownMode)] = 99;
        assertReject(() -> SkillVfxEditorProtocolV3.decodeVisual(unknownMode));
    }

    private static void selectionAndPreview() {
        var selection = new SkillVfxEditorProtocolSelection();
        assert selection.select(true, true, true).orElseThrow() == SkillVfxEditorProtocolSelection.Version.V3;
        selection.reset(); assert selection.select(false, true, true).orElseThrow() == SkillVfxEditorProtocolSelection.Version.V2;
        selection.reset(); assert selection.select(false, false, true).orElseThrow() == SkillVfxEditorProtocolSelection.Version.V1;
        selection.reset(); assert selection.select(true, true).orElseThrow() == SkillVfxEditorProtocolSelection.Version.V2;

        SkillVfxModel.Visual visual = visual("projects:vfx/preview", List.of(
                SkillVfxModel.defaults("line", SkillVfxModel.PrimitiveType.LINE).withMotion(
                        new MotionSpec(MotionMode.REVEAL, MotionDirection.REVERSE, MotionEasing.LINEAR, .25, 0))));
        var snapshot = new SkillVfxModel.Snapshot(UUID.randomUUID(), 1, "projects:test", "Test", visual.id(),
                "base", "effective", false,
                List.of(new SkillVfxModel.GameplayAction("Wait", "Wait", Map.of("ticks", "1"))), visual);
        var result = SkillVfxPreviewBuilder.build(snapshot, visual, SkillVfxModel.Hook.TELEGRAPH,
                new AbilityVfx.Frame(new AbilityVfx.Vec(0, 0, 0), new AbilityVfx.Vec(0, 0, 1), new AbilityVfx.Vec(0, 1, 0)),
                UUID.randomUUID(), "minecraft:overworld", 0);
        assert result.valid();
        assert result.cue().primitives().getFirst().motion().equals(visual.emissions(SkillVfxModel.Hook.TELEGRAPH)
                .getFirst().primitives().getFirst().motion());
    }

    private static SkillVfxModel.Visual visual(String id, List<SkillVfxModel.Primitive> primitives) {
        return new SkillVfxModel.Visual(id, List.of(new SkillVfxModel.HookBinding(
                SkillVfxModel.Hook.TELEGRAPH,
                List.of(new SkillVfxModel.Emission("emission", 0, primitives)))));
    }

    private static SkillVfxModel.Visual withMotion(SkillVfxModel.Visual source, MotionSpec motion) {
        List<SkillVfxModel.HookBinding> hooks = new java.util.ArrayList<>();
        for (SkillVfxModel.HookBinding hook : source.hooks()) {
            List<SkillVfxModel.Emission> emissions = new java.util.ArrayList<>();
            for (SkillVfxModel.Emission emission : hook.emissions()) {
                List<SkillVfxModel.Primitive> primitives = emission.primitives().stream()
                        .map(primitive -> primitive.withMotion(motion)).toList();
                emissions.add(new SkillVfxModel.Emission(emission.id(), emission.actionIndex(), primitives));
            }
            hooks.add(new SkillVfxModel.HookBinding(hook.hook(), emissions));
        }
        return new SkillVfxModel.Visual(source.id(), hooks);
    }

    private static int firstIdOffset(byte[] wire) {
        int bodyLength = ((wire[1] & 255) << 8) | (wire[2] & 255);
        return 3 + bodyLength + 1 + 2 + 2;
    }

    private static int motionModeOffset(byte[] wire) {
        int bodyLength = ((wire[1] & 255) << 8) | (wire[2] & 255);
        int at = 3 + bodyLength + 1 + 2;
        int idLength = ((wire[at] & 255) << 8) | (wire[at + 1] & 255); at += 2 + idLength;
        at += 1;
        int appearanceLength = ((wire[at] & 255) << 8) | (wire[at + 1] & 255); at += 2 + appearanceLength;
        return at;
    }

    private static void assertReject(Runnable operation) {
        try { operation.run(); throw new AssertionError("expected v3 rejection"); }
        catch (IllegalArgumentException expected) { }
    }
}
