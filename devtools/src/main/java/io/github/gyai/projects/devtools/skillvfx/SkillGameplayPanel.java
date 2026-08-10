package io.github.gyai.projects.devtools.skillvfx;
import java.util.*;
/** Read-only ordered gameplay projection; descriptions originate in the received snapshot metadata. */
public final class SkillGameplayPanel { private int selected; public List<SkillVfxModel.GameplayAction> actions(AbilityVisualEditorDocument d){return d==null?List.of():d.baseline().gameplay();}public void select(int i,AbilityVisualEditorDocument d){selected=Math.clamp(i,0,Math.max(0,actions(d).size()-1));}public int selected(){return selected;} }
