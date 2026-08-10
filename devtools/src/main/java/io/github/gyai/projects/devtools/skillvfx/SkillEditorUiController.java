package io.github.gyai.projects.devtools.skillvfx;

import java.util.*;

/** Pure interaction adapter used by the Minecraft widgets.  It deliberately owns no document or network state. */
public final class SkillEditorUiController {
    public enum ScalarMode { LITERAL, FROM_GAMEPLAY }
    public record Header(boolean undo, boolean redo, boolean refresh, boolean apply, boolean revert,
                         boolean dirty, String dirtyLabel) { }
    public record ParseResult<T>(T value, String error) { public boolean valid(){ return error.isEmpty(); } }
    private SkillEditorUiController() { }

    public static Header header(AbilityVisualEditorDocument document, boolean pending, boolean connected,
                                boolean permitted, boolean hasVisual) {
        boolean dirty=document!=null&&document.dirty();
        return new Header(document!=null&&document.history().canUndo()&&!pending,
                document!=null&&document.history().canRedo()&&!pending, connected&&!pending,
                SkillEditorController.canApply(document,pending,connected,permitted,hasVisual),
                document!=null&&!pending&&connected&&permitted&&hasVisual,
                dirty, dirty ? "Unsaved visual changes" : "Server baseline");
    }

    public static List<String> inspectorFields(SkillVfxModel.Primitive primitive) {
        return primitive==null ? List.of() : AbilityVisualPropertySchemas.descriptors(primitive.type()).stream().map(io.github.gyai.projects.editor.core.PropertyDescriptor::id).toList();
    }
    public static ScalarMode mode(SkillVfxModel.Scalar scalar) {
        return scalar instanceof SkillVfxModel.FromGameplay ? ScalarMode.FROM_GAMEPLAY : ScalarMode.LITERAL;
    }
    public static ParseResult<Double> number(String raw) {
        try { double value=Double.parseDouble(raw); return Double.isFinite(value) ? new ParseResult<>(value,"") : new ParseResult<>(null,"Enter a finite number"); }
        catch (RuntimeException ignored) { return new ParseResult<>(null,"Enter a valid number"); }
    }
    public static ParseResult<Integer> integer(String raw, int min, int max) {
        try { int value=Integer.parseInt(raw); return value>=min&&value<=max ? new ParseResult<>(value,"") : new ParseResult<>(null,"Value must be between "+min+" and "+max); }
        catch (RuntimeException ignored) { return new ParseResult<>(null,"Enter a whole number"); }
    }
    public static ParseResult<Long> longValue(String raw) {
        try { return new ParseResult<>(Long.parseLong(raw),""); } catch (RuntimeException ignored) { return new ParseResult<>(null,"Enter a whole number"); }
    }
    public static ParseResult<Integer> argb(String raw) {
        String text=raw==null?"":raw.trim(); if(text.startsWith("#")) text=text.substring(1);
        if(text.length()==6) text="FF"+text;
        try { if(text.length()!=8) throw new NumberFormatException(); return new ParseResult<>((int)Long.parseLong(text,16),""); }
        catch (RuntimeException ignored) { return new ParseResult<>(null,"Use #AARRGGBB or #RRGGBB"); }
    }
    public static String argb(int value) { return String.format(Locale.ROOT,"#%08X",value); }
    public static SkillVfxModel.Scalar scalar(ScalarMode mode, double literal, SkillVfxModel.ActionField field) {
        return mode==ScalarMode.FROM_GAMEPLAY ? new SkillVfxModel.FromGameplay(field) : new SkillVfxModel.Literal(literal);
    }
    public static String feedback(SkillEditorController.Feedback feedback, String serverMessage) {
        String detail=serverMessage==null?"":serverMessage;
        return switch(feedback) { case APPLIED -> "Session override applied; it is cleared when the server restarts.";
            case REVERTED -> "Session override reverted."; case STALE, CONFLICT -> "The server changed. Your draft is preserved; refresh before applying.";
            case DENIED -> "The server denied this editor action."; case MALFORMED -> "The server rejected the visual draft.";
            case REFRESH_REQUIRED -> detail.isBlank()?"Refresh required.":detail; case NONE -> ""; };
    }
}
