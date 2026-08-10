package io.github.gyai.projects.client.vfx;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/** Motion domain, planner, legacy-sampler and additive runtime-v2 regression coverage. */
public final class AbilityVfxMotionFoundationTest {
    private static final AbilityVfx.Frame FRAME = new AbilityVfx.Frame(
            new AbilityVfx.Vec(10, 20, 30), new AbilityVfx.Vec(0, 0, 1), new AbilityVfx.Vec(0, 1, 0));

    public static void main(String[] args) throws Exception {
        ordinalsAndMatrix();
        plannerSemantics();
        legacyEqualityAndRanges();
        runtimeMotionMatrix();
        runtimeV2Strictness();
    }

    private static void ordinalsAndMatrix() {
        assert MotionMode.values()[0] == MotionMode.STATIC;
        assert MotionMode.values()[1] == MotionMode.REVEAL;
        assert MotionMode.values()[2] == MotionMode.TRAVEL;
        assert MotionDirection.values()[0] == MotionDirection.FORWARD;
        assert MotionDirection.values()[1] == MotionDirection.REVERSE;
        assert MotionEasing.values()[0] == MotionEasing.LINEAR;
        assert MotionEasing.values()[1] == MotionEasing.EASE_IN;
        assert MotionEasing.values()[2] == MotionEasing.EASE_OUT;
        assert MotionEasing.values()[3] == MotionEasing.EASE_IN_OUT;
        for (AbilityVfx.Type type : AbilityVfx.Type.values()) {
            assert new MotionSpec(MotionMode.STATIC, MotionDirection.FORWARD, MotionEasing.LINEAR, 0, 0).supports(type);
            assert new MotionSpec(MotionMode.REVEAL, MotionDirection.FORWARD, MotionEasing.LINEAR, .5, 0).supports(type);
            boolean ordered = switch (type) {
                case LINE, ARC, SPIRAL, WAVE, BEZIER -> true;
                default -> false;
            };
            assert new MotionSpec(MotionMode.REVEAL, MotionDirection.REVERSE, MotionEasing.LINEAR, 0, 0).supports(type)
                    == switch (type) {
                        case LINE, ARC, CIRCLE, SPIRAL, WAVE, BEZIER -> true;
                        default -> false;
                    };
            assert new MotionSpec(MotionMode.TRAVEL, MotionDirection.FORWARD, MotionEasing.LINEAR, 0, .5).supports(type) == ordered;
            assert new MotionSpec(MotionMode.TRAVEL, MotionDirection.REVERSE, MotionEasing.LINEAR, 0, .5).supports(type) == ordered;
        }
        assert !new MotionSpec(MotionMode.STATIC, MotionDirection.REVERSE, MotionEasing.LINEAR, 0, 0).supports(AbilityVfx.Type.LINE);
        assert !new MotionSpec(MotionMode.REVEAL, MotionDirection.FORWARD, MotionEasing.LINEAR, 0, .1).supports(AbilityVfx.Type.LINE);
        assert !new MotionSpec(MotionMode.TRAVEL, MotionDirection.FORWARD, MotionEasing.LINEAR, 0, 0).supports(AbilityVfx.Type.POINT);
    }

