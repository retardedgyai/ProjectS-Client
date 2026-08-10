package io.github.gyai.projects.devtools.skillvfx;

import io.github.gyai.projects.client.vfx.AbilityVfx;
import io.github.gyai.projects.client.vfx.MotionDirection;
import io.github.gyai.projects.client.vfx.MotionEasing;
import io.github.gyai.projects.client.vfx.MotionMode;
import io.github.gyai.projects.client.vfx.MotionSpec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Additive Motion envelope over the unchanged canonical v1 editor body. */
public final class SkillVfxEditorProtocolV3 {
    public static final int VERSION = 3;
    public static final int MAX_PACKET = SkillVfxEditorProtocol.MAX_PACKET;
    public static final int MAX_STRING = SkillVfxEditorProtocol.MAX_STRING;
    private static final int MAX_TABLES = 2;
    private static final int MAX_TABLE_ENTRIES = SkillVfxEditorProtocol.MAX_HOOKS
            * SkillVfxEditorProtocol.MAX_EMISSIONS * SkillVfxEditorProtocol.MAX_PRIMITIVES;

    private SkillVfxEditorProtocolV3() { }

    public static byte[] encodeRequest(SkillVfxEditorProtocol.Request request) {
        if (request == null) throw bad("request");
        SkillVfxModel.Visual visual = request.visualBytes() == null
                ? null : SkillVfxEditorProtocol.decodeVisual(request.visualBytes());
        return frame(SkillVfxEditorProtocol.encodeRequest(request),
                visual == null ? List.of() : List.of(visual));
    }

    /** Uses the supplied working visual for the authoritative Appearance/Motion table. */
    public static byte[] encodeRequest(SkillVfxEditorProtocol.Request request,
                                       SkillVfxModel.Visual visual) {
        if (request == null || (request.visualBytes() == null) != (visual == null)) {
            throw bad("request visual");
        }
        if (visual != null) {
            SkillVfxModel.Visual base = SkillVfxEditorProtocol.decodeVisual(request.visualBytes());
            if (!primitiveIds(base).equals(primitiveIds(visual))) throw bad("request primitive ids");
        }
        return frame(SkillVfxEditorProtocol.encodeRequest(request),
                visual == null ? List.of() : List.of(visual));
    }

    public static SkillVfxEditorProtocol.Request decodeRequest(byte[] bytes) {
        Frame frame = unframe(bytes);
        SkillVfxEditorProtocol.Request request = SkillVfxEditorProtocol.decodeRequest(frame.body());
        int expected = request.visualBytes() == null ? 0 : 1;
        if (frame.tables().size() != expected) throw bad("request table count");
        if (request.visualBytes() == null) return request;
        SkillVfxModel.Visual visual = patch(
                SkillVfxEditorProtocol.decodeVisual(request.visualBytes()), frame.tables().getFirst());
        return new SkillVfxEditorProtocol.Request(request.operation(), request.correlation(), request.session(),
                request.abilityId(), request.revision(), request.baseFingerprint(),
                request.effectiveFingerprint(), SkillVfxEditorProtocol.encodeVisual(visual));
    }

    /** Motion-aware request view; the raw Request keeps its legacy byte-shaped API. */
    public static SkillVfxModel.Visual decodeRequestVisual(byte[] bytes) {
        Frame frame = unframe(bytes);
        SkillVfxEditorProtocol.Request request = SkillVfxEditorProtocol.decodeRequest(frame.body());
        if (request.visualBytes() == null || frame.tables().size() != 1) throw bad("request table count");
        return patch(SkillVfxEditorProtocol.decodeVisual(request.visualBytes()), frame.tables().getFirst());
    }

    public static byte[] encodeState(SkillVfxEditorProtocol.State state) {
        if (state == null) throw bad("state");
        List<SkillVfxModel.Visual> visuals = state.snapshot() == null
                ? List.of() : List.of(state.snapshot().base(), state.snapshot().effective());
        return frame(SkillVfxEditorProtocol.encodeState(state), visuals);
    }

