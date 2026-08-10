package io.github.gyai.projects.devtools.skillvfx;

import java.util.*;

/** Immutable transport-independent v0.1 Skill/VFX snapshot.  The wire codec and widgets both use this shape. */
public final class SkillVfxModel {
    private SkillVfxModel() { }
    public enum Hook { CAST, TELEGRAPH, TRAVEL, HIT, EXPIRE, CANCEL }
    public enum PrimitiveType { POINT, LINE, ARC, CIRCLE, CONE, SPIRAL, SPHERE, WAVE, BEZIER, BURST }
    public enum ActionField { RADIUS, INNER_RADIUS, WIDTH, LENGTH }
    public sealed interface Scalar permits Literal, FromGameplay { }
    public record Literal(double value) implements Scalar { public Literal { if (!Double.isFinite(value)) throw new IllegalArgumentException("scalar"); } }
    public record FromGameplay(ActionField field) implements Scalar { public FromGameplay { Objects.requireNonNull(field); } }
    public record Vec(double x, double y, double z) { public Vec { if (!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z)) throw new IllegalArgumentException("vector"); } }
    public record Primitive(String id, PrimitiveType type, int delayTicks, int durationTicks, int argb, double width,
                            int density, long seed, Vec offset, double yaw, Map<String, Scalar> values,
                            List<Vec> controls) {
        public Primitive {
            requireId(id); Objects.requireNonNull(type); Objects.requireNonNull(offset);
            if (delayTicks < 0 || durationTicks < 1 || durationTicks > 1200 || width <= 0 || !Double.isFinite(width) || density < 1 || density > 256) throw new IllegalArgumentException("primitive bounds");
            values = Collections.unmodifiableMap(new TreeMap<>(values == null ? Map.of() : values));
            controls = List.copyOf(controls == null ? List.of() : controls); if (controls.size() > 8) throw new IllegalArgumentException("controls");
        }
        public Scalar value(String key) { return values.get(key); }
        public Primitive withValue(String key, Scalar value) { TreeMap<String, Scalar> copy=new TreeMap<>(values); if(value==null)copy.remove(key);else copy.put(key,value); return new Primitive(id,type,delayTicks,durationTicks,argb,width,density,seed,offset,yaw,copy,controls); }
        public Primitive withId(String value) { return new Primitive(value,type,delayTicks,durationTicks,argb,width,density,seed,offset,yaw,values,controls); }
    }
    public record Emission(String id, int actionIndex, List<Primitive> primitives) {
        public Emission { requireId(id); if(actionIndex < -1) throw new IllegalArgumentException("action index"); primitives=List.copyOf(primitives==null?List.of():primitives); unique(primitives.stream().map(Primitive::id).toList()); }
    }
    public record HookBinding(Hook hook, List<Emission> emissions) { public HookBinding { Objects.requireNonNull(hook); emissions=List.copyOf(emissions==null?List.of():emissions); unique(emissions.stream().map(Emission::id).toList()); } }
    public record Visual(String id, List<HookBinding> hooks) {
        public Visual {
            requireId(id); hooks=List.copyOf(hooks==null?List.of():hooks); unique(hooks.stream().map(HookBinding::hook).toList());
            // Primitive ids are document identities, rather than emission-local labels.  Reject
            // invalid decoded documents; callers must not silently rewrite server supplied ids.
            ArrayList<String> primitiveIds=new ArrayList<>();
            for(HookBinding binding:hooks) for(Emission emission:binding.emissions()) for(Primitive primitive:emission.primitives()) primitiveIds.add(primitive.id());
            unique(primitiveIds);
        }
        public List<Emission> emissions(Hook hook) { for(HookBinding b:hooks) if(b.hook==hook)return b.emissions; return List.of(); }
        public Visual withHooks(List<HookBinding> next) { return new Visual(id,next); }
    }
    public record GameplayAction(String type, String description, Map<String,String> fields) { public GameplayAction { type=clean(type,64);description=clean(description,256);fields=Collections.unmodifiableMap(new LinkedHashMap<>(fields==null?Map.of():fields)); } }
    public record Snapshot(UUID session, long revision, String abilityId, String abilityName, String visualId,
                           String baseFingerprint, String effectiveFingerprint, boolean overrideActive,
                           List<GameplayAction> gameplay, Visual visual) {
        public Snapshot { Objects.requireNonNull(session); if(revision<0)throw new IllegalArgumentException("revision"); abilityId=clean(abilityId,96);abilityName=clean(abilityName,128);visualId=clean(visualId,96);baseFingerprint=clean(baseFingerprint,128);effectiveFingerprint=clean(effectiveFingerprint,128);gameplay=List.copyOf(gameplay==null?List.of():gameplay);Objects.requireNonNull(visual); }
    }
    public static Primitive defaults(String id, PrimitiveType type) {
        Map<String,Scalar> values=new TreeMap<>();
        switch(type) { case POINT -> values.put("size",new Literal(1)); case LINE -> values.put("length",new Literal(1)); case ARC -> {values.put("radius",new Literal(1));values.put("sweepAngle",new Literal(1));} case CIRCLE,SPHERE -> values.put("radius",new Literal(1)); case BURST -> {values.put("radius",new Literal(1));values.put("count",new Literal(8));} case CONE -> {values.put("length",new Literal(1));values.put("angle",new Literal(.5));} case SPIRAL -> {values.put("radius",new Literal(1));values.put("turns",new Literal(1));} case WAVE -> {values.put("radius",new Literal(1));values.put("length",new Literal(1));} case BEZIER -> { } }
        List<Vec> controls=switch(type) {
            case LINE -> List.of(new Vec(0,0,0),new Vec(0,0,1));
            case BEZIER -> List.of(new Vec(0,0,0),new Vec(0,0,.5),new Vec(0,0,1));
            default -> List.of();
        };
        return new Primitive(id,type,0,20,0xFFFFFFFF,0.1,16,0L,new Vec(0,0,0),0,values,controls);
    }
    public static String nextId(Collection<String> used, String kind) { String base=kind.toLowerCase(Locale.ROOT).replace('_','-'); for(int n=1;;n++){String id=base+"-"+n;if(!used.contains(id))return id;} }
    private static void unique(List<?> ids) { if(new HashSet<>(ids).size()!=ids.size()) throw new IllegalArgumentException("duplicate stable id"); }
    private static void requireId(String id) { clean(id,64); }
    private static String clean(String value,int max){if(value==null||value.isBlank()||value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>max)throw new IllegalArgumentException("string");return value;}
}
