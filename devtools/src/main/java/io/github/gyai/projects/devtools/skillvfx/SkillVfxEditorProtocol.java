package io.github.gyai.projects.devtools.skillvfx;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.util.*;

/** Client source mirror of the server's fixed-endian Skill/VFX editor v1 codec. */
public final class SkillVfxEditorProtocol {
    public static final int VERSION = 1, MAX_PACKET = 64 * 1024, MAX_ABILITIES = 64,
            MAX_ACTIONS = 64, MAX_HOOKS = 5, MAX_EMISSIONS = 16, MAX_PRIMITIVES = 16,
            MAX_POINTS = 8, MAX_STRING = 256;
    private static final String[] TARGETS = {"SELF", "PRIMARY_TARGET"};
    private static final String[] DAMAGE_TYPES = {"PHYSICAL", "MAGICAL", "TRUE"};
    private static final String[] DAMAGE_KINDS = {"NORMAL_ATTACK", "DIRECT_SKILL", "DAMAGE_OVER_TIME", "REFLECTED", "PERCENT_HEALTH"};
    private static final String[] ATTACK_TAGS = {"MELEE", "PROJECTILE", "MAGIC", "PHYSICAL", "NORMAL_ATTACK", "SKILL", "SHATTER", "FIRE", "ICE", "LIGHTNING"};
    private static final String[] ELEMENTS = {"FIRE", "ICE", "LIGHTNING"};
    private static final String[] SCALAR_NAMES = {"size", "radius", "length", "height", "angle", "startAngle", "sweepAngle", "turns"};

    public enum Operation { CATALOG, FETCH, APPLY_VISUAL_SESSION, REVERT_VISUAL_SESSION }
    public enum Status { OK, PERMISSION_DENIED, MALFORMED, STALE, NOT_FOUND, CONFLICT }

    /** visualBytes is the exact visual sub-packet because client DTOs intentionally do not duplicate server visual types. */
    public record Request(Operation operation, long correlation, UUID session, String abilityId, long revision,
                          String baseFingerprint, String effectiveFingerprint, byte[] visualBytes) {
        public Request {
            visualBytes = visualBytes == null ? null : visualBytes.clone();
        }
        @Override public byte[] visualBytes() { return visualBytes == null ? null : visualBytes.clone(); }
    }
    public record CatalogItem(String abilityId, String displayName, boolean hasVisual, String visualId, long revision,
                              String baseFingerprint, String effectiveFingerprint, boolean sessionOverride) { }
    public record Snapshot(String abilityId, String displayName, List<SkillVfxModel.GameplayAction> gameplay,
                           String visualId, SkillVfxModel.Visual base, SkillVfxModel.Visual effective, long revision,
                           String baseFingerprint, String effectiveFingerprint, boolean sessionOverride) {
        public Snapshot { gameplay = List.copyOf(gameplay == null ? List.of() : gameplay); }
    }
    /** canonical is retained only for exact re-emission of an authority-provided State. */
    public record State(Status status, long correlation, UUID session, List<CatalogItem> catalog, Snapshot snapshot,
                        boolean previewAllowed, String message, byte[] canonical) {
        public State {
            catalog = List.copyOf(catalog == null ? List.of() : catalog);
            canonical = canonical == null ? null : canonical.clone();
        }
        @Override public byte[] canonical() { return canonical == null ? null : canonical.clone(); }
    }

    private SkillVfxEditorProtocol() { }

    public static byte[] encodeRequest(Request value) {
        validateRequest(value);
        return out(o -> {
            o.writeByte(VERSION); o.writeByte(value.operation().ordinal()); o.writeLong(value.correlation());
            uuid(o, value.session()); str(o, value.abilityId()); o.writeLong(value.revision());
            str(o, value.baseFingerprint()); str(o, value.effectiveFingerprint());
            byte[] visual = value.visualBytes(); o.writeBoolean(visual != null); if (visual != null) o.write(visual);
        });
    }

    public static Request decodeRequest(byte[] bytes) {
        return in(bytes, i -> {
            version(i);
            Operation operation = enumValue(Operation.values(), i.readUnsignedByte());
            long correlation = i.readLong();
            UUID session = uuid(i);
            String abilityId = str(i);
            long revision = i.readLong();
            String base = str(i), effective = str(i);
            byte[] visual = i.readBoolean() ? i.readAllBytes() : null;
            if (visual != null) decodeVisual(visual); // also enforces the visual's no-trailing rule
            Request request = new Request(operation, correlation, session, abilityId, revision, base, effective, visual);
            validateRequest(request);
            return request;
        });
    }

