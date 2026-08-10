package io.github.gyai.projects.devtools.skillvfx;

import java.util.*;

/** Pure, bounded presentation geometry shared by the live picker and 640x360 tests. */
public final class SkillVfxPickerPresentation {
    public record Item(String id, String label, String detail) { public Item { Objects.requireNonNull(id); Objects.requireNonNull(label); Objects.requireNonNull(detail); } }
    public record Rect(int x,int y,int width,int height) { public boolean within(Rect outer){return x>=outer.x&&y>=outer.y&&x+width<=outer.x+outer.width&&y+height<=outer.y+outer.height;} }
    public record Row(Item item,Rect button,Rect detail) { public boolean within(Rect bounds){return button.within(bounds)&&detail.within(bounds);} }
    public record Page(Rect bounds,String title,List<Row> rows,Rect previous,Rect next,Rect cancel,int page,int pages) {
        public boolean withinScreen(Rect screen){return bounds.within(screen)&&rows.stream().allMatch(row->row.within(bounds)&&row.within(screen))&&previous.within(bounds)&&next.within(bounds)&&cancel.within(bounds);}
    }
    private static final int ROWS=4;
    private SkillVfxPickerPresentation() { }
    public static Page page(int screenWidth,int screenHeight,String title,List<Item> items,int requestedPage) {
        Rect screen=new Rect(0,0,Math.max(0,screenWidth),Math.max(0,screenHeight)); int x=8,y=56,w=Math.max(1,Math.min(300,Math.max(1,screen.width()-16))); int h=Math.max(1,Math.min(Math.max(1,screen.height()-56),214)); Rect bounds=new Rect(x,y,w,h);
        int pages=Math.max(1,(items.size()+ROWS-1)/ROWS),page=Math.clamp(requestedPage,0,pages-1),first=page*ROWS; ArrayList<Row> rows=new ArrayList<>();
        for(int i=first;i<Math.min(first+ROWS,items.size());i++){int row=i-first,ry=y+18+row*46;rows.add(new Row(items.get(i),new Rect(x,ry,w,20),new Rect(x+4,ry+22,Math.max(1,w-8),12)));}
        int controlsY=Math.min(y+h-22,y+18+ROWS*46+4);return new Page(bounds,title,List.copyOf(rows),new Rect(x,controlsY,52,20),new Rect(x+56,controlsY,52,20),new Rect(x+112,controlsY,72,20),page,pages);
    }
}