    private static void plannerSemantics() {
        double[] times = {0, .25, .5, .75, 1};
        for (MotionEasing easing : MotionEasing.values()) {
            for (double time : times) {
                double expected = switch (easing) {
                    case LINEAR -> time;
                    case EASE_IN -> time * time;
                    case EASE_OUT -> 1 - (1 - time) * (1 - time);
                    case EASE_IN_OUT -> 3 * time * time - 2 * time * time * time;
                };
                assert near(AbilityVfxMotionPlanner.ease(easing, time), expected);
                assert near(AbilityVfxMotionPlanner.plan(
                        new MotionSpec(MotionMode.REVEAL, MotionDirection.FORWARD, easing, .25, 0), time).logicalProgress(),
                        .25 + .75 * expected);
            }
        }
        var forward = AbilityVfxMotionPlanner.plan(
                new MotionSpec(MotionMode.REVEAL, MotionDirection.FORWARD, MotionEasing.LINEAR, .25, 0), .5);
        assert near(forward.logicalProgress(), .625) && near(forward.sampleStart(), 0) && near(forward.sampleEnd(), .625);
        var reverse = AbilityVfxMotionPlanner.plan(
                new MotionSpec(MotionMode.REVEAL, MotionDirection.REVERSE, MotionEasing.LINEAR, 0, 0), .25);
        assert near(reverse.physicalHead(), .75) && near(reverse.sampleStart(), 1) && near(reverse.sampleEnd(), .75);
        var travel = AbilityVfxMotionPlanner.plan(
                new MotionSpec(MotionMode.TRAVEL, MotionDirection.FORWARD, MotionEasing.LINEAR, 0, .5), .25);
        assert near(travel.sampleStart(), 0) && near(travel.sampleEnd(), .25);
        travel = AbilityVfxMotionPlanner.plan(
                new MotionSpec(MotionMode.TRAVEL, MotionDirection.FORWARD, MotionEasing.LINEAR, 0, .5), .9);
        assert near(travel.sampleStart(), .4) && near(travel.sampleEnd(), .9);
        var reverseTravel = AbilityVfxMotionPlanner.plan(
                new MotionSpec(MotionMode.TRAVEL, MotionDirection.REVERSE, MotionEasing.LINEAR, 0, .5), .9);
        assert near(reverseTravel.sampleStart(), .6) && near(reverseTravel.sampleEnd(), .1);
        var stat = AbilityVfxMotionPlanner.plan(
                new MotionSpec(MotionMode.STATIC, MotionDirection.FORWARD, MotionEasing.LINEAR, 0, 0), 0);
        assert near(stat.sampleStart(), 0) && near(stat.sampleEnd(), 1) && near(stat.physicalHead(), 1);
        assert near(AbilityVfxMotionPlanner.plan(MotionSpec.LEGACY_DEFAULT, -10).normalizedTime(), 0);
        assert near(AbilityVfxMotionPlanner.plan(MotionSpec.LEGACY_DEFAULT, 10).normalizedTime(), 1);
    }

    private static void legacyEqualityAndRanges() {
        for (AbilityVfx.Type type : AbilityVfx.Type.values()) {
            AbilityVfx.Primitive legacy = primitive(type, MotionSpec.LEGACY_DEFAULT);
            for (double progress : new double[] {0, .25, .5, 1}) {
                List<AbilityVfx.Command> oldPath = AbilityVfx.sample(legacy, FRAME, progress, AbilityVfx.Quality.HIGH);
                List<AbilityVfx.Command> motionPath = AbilityVfx.sample(legacy, FRAME,
                        AbilityVfxMotionPlanner.plan(legacy.motion(), progress), AbilityVfx.Quality.HIGH);
                assert oldPath.equals(motionPath) : type;
            }
            AbilityVfx.Primitive staticPrimitive = primitive(type,
                    new MotionSpec(MotionMode.STATIC, MotionDirection.FORWARD, MotionEasing.LINEAR, 0, 0));
            assert AbilityVfx.sample(staticPrimitive, FRAME,
                    AbilityVfxMotionPlanner.plan(staticPrimitive.motion(), .1), AbilityVfx.Quality.HIGH)
                    .equals(AbilityVfx.sample(staticPrimitive, FRAME, 1, AbilityVfx.Quality.HIGH));
        }
        for (AbilityVfx.Type type : new AbilityVfx.Type[] {
                AbilityVfx.Type.LINE, AbilityVfx.Type.ARC, AbilityVfx.Type.CIRCLE,
                AbilityVfx.Type.SPIRAL, AbilityVfx.Type.WAVE, AbilityVfx.Type.BEZIER}) {
            AbilityVfx.Primitive reverse = primitive(type,
                    new MotionSpec(MotionMode.REVEAL, MotionDirection.REVERSE, MotionEasing.LINEAR, 0, 0));
            assert !AbilityVfx.sample(reverse, FRAME,
                    AbilityVfxMotionPlanner.plan(reverse.motion(), .5), AbilityVfx.Quality.HIGH).isEmpty();
        }
        for (AbilityVfx.Type type : new AbilityVfx.Type[] {
                AbilityVfx.Type.LINE, AbilityVfx.Type.ARC, AbilityVfx.Type.SPIRAL,
                AbilityVfx.Type.WAVE, AbilityVfx.Type.BEZIER}) {
            for (MotionDirection direction : MotionDirection.values()) {
                AbilityVfx.Primitive travel = primitive(type,
                        new MotionSpec(MotionMode.TRAVEL, direction, MotionEasing.EASE_OUT, .1, .5));
                assert !AbilityVfx.sample(travel, FRAME,
                        AbilityVfxMotionPlanner.plan(travel.motion(), .75), AbilityVfx.Quality.MEDIUM).isEmpty();
            }
        }
        for (AbilityVfx.Type type : new AbilityVfx.Type[] {
                AbilityVfx.Type.POINT, AbilityVfx.Type.CONE, AbilityVfx.Type.SPHERE, AbilityVfx.Type.BURST}) {
            try {
                primitive(type, new MotionSpec(MotionMode.TRAVEL, MotionDirection.FORWARD, MotionEasing.LINEAR, 0, 0));
                throw new AssertionError("unsupported travel accepted: " + type);
            } catch (IllegalArgumentException expected) { }
        }
    }

