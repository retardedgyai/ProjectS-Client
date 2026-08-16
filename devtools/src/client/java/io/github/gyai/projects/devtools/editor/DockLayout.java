package io.github.gyai.projects.devtools.editor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Deterministic v0.1 split layout. Gaps are included in all fit calculations. */
public final class DockLayout {
    private static final int GAP = 4;
    public enum Axis { HORIZONTAL, VERTICAL }
    public record Rect(int x, int y, int width, int height) { }
    public record PanelBounds(EditorPanel panel, Rect bounds) { }
    private final LinkedHashMap<String, EditorPanel> panels = new LinkedHashMap<>();
    private Axis axis = Axis.HORIZONTAL;
    private int divider = 320;
    private boolean collapsed;

    public DockLayout(EditorPanel... initial) { for (EditorPanel panel : initial) if (panels.putIfAbsent(panel.id(), panel) != null) throw new IllegalArgumentException("Duplicate panel: " + panel.id()); }
    public Map<String, EditorPanel> panels(){ return Collections.unmodifiableMap(new LinkedHashMap<>(panels)); }
    public List<EditorPanel> orderedPanels(){ return List.copyOf(panels.values()); }
    public void setVisible(String id, boolean visible){ EditorPanel panel=panels.get(id); if(panel!=null) panels.put(id,panel.withVisible(visible)); }
    public void split(Axis value){ axis=java.util.Objects.requireNonNull(value); } public Axis axis(){return axis;}
    public void resize(int requested, int total){ List<EditorPanel> visible=visible(); if(visible.size()<2){divider=0;return;} int safeTotal=Math.max(0,total), gap=gapFor(safeTotal,2), usable=safeTotal-gap, leadMinimum=axisMinimum(visible.getFirst()), trailingMinimum=visible.subList(1,visible.size()).stream().mapToInt(this::axisMinimum).max().orElse(0); divider=splitLead(requested,usable,leadMinimum,trailingMinimum); }
    public int divider(){return divider;} public void collapse(boolean value){collapsed=value;} public boolean collapsed(){return collapsed;}
    public List<PanelBounds> bounds(int x,int y,int width,int height){ int safeWidth=Math.max(0,width), safeHeight=Math.max(0,height); List<EditorPanel> visible=visible(); if(visible.isEmpty())return List.of(); if(collapsed||visible.size()==1)return List.of(new PanelBounds(visible.getFirst(),new Rect(x,y,safeWidth,safeHeight))); boolean horizontal=axis==Axis.HORIZONTAL; int total=horizontal?safeWidth:safeHeight, gap=gapFor(total,2), usable=total-gap, leadMinimum=axisMinimum(visible.getFirst()), trailingMinimum=visible.subList(1,visible.size()).stream().mapToInt(this::axisMinimum).max().orElse(0); int leading=splitLead(divider,usable,leadMinimum,trailingMinimum); int trailing=Math.max(0,usable-leading); divider=leading; List<PanelBounds> result=new ArrayList<>(); if(horizontal){result.add(new PanelBounds(visible.getFirst(),new Rect(x,y,leading,safeHeight))); appendStack(result,visible.subList(1,visible.size()),x+leading+gap,y,trailing,safeHeight,false);}else{result.add(new PanelBounds(visible.getFirst(),new Rect(x,y,safeWidth,leading))); appendStack(result,visible.subList(1,visible.size()),x,y+leading+gap,safeWidth,trailing,true);} return List.copyOf(result); }
    private int axisMinimum(EditorPanel panel){return axis==Axis.HORIZONTAL?panel.minWidth():panel.minHeight();}
    private static int gapFor(int total,int panes){return panes<2?0:Math.min(GAP,Math.max(0,total)/(panes-1));}
    private static int splitLead(int requested,int usable,int leadMinimum,int trailingMinimum){if(usable>=leadMinimum+trailingMinimum)return Math.clamp(requested,leadMinimum,usable-trailingMinimum); if(usable==0)return 0; long denominator=(long)leadMinimum+trailingMinimum; return denominator==0?usable/2:(int)Math.clamp((usable*(long)leadMinimum+denominator/2)/denominator,0L,(long)usable);}
    private void appendStack(List<PanelBounds> result,List<EditorPanel> rest,int x,int y,int width,int height,boolean horizontal){ int total=Math.max(0,horizontal?width:height), count=rest.size(), gap=gapFor(total,count), usable=Math.max(0,total-gap*(count-1)); int minimumTotal=rest.stream().mapToInt(panel->horizontal?panel.minWidth():panel.minHeight()).sum(); int[] sizes=new int[count]; if(usable>=minimumTotal){int remaining=usable;for(int i=0;i<count;i++){sizes[i]=i==count-1?remaining:(horizontal?rest.get(i).minWidth():rest.get(i).minHeight());remaining-=sizes[i];}}else{int base=usable/count, remainder=usable%count;for(int i=0;i<count;i++)sizes[i]=base+(i<remainder?1:0);} int cursor=horizontal?x:y; for(int i=0;i<count;i++){if(horizontal){result.add(new PanelBounds(rest.get(i),new Rect(cursor,y,sizes[i],Math.max(0,height))));cursor+=sizes[i]+gap;}else{result.add(new PanelBounds(rest.get(i),new Rect(x,cursor,Math.max(0,width),sizes[i])));cursor+=sizes[i]+gap;}} }
    private List<EditorPanel> visible(){return panels.values().stream().filter(EditorPanel::visible).toList();}
    public void reset(){axis=Axis.HORIZONTAL;divider=320;collapsed=false;panels.replaceAll((id,panel)->panel.withVisible(true));}
}
