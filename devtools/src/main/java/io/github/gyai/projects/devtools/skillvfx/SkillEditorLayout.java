package io.github.gyai.projects.devtools.skillvfx;

import java.util.*;
import java.util.function.ToIntFunction;

/** Frontend-only viewport-first layout: layers | live viewport | inspector above timeline. */
public final class SkillEditorLayout {
 public record Rect(int x,int y,int width,int height){
  public boolean overlaps(Rect other){return x<other.x+other.width&&other.x<x+width&&y<other.y+other.height&&other.y<y+height;}
  public boolean within(Rect outer){return x>=outer.x&&y>=outer.y&&x+width<=outer.x+outer.width&&y+height<=outer.y+outer.height;}
 }
 public record Bounds(Rect header,Rect tree,Rect preview,Rect inspector,Rect timeline){}
 /** Explicit, testable inspector header regions; text never determines their placement. */
 public record InspectorHeader(Rect title,Rect page,Rect previous,Rect next,Rect content){
  public boolean nonOverlapping(){return !title.overlaps(page)&&!title.overlaps(previous)&&!title.overlaps(next)&&!page.overlaps(previous)&&!page.overlaps(next)&&!previous.overlaps(next);}
 }
 /** A standard field is always presented label, input, then Japanese help. */
 public record InspectorField(Rect label,Rect input,Rect feedback,Rect help){
  public boolean ordered(){return label.y()+label.height<=input.y()&&input.y()+input.height<=feedback.y()&&feedback.y()+feedback.height<=help.y();}
 }
 /** Input height is part of the property contract: coordinate editors reserve every row. */
 public record InspectorProperty(String id,String label,String help,int inputHeight) { }
 public record InspectorEntry(InspectorProperty property,InspectorField field) { }
 public record InspectorPage(List<InspectorEntry> entries,int pages,int page){
  public boolean reachable(){return pages>0&&page>=0&&page<pages;}
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
 /** Bounded central-panel controls: primary world confirmation or secondary playback detail. */
 public record PreviewPage(boolean detail,Rect toggle,Rect world,Rect play,Rect stop,Rect restart,Rect loop,Rect speed,Rect quality,Rect anchor,List<PreviewStatusLine> lines){
  public boolean fits(Rect preview,ToIntFunction<String> measurer){return List.of(toggle,world,play,stop,restart,loop,speed,quality,anchor).stream().allMatch(rect->rect.width()==0||rect.within(preview))&&lines.stream().allMatch(line->line.bounds().within(preview)&&line.fits(measurer));}
 }
 /** Six always-reachable primary actions; the screen uses the same right-aligned two-row order. */
 public List<Rect> primaryActions(int width){int[] widths={52,66,84,72,58,58};int right=Math.max(0,width)-8,y=width<760?32:8;ArrayList<Rect> out=new ArrayList<>();for(int value:widths){right-=value;out.add(new Rect(right,y,value,20));right-=4;}return List.copyOf(out);}
 /** Header includes the same six actions plus the DevTools world-confirmation action. */
 public List<Rect> headerActions(int width){int[] widths={52,92,66,84,72,58,58};int right=Math.max(0,width)-8,y=width<760?32:8;ArrayList<Rect> out=new ArrayList<>();for(int value:widths){right-=value;out.add(new Rect(right,y,value,20));right-=4;}return List.copyOf(out);}
 /** Two coordinate rows fit safely in a compact inspector input region. */
 public int nextControlPointOffset(int size,int current){if(size<=2)return 0;int last=Math.max(0,size-2),normalized=Math.clamp(current,0,last);return normalized>=last?0:Math.min(last,normalized+2);}
 private int left=190,right=240,bottom=120;private boolean tree=true,preview=true,inspector=true,timeline=true;
 public Bounds bounds(int width,int height){
   int w=Math.max(0,width),h=Math.max(0,height),margin=Math.min(8,w/8),gap=Math.min(8,w/12),top=Math.min(52,h),body=Math.max(0,h-top);
   int b=timeline?Math.min(Math.max(0,body),Math.clamp(bottom,40,Math.max(40,body-80))):0;
   int inner=Math.max(0,w-margin*2),panelGaps=(tree?gap:0)+(inspector?gap:0);
   int remaining=Math.max(0,inner-panelGaps);int l=tree?Math.min(remaining,Math.clamp(left,80,Math.max(80,inner*22/100))):0;remaining-=l;
   int r=inspector?Math.min(remaining,Math.clamp(right,100,Math.max(100,inner*28/100))):0;remaining-=r;
   int mid=preview?remaining:0;
   if(!preview&&remaining>0&&tree)l+=remaining; else if(!preview&&remaining>0&&inspector)r+=remaining;
   int panelHeight=Math.max(0,body-b),x=margin;
   Rect treeRect=new Rect(x,top,l,panelHeight);x+=l+(tree?gap:0);
   Rect previewRect=new Rect(x,top,mid,panelHeight);x+=mid+(inspector?gap:0);
   Rect inspectorRect=new Rect(x,top,r,panelHeight);
   return new Bounds(new Rect(margin,6,inner,40),treeRect,previewRect,inspectorRect,
           new Rect(margin,h-b+(timeline?gap:0),inner,
                   Math.max(0,b-(timeline?gap:0))));
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
 /**
  * Builds pages from real glyph widths and input heights.  Minecraft's font is
  * injected by the screen (font::width); tests use a wider Japanese measurer.
  */
 public InspectorPage inspectorPage(Rect inspector,List<InspectorProperty> properties,int requestedPage,ToIntFunction<String> measurer,int lineHeight){
  Rect content=inspectorHeader(inspector).content();int usable=Math.max(1,content.height()),line=Math.max(1,lineHeight),gap=5;
  ArrayList<List<InspectorProperty>> pages=new ArrayList<>();ArrayList<InspectorProperty> current=new ArrayList<>();int used=0;
  for(var property:properties){int labelLines=Math.max(1,wrap(property.label(),Math.max(1,content.width()),measurer).size()),helpLines=Math.max(1,wrap(property.help(),Math.max(1,content.width()),measurer).size());int needed=labelLines*line+2+Math.max(18,property.inputHeight())+3+line+helpLines*line+gap;
   if(!current.isEmpty()&&used+needed>usable){pages.add(List.copyOf(current));current.clear();used=0;}current.add(property);used+=needed;
  }
  if(current.isEmpty()&&pages.isEmpty())pages.add(List.of()); else if(!current.isEmpty())pages.add(List.copyOf(current));
  int page=Math.clamp(requestedPage,0,pages.size()-1),y=content.y();ArrayList<InspectorEntry> entries=new ArrayList<>();
  for(var property:pages.get(page)){List<String> label=wrap(property.label(),Math.max(1,content.width()),measurer),help=wrap(property.help(),Math.max(1,content.width()),measurer);int inputY=y+label.size()*line+2,feedbackY=inputY+Math.max(18,property.inputHeight())+3,helpY=feedbackY+line;InspectorField field=new InspectorField(new Rect(content.x(),y,content.width(),label.size()*line),new Rect(content.x(),inputY,content.width(),Math.max(18,property.inputHeight())),new Rect(content.x(),feedbackY,content.width(),line),new Rect(content.x(),helpY,content.width(),help.size()*line));entries.add(new InspectorEntry(property,field));y=helpY+help.size()*line+gap;}
  return new InspectorPage(List.copyOf(entries),pages.size(),page);
 }
 public List<String> wrap(String value,int maxWidth,ToIntFunction<String> measurer){
  ArrayList<String> out=new ArrayList<>();StringBuilder line=new StringBuilder();for(int at=0;at<value.length();){int cp=value.codePointAt(at),next=at+Character.charCount(cp);String candidate=line+new String(Character.toChars(cp));if(line.length()>0&&measurer.applyAsInt(candidate)>maxWidth){out.add(line.toString());line.setLength(0);}line.appendCodePoint(cp);at=next;}if(!line.isEmpty())out.add(line.toString());return out.isEmpty()?List.of(""):List.copyOf(out);
 }
 /** Legacy compact seam retained for callers that only need a conservative count. */
 public int inspectorPageSize(Rect inspector){return Math.max(1,inspectorHeader(inspector).content().height()/52);}
 public InspectorField inspectorField(Rect inspector,int index){Rect content=inspectorHeader(inspector).content();int y=content.y()+Math.max(0,index)*52;return new InspectorField(new Rect(content.x(),y,content.width(),10),new Rect(content.x(),y+12,content.width(),18),new Rect(content.x(),y+30,content.width(),4),new Rect(content.x(),y+34,content.width(),10));}
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
  int x=preview.x()+6,w=Math.max(0,preview.width()-12),y=previewHelp(preview,measurer).controlsY()+69;
  String current="現在: "+hook+(primitive.isBlank()?"":" > "+primitive);
  String state=localError==null||localError.isBlank()?(allowed?"状態: プレビュー可能":"状態: 権限なし"):(allowed?"状態: エラー: ":"状態: 権限なし / ")+localError;
  List<String> text=List.of(shorten(current,w,measurer),shorten("表示: "+anchor,w,measurer),shorten(state,w,measurer));
  ArrayList<PreviewStatusLine> lines=new ArrayList<>();for(int i=0;i<text.size();i++)lines.add(new PreviewStatusLine(text.get(i),new Rect(x,y+i*12,w,10)));
  return new PreviewStatus(List.copyOf(lines));
 }
  /** Controls frame the viewport instead of consuming its center stage. */
  public PreviewPage previewPage(Rect preview,boolean detail,String hook,String primitive,String anchor,boolean allowed,String error,ToIntFunction<String> measurer){
   int x=preview.x()+8,w=Math.max(1,preview.width()-16),top=preview.y()+28;
   Rect toggle=new Rect(x,top,Math.min(92,w),20);ArrayList<PreviewStatusLine> lines=new ArrayList<>();
   if(!detail){
    int statusY=top+25;String state=error==null||error.isBlank()?(allowed?"状態: プレビュー可能":"状態: 権限なし"):(allowed?"状態: エラー: ":"状態: 権限なし / ")+error;
    lines.add(new PreviewStatusLine(shorten("選択: "+hook+(primitive.isBlank()?"":" > "+primitive),w,measurer),new Rect(x,statusY,w,10)));
    lines.add(new PreviewStatusLine(shorten("基準: "+anchor,w,measurer),new Rect(x,statusY+12,w,10)));
    lines.add(new PreviewStatusLine(shorten(state,w,measurer),new Rect(x,statusY+24,w,10)));
    Rect world=new Rect(x,Math.max(statusY+38,preview.y()+preview.height()-28),Math.min(176,w),20);
    return new PreviewPage(false,toggle,world,zero(x,top),zero(x,top),zero(x,top),zero(x,top),zero(x,top),zero(x,top),zero(x,top),List.copyOf(lines));
   }
   int row=Math.max(top+24,preview.y()+preview.height()-52);
   Rect play=new Rect(x,row,Math.min(72,w),20),stop=new Rect(x+76,row,Math.min(48,Math.max(0,w-76)),20),restart=new Rect(x+128,row,Math.min(68,Math.max(0,w-128)),20);
   row+=24;Rect loop=new Rect(x,row,Math.min(72,w),20),speed=new Rect(x+76,row,Math.min(82,Math.max(0,w-76)),20),quality=new Rect(x+162,row,Math.min(76,Math.max(0,w-162)),20),anchorRect=new Rect(x+242,row,Math.min(100,Math.max(0,w-242)),20);
   return new PreviewPage(true,toggle,zero(x,row),play,stop,restart,loop,speed,quality,anchorRect,List.of());
  }
 private static Rect zero(int x,int y){return new Rect(x,y,0,0);}
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