    private static void runtimeV2Strictness() throws Exception {
        V2Wire packet = v2Wire(new MotionSpec(MotionMode.TRAVEL, MotionDirection.REVERSE,
                MotionEasing.EASE_IN_OUT, .25, .5));
        AbilityVfx.Decoded decoded = AbilityVfx.decodeV2(packet.bytes());
        assert decoded.valid() && decoded.cue().primitives().size() == 1;
        assert decoded.cue().primitives().getFirst().motion().equals(
                new MotionSpec(MotionMode.TRAVEL, MotionDirection.REVERSE, MotionEasing.EASE_IN_OUT, .25, .5));
        assert !AbilityVfx.decodeV2(Arrays.copyOf(packet.bytes(), packet.bytes().length + 1)).valid();
        byte[] wrongVersion = packet.bytes().clone(); wrongVersion[0] = 1;
        assert !AbilityVfx.decodeV2(wrongVersion).valid();
        byte[] unknownMotion = packet.bytes().clone(); unknownMotion[packet.motionOffset()] = 99;
        assert !AbilityVfx.decodeV2(unknownMotion).valid();
    }

    private static void runtimeMotionMatrix() {
        MotionSpec reveal = new MotionSpec(MotionMode.REVEAL, MotionDirection.FORWARD, MotionEasing.LINEAR, 0, 0);
        AbilityVfx.Primitive spiral = primitive(AbilityVfx.Type.SPIRAL, reveal);
        List<AbilityVfx.Command> spiralZero = AbilityVfx.sample(spiral, FRAME,
                AbilityVfxMotionPlanner.plan(reveal, 0), AbilityVfx.Quality.HIGH);
        List<AbilityVfx.Command> spiralOne = AbilityVfx.sample(spiral, FRAME,
                AbilityVfxMotionPlanner.plan(reveal, 1), AbilityVfx.Quality.HIGH);
        assert spiralZero.size() == 32 && spiralOne.size() == 32;
        assert near(localY(spiralZero.getFirst().a()), 0) && near(localY(spiralOne.getLast().b()), 2);

        MotionSpec travelForward = new MotionSpec(MotionMode.TRAVEL, MotionDirection.FORWARD,
                MotionEasing.LINEAR, .25, .25);
        MotionSpec travelReverse = new MotionSpec(MotionMode.TRAVEL, MotionDirection.REVERSE,
                MotionEasing.LINEAR, .25, .25);
        AbilityVfx.Primitive forward = primitive(AbilityVfx.Type.SPIRAL, travelForward);
        AbilityVfx.Primitive reverse = primitive(AbilityVfx.Type.SPIRAL, travelReverse);
        var forwardPlan = AbilityVfxMotionPlanner.plan(travelForward, .5);
        var reversePlan = AbilityVfxMotionPlanner.plan(travelReverse, .5);
        assert near(forwardPlan.sampleStart(), .375) && near(forwardPlan.sampleEnd(), .625);
        assert near(reversePlan.sampleStart(), .625) && near(reversePlan.sampleEnd(), .375);
        var forwardCommands = AbilityVfx.sample(forward, FRAME, forwardPlan, AbilityVfx.Quality.HIGH);
        var reverseCommands = AbilityVfx.sample(reverse, FRAME, reversePlan, AbilityVfx.Quality.HIGH);
        assert near(localY(forwardCommands.getFirst().a()), .75) && near(localY(forwardCommands.getLast().b()), 1.25);
        assert near(localY(reverseCommands.getFirst().a()), 1.25) && near(localY(reverseCommands.getLast().b()), .75);

        for (int density : new int[] {1, 2, 3, 63, 64, 256}) {
            AbilityVfx.Primitive bounded = new AbilityVfx.Primitive(AbilityVfx.Type.SPIRAL, 0, 100,
                    new AbilityVfx.Color(1, 2, 3, 4), .5, density, 7, new AbilityVfx.Vec(0, 0, 0), 0,
                    1, 4, 4, 2, 1, 0, Math.PI, 2, 8,
                    List.of(new AbilityVfx.Vec(0, 0, 0), new AbilityVfx.Vec(0, 0, 4),
                            new AbilityVfx.Vec(2, 1, 6), new AbilityVfx.Vec(4, 0, 8)),
                    AbilityVfx.Appearance.DEBUG_QUAD, reveal);
            assert AbilityVfx.sample(bounded, FRAME,
                    AbilityVfxMotionPlanner.plan(reveal, 1), AbilityVfx.Quality.HIGH).size() == density;
            assert density <= AbilityVfx.MAX_SAMPLES_PER_PRIMITIVE;
        }

        AbilityVfx.Primitive line = primitive(AbilityVfx.Type.LINE, reveal);
        AbilityVfx.Primitive lineReverse = primitive(AbilityVfx.Type.LINE,
                new MotionSpec(MotionMode.REVEAL, MotionDirection.REVERSE, MotionEasing.LINEAR, 0, 0));
        var lineForwardCommands = AbilityVfx.sample(line, FRAME,
                AbilityVfxMotionPlanner.plan(reveal, 1), AbilityVfx.Quality.HIGH);
        var lineReverseCommands = AbilityVfx.sample(lineReverse, FRAME,
                AbilityVfxMotionPlanner.plan(lineReverse.motion(), 1), AbilityVfx.Quality.HIGH);
        assert near(lineForwardCommands.getFirst().a().z(), 30) && near(lineForwardCommands.getLast().b().z(), 34);
        assert near(lineReverseCommands.getFirst().a().z(), 34) && near(lineReverseCommands.getLast().b().z(), 30);

        AbilityVfx.Primitive bezier = primitive(AbilityVfx.Type.BEZIER, reveal);
        AbilityVfx.Primitive bezierReverse = primitive(AbilityVfx.Type.BEZIER,
                new MotionSpec(MotionMode.REVEAL, MotionDirection.REVERSE, MotionEasing.LINEAR, 0, 0));
        var bezierForwardCommands = AbilityVfx.sample(bezier, FRAME,
                AbilityVfxMotionPlanner.plan(reveal, 1), AbilityVfx.Quality.HIGH);
        var bezierReverseCommands = AbilityVfx.sample(bezierReverse, FRAME,
                AbilityVfxMotionPlanner.plan(bezierReverse.motion(), 1), AbilityVfx.Quality.HIGH);
        assert near(bezierForwardCommands.getFirst().a().z(), 30) && near(bezierForwardCommands.getLast().b().z(), 38);
        assert near(bezierReverseCommands.getFirst().a().z(), 38) && near(bezierReverseCommands.getLast().b().z(), 30);

        AbilityVfx.Primitive circle = primitive(AbilityVfx.Type.CIRCLE,
                new MotionSpec(MotionMode.REVEAL, MotionDirection.REVERSE, MotionEasing.LINEAR, 0, 0));
        var circleFirst = AbilityVfx.sample(circle, FRAME,
                AbilityVfxMotionPlanner.plan(circle.motion(), 1), AbilityVfx.Quality.HIGH);
        var circleSecond = AbilityVfx.sample(circle, FRAME,
                AbilityVfxMotionPlanner.plan(circle.motion(), 1), AbilityVfx.Quality.HIGH);
        assert circleFirst.equals(circleSecond) && circleFirst.size() == 32;

        MotionSpec reverseReveal = new MotionSpec(MotionMode.REVEAL, MotionDirection.REVERSE,
                MotionEasing.LINEAR, 0, 0);
        AbilityVfx.Primitive reverseSpiral = primitive(AbilityVfx.Type.SPIRAL, reverseReveal);
        var reverseZeroPlan = AbilityVfxMotionPlanner.plan(reverseReveal, 0);
        var reverseZero = AbilityVfx.sample(reverseSpiral, FRAME, reverseZeroPlan, AbilityVfx.Quality.HIGH);
        assert near(reverseZeroPlan.sampleStart(), 1) && near(reverseZeroPlan.sampleEnd(), 1);
        assert near(localY(reverseZero.getFirst().a()), 2) && near(localY(reverseZero.getLast().b()), 2);

        var reverseOnePlan = AbilityVfxMotionPlanner.plan(reverseReveal, 1);
        var reverseOne = AbilityVfx.sample(reverseSpiral, FRAME, reverseOnePlan, AbilityVfx.Quality.HIGH);
        assert near(reverseOnePlan.sampleStart(), 1) && near(reverseOnePlan.sampleEnd(), 0);
        assert near(localY(reverseOne.getFirst().a()), 2) && near(localY(reverseOne.getLast().b()), 0);
        assert near(reverseOne.getFirst().a().z(), 30) && near(reverseOne.getLast().b().z(), 30);

        MotionSpec phasedReverse = new MotionSpec(MotionMode.REVEAL, MotionDirection.REVERSE,
                MotionEasing.LINEAR, .25, 0);
        var phasedPlan = AbilityVfxMotionPlanner.plan(phasedReverse, .5);
        var phased = AbilityVfx.sample(primitive(AbilityVfx.Type.SPIRAL, phasedReverse), FRAME,
                phasedPlan, AbilityVfx.Quality.HIGH);
        assert near(phasedPlan.logicalProgress(), .625) && near(phasedPlan.sampleStart(), 1)
                && near(phasedPlan.sampleEnd(), .375);
        assert near(localY(phased.getFirst().a()), 2) && near(localY(phased.getLast().b()), .75);
        assert near(phased.getFirst().a().z(), 30) && near(phased.getLast().b().z(), 26);
    }

