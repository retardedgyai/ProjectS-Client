package io.github.gyai.projects.devtools.skillvfx;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Executable client-only parity/boundary evidence for the authority-owned v1 codec. */
public final class SkillVfxEditorProtocolParityTest {
    private static final UUID SESSION = UUID.fromString("01234567-89ab-cdef-0123-456789abcdef");

    public static void main(String[] args) throws Exception {
        goldenStateIsExact();
        allRequestOperationsAndShapes();
        visualRoundTripsAllPrimitiveTypesAndSlots();
        decoderRejectsWireCorruption();
        encoderBoundsRejectBeforeNarrowing();
    }

    private static void goldenStateIsExact() throws Exception {
        Path fixture = Path.of("devtools/src/test/resources/protocol/skill-vfx-editor-v1-golden.hex");
        String text = Files.readString(fixture);
        check(sha(text.replace("\r\n", "\n").getBytes(StandardCharsets.UTF_8)).equals("c9f44b45e7a30a54479b0f7488ebc52cf969431a79285a059d8bdef9cce7ddab"), "fixture LF hash");
        byte[] wire = HexFormat.of().parseHex(text.replaceAll("\\s", ""));
        check(sha(wire).equals("7178538049f2a7ac6807f1a3190e6840ebb0c8b6690c42a56090612383a4fdd4"), "fixture bytes hash");
        SkillVfxEditorProtocol.State state = SkillVfxEditorProtocol.decodeState(wire);
        check(state.status() == SkillVfxEditorProtocol.Status.OK && state.correlation() == 7 && state.session() != null, "state envelope");
        check(state.catalog().size() == 2 && state.catalog().getFirst().hasVisual() && !state.catalog().getLast().hasVisual(), "catalog rows");
        check(state.catalog().getLast().visualId().isEmpty() && state.catalog().getLast().baseFingerprint().isEmpty(), "unbound canonical row");
        SkillVfxEditorProtocol.Snapshot snapshot = state.snapshot();
        check(snapshot != null && snapshot.abilityId().equals("projects:dev-shared-arcane-burst") && snapshot.visualId().equals("projects:vfx/dev-arcane-burst"), "arcane IDs");
        check(snapshot.gameplay().size() == 3, "arcane action count");
        Map<String, String> telegraph = snapshot.gameplay().get(0).fields();
        check(snapshot.gameplay().get(0).type().equals("CircleTelegraph") && telegraph.get("target").equals("PRIMARY_TARGET") && telegraph.get("origin").equals("PRIMARY_TARGET") && telegraph.get("radius").equals("3.0") && telegraph.get("duration").equals("20") && telegraph.get("lockAtCreation").equals("true"), "telegraph values");
        check(snapshot.gameplay().get(1).type().equals("Wait") && snapshot.gameplay().get(1).fields().get("ticks").equals("20"), "wait value");
        Map<String, String> damage = snapshot.gameplay().get(2).fields();
        check(snapshot.gameplay().get(2).type().equals("Damage") && damage.get("target").equals("PRIMARY_TARGET") && damage.get("damageType").equals("MAGICAL") && damage.get("damageKind").equals("DIRECT_SKILL") && damage.get("fixedDamage").equals("12.0") && damage.get("coefficient").equals("0.5") && damage.get("critical").equals("true") && damage.get("tags").equals("MAGIC,SKILL"), "damage metadata");
        SkillVfxModel.Emission emission = snapshot.effective().emissions(SkillVfxModel.Hook.TELEGRAPH).getFirst();
        check(emission.actionIndex() == 0 && emission.primitives().getFirst().value("radius") instanceof SkillVfxModel.FromGameplay field && field.field() == SkillVfxModel.ActionField.RADIUS, "hook/action/radius binding");
        check(Arrays.equals(wire, SkillVfxEditorProtocol.encodeState(state)), "canonical state re-emission");
    }

