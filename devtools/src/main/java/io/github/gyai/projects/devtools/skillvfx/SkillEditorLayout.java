package io.github.gyai.projects.devtools.skillvfx;

import java.util.*;
import java.util.function.ToIntFunction;

/** Frontend-only fixed editor layout: tree | preview | inspector above timeline. */
public final class SkillEditorLayout {
 public record Rect(int x,int y,int width,int height){
  public boolean overlaps(Rect other){return x<other.x+other.width&&other.x<x+width&&y<other.y+other.height&&other.y<y+height;}
  public boolean within(Rect outer){return x>=outer.x&&y>=outer.y&&x+width<=outer.x+outer.width&&y+height<=outer.y+outer.height;}
 }
 public record Bounds(Rect tree,Rect preview,Rect inspector,Rect timeline){}
 /** Explicit, testable inspector header regions; text never determines their placement. */
 public record InspectorHeader(Rect title,Rect page,Rect previous,Rect next,Rect content){
  public boolean nonOverlapping(){return !title.overlaps(page)&&!title.overlaps(previous)&&!title.overlaps(next)&&!page.overlaps(previous)&&!page.overlaps(next)&&!previous.overlaps(next);}
 }
 /** A standard field is always presented label, input, then Japanese help. */
 public record InspectorField(Rect label,Rect input,Rect help){
  public boolean ordered(){return label.y()+label.height<=input.y()&&input.y()+input.height<=help.y();}
 }
 /** Short Japanese lines avoid relying on global label clipping in the Preview panel. */
 public record PreviewHelpLine(String text,Rect bounds){
  public boolean fits(ToIntFunction<String> measurer){return measurer.applyAsInt(text)<=bounds.width();}
 }
 public record PreviewHelp(List<PreviewHelpLine> lines,int controlsY,Rect controlsBounds){
  public boolean fits(Rect preview,ToIntFunction<String> measurer){return lines.stream().allMatch(line->line.bounds().within(preview)&&line.fits(measurer));}
 }
 public record PreviewStatusLine(String text,Rect bounds){
  public boolean fits(ToIntFunction<String> measurer){return measurer.applyAsInt(text)<=bounds.width();}
 }
 public record PreviewStatus(List<PreviewStatusLine> lines){
  public boolean fits(Rect preview,ToIntFunction<String> measurer){return lines.stream().allMatch(line->line.bounds().within(preview)&&line.fits(measurer));}
 }
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
 /**
  * Fixed header geometry leaves a title row and a second pager row. This is a
  * pure seam so narrow-window behavior does not depend on estimated glyph widths.
  */
 public InspectorHeader inspectorHeader(Rect inspector){
  int x=inspector.x()+6,w=Math.max(0,inspector.width()-12),titleY=inspector.y()+22,pagerY=inspector.y()+37;
  int previousWidth=Math.min(48,Math.max(24,(w-28)/3)),nextWidth=previousWidth;
  int pageWidth=Math.max(1,w-previousWidth-nextWidth-8);
  Rect title=new Rect(x,titleY,w,12),page=new Rect(x,pagerY,pageWidth,20);
  Rect previous=new Rect(x+pageWidth+4,pagerY,previousWidth,20),next=new Rect(previous.x()+previous.width()+4,pagerY,nextWidth,20);
  return new InspectorHeader(title,page,previous,next,new Rect(x,pagerY+24,w,Math.max(0,inspector.y()+inspector.height()-6-(pagerY+24))));
 }
 /** Number of complete label/input/help rows which fit below the persistent pager. */
 public int inspectorPageSize(Rect inspector){return Math.max(1,inspectorHeader(inspector).content().height()/52);}
 public InspectorField inspectorField(Rect inspector,int index){
  Rect content=inspectorHeader(inspector).content();int y=content.y()+Math.max(0,index)*52;
  return new InspectorField(new Rect(content.x(),y,content.width(),10),new Rect(content.x(),y+12,content.width(),18),new Rect(content.x(),y+34,content.width(),10));
 }
 /** Compact title text; the title has its own deterministic region above the pager. */
 public String inspectorTitle(SkillVfxModel.PrimitiveType type,int first,int last,int total,Rect inspector){return inspector.width()<=260?"詳細: "+SkillVfxDisplay.primitive(type):SkillVfxDisplay.primitive(type)+" ("+type+")";}
 public String inspectorPageLabel(int first,int last,int total){return (first+1)+"〜"+last+" / "+total;}
 public int inspectorTitleWidth(Rect inspector){return inspectorHeader(inspector).title().width();}
 /** Fits every rendered Preview help label using the supplied Minecraft font measurement. */
 public PreviewHelp previewHelp(Rect preview,ToIntFunction<String> measurer){
  int x=preview.x()+6,w=Math.max(0,preview.width()-12),y=preview.y()+26;
  List<String> text=List.of("再生すると VFX は、","プレイヤー周囲のワールド内に","表示されます。カメラを動かして","見え方を確認してください。");
  ArrayList<PreviewHelpLine> lines=new ArrayList<>();for(int i=0;i<text.size();i++)lines.add(new PreviewHelpLine(shorten(text.get(i),w,measurer),new Rect(x,y+i*12,w,10)));
  int controlsY=y+text.size()*12+2;
  return new PreviewHelp(List.copyOf(lines),controlsY,new Rect(x,controlsY,w,68));
 }
 /** Three fixed status lines prevent selection/error text from spilling into the Inspector. */
 public PreviewStatus previewStatus(Rect preview,String hook,String primitive,String anchor,boolean allowed,String localError,ToIntFunction<String> measurer){
  int x=preview.x()+6,w=Math.max(0,preview.width()-12),y=previewHelp(preview,measurer).controlsY()+73;
  String current="現在: "+hook+(primitive.isBlank()?"":" > "+primitive);
  String state=localError==null||localError.isBlank()?(allowed?"状態: プレビュー可能":"状態: 権限なし"):(allowed?"状態: エラー: ":"状態: 権限なし / ")+localError;
  List<String> text=List.of(shorten(current,w,measurer),shorten("表示: "+anchor,w,measurer),shorten(state,w,measurer));
  ArrayList<PreviewStatusLine> lines=new ArrayList<>();for(int i=0;i<text.size();i++)lines.add(new PreviewStatusLine(text.get(i),new Rect(x,y+i*12,w,10)));
  return new PreviewStatus(List.copyOf(lines));
 }
 private static String shorten(String value,int maxWidth,ToIntFunction<String> measurer){
  if(measurer.applyAsInt(value)<=maxWidth)return value;
  StringBuilder out=new StringBuilder();for(int at=0;at<value.length();){int codePoint=value.codePointAt(at),next=at+Character.charCount(codePoint);String candidate=out.toString()+new String(Character.toChars(codePoint))+"…";if(measurer.applyAsInt(candidate)>maxWidth)break;out.appendCodePoint(codePoint);at=next;}
  return out.isEmpty()?(measurer.applyAsInt("…")<=maxWidth?"…":""):out+"…";
 }
 /** Compact UI uses these values directly for real widgets, keeping 640x360 reachability testable without Minecraft. */
 public int treeNodeRows(Rect tree){return Math.max(1,(tree.height()-114)/20);}
 public int treeActionRows(Rect tree){return Math.max(0,(tree.height()-28)/22);}
 public boolean bezierControlPage(Rect inspector){return inspector.height()<240;}
 public void dragLeft(int x,int width){left=Math.clamp(x,80,Math.max(80,width-100));}public void dragRight(int x,int width){right=Math.clamp(width-x,100,Math.max(100,width-80));}public void dragBottom(int y,int height){bottom=Math.clamp(height-y,40,Math.max(40,height-132));}
 public void visibleTree(boolean v){tree=v;}public void visiblePreview(boolean v){preview=v;}public void visibleInspector(boolean v){inspector=v;}public void visibleTimeline(boolean v){timeline=v;}
 public boolean treeVisible(){return tree;}public boolean previewVisible(){return preview;}public boolean inspectorVisible(){return inspector;}public boolean timelineVisible(){return timeline;}
 public void reset(){left=190;right=240;bottom=120;tree=preview=inspector=timeline=true;}
}
