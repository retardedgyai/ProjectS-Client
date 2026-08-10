package io.github.gyai.projects.devtools.skillvfx;

import io.github.gyai.projects.editor.core.EditorCommand;
import java.util.*;

/** All draft mutations are reversible snapshot commands, avoiding widget-owned mutations. */
public final class AbilityVisualCommands {
    private AbilityVisualCommands() { }
    public static EditorCommand<AbilityVisualEditorDocument> setScalar(String primitive,String field,SkillVfxModel.Scalar value){return change("Set "+field,d->d.setPrimitiveValue(primitive,field,value));}
    public static EditorCommand<AbilityVisualEditorDocument> setControlPoints(String primitive,List<SkillVfxModel.Vec> controls){return change("Set control points",d->d.setControlPoints(primitive,controls));}
    public static EditorCommand<AbilityVisualEditorDocument> replacePrimitive(String primitive,SkillVfxModel.Primitive value){return change("Set primitive property",d->d.replacePrimitive(primitive,value));}
    public static EditorCommand<AbilityVisualEditorDocument> add(SkillVfxModel.Hook hook,String emission,SkillVfxModel.Primitive p){return add(hook,emission,p,Integer.MAX_VALUE);}
    public static EditorCommand<AbilityVisualEditorDocument> add(SkillVfxModel.Hook hook,String emission,SkillVfxModel.Primitive p,int index){return changeSelecting("Add "+p.type(),"primitive",p.id(),d->d.addPrimitive(hook,emission,p,index));}
    public static EditorCommand<AbilityVisualEditorDocument> remove(SkillVfxModel.Hook hook,String emission,String id,String nextSelection){return changeSelecting("Remove primitive",nextSelection==null?"hook":"primitive",nextSelection,d->d.removePrimitive(hook,emission,id));}
    /** Legacy id-only entry point remains command-backed; callers needing deterministic selection use the contextual overload. */
    public static EditorCommand<AbilityVisualEditorDocument> remove(String id){return change("Remove primitive",d->d.removePrimitive(id));}
    public static EditorCommand<AbilityVisualEditorDocument> movePrimitive(SkillVfxModel.Hook hook,String emission,String id,int target){return change("Move primitive",d->d.movePrimitive(hook,emission,id,target));}
    public static EditorCommand<AbilityVisualEditorDocument> addEmission(SkillVfxModel.Hook hook,SkillVfxModel.Emission emission){return changeSelecting("Add emission","primitive",emission.primitives().getFirst().id(),d->d.addEmission(hook,emission));}
    public static EditorCommand<AbilityVisualEditorDocument> removeEmission(SkillVfxModel.Hook hook,String id){return changeSelecting("Remove emission","hook",null,d->d.removeEmission(hook,id));}
    public static EditorCommand<AbilityVisualEditorDocument> moveEmission(SkillVfxModel.Hook hook,String id,int target){return change("Move emission",d->d.moveEmission(hook,id,target));}
    public static EditorCommand<AbilityVisualEditorDocument> setEmissionActionIndex(SkillVfxModel.Hook hook,String id,int actionIndex){return change("Set emission action binding",d->d.setEmissionActionIndex(hook,id,actionIndex));}
    public static EditorCommand<AbilityVisualEditorDocument> duplicateEmission(SkillVfxModel.Hook hook,SkillVfxModel.Emission copy){return changeSelecting("Duplicate emission","primitive",copy.primitives().getFirst().id(),d->d.addEmission(hook,copy));}
    public static EditorCommand<AbilityVisualEditorDocument> duplicate(SkillVfxModel.Hook hook,String emission,String id,SkillVfxModel.Primitive copy,int index){return changeSelecting("Duplicate primitive","primitive",copy.id(),d->d.addPrimitive(hook,emission,copy,index));}
    /** Compatibility entry points for existing callers; new authoring UI allocates the copy before execute. */
    public static EditorCommand<AbilityVisualEditorDocument> duplicateEmission(SkillVfxModel.Hook hook,String id){return change("Duplicate emission",d->{var source=d.visual().emissions(hook).stream().filter(e->e.id().equals(id)).findFirst().orElseThrow();Set<String> used=SkillVfxAuthoring.primitiveIds(d.visual());List<SkillVfxModel.Primitive> copy=new ArrayList<>();for(var p:source.primitives()){String next=SkillVfxModel.nextId(used,p.type().name());used.add(next);copy.add(p.withId(next));}d.addEmission(hook,new SkillVfxModel.Emission(SkillVfxModel.nextId(SkillVfxAuthoring.emissionIds(d.visual()),"emission"),source.actionIndex(),copy));});}
    public static EditorCommand<AbilityVisualEditorDocument> duplicate(SkillVfxModel.Hook hook,String emission,String id){return change("Duplicate primitive",d->{var e=d.visual().emissions(hook).stream().filter(x->x.id().equals(emission)).findFirst().orElseThrow();var p=e.primitives().stream().filter(x->x.id().equals(id)).findFirst().orElseThrow();String copy=SkillVfxModel.nextId(SkillVfxAuthoring.primitiveIds(d.visual()),p.type().name());d.addPrimitive(hook,emission,p.withId(copy),e.primitives().indexOf(p)+1);});}
    public static EditorCommand<AbilityVisualEditorDocument> setAppearance(String primitive,SkillVfxModel.Appearance appearance){return change("Set appearance",d->d.setAppearance(primitive,appearance));}
    public static EditorCommand<AbilityVisualEditorDocument> change(String description,java.util.function.Consumer<AbilityVisualEditorDocument> mutation){return changeSelecting(description,null,null,mutation);}
    private static EditorCommand<AbilityVisualEditorDocument> changeSelecting(String description,String selectionKind,String selectionId,java.util.function.Consumer<AbilityVisualEditorDocument> mutation){return new EditorCommand<>(){SkillVfxModel.Visual before;io.github.gyai.projects.editor.core.EditorSelection beforeSelection;public String description(){return description;}public void execute(AbilityVisualEditorDocument d){if(before==null){before=d.visual();beforeSelection=d.selection();}mutation.accept(d);if(selectionKind!=null)d.select(selectionKind,selectionId);}public void undo(AbilityVisualEditorDocument d){d.replaceVisual(before);d.restoreSelection(beforeSelection);}};}
}
