package io.github.gyai.projects.devtools.skillvfx;

/** Pure UI capability/feedback mapper, shared by themed components. */
public final class SkillEditorController {
 public enum Feedback { NONE,APPLIED,REVERTED,STALE,CONFLICT,DENIED,MALFORMED,REFRESH_REQUIRED }
 private SkillEditorController(){} public static boolean canApply(AbilityVisualEditorDocument doc,boolean pending,boolean connected,boolean permitted,boolean hasVisual){return doc!=null&&doc.dirty()&&!pending&&connected&&permitted&&hasVisual;}public static Feedback feedback(SkillVfxEditorProtocol.Status status,boolean apply){return switch(status){case OK->apply?Feedback.APPLIED:Feedback.REVERTED;case STALE->Feedback.STALE;case CONFLICT->Feedback.CONFLICT;case PERMISSION_DENIED->Feedback.DENIED;case MALFORMED->Feedback.MALFORMED;default->Feedback.REFRESH_REQUIRED;};}
}