    private static void allRequestOperationsAndShapes() {
        SkillVfxModel.Visual visual = visual("projects:vfx/request", List.of(SkillVfxModel.defaults("circle", SkillVfxModel.PrimitiveType.CIRCLE)));
        byte[] visualBytes = SkillVfxEditorProtocol.encodeVisual(visual);
        List<SkillVfxEditorProtocol.Request> valid = List.of(
                request(SkillVfxEditorProtocol.Operation.CATALOG, "", 0, "", "", null),
                request(SkillVfxEditorProtocol.Operation.FETCH, "projects:arcane", 0, "", "", null),
                request(SkillVfxEditorProtocol.Operation.APPLY_VISUAL_SESSION, "projects:arcane", 4, "base", "effective", visualBytes),
                request(SkillVfxEditorProtocol.Operation.REVERT_VISUAL_SESSION, "projects:arcane", 4, "base", "effective", null));
        for (SkillVfxEditorProtocol.Request request : valid) {
            byte[] bytes = SkillVfxEditorProtocol.encodeRequest(request);
            SkillVfxEditorProtocol.Request decoded = SkillVfxEditorProtocol.decodeRequest(bytes);
            check(decoded.operation() == request.operation() && decoded.correlation() == request.correlation() && Arrays.equals(decoded.visualBytes(), request.visualBytes()), "request roundtrip " + request.operation());
        }
        reject(() -> SkillVfxEditorProtocol.encodeRequest(request(SkillVfxEditorProtocol.Operation.CATALOG, "x", 0, "", "", null)));
        reject(() -> SkillVfxEditorProtocol.encodeRequest(request(SkillVfxEditorProtocol.Operation.FETCH, "projects:x", 1, "", "", null)));
        reject(() -> SkillVfxEditorProtocol.encodeRequest(request(SkillVfxEditorProtocol.Operation.FETCH, "projects:x", 0, "base", "", null)));
        reject(() -> SkillVfxEditorProtocol.encodeRequest(request(SkillVfxEditorProtocol.Operation.APPLY_VISUAL_SESSION, "projects:x", 0, "", "effective", visualBytes)));
        reject(() -> SkillVfxEditorProtocol.encodeRequest(request(SkillVfxEditorProtocol.Operation.REVERT_VISUAL_SESSION, "projects:x", 0, "base", "effective", visualBytes)));
        reject(() -> SkillVfxEditorProtocol.encodeRequest(new SkillVfxEditorProtocol.Request(SkillVfxEditorProtocol.Operation.FETCH, 0, SESSION, "projects:x", 0, "", "", null)));
    }

    private static void visualRoundTripsAllPrimitiveTypesAndSlots() {
        List<SkillVfxModel.Primitive> primitives = new ArrayList<>();
        int seed = 1;
        for (SkillVfxModel.PrimitiveType type : SkillVfxModel.PrimitiveType.values()) {
            SkillVfxModel.Primitive primitive = SkillVfxModel.defaults("p-" + type.name().toLowerCase(Locale.ROOT), type);
            if (type == SkillVfxModel.PrimitiveType.CIRCLE) primitive = primitive.withValue("radius", new SkillVfxModel.FromGameplay(SkillVfxModel.ActionField.RADIUS));
            if (type == SkillVfxModel.PrimitiveType.BURST) primitive = primitive.withValue("count", new SkillVfxModel.Literal(1));
            primitive = new SkillVfxModel.Primitive(primitive.id(), primitive.type(), primitive.delayTicks(), primitive.durationTicks(), primitive.argb(), primitive.width(), primitive.density(), seed++, primitive.offset(), primitive.yaw(), primitive.values(), primitive.controls());
            primitives.add(primitive);
        }
        SkillVfxModel.Visual authored = visual("projects:vfx/all-primitives", primitives);
        byte[] first = SkillVfxEditorProtocol.encodeVisual(authored);
        byte[] second = SkillVfxEditorProtocol.encodeVisual(authored);
        check(Arrays.equals(first, second), "deterministic visual bytes");
        SkillVfxModel.Visual decoded = SkillVfxEditorProtocol.decodeVisual(first);
        check(decoded.equals(authored) && decoded.emissions(SkillVfxModel.Hook.TELEGRAPH).getFirst().primitives().size() == 10, "all primitive roundtrip");
        for (SkillVfxModel.ActionField field : SkillVfxModel.ActionField.values()) {
            SkillVfxModel.Primitive circle = SkillVfxModel.defaults("field-" + field, SkillVfxModel.PrimitiveType.CIRCLE).withValue("radius", new SkillVfxModel.FromGameplay(field));
            SkillVfxModel.Visual fieldVisual = visual("projects:vfx/field", List.of(circle));
            check(SkillVfxEditorProtocol.decodeVisual(SkillVfxEditorProtocol.encodeVisual(fieldVisual)).equals(fieldVisual), "action field " + field);
        }
    }