    public static SkillVfxEditorProtocol.State decodeState(byte[] bytes) {
        Frame frame = unframe(bytes);
        SkillVfxEditorProtocol.State state = SkillVfxEditorProtocol.decodeState(frame.body());
        if (state.snapshot() == null) {
            if (!frame.tables().isEmpty()) throw bad("state table count");
            return state;
        }
        if (frame.tables().size() != 2) throw bad("state table count");
        SkillVfxEditorProtocol.Snapshot old = state.snapshot();
        SkillVfxEditorProtocol.Snapshot snapshot = new SkillVfxEditorProtocol.Snapshot(
                old.abilityId(), old.displayName(), old.gameplay(), old.visualId(),
                patch(old.base(), frame.tables().get(0)),
                patch(old.effective(), frame.tables().get(1)), old.revision(),
                old.baseFingerprint(), old.effectiveFingerprint(), old.sessionOverride());
        return new SkillVfxEditorProtocol.State(state.status(), state.correlation(), state.session(),
                state.catalog(), snapshot, state.previewAllowed(), state.message(), frame.body());
    }

    public static byte[] encodeVisual(SkillVfxModel.Visual visual) {
        return frame(SkillVfxEditorProtocol.encodeVisual(visual), List.of(visual));
    }

    public static SkillVfxModel.Visual decodeVisual(byte[] bytes) {
        Frame frame = unframe(bytes);
        if (frame.tables().size() != 1) throw bad("visual table count");
        return patch(SkillVfxEditorProtocol.decodeVisual(frame.body()), frame.tables().getFirst());
    }

    private record Entry(SkillVfxModel.Appearance appearance, MotionSpec motion) { }
    private record Frame(byte[] body, List<Map<String, Entry>> tables) { }

