package io.github.gyai.projects.devtools.skillvfx;

import java.util.*;

/** Frontend-only fixed editor layout: tree | preview | inspector above timeline. */
public final class SkillEditorLayout {
 public record Rect(int x,int y,int width,int height){} public record Bounds(Rect tree,Rect preview,Rect inspector,Rect timeline){}
 /** Six always-reachable primary actions; the screen uses the same right-aligned two-row order. */
 public List<Rect> primaryActions(int width){int[] widths={52,66,84,72,58,58};int right=Math.max(0,width)-8,y=width<760?32:8;ArrayList<Rect> out=new ArrayList<>();for(int value:widths){right-=value;out.add(new Rect(right,y,value,20));right-=4;}return List.copyOf(out);}
 private int left=190,right=240,bottom=120;private boolean tree=true,preview=true,inspector=true,timeline=true;
 public Bounds bounds(int width,int height){
  int w=Math.max(0,width),h=Math.max(0,height),top=Math.min(52,h),body=Math.max(0,h-top);
  int b=timeline?Math.min(Math.max(0,body),Math.clamp(bottom,40,Math.max(40,body-80))):0;
  int remaining=w;int l=tree?Math.min(remaining,Math.max(80,left)):0;remaining-=l;
  int r=inspector?Math.min(remaining,Math.max(100,right)):0;remaining-=r;
  int mid=preview?remaining:0;
  if(!preview&&remaining>0&&tree)l+=remaining; else if(!preview&&remaining>0&&inspector)r+=remaining;
  int panelHeight=Math.max(0,body-b);
  return new Bounds(new Rect(0,top,l,panelHeight),new Rect(l,top,mid,panelHeight),new Rect(l+mid,top,r,panelHeight),new Rect(0,h-b,w,b));
 }
 /** Number of complete inspector rows which fit with the persistent pager. */
 public int inspectorPageSize(Rect inspector){return Math.max(1,(inspector.height()-42)/30);}
 /** Compact left-side title that cannot intrude into the fixed 110px pager reservation. */
 public String inspectorTitle(SkillVfxModel.PrimitiveType type,int first,int last,int total,Rect inspector){return inspector.width()<=260?"詳細 "+(first+1)+"/"+total:SkillVfxDisplay.primitive(type)+"  ("+type+")  "+(first+1)+"〜"+last+" / "+total;}
 public int inspectorTitleWidth(Rect inspector){return Math.max(0,inspector.width()-110);}
 /** Compact UI uses these values directly for real widgets, keeping 640x360 reachability testable without Minecraft. */
 public int treeNodeRows(Rect tree){return Math.max(1,(tree.height()-114)/20);}
 public int treeActionRows(Rect tree){return Math.max(0,(tree.height()-28)/22);}
 public boolean bezierControlPage(Rect inspector){return inspector.height()<240;}
 public void dragLeft(int x,int width){left=Math.clamp(x,80,Math.max(80,width-100));}public void dragRight(int x,int width){right=Math.clamp(width-x,100,Math.max(100,width-80));}public void dragBottom(int y,int height){bottom=Math.clamp(height-y,40,Math.max(40,height-132));}
 public void visibleTree(boolean v){tree=v;}public void visiblePreview(boolean v){preview=v;}public void visibleInspector(boolean v){inspector=v;}public void visibleTimeline(boolean v){timeline=v;}
 public boolean treeVisible(){return tree;}public boolean previewVisible(){return preview;}public boolean inspectorVisible(){return inspector;}public boolean timelineVisible(){return timeline;}
 public void reset(){left=190;right=240;bottom=120;tree=preview=inspector=timeline=true;}
}
