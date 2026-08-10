package io.github.gyai.projects.editor.core;

import java.util.List;

public final class EditorCoreFoundationTest {
    private record Doc(String id, StringBuilder value) implements EditorDocument { }
    public static void main(String[] args) {
        historyTracksSavedBaselineAndBranchDivergence();
        historyDoesNotMoveCursorWhenCallbacksFail();
        schemaIsOrderedAndWritesPerDocument();
    }
    private static void historyDoesNotMoveCursorWhenCallbacksFail() {
        Doc undoDoc=new Doc("undo",new StringBuilder()); EditorHistory<Doc> undoHistory=new EditorHistory<>();
        undoHistory.execute(undoDoc,command("a","A")); undoHistory.markSaved(); undoHistory.execute(undoDoc,new EditorCommand<>() { public String description(){return "broken undo";} public void execute(Doc d){d.value.append("B");} public void undo(Doc d){throw new IllegalStateException("undo failed");} });
        assert undoHistory.canUndo() && !undoHistory.canRedo() && undoHistory.dirtyState()==DirtyState.DIRTY;
        try { undoHistory.undo(undoDoc); throw new AssertionError("undo should throw"); } catch (IllegalStateException expected) { }
        assert undoHistory.canUndo() && !undoHistory.canRedo() && undoHistory.dirtyState()==DirtyState.DIRTY;
        Doc redoDoc=new Doc("redo",new StringBuilder()); EditorHistory<Doc> redoHistory=new EditorHistory<>(); redoHistory.execute(redoDoc,command("a","A")); redoHistory.markSaved(); redoHistory.execute(redoDoc,new EditorCommand<>() { public String description(){return "broken redo";} public void execute(Doc d){d.value.append("B");} public void undo(Doc d){d.value.deleteCharAt(d.value.length()-1);} public void redo(Doc d){throw new IllegalStateException("redo failed");} });
        assert redoHistory.undo(redoDoc) && redoHistory.canUndo() && redoHistory.canRedo() && redoHistory.dirtyState()==DirtyState.CLEAN;
        try { redoHistory.redo(redoDoc); throw new AssertionError("redo should throw"); } catch (IllegalStateException expected) { }
        assert redoHistory.canUndo() && redoHistory.canRedo() && redoHistory.dirtyState()==DirtyState.CLEAN;
    }
    private static void historyTracksSavedBaselineAndBranchDivergence() {
        Doc doc = new Doc("doc", new StringBuilder()); EditorHistory<Doc> history = new EditorHistory<>();
        EditorCommand<Doc> a=command("a","A"), b=command("b","B"), c=command("c","C");
        history.execute(doc,a); history.execute(doc,b); history.markSaved(); assert history.dirtyState()==DirtyState.CLEAN;
        assert history.undo(doc) && doc.value.toString().equals("A") && history.dirtyState()==DirtyState.DIRTY;
        assert history.redo(doc) && doc.value.toString().equals("AB") && history.dirtyState()==DirtyState.CLEAN;
        assert history.undo(doc); history.execute(doc,c); assert doc.value.toString().equals("AC") && !history.canRedo() && history.dirtyState()==DirtyState.DIRTY;
        history.reset(); assert history.size()==0 && history.dirtyState()==DirtyState.CLEAN && !history.canUndo() && !history.canRedo();
    }
    private static void schemaIsOrderedAndWritesPerDocument() {
        Doc first=new Doc("first",new StringBuilder("a")), second=new Doc("second",new StringBuilder("b"));
        PropertyDescriptor<Doc,String> name=new PropertyDescriptor<>("name","Name",String.class,d->d.value.toString(),(d,v)->{d.value.setLength(0);d.value.append(v);},v->!v.isBlank(),"required","Identity","General");
        PropertyDescriptor<Doc,Integer> size=new PropertyDescriptor<>("size","Size",Integer.class,d->d.value.length(),(d,v)->{},v->v>=0,"positive","Metrics","General");
        PropertySchema<Doc> schema=new PropertySchema<>(List.of(name,size));
        assert schema.properties().stream().map(PropertyDescriptor::id).toList().equals(List.of("name","size"));
        name.write(first,"one"); name.write(second,"two"); assert name.read(first).equals("one") && name.read(second).equals("two"); assert !name.validate("").valid();
        try { new PropertySchema<>(List.of(name,name)); throw new AssertionError("duplicate accepted"); } catch (IllegalArgumentException expected) { }
    }
    private static EditorCommand<Doc> command(String description,String value){return new EditorCommand<>(){public String description(){return description;}public void execute(Doc doc){doc.value.append(value);}public void undo(Doc doc){doc.value.delete(doc.value.length()-value.length(),doc.value.length());}};}
}