    private static void decoderRejectsWireCorruption() throws Exception {
        byte[] wire = fixtureBytes();
        reject(() -> SkillVfxEditorProtocol.decodeState(null));
        reject(() -> SkillVfxEditorProtocol.decodeState(Arrays.copyOf(wire, 4)));
        reject(() -> SkillVfxEditorProtocol.decodeState(append(wire, (byte) 0)));
        byte[] wrongVersion = wire.clone(); wrongVersion[0] = 2; reject(() -> SkillVfxEditorProtocol.decodeState(wrongVersion));
        byte[] invalidStatus = wire.clone(); invalidStatus[1] = 127; reject(() -> SkillVfxEditorProtocol.decodeState(invalidStatus));
        byte[] nonpositive = wire.clone(); Arrays.fill(nonpositive, 2, 10, (byte) 0); reject(() -> SkillVfxEditorProtocol.decodeState(nonpositive));
        byte[] invalidUtf8 = wire.clone(); int arcane = indexOf(invalidUtf8, "Arcane".getBytes(StandardCharsets.UTF_8)); check(arcane >= 0, "fixture utf8 marker"); invalidUtf8[arcane] = (byte) 0xff; reject(() -> SkillVfxEditorProtocol.decodeState(invalidUtf8));
        byte[] unsorted = stateWithRows(List.of(row("projects:z", true), row("projects:a", true)));
        byte[] duplicate = stateWithRows(List.of(row("projects:a", true), row("projects:a", true)));
        byte[] malformedUnbound = stateWithRows(List.of(new Row("projects:a", "A", false, "x", 0, "", "", false)));
        reject(() -> SkillVfxEditorProtocol.decodeState(unsorted));
        reject(() -> SkillVfxEditorProtocol.decodeState(duplicate));
        reject(() -> SkillVfxEditorProtocol.decodeState(malformedUnbound));
        reject(() -> SkillVfxEditorProtocol.decodeVisual(new byte[] {1, 0, 14, 'p','r','o','j','e','c','t','s',':','v','f','x','/', 'x', 6}));
        byte[] request = SkillVfxEditorProtocol.encodeRequest(request(SkillVfxEditorProtocol.Operation.FETCH, "projects:x", 0, "", "", null));
        byte[] invalidOp = request.clone(); invalidOp[1] = 127; reject(() -> SkillVfxEditorProtocol.decodeRequest(invalidOp));
    }

