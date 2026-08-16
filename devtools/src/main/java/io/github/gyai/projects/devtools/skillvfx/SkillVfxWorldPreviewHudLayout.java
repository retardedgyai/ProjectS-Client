package io.github.gyai.projects.devtools.skillvfx;

import java.util.*;
import java.util.function.ToIntFunction;

/** Pure, font-measured corner-HUD layout.  Never assumes key labels are ASCII-width. */
public final class SkillVfxWorldPreviewHudLayout {
 public record Presentation(List<String> lines,int width,int height) { }
 private SkillVfxWorldPreviewHudLayout() { }
 public static Presentation layout(int maximumWidth,ToIntFunction<String> width,String title,String status,String controls){int max=Math.max(1,maximumWidth);ArrayList<String> lines=new ArrayList<>();for(String source:List.of(title,status,controls))lines.addAll(wrap(source,max,width));int actual=lines.stream().mapToInt(width::applyAsInt).max().orElse(0);return new Presentation(List.copyOf(lines),Math.min(max,actual+6),lines.size()*12+6);}
 public static List<String> wrap(String value,int max,ToIntFunction<String> width){ArrayList<String> out=new ArrayList<>();StringBuilder line=new StringBuilder();for(int at=0;at<value.length();){int cp=value.codePointAt(at),next=at+Character.charCount(cp);String candidate=line+new String(Character.toChars(cp));if(line.length()>0&&width.applyAsInt(candidate)>max){out.add(line.toString());line.setLength(0);}line.appendCodePoint(cp);at=next;}if(!line.isEmpty())out.add(line.toString());return out.isEmpty()?List.of(""):List.copyOf(out);}
}
