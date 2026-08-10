package io.github.gyai.projects.devtools.skillvfx;

import io.github.gyai.projects.editor.core.EditorCommand;
import java.util.*;

/** All draft mutations are reversible snapshot commands, avoiding widget-owned mutations. */
public final class AbilityVisualCommands {
    private AbilityVisualCommands() { }
    public static EditorCommand<AbilityVisualEditorDocument> setScalar(String primitive,String field,SkillVfxModel.Scalar value){return change("Set "+field,d->d.setPrimitiveValue(primitive,field,value));}
    public static EditorCommand<AbilityVisualEditorDocument> setControlPoints(String primitive,List<SkillVfxModel.Vec> controls){return change("Set control points",d->d.setControlPoints(primitive,controls));}
    public static EditorCommand<AbilityVisualEditorDocument> replacePrimitive(String primitive,SkillVfxModel.Primitive value){return change("Set primitive property",d->d.replacePrimitive(primitive,value));}
    public static EditorCommand<AbilityVisualEditorDocument> add(SkillVfxModel.Hook hook,String emission,SkillVfxModel.Primitive p){return change("Add "+p.type(),d->d.addPrimitive(hook,emission,p));}
    public static EditorCommand<AbilityVisualEditorDocument> remove(String id){return change("Remove primitive",d->d.removePrimitive(id));}
    public static EditorCommand<AbilityVisualEditorDocument> movePrimitive(SkillVfxModel.Hook hook,String emission,String id,int target){return change("Move primitive",d->d.movePrimitive(hook,emission,id,target));}
    public static EditorCommand<AbilityVisualEditorDocument> addEmission(SkillVfxModel.Hook hook,SkillVfxModel.Emission emission){return change("Add emission",d->d.addEmission(hook,emission));}
    public static EditorCommand<AbilityVisualEditorDocument> removeEmission(SkillVfxModel.Hook hook,String id){return change("Remove emission",d->d.removeEmission(hook,id));}
    public static EditorCommand<AbilityVisualEditorDocument> moveEmission(SkillVfxModel.Hook hook,String id,int target){return change("Move emission",d->d.moveEmission(hook,id,target));}
    public static EditorCommand<AbilityVisualEditorDocument> setEmissionActionIndex(SkillVfxModel.Hook hook,String id,int actionIndex){return change("Set emission action binding",d->d.setEmissionActionIndex(hook,id,actionIndex));}
    public static EditorCommand<AbilityVisualEditorDocument> duplicateEmission(SkillVfxModel.Hook hook,String id){return change("Duplicate emission",d->{var source=d.visual().emissions(hook).stream().filter(e->e.id().equals(id)).findFirst().orElseThrow();Set<String> emissionIds=new HashSet<>(),primitiveIds=new HashSet<>();for(var binding:d.visual().hooks())for(var e:binding.emissions()){emissionIds.add(e.id());for(var primitive:e.primitives())primitiveIds.add(primitive.id());}List<SkillVfxModel.Primitive> copy=new ArrayList<>();for(var primitive:source.primitives()){String next=SkillVfxModel.nextId(primitiveIds,primitive.type().name());primitiveIds.add(next);copy.add(primitive.withId(next));}d.addEmission(hook,new SkillVfxModel.Emission(SkillVfxModel.nextId(emissionIds,"emission"),source.actionIndex(),copy));});}
    public static EditorCommand<AbilityVisualEditorDocument> duplicate(SkillVfxModel.Hook hook,String emission,String id){return change("Duplicate primitive",d->{SkillVfxModel.Emission e=d.visual().emissions(hook).stream().filter(x->x.id().equals(emission)).findFirst().orElseThrow();SkillVfxModel.Primitive p=e.primitives().stream().filter(x->x.id().equals(id)).findFirst().orElseThrow();Set<String> used=new HashSet<>();for(SkillVfxModel.HookBinding b:d.visual().hooks())for(SkillVfxModel.Emission x:b.emissions())for(SkillVfxModel.Primitive q:x.primitives())used.add(q.id());d.addPrimitive(hook,emission,p.withId(SkillVfxModel.nextId(used,p.type().name())));});}
    public static EditorCommand<AbilityVisualEditorDocument> change(String description,java.util.function.Consumer<AbilityVisualEditorDocument> mutation){return new EditorCommand<>(){SkillVfxModel.Visual before;public String description(){return description;}public void execute(AbilityVisualEditorDocument d){if(before==null)before=d.visual();mutation.accept(d);}public void undo(AbilityVisualEditorDocument d){d.replaceVisual(before);}};}
}
