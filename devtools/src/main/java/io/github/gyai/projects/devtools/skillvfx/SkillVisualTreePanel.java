package io.github.gyai.projects.devtools.skillvfx;
import java.util.*;
/** Selection/tree projection with TRAVEL explicitly reserved. */
public final class SkillVisualTreePanel { public List<SkillVfxModel.Hook> hooks(){return List.of(SkillVfxModel.Hook.CAST,SkillVfxModel.Hook.TELEGRAPH,SkillVfxModel.Hook.HIT,SkillVfxModel.Hook.EXPIRE,SkillVfxModel.Hook.CANCEL,SkillVfxModel.Hook.TRAVEL);}public boolean editable(SkillVfxModel.Hook hook){return hook!=SkillVfxModel.Hook.TRAVEL;} }