    public static State decodeState(byte[] bytes) {
        return in(bytes, i -> {
            version(i);
            Status status = enumValue(Status.values(), i.readUnsignedByte());
            long correlation = i.readLong();
            if (correlation <= 0) throw new IOException("correlation");
            UUID session = uuid(i);
            boolean preview = i.readBoolean();
            String message = str(i);
            int count = i.readUnsignedShort();
            if (count > MAX_ABILITIES) throw new IOException("catalog");
            List<CatalogItem> catalog = new ArrayList<>(count);
            for (int index = 0; index < count; index++) {
                String ability = str(i), name = str(i);
                boolean hasVisual = i.readBoolean();
                catalog.add(new CatalogItem(ability, name, hasVisual, str(i), i.readLong(), str(i), str(i), i.readBoolean()));
            }
            Snapshot snapshot = i.readBoolean() ? snapshot(i) : null;
            State state = new State(status, correlation, session, catalog, snapshot, preview, message, bytes);
            validateState(state);
            return state;
        });
    }

    /** Received state is re-emitted byte-for-byte after validation; client never authors State messages. */
    public static byte[] encodeState(State value) {
        validateState(value);
        if (value.canonical() == null) throw new IllegalArgumentException("client only encodes received state");
        return value.canonical();
    }

    public static byte[] encodeVisual(SkillVfxModel.Visual visual) { return out(o -> visual(o, visual)); }
    public static SkillVfxModel.Visual decodeVisual(byte[] bytes) { return in(bytes, SkillVfxEditorProtocol::visual); }

    private static void validateRequest(Request request) {
        if (request == null || request.operation() == null || request.correlation() <= 0 || request.session() == null
                || request.abilityId() == null || request.baseFingerprint() == null || request.effectiveFingerprint() == null) {
            throw new IllegalArgumentException("request");
        }
        validateString(request.abilityId()); validateString(request.baseFingerprint()); validateString(request.effectiveFingerprint());
        switch (request.operation()) {
            case CATALOG -> {
                if (!request.abilityId().isEmpty() || request.revision() != 0 || !request.baseFingerprint().isEmpty()
                        || !request.effectiveFingerprint().isEmpty() || request.visualBytes() != null) throw new IllegalArgumentException("catalog shape");
            }
            case FETCH -> {
                if (request.abilityId().isBlank() || request.revision() != 0 || !request.baseFingerprint().isEmpty()
                        || !request.effectiveFingerprint().isEmpty() || request.visualBytes() != null) throw new IllegalArgumentException("fetch shape");
            }
            case APPLY_VISUAL_SESSION -> {
                if (request.abilityId().isBlank() || request.revision() < 0 || request.baseFingerprint().isBlank()
                        || request.effectiveFingerprint().isBlank() || request.visualBytes() == null) throw new IllegalArgumentException("apply shape");
                decodeVisual(request.visualBytes());
            }
            case REVERT_VISUAL_SESSION -> {
                if (request.abilityId().isBlank() || request.revision() < 0 || request.baseFingerprint().isBlank()
                        || request.effectiveFingerprint().isBlank() || request.visualBytes() != null) throw new IllegalArgumentException("revert shape");
            }
        }
    }

    private static void validateState(State state) {
        if (state == null || state.status() == null || state.correlation() <= 0 || state.session() == null
                || state.catalog() == null || state.catalog().size() > MAX_ABILITIES || state.message() == null) throw new IllegalArgumentException("state");
        validateString(state.message());
        String previous = "";
        for (CatalogItem item : state.catalog()) {
            if (item == null || item.abilityId() == null || item.displayName() == null || item.visualId() == null
                    || item.baseFingerprint() == null || item.effectiveFingerprint() == null || item.abilityId().compareTo(previous) <= 0) {
                throw new IllegalArgumentException("catalog");
            }
            validateString(item.abilityId()); validateString(item.displayName()); validateString(item.visualId());
            validateString(item.baseFingerprint()); validateString(item.effectiveFingerprint());
            if (!item.hasVisual() && (!item.visualId().isEmpty() || !item.baseFingerprint().isEmpty()
                    || !item.effectiveFingerprint().isEmpty() || item.sessionOverride())) throw new IllegalArgumentException("unbound catalog item");
            if (item.hasVisual() && (item.visualId().isBlank() || item.baseFingerprint().isBlank() || item.effectiveFingerprint().isBlank())) throw new IllegalArgumentException("bound catalog item");
            previous = item.abilityId();
        }
        if (state.snapshot() != null) validateSnapshot(state.snapshot());
    }