    private static AbilityVfx.Primitive primitive(AbilityVfx.Type type, MotionSpec motion) {
        return new AbilityVfx.Primitive(type, 0, 100, new AbilityVfx.Color(1, 2, 3, 4), .5, 32, 7,
                new AbilityVfx.Vec(0, 0, 0), 0, 1, 4, 4, 2, 1, 0, Math.PI, 2, 8,
                List.of(new AbilityVfx.Vec(0, 0, 0), new AbilityVfx.Vec(0, 0, 4),
                        new AbilityVfx.Vec(2, 1, 6), new AbilityVfx.Vec(4, 0, 8)),
                AbilityVfx.Appearance.DEBUG_QUAD, motion);
    }

    private record V2Wire(byte[] bytes, int motionOffset) { }

    private static V2Wire v2Wire(MotionSpec motion) throws Exception {
        ByteArrayOutputStream headerBytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(headerBytes)) {
            out.writeByte(2);
            uuid(out, UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee")); out.writeLong(1);
            uuid(out, UUID.fromString("01234567-89ab-cdef-0123-456789abcdef"));
            uuid(out, UUID.fromString("fedcba98-7654-3210-fedc-ba9876543210"));
            string(out, "projects:motion/test"); out.writeByte(0); out.writeInt(-1); out.writeInt(0);
            uuid(out, UUID.fromString("11111111-2222-3333-4444-555555555555")); string(out, "minecraft:overworld");
            for (double value : new double[] {0, 0, 0, 0, 0, 1, 0, 1, 0}) out.writeDouble(value);
            out.writeLong(0); out.writeLong(0); out.writeInt(20); out.writeByte(1);
        }
        ByteArrayOutputStream bodyBytes = new ByteArrayOutputStream();
        int modeOffset;
        try (DataOutputStream out = new DataOutputStream(bodyBytes)) {
            out.writeShort(0); out.writeShort(20); out.write(new byte[] {1, 2, 3, 4}); out.writeDouble(.5);
            out.writeShort(32); out.writeLong(7); for (int i = 0; i < 3; i++) out.writeDouble(0); out.writeDouble(0);
            for (double value : new double[] {0, 0, 4, 0, 0, 0, 0, 0}) out.writeDouble(value);
            out.writeShort(0); out.writeByte(0); out.writeByte(AbilityVfx.AppearanceKind.DEBUG_QUAD.ordinal());
            string(out, "projects:debug_quad"); modeOffset = bodyBytes.size();
            out.writeByte(motion.mode().ordinal()); out.writeByte(motion.direction().ordinal());
            out.writeByte(motion.easing().ordinal()); out.writeDouble(motion.phase()); out.writeDouble(motion.trailFraction());
        }
        ByteArrayOutputStream packet = new ByteArrayOutputStream();
        packet.write(headerBytes.toByteArray());
        try (DataOutputStream out = new DataOutputStream(packet)) {
            out.writeByte(AbilityVfx.Type.LINE.ordinal()); out.writeByte(3); out.writeShort(bodyBytes.size());
            out.write(bodyBytes.toByteArray());
        }
        return new V2Wire(packet.toByteArray(), headerBytes.size() + 4 + modeOffset);
    }

    private static boolean near(double left, double right) { return Math.abs(left - right) < 1e-9; }
    private static double localY(AbilityVfx.Vec value) { return value.y() - FRAME.origin().y(); }
    private static void uuid(DataOutputStream out, UUID value) throws Exception {
        out.writeLong(value.getMostSignificantBits()); out.writeLong(value.getLeastSignificantBits());
    }
    private static void string(DataOutputStream out, String value) throws Exception {
        byte[] bytes = value.getBytes(java.nio.charset.StandardCharsets.UTF_8); out.writeShort(bytes.length); out.write(bytes);
    }
}
