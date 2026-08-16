package io.github.gyai.projects.devtools.skillvfx;

/** Pure identity guard for the retained, transparent authoring screen. */
public final class SkillVfx3dAuthoringLifecycle {
 private Object owner,document,connection,level; private String dimension="",primitive=""; private boolean active;
 public boolean enter(Object owner,Object document,Object connection,Object level,String dimension,String primitive){if(owner==null||document==null||connection==null||level==null||dimension==null||primitive==null)return false;this.owner=owner;this.document=document;this.connection=connection;this.level=level;this.dimension=dimension;this.primitive=primitive;active=true;return true;}
 public boolean valid(Object currentOwner,Object currentDocument,Object currentConnection,Object currentLevel,String currentDimension,boolean alive){return active&&alive&&owner==currentOwner&&document==currentDocument&&connection==currentConnection&&level==currentLevel&&dimension.equals(currentDimension);}
 public boolean returning(Object currentOwner,Object currentDocument,Object currentConnection,Object currentLevel,String currentDimension,boolean alive){return valid(currentOwner,currentDocument,currentConnection,currentLevel,currentDimension,alive);}
 public boolean active(){return active;} public String primitive(){return primitive;} public void clear(){active=false;owner=null;document=null;connection=null;level=null;dimension="";primitive="";}
}