    private static void validateSnapshot(Snapshot snapshot) {
        if (snapshot.abilityId() == null || snapshot.displayName() == null || snapshot.visualId() == null || snapshot.base() == null
                || snapshot.effective() == null || snapshot.revision() < 0 || snapshot.baseFingerprint() == null || snapshot.effectiveFingerprint() == null) {
            throw new IllegalArgumentException("snapshot");
        }
        validateString(snapshot.abilityId()); validateString(snapshot.displayName()); validateString(snapshot.visualId());
        validateString(snapshot.baseFingerprint()); validateString(snapshot.effectiveFingerprint());
        if (snapshot.gameplay().isEmpty() || snapshot.gameplay().size() > MAX_ACTIONS) throw new IllegalArgumentException("actions");
        for (SkillVfxModel.GameplayAction action : snapshot.gameplay()) if (action == null) throw new IllegalArgumentException("action");
        encodeVisual(snapshot.base()); encodeVisual(snapshot.effective());
    }

    private static Snapshot snapshot(DataInputStream input) throws IOException {
        Object[] ability = ability(input);
        String visualId = str(input);
        SkillVfxModel.Visual base = visual(input), effective = visual(input);
        return new Snapshot((String) ability[0], (String) ability[1], castActions(ability[2]), visualId, base, effective,
                input.readLong(), str(input), str(input), input.readBoolean());
    }

    @SuppressWarnings("unchecked") private static List<SkillVfxModel.GameplayAction> castActions(Object actions) {
        return (List<SkillVfxModel.GameplayAction>) actions;
    }