    private static void encoderBoundsRejectBeforeNarrowing() {
        String tooLong = "x".repeat(SkillVfxEditorProtocol.MAX_STRING + 1);
        reject(() -> SkillVfxEditorProtocol.encodeRequest(request(SkillVfxEditorProtocol.Operation.FETCH, tooLong, 0, "", "", null)));
        List<SkillVfxModel.HookBinding> hooks = new ArrayList<>();
        for (SkillVfxModel.Hook hook : SkillVfxModel.Hook.values()) hooks.add(new SkillVfxModel.HookBinding(hook, List.of(new SkillVfxModel.Emission("e" + hook, 0, List.of(SkillVfxModel.defaults("p" + hook, SkillVfxModel.PrimitiveType.CIRCLE))))));
        reject(() -> SkillVfxEditorProtocol.encodeVisual(new SkillVfxModel.Visual("projects:vfx/hooks", hooks)));
        List<SkillVfxModel.Emission> emissions = new ArrayList<>(); for (int i = 0; i < 17; i++) emissions.add(new SkillVfxModel.Emission("e" + i, 0, List.of(SkillVfxModel.defaults("p" + i, SkillVfxModel.PrimitiveType.CIRCLE))));
        reject(() -> SkillVfxEditorProtocol.encodeVisual(new SkillVfxModel.Visual("projects:vfx/emissions", List.of(new SkillVfxModel.HookBinding(SkillVfxModel.Hook.CAST, emissions)))));
        List<SkillVfxModel.Primitive> primitives = new ArrayList<>(); for (int i = 0; i < 17; i++) primitives.add(SkillVfxModel.defaults("p" + i, SkillVfxModel.PrimitiveType.CIRCLE));
        reject(() -> SkillVfxEditorProtocol.encodeVisual(visual("projects:vfx/primitives", primitives)));
        reject(() -> new SkillVfxModel.Primitive("controls", SkillVfxModel.PrimitiveType.BEZIER, 0, 1, 0, 1, 1, 0, new SkillVfxModel.Vec(0, 0, 0), 0, Map.of(), Collections.nCopies(9, new SkillVfxModel.Vec(0, 0, 0))));
    }

    private static SkillVfxEditorProtocol.Request request(SkillVfxEditorProtocol.Operation operation, String ability, long revision, String base, String effective, byte[] visual) { return new SkillVfxEditorProtocol.Request(operation, 9, SESSION, ability, revision, base, effective, visual); }
    private static SkillVfxModel.Visual visual(String id, List<SkillVfxModel.Primitive> primitives) { return new SkillVfxModel.Visual(id, List.of(new SkillVfxModel.HookBinding(SkillVfxModel.Hook.TELEGRAPH, List.of(new SkillVfxModel.Emission("emission", 0, primitives))))); }
    private record Row(String id, String name, boolean has, String visual, long revision, String base, String effective, boolean override) { }
    private static Row row(String id, boolean has) { return has ? new Row(id, id, true, "projects:vfx/x", 1, "base", "effective", false) : new Row(id, id, false, "", 0, "", "", false); }
    private static byte[] stateWithRows(List<Row> rows) throws IOException { ByteArrayOutputStream bytes = new ByteArrayOutputStream(); try (DataOutputStream out = new DataOutputStream(bytes)) { out.writeByte(1); out.writeByte(0); out.writeLong(1); out.writeLong(SESSION.getMostSignificantBits()); out.writeLong(SESSION.getLeastSignificantBits()); out.writeBoolean(false); string(out, ""); out.writeShort(rows.size()); for (Row row : rows) { string(out, row.id); string(out, row.name); out.writeBoolean(row.has); string(out, row.visual); out.writeLong(row.revision); string(out, row.base); string(out, row.effective); out.writeBoolean(row.override); } out.writeBoolean(false); } return bytes.toByteArray(); }
    private static void string(DataOutputStream out, String value) throws IOException { byte[] bytes = value.getBytes(StandardCharsets.UTF_8); out.writeShort(bytes.length); out.write(bytes); }
    private static byte[] fixtureBytes() throws IOException { return HexFormat.of().parseHex(Files.readString(Path.of("devtools/src/test/resources/protocol/skill-vfx-editor-v1-golden.hex")).replaceAll("\\s", "")); }
    private static int indexOf(byte[] body, byte[] needle) { outer: for (int index = 0; index <= body.length - needle.length; index++) { for (int part = 0; part < needle.length; part++) if (body[index + part] != needle[part]) continue outer; return index; } return -1; }
    private static byte[] append(byte[] value, byte trailing) { byte[] copy = Arrays.copyOf(value, value.length + 1); copy[value.length] = trailing; return copy; }
    private static String sha(byte[] bytes) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
    private static void reject(Runnable action) { try { action.run(); throw new AssertionError("expected protocol rejection"); } catch (IllegalArgumentException expected) { } }
    private static void check(boolean condition, String label) { if (!condition) throw new AssertionError(label); }
}