    private static byte[] frame(byte[] body, List<SkillVfxModel.Visual> visuals) {
        if (body == null || body.length > 0xffff || visuals == null || visuals.size() > MAX_TABLES) {
            throw bad("frame bounds");
        }
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                output.writeByte(VERSION);
                output.writeShort(body.length);
                output.write(body);
                output.writeByte(visuals.size());
                for (SkillVfxModel.Visual visual : visuals) writeTable(output, visual);
            }
            if (bytes.size() > MAX_PACKET) throw bad("packet");
            return bytes.toByteArray();
        } catch (IOException exception) {
            throw bad("write", exception);
        }
    }

    private static Frame unframe(byte[] bytes) {
        if (bytes == null || bytes.length > MAX_PACKET) throw bad("packet");
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
            if (input.readUnsignedByte() != VERSION) throw new IOException("version");
            int bodyLength = input.readUnsignedShort();
            byte[] body = input.readNBytes(bodyLength);
            if (body.length != bodyLength) throw new EOFException();
            int count = input.readUnsignedByte();
            if (count > MAX_TABLES) throw new IOException("table count");
            List<Map<String, Entry>> tables = new ArrayList<>(count);
            for (int index = 0; index < count; index++) tables.add(readTable(input));
            if (input.available() != 0) throw new IOException("trailing");
            return new Frame(body, List.copyOf(tables));
        } catch (IOException | RuntimeException exception) {
            throw bad("malformed v3 editor packet", exception);
        }
    }

    private static void writeTable(DataOutputStream output, SkillVfxModel.Visual visual) throws IOException {
        List<SkillVfxModel.Primitive> primitives = primitives(visual);
        if (primitives.size() > MAX_TABLE_ENTRIES) throw new IOException("table entries");
        output.writeShort(primitives.size());
        Set<String> ids = new HashSet<>();
        for (SkillVfxModel.Primitive primitive : primitives) {
            if (!ids.add(primitive.id())) throw new IOException("duplicate primitive id");
            string(output, primitive.id());
            output.writeByte(primitive.appearance().kind().ordinal());
            string(output, primitive.appearance().id());
            output.writeByte(primitive.motion().mode().ordinal());
            output.writeByte(primitive.motion().direction().ordinal());
            output.writeByte(primitive.motion().easing().ordinal());
            output.writeDouble(primitive.motion().phase());
            output.writeDouble(primitive.motion().trailFraction());
        }
    }

    private static Map<String, Entry> readTable(DataInputStream input) throws IOException {
        int count = input.readUnsignedShort();
        if (count > MAX_TABLE_ENTRIES) throw new IOException("table entries");
        Map<String, Entry> result = new HashMap<>();
        for (int index = 0; index < count; index++) {
            String id = string(input);
            AbilityVfx.AppearanceKind kind = enumValue(AbilityVfx.AppearanceKind.values(), input.readUnsignedByte());
            SkillVfxModel.Appearance appearance = new SkillVfxModel.Appearance(kind, string(input));
            MotionMode mode = enumValue(MotionMode.values(), input.readUnsignedByte());
            MotionDirection direction = enumValue(MotionDirection.values(), input.readUnsignedByte());
            MotionEasing easing = enumValue(MotionEasing.values(), input.readUnsignedByte());
            MotionSpec motion = new MotionSpec(mode, direction, easing, input.readDouble(), input.readDouble());
            if (result.put(id, new Entry(appearance, motion)) != null) throw new IOException("duplicate id");
        }
        return Map.copyOf(result);
    }

    private static SkillVfxModel.Visual patch(SkillVfxModel.Visual visual, Map<String, Entry> entries) {
        List<SkillVfxModel.Primitive> all = primitives(visual);
        Set<String> ids = new HashSet<>();
        for (SkillVfxModel.Primitive primitive : all) ids.add(primitive.id());
        if (entries.size() != all.size() || !entries.keySet().equals(ids)) throw bad("primitive ids");
        List<SkillVfxModel.HookBinding> hooks = new ArrayList<>();
        for (SkillVfxModel.HookBinding hook : visual.hooks()) {
            List<SkillVfxModel.Emission> emissions = new ArrayList<>();
            for (SkillVfxModel.Emission emission : hook.emissions()) {
                List<SkillVfxModel.Primitive> patched = new ArrayList<>();
                for (SkillVfxModel.Primitive primitive : emission.primitives()) {
                    Entry entry = entries.get(primitive.id());
                    patched.add(primitive.withAppearance(entry.appearance()).withMotion(entry.motion()));
                }
                emissions.add(new SkillVfxModel.Emission(emission.id(), emission.actionIndex(), patched));
            }
            hooks.add(new SkillVfxModel.HookBinding(hook.hook(), emissions));
        }
        return new SkillVfxModel.Visual(visual.id(), hooks);
    }

    private static List<SkillVfxModel.Primitive> primitives(SkillVfxModel.Visual visual) {
        if (visual == null) throw bad("visual");
        List<SkillVfxModel.Primitive> result = new ArrayList<>();
        for (SkillVfxModel.HookBinding hook : visual.hooks()) {
            for (SkillVfxModel.Emission emission : hook.emissions()) result.addAll(emission.primitives());
        }
        return result;
    }

    private static Set<String> primitiveIds(SkillVfxModel.Visual visual) {
        Set<String> result = new HashSet<>();
        for (SkillVfxModel.Primitive primitive : primitives(visual)) {
            if (!result.add(primitive.id())) throw bad("duplicate primitive id");
        }
        return result;
    }

    private static void string(DataOutputStream output, String value) throws IOException {
        if (value == null) throw new IOException("string");
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_STRING) throw new IOException("string");
        output.writeShort(bytes.length);
        output.write(bytes);
    }

    private static String string(DataInputStream input) throws IOException {
        int length = input.readUnsignedShort();
        if (length > MAX_STRING) throw new IOException("string");
        byte[] bytes = input.readNBytes(length);
        if (bytes.length != length) throw new EOFException();
        try {
            String value = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
            if (!Arrays.equals(bytes, value.getBytes(StandardCharsets.UTF_8))) throw new IOException("utf8");
            return value;
        } catch (CharacterCodingException exception) {
            throw new IOException("utf8", exception);
        }
    }

    private static <T> T enumValue(T[] values, int ordinal) throws IOException {
        if (ordinal < 0 || ordinal >= values.length) throw new IOException("enum");
        return values[ordinal];
    }

    private static IllegalArgumentException bad(String message) { return new IllegalArgumentException(message); }
    private static IllegalArgumentException bad(String message, Exception cause) {
        return new IllegalArgumentException(message, cause);
    }
}