    private static Object[] ability(DataInputStream input) throws IOException {
        if (input.readUnsignedByte() != VERSION) throw new IOException("ability schema");
        String id = str(input), name = str(input);
        if (!namespaced(id) || name.isBlank()) throw new IOException("ability");
        int count = input.readUnsignedByte();
        if (count == 0 || count > MAX_ACTIONS) throw new IOException("actions");
        List<SkillVfxModel.GameplayAction> actions = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            switch (input.readUnsignedByte()) {
                case 0 -> actions.add(new SkillVfxModel.GameplayAction("Wait", "Wait", Map.of("ticks", Integer.toString(input.readInt()))));
                case 1 -> {
                    String target = enumName(TARGETS, input.readUnsignedByte());
                    String origin = enumName(TARGETS, input.readUnsignedByte());
                    double radius = input.readDouble(); int duration = input.readInt(); boolean locked = input.readBoolean();
                    actions.add(new SkillVfxModel.GameplayAction("CircleTelegraph", "Circle telegraph", Map.of(
                            "target", target, "origin", origin, "radius", Double.toString(radius), "duration", Integer.toString(duration), "lockAtCreation", Boolean.toString(locked))));
                }
                case 2 -> {
                    String target = enumName(TARGETS, input.readUnsignedByte());
                    String damageType = enumName(DAMAGE_TYPES, input.readUnsignedByte());
                    String damageKind = enumName(DAMAGE_KINDS, input.readUnsignedByte());
                    double fixedDamage = input.readDouble(), coefficient = input.readDouble(); boolean critical = input.readBoolean();
                    int tagMask = input.readUnsignedShort();
                    Map<String, String> fields = new LinkedHashMap<>();
                    fields.put("target", target); fields.put("damageType", damageType); fields.put("damageKind", damageKind);
                    fields.put("fixedDamage", Double.toString(fixedDamage)); fields.put("coefficient", Double.toString(coefficient));
                    fields.put("critical", Boolean.toString(critical)); fields.put("tagMask", Integer.toString(tagMask));
                    List<String> tags = new ArrayList<>();
                    for (int tag = 0; tag < ATTACK_TAGS.length; tag++) if ((tagMask & (1 << tag)) != 0) tags.add(ATTACK_TAGS[tag]);
                    fields.put("tags", String.join(",", tags));
                    for (String element : ELEMENTS) {
                        fields.put("element." + element + ".value", Double.toString(input.readDouble()));
                        fields.put("element." + element + ".scalingRate", Double.toString(input.readDouble()));
                    }
                    actions.add(new SkillVfxModel.GameplayAction("Damage", "Damage", fields));
                }
                default -> throw new IOException("action");
            }
        }
        return new Object[] {id, name, List.copyOf(actions)};
    }

    private static void visual(DataOutputStream output, SkillVfxModel.Visual visual) throws IOException {
        validateVisual(visual);
        output.writeByte(VERSION); str(output, visual.id()); output.writeByte(visual.hooks().size());
        for (SkillVfxModel.HookBinding binding : visual.hooks()) {
            if (binding == null || binding.hook() == null || binding.emissions() == null || binding.emissions().isEmpty() || binding.emissions().size() > MAX_EMISSIONS) throw new IOException("emissions");
            output.writeByte(binding.hook().ordinal()); output.writeByte(binding.emissions().size());
            for (SkillVfxModel.Emission emission : binding.emissions()) {
                if (emission == null || emission.primitives() == null || emission.primitives().isEmpty() || emission.primitives().size() > MAX_PRIMITIVES) throw new IOException("primitives");
                str(output, emission.id()); output.writeInt(emission.actionIndex()); output.writeByte(emission.primitives().size());
                for (SkillVfxModel.Primitive primitive : emission.primitives()) primitive(output, primitive);
            }
        }
    }

    private static SkillVfxModel.Visual visual(DataInputStream input) throws IOException {
        if (input.readUnsignedByte() != VERSION) throw new IOException("visual schema");
        String id = str(input);
        int hookCount = input.readUnsignedByte();
        if (hookCount > MAX_HOOKS) throw new IOException("hooks");
        List<SkillVfxModel.HookBinding> hooks = new ArrayList<>(hookCount);
        for (int hookIndex = 0; hookIndex < hookCount; hookIndex++) {
            SkillVfxModel.Hook hook = enumValue(SkillVfxModel.Hook.values(), input.readUnsignedByte());
            int emissionCount = input.readUnsignedByte();
            if (emissionCount == 0 || emissionCount > MAX_EMISSIONS) throw new IOException("emissions");
            List<SkillVfxModel.Emission> emissions = new ArrayList<>(emissionCount);
            for (int emissionIndex = 0; emissionIndex < emissionCount; emissionIndex++) {
                String emissionId = str(input); int actionIndex = input.readInt(); int primitiveCount = input.readUnsignedByte();
                if (primitiveCount == 0 || primitiveCount > MAX_PRIMITIVES) throw new IOException("primitives");
                List<SkillVfxModel.Primitive> primitives = new ArrayList<>(primitiveCount);
                for (int primitiveIndex = 0; primitiveIndex < primitiveCount; primitiveIndex++) primitives.add(primitive(input));
                emissions.add(new SkillVfxModel.Emission(emissionId, actionIndex, primitives));
            }
            hooks.add(new SkillVfxModel.HookBinding(hook, emissions));
        }
        SkillVfxModel.Visual visual = new SkillVfxModel.Visual(id, hooks);
        validateVisual(visual);
        return visual;
    }

    private static void primitive(DataOutputStream output, SkillVfxModel.Primitive primitive) throws IOException {
        if (primitive == null || primitive.controls() == null || primitive.controls().size() > MAX_POINTS) throw new IOException("points");
        str(output, primitive.id()); output.writeByte(primitive.type().ordinal()); output.writeInt(primitive.delayTicks());
        output.writeInt(primitive.durationTicks()); output.writeInt(primitive.argb()); output.writeDouble(primitive.width());
        output.writeInt(primitive.density()); output.writeLong(primitive.seed()); vec(output, primitive.offset()); output.writeDouble(primitive.yaw());
        for (String name : SCALAR_NAMES) scalar(output, primitive.value(name));
        SkillVfxModel.Scalar count = primitive.value("count");
        if (count != null && !(count instanceof SkillVfxModel.Literal)) throw new IOException("count");
        output.writeInt(count instanceof SkillVfxModel.Literal literal ? checkedInt(literal.value()) : 0);
        output.writeByte(primitive.controls().size());
        for (SkillVfxModel.Vec control : primitive.controls()) vec(output, control);
    }

    private static SkillVfxModel.Primitive primitive(DataInputStream input) throws IOException {
        String id = str(input); SkillVfxModel.PrimitiveType type = enumValue(SkillVfxModel.PrimitiveType.values(), input.readUnsignedByte());
        int delay = input.readInt(), duration = input.readInt(), argb = input.readInt(); double width = input.readDouble();
        int density = input.readInt(); long seed = input.readLong(); SkillVfxModel.Vec offset = vec(input); double yaw = input.readDouble();
        Map<String, SkillVfxModel.Scalar> values = new TreeMap<>();
        for (String name : SCALAR_NAMES) { SkillVfxModel.Scalar scalar = scalar(input); if (scalar != null) values.put(name, scalar); }
        int count = input.readInt(); if (count != 0) values.put("count", new SkillVfxModel.Literal(count));
        int controlCount = input.readUnsignedByte(); if (controlCount > MAX_POINTS) throw new IOException("points");
        List<SkillVfxModel.Vec> controls = new ArrayList<>(controlCount);
        for (int index = 0; index < controlCount; index++) controls.add(vec(input));
        return new SkillVfxModel.Primitive(id, type, delay, duration, argb, width, density, seed, offset, yaw, values, controls);
    }

    /** Mirrors AbilityVisualDefinition's server-side construction rules before a client packet is emitted. */
    private static void validateVisual(SkillVfxModel.Visual visual) throws IOException {
        if (visual == null || !namespaced(visual.id()) || visual.hooks() == null || visual.hooks().size() > MAX_HOOKS) throw new IOException("hooks");
        EnumSet<SkillVfxModel.Hook> seenHooks = EnumSet.noneOf(SkillVfxModel.Hook.class);
        Set<String> primitiveIds = new HashSet<>();
        for (SkillVfxModel.HookBinding binding : visual.hooks()) {
            if (binding == null || binding.hook() == null || binding.hook() == SkillVfxModel.Hook.TRAVEL || !seenHooks.add(binding.hook())
                    || binding.emissions() == null || binding.emissions().isEmpty() || binding.emissions().size() > MAX_EMISSIONS) throw new IOException("emissions");
            Set<String> emissionIds = new HashSet<>();
            for (SkillVfxModel.Emission emission : binding.emissions()) {
                if (emission == null || emission.id() == null || emission.id().isBlank() || emission.id().length() > 64 || !emissionIds.add(emission.id())
                        || emission.actionIndex() < -1 || emission.primitives() == null || emission.primitives().isEmpty() || emission.primitives().size() > MAX_PRIMITIVES) throw new IOException("primitives");
                for (SkillVfxModel.Primitive primitive : emission.primitives()) {
                    if (primitive == null || primitive.id() == null || primitive.id().isBlank() || primitive.id().length() > 64 || !primitiveIds.add(primitive.id())) throw new IOException("primitive");
                    validatePrimitive(primitive);
                }
            }
        }
    }

    private static void validatePrimitive(SkillVfxModel.Primitive primitive) throws IOException {
        if (primitive.type() == null || primitive.delayTicks() < 0 || primitive.delayTicks() > 200 || primitive.durationTicks() < 1
                || primitive.durationTicks() > 1200 || primitive.delayTicks() + primitive.durationTicks() > 1200 || !finite(primitive.width())
                || primitive.width() <= 0 || primitive.width() > 16 || primitive.density() < 1 || primitive.density() > 256 || primitive.offset() == null
                || !validVec(primitive.offset()) || !finite(primitive.yaw()) || primitive.controls() == null || primitive.controls().size() > MAX_POINTS) throw new IOException("primitive bounds");
        for (SkillVfxModel.Vec control : primitive.controls()) if (!validVec(control)) throw new IOException("vector");
        Map<String, SkillVfxModel.Scalar> values = primitive.values();
        for (Map.Entry<String, SkillVfxModel.Scalar> entry : values.entrySet()) {
            if (!Arrays.asList(SCALAR_NAMES).contains(entry.getKey()) && !entry.getKey().equals("count")) throw new IOException("unknown primitive value");
        }
        SkillVfxModel.Scalar size = primitive.value("size"), radius = primitive.value("radius"), length = primitive.value("length"), height = primitive.value("height");
        SkillVfxModel.Scalar angle = primitive.value("angle"), start = primitive.value("startAngle"), sweep = primitive.value("sweepAngle"), turns = primitive.value("turns");
        if (!bounded(size, 128) || !bounded(radius, 128) || !bounded(length, 128) || !bounded(height, 128) || !bounded(angle, Math.PI)
                || !bounded(sweep, 4 * Math.PI) || !bounded(turns, 32)) throw new IOException("scalar bounds");
        int count = 0; SkillVfxModel.Scalar countScalar = primitive.value("count");
        if (countScalar != null) { if (!(countScalar instanceof SkillVfxModel.Literal literal)) throw new IOException("count"); count = checkedInt(literal.value()); }
        if (count < 0 || count > 64) throw new IOException("count");
        boolean common = zero(size) && zero(radius) && zero(length) && zero(height) && zero(angle) && zero(start) && zero(sweep) && zero(turns) && count == 0;
        boolean valid = switch (primitive.type()) {
            case POINT -> positive(size) && zero(radius) && zero(length) && zero(height) && zero(angle) && zero(start) && zero(sweep) && zero(turns) && count == 0 && primitive.controls().isEmpty();
            case LINE -> positive(length) && zero(size) && zero(radius) && zero(height) && zero(angle) && zero(start) && zero(sweep) && zero(turns) && count == 0 && (primitive.controls().isEmpty() || primitive.controls().size() == 2);
            case ARC -> positive(radius) && nonZero(sweep) && zero(size) && zero(length) && zero(height) && zero(angle) && zero(turns) && count == 0 && primitive.controls().isEmpty();
            case CIRCLE, SPHERE -> positive(radius) && zero(size) && zero(length) && zero(height) && zero(angle) && zero(sweep) && zero(turns) && count == 0 && primitive.controls().isEmpty();
            case CONE -> positive(length) && angle(angle) && zero(size) && zero(radius) && zero(height) && zero(start) && zero(sweep) && zero(turns) && count == 0 && primitive.controls().isEmpty();
            case SPIRAL -> positive(radius) && positive(turns) && nonNegative(height) && zero(size) && zero(length) && zero(angle) && zero(start) && zero(sweep) && count == 0 && primitive.controls().isEmpty();
            case WAVE -> positive(length) && positive(radius) && nonNegative(height) && zero(size) && zero(angle) && zero(start) && zero(sweep) && zero(turns) && count == 0 && primitive.controls().isEmpty();
            case BEZIER -> common && (primitive.controls().size() == 3 || primitive.controls().size() == 4);
            case BURST -> positive(radius) && zero(size) && zero(length) && zero(height) && zero(angle) && zero(start) && zero(sweep) && zero(turns) && count > 0 && primitive.controls().isEmpty();
        };
        if (!valid) throw new IOException("primitive type slots");
    }

    private static boolean finite(double value) { return Double.isFinite(value); }
    private static boolean validVec(SkillVfxModel.Vec value) { return value != null && finite(value.x()) && finite(value.y()) && finite(value.z()) && Math.abs(value.x()) <= 128 && Math.abs(value.y()) <= 128 && Math.abs(value.z()) <= 128; }
    private static boolean bounded(SkillVfxModel.Scalar value, double maximum) { return !(value instanceof SkillVfxModel.Literal literal) || Math.abs(literal.value()) <= maximum; }
    private static boolean zero(SkillVfxModel.Scalar value) { return value == null || value instanceof SkillVfxModel.Literal literal && literal.value() == 0; }
    private static boolean nonZero(SkillVfxModel.Scalar value) { return value != null && (!(value instanceof SkillVfxModel.Literal literal) || literal.value() != 0); }
    private static boolean positive(SkillVfxModel.Scalar value) { return value != null && (!(value instanceof SkillVfxModel.Literal literal) || literal.value() > 0); }
    private static boolean nonNegative(SkillVfxModel.Scalar value) { return value == null || !(value instanceof SkillVfxModel.Literal literal) || literal.value() >= 0; }
    private static boolean angle(SkillVfxModel.Scalar value) { return value != null && (!(value instanceof SkillVfxModel.Literal literal) || literal.value() > 0 && literal.value() < Math.PI); }

    private static int checkedInt(double value) throws IOException {
        if (!Double.isFinite(value) || value != Math.rint(value) || value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) throw new IOException("count");
        return (int) value;
    }
    private static void scalar(DataOutputStream output, SkillVfxModel.Scalar value) throws IOException {
        if (value == null) output.writeByte(0);
        else if (value instanceof SkillVfxModel.Literal literal) { output.writeByte(1); output.writeDouble(literal.value()); }
        else if (value instanceof SkillVfxModel.FromGameplay field) { output.writeByte(2); output.writeByte(field.field().ordinal()); }
        else throw new IOException("scalar");
    }
    private static SkillVfxModel.Scalar scalar(DataInputStream input) throws IOException {
        return switch (input.readUnsignedByte()) {
            case 0 -> null;
            case 1 -> new SkillVfxModel.Literal(input.readDouble());
            case 2 -> new SkillVfxModel.FromGameplay(enumValue(SkillVfxModel.ActionField.values(), input.readUnsignedByte()));
            default -> throw new IOException("scalar");
        };
    }
    private static void vec(DataOutputStream output, SkillVfxModel.Vec value) throws IOException {
        if (value == null) throw new IOException("vector"); output.writeDouble(value.x()); output.writeDouble(value.y()); output.writeDouble(value.z());
    }
    private static SkillVfxModel.Vec vec(DataInputStream input) throws IOException { return new SkillVfxModel.Vec(input.readDouble(), input.readDouble(), input.readDouble()); }
    private static void uuid(DataOutputStream output, UUID value) throws IOException {
        if (value == null) throw new IOException("uuid"); output.writeLong(value.getMostSignificantBits()); output.writeLong(value.getLeastSignificantBits());
    }
    private static UUID uuid(DataInputStream input) throws IOException { return new UUID(input.readLong(), input.readLong()); }
    private static void str(DataOutputStream output, String value) throws IOException {
        if (value == null) throw new IOException("string"); byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_STRING) throw new IOException("string"); output.writeShort(bytes.length); output.write(bytes);
    }
    private static String str(DataInputStream input) throws IOException {
        int length = input.readUnsignedShort(); if (length > MAX_STRING) throw new IOException("string");
        byte[] bytes = input.readNBytes(length); if (bytes.length != length) throw new EOFException();
        try {
            String value = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
            if (!Arrays.equals(bytes, value.getBytes(StandardCharsets.UTF_8))) throw new IOException("utf8");
            return value;
        } catch (CharacterCodingException exception) { throw new IOException("utf8", exception); }
    }
    private static void validateString(String value) { if (value == null || value.getBytes(StandardCharsets.UTF_8).length > MAX_STRING) throw new IllegalArgumentException("string"); }
    private static boolean namespaced(String value) {
        return value != null && value.length() <= 96 && value.matches("[a-z][a-z0-9._-]*:[a-z][a-z0-9._/-]*")
                && !value.contains("..") && !value.contains("//") && !value.endsWith("/");
    }
    private static String enumName(String[] values, int ordinal) throws IOException { if (ordinal < 0 || ordinal >= values.length) throw new IOException("enum"); return values[ordinal]; }
    private static <T> T enumValue(T[] values, int ordinal) throws IOException { if (ordinal < 0 || ordinal >= values.length) throw new IOException("enum"); return values[ordinal]; }
    private static void version(DataInputStream input) throws IOException { if (input.readUnsignedByte() != VERSION) throw new IOException("version"); }
    private interface Reader<T> { T read(DataInputStream input) throws IOException; }
    private interface Writer { void write(DataOutputStream output) throws IOException; }
    private static <T> T in(byte[] bytes, Reader<T> reader) {
        if (bytes == null || bytes.length > MAX_PACKET) throw new IllegalArgumentException("packet");
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
            T value = reader.read(input); if (input.available() != 0) throw new IOException("trailing"); return value;
        } catch (IOException | RuntimeException exception) { throw new IllegalArgumentException("Malformed skill editor packet", exception); }
    }
    private static byte[] out(Writer writer) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) { writer.write(output); }
            if (bytes.size() > MAX_PACKET) throw new IllegalArgumentException("packet");
            return bytes.toByteArray();
        } catch (IOException exception) { throw new IllegalArgumentException(exception); }
    }
}
