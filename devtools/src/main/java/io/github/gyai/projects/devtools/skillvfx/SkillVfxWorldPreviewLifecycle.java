package io.github.gyai.projects.devtools.skillvfx;

/** Pure identity/lifecycle contract for the temporarily detached editor screen. */
public final class SkillVfxWorldPreviewLifecycle {
 public enum KeyAction { NONE, TOGGLE, RESTART, RETURN }
 private Object screen,document,connection,level;private String dimension="";private boolean active;
 public boolean enter(Object nextScreen,Object nextDocument,Object nextConnection,Object nextLevel,String nextDimension){if(active||nextScreen==null||nextDocument==null||nextConnection==null||nextLevel==null||nextDimension==null||nextDimension.isBlank())return false;screen=nextScreen;document=nextDocument;connection=nextConnection;level=nextLevel;dimension=nextDimension;active=true;return true;}
 public boolean valid(Object currentScreen,Object currentDocument,Object currentConnection,Object currentLevel,String currentDimension,boolean playerAlive){return active&&currentScreen==null&&document==currentDocument&&connection==currentConnection&&level==currentLevel&&dimension.equals(currentDimension)&&playerAlive;}
 /** Queued keys are always consumed; only a valid detached world may act on them. */
 public KeyAction key(boolean toggle,boolean restart,boolean back,boolean valid){if(!valid)return KeyAction.NONE;if(back)return KeyAction.RETURN;if(restart)return KeyAction.RESTART;return toggle?KeyAction.TOGGLE:KeyAction.NONE;}
 /** Returns true exactly when detached playback may advance this client tick. */
 public boolean advance(boolean valid){return active&&valid;}
 public Object returnScreen(){return screen;}
 public Object document(){return document;}
 public boolean active(){return active;}
 public void clear(){active=false;screen=document=connection=level=null;dimension="";}
}
