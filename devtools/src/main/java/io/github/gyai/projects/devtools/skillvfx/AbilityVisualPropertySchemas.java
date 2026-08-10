package io.github.gyai.projects.devtools.skillvfx;

import io.github.gyai.projects.editor.core.*;
import java.util.*;

/** Single source of inspector metadata; UI clients never select fields by a primitive-type if-chain. */
public final class AbilityVisualPropertySchemas {
    private static final List<String> COMMON=List.of("delayTicks","durationTicks","argb","opacity","width","density","seed","offsetX","offsetY","offsetZ","yaw");
    private static final Map<SkillVfxModel.PrimitiveType,List<String>> TYPE_FIELDS=Map.of(
            SkillVfxModel.PrimitiveType.POINT,List.of("size"), SkillVfxModel.PrimitiveType.LINE,List.of("length","controlPoints"),
            SkillVfxModel.PrimitiveType.ARC,List.of("radius","startAngle","sweepAngle"), SkillVfxModel.PrimitiveType.CIRCLE,List.of("radius","startAngle"),
            SkillVfxModel.PrimitiveType.CONE,List.of("length","angle"), SkillVfxModel.PrimitiveType.SPIRAL,List.of("radius","height","turns"),
            SkillVfxModel.PrimitiveType.SPHERE,List.of("radius"), SkillVfxModel.PrimitiveType.WAVE,List.of("radius","length","height"),
            SkillVfxModel.PrimitiveType.BEZIER,List.of("controlPoints"), SkillVfxModel.PrimitiveType.BURST,List.of("radius","count"));
    private AbilityVisualPropertySchemas() { }
    public static List<String> fields(SkillVfxModel.PrimitiveType type) { ArrayList<String> fields=new ArrayList<>(COMMON);fields.addAll(TYPE_FIELDS.get(type));return List.copyOf(fields); }
    public static Map<SkillVfxModel.PrimitiveType,List<String>> all() { return TYPE_FIELDS; }
    public static PropertySchema<AbilityVisualEditorDocument> schema(SkillVfxModel.PrimitiveType type) {
        List<PropertyDescriptor<AbilityVisualEditorDocument,?>> out=new ArrayList<>();
        for(String id:fields(type)) out.add(new PropertyDescriptor<AbilityVisualEditorDocument,Object>(id,label(id),Object.class,d->d.selectedValue(id),(d,v)->d.setSelectedValue(id,v),v->v!=null,"A value is required",id.startsWith("offset")?"Transform":"Visual","Scalar"));
        return new PropertySchema<>(out);
    }
    public static List<PropertyDescriptor<AbilityVisualEditorDocument,?>> descriptors(SkillVfxModel.PrimitiveType type){return schema(type).properties();}
    public static String description(String id){return switch(id){case "delayTicks"->"Ticks before this primitive begins";case "durationTicks"->"How long this primitive remains visible";case "argb"->"ARGB hexadecimal color";case "opacity"->"Alpha channel of the ARGB color";case "controlPoints"->"Bezier or line control point coordinates";case "radius","length","width","height"->"Literal value or gameplay action binding";default->"Visual primitive property";};}
    private static String label(String id) { return switch(id) {case "delayTicks"->"Delay";case "durationTicks"->"Duration";case "argb"->"Color (ARGB)";case "opacity"->"Opacity";case "controlPoints"->"Control points";default->id;}; }
}
