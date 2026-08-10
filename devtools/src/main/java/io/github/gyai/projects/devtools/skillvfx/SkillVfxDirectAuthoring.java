package io.github.gyai.projects.devtools.skillvfx;

import io.github.gyai.projects.client.vfx.AbilityVfx;
import java.util.*;

/**
 * Frontend-independent guide, projection, picking and single-gesture history math.
 * The guide deliberately delegates all primitive geometry to {@link AbilityVfx#sample}.
 */
public final class SkillVfxDirectAuthoring {
    public record Vec(double x,double y,double z) {
        public Vec { if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z))throw new IllegalArgumentException("finite vector"); }
        public Vec add(Vec v){return new Vec(x+v.x,y+v.y,z+v.z);} public Vec scale(double n){return new Vec(x*n,y*n,z*n);}
    }
    public record ScreenPoint(double x,double y,double depth) { public ScreenPoint { if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(depth))throw new IllegalArgumentException("finite screen point"); } }
    public record Handle(String id,String label,Vec position,Kind kind) { public enum Kind { AXIS_X, AXIS_Y, AXIS_Z, POINT, RADIUS, HEIGHT, TURNS } }
    public record Projection(Vec right,Vec up,Vec forward,Vec origin,double focal,double guiScale) {
        public Projection { Objects.requireNonNull(right);Objects.requireNonNull(up);Objects.requireNonNull(forward);Objects.requireNonNull(origin);if(focal<=0||guiScale<=0||!Double.isFinite(focal)||!Double.isFinite(guiScale))throw new IllegalArgumentException("projection"); }
        public Optional<ScreenPoint> project(Vec world){Vec d=new Vec(world.x-origin.x,world.y-origin.y,world.z-origin.z);double z=dot(d,forward);if(z<=1e-6)return Optional.empty();return Optional.of(new ScreenPoint(dot(d,right)*focal/z/guiScale,dot(d,up)*focal/z/guiScale,z));}
    }
    public record Pick(Handle handle,double distance) { }
    public record DragDelta(double x,double y) { public DragDelta { if(!Double.isFinite(x)||!Double.isFinite(y))throw new IllegalArgumentException("finite drag"); } }
    private SkillVfxDirectAuthoring() { }
    public static List<AbilityVfx.Command> guide(SkillVfxModel.Primitive primitive, AbilityVfx.Frame frame) {
        return guide(primitive,frame,null);
    }
    public static List<AbilityVfx.Command> guide(SkillVfxModel.Primitive primitive, AbilityVfx.Frame frame, SkillVfxModel.GameplayAction action) { try { if(primitive==null||frame==null)return List.of(); return AbilityVfx.sample(SkillVfxPreviewBuilder.convert(primitive,action),frame,1,AbilityVfx.Quality.HIGH); } catch(RuntimeException ignored) { return List.of(); } }
    public static List<AbilityVfx.Command> guide(SkillVfxModel.Primitive primitive, AbilityVfx.Frame frame, SkillVfxModel.GameplayAction action,double progress) { try { if(primitive==null||frame==null)return List.of(); return AbilityVfx.sample(SkillVfxPreviewBuilder.convert(primitive,action),frame,Math.clamp(progress,0,1),AbilityVfx.Quality.HIGH); } catch(RuntimeException ignored) { return List.of(); } }
    public static List<Handle> worldHandles(SkillVfxModel.Primitive p, AbilityVfx.Frame frame){if(frame==null||p==null)return List.of();ArrayList<Handle> out=new ArrayList<>();for(var h:handles(p)){var local=new Vec(h.position().x()-p.offset().x(),h.position().y()-p.offset().y(),h.position().z()-p.offset().z());var oriented=h.kind().name().startsWith("AXIS_")?local:rotate(local,p.yaw());var q=new Vec(p.offset().x()+oriented.x(),p.offset().y()+oriented.y(),p.offset().z()+oriented.z());var world=frame.world(new AbilityVfx.Vec(q.x(),q.y(),q.z()));if(world!=null)out.add(new Handle(h.id(),h.label(),new Vec(world.x(),world.y(),world.z()),h.kind()));}return List.copyOf(out);}
    public static Projection fromBasis(Vec right,Vec up,Vec forward,Vec origin,double focal){Vec f=normalize(forward),r=normalize(right.add(f.scale(-dot(right,f)))),u=normalize(up.add(f.scale(-dot(up,f))).add(r.scale(-dot(up,r))));return new Projection(r,u,f,origin,focal,1);}
    public static Projection fromFov(Vec forward,Vec worldUp,Vec origin,double verticalFovRadians,int guiHeight,double guiScale){if(guiHeight<=0||verticalFovRadians<=0||verticalFovRadians>=Math.PI||guiScale<=0)throw new IllegalArgumentException("fov");Vec f=normalize(forward),reference=Math.abs(dot(normalize(worldUp),f))>.999?new Vec(0,0,1):worldUp,r=normalize(cross(reference,f)),u=normalize(cross(f,r));return new Projection(r,u,f,origin,(guiHeight/2d)/Math.tan(verticalFovRadians/2),guiScale);}
    /** Transform axes plus only model-backed, literal shape controls. */
    public static List<Handle> handles(SkillVfxModel.Primitive p) {
        if(p==null)return List.of(); Vec o=new Vec(p.offset().x(),p.offset().y(),p.offset().z());ArrayList<Handle> out=new ArrayList<>(List.of(
                new Handle("offsetX","移動 X",o.add(new Vec(1,0,0)),Handle.Kind.AXIS_X),new Handle("offsetY","移動 Y",o.add(new Vec(0,1,0)),Handle.Kind.AXIS_Y),new Handle("offsetZ","移動 Z",o.add(new Vec(0,0,1)),Handle.Kind.AXIS_Z)));
        if(p.type()==SkillVfxModel.PrimitiveType.LINE||p.type()==SkillVfxModel.PrimitiveType.BEZIER)for(int i=0;i<p.controls().size();i++){var c=p.controls().get(i);out.add(new Handle("control:"+i,p.type()==SkillVfxModel.PrimitiveType.LINE?(i==0?"始点":"終点"):(i==0?"始点":i==p.controls().size()-1?"終点":"制御点"+i),o.add(new Vec(c.x(),c.y(),c.z())),Handle.Kind.POINT));}
        literal(p,"radius").ifPresent(v->out.add(new Handle("radius","半径",o.add(new Vec(v,0,0)),Handle.Kind.RADIUS)));
        if(p.type()==SkillVfxModel.PrimitiveType.SPIRAL){literal(p,"height").ifPresent(v->out.add(new Handle("height","高さ",o.add(new Vec(0,v,0)),Handle.Kind.HEIGHT)));literal(p,"turns").ifPresent(v->out.add(new Handle("turns","回転数",o.add(new Vec(0,0,v)),Handle.Kind.TURNS)));}
        return List.copyOf(out);
    }
    public static Optional<Pick> pick(Collection<Handle> handles,Projection projection,double mouseX,double mouseY,double radius){
        Pick best=null;for(Handle h:handles){var point=projection.project(h.position());if(point.isEmpty())continue;double dx=point.get().x-mouseX,dy=point.get().y-mouseY,d=Math.hypot(dx,dy);if(d>radius)continue;Pick candidate=new Pick(h,d);if(best==null||candidate.distance()<best.distance()||(candidate.distance()==best.distance()&&point.get().depth()<projection.project(best.handle().position()).orElseThrow().depth()))best=candidate;}return Optional.ofNullable(best);
    }
    /** Maps GUI mouse motion onto the handle's rendered model axes. */
    public static DragDelta dragDelta(SkillVfxModel.Primitive p,Handle handle,AbilityVfx.Frame frame,Projection projection,double mouseDx,double mouseDy,boolean zMode){
        if(p==null||handle==null||frame==null||projection==null)return new DragDelta(0,0);Vec localX=new Vec(1,0,0),localY=new Vec(0,1,0),localZ=new Vec(0,0,1);boolean shape=!handle.kind().name().startsWith("AXIS_");if(shape){localX=rotate(localX,p.yaw());localY=rotate(localY,p.yaw());localZ=rotate(localZ,p.yaw());}Vec wx=worldDirection(frame,localX),wy=worldDirection(frame,localY),wz=worldDirection(frame,localZ);if(handle.kind()==Handle.Kind.AXIS_X||handle.kind()==Handle.Kind.RADIUS)return new DragDelta(projectedAmount(handle.position(),wx,projection,mouseDx,mouseDy),0);if(handle.kind()==Handle.Kind.AXIS_Y||handle.kind()==Handle.Kind.HEIGHT)return new DragDelta(0,projectedAmount(handle.position(),wy,projection,mouseDx,mouseDy));if(handle.kind()==Handle.Kind.AXIS_Z||handle.kind()==Handle.Kind.TURNS)return new DragDelta(projectedAmount(handle.position(),wz,projection,mouseDx,mouseDy),0);if(handle.kind()!=Handle.Kind.POINT)return new DragDelta(0,0);if(zMode)return new DragDelta(projectedAmount(handle.position(),wz,projection,mouseDx,mouseDy),0);return solvePlane(handle.position(),wx,wy,projection,mouseDx,mouseDy);
    }
    private static DragDelta solvePlane(Vec at,Vec xAxis,Vec yAxis,Projection projection,double dx,double dy){var p=projection.project(at);var px=projection.project(at.add(xAxis));var py=projection.project(at.add(yAxis));if(p.isEmpty()||px.isEmpty()||py.isEmpty())return new DragDelta(0,0);double ax=px.get().x()-p.get().x(),ay=px.get().y()-p.get().y(),bx=py.get().x()-p.get().x(),by=py.get().y()-p.get().y(),det=ax*by-ay*bx;if(Math.abs(det)<1e-8)return new DragDelta(projectedAmount(at,xAxis,projection,dx,dy),projectedAmount(at,yAxis,projection,dx,dy));return new DragDelta((dx*by-dy*bx)/det,(ax*dy-ay*dx)/det);}
    private static double projectedAmount(Vec at,Vec axis,Projection projection,double dx,double dy){var p=projection.project(at);var q=projection.project(at.add(axis));if(p.isEmpty()||q.isEmpty())return 0;double ax=q.get().x()-p.get().x(),ay=q.get().y()-p.get().y(),den=ax*ax+ay*ay;return den<1e-8?0:(dx*ax+dy*ay)/den;}
    private static Vec worldDirection(AbilityVfx.Frame frame,Vec local){var o=frame.world(new AbilityVfx.Vec(0,0,0));var q=frame.world(new AbilityVfx.Vec(local.x(),local.y(),local.z()));return new Vec(q.x()-o.x(),q.y()-o.y(),q.z()-o.z());}
    public static Vec axisDrag(Vec start,Handle.Kind axis,double amount){return switch(axis){case AXIS_X->new Vec(start.x+amount,start.y,start.z);case AXIS_Y->new Vec(start.x,start.y+amount,start.z);case AXIS_Z->new Vec(start.x,start.y,start.z+amount);default->start;};}
    /** Materializes the protocol-supported LINE control representation atomically. */
    public static boolean materializeLine(AbilityVisualEditorDocument document,String id){var p=SkillVfxMutation.current(document,id).orElse(null);if(p==null||p.type()!=SkillVfxModel.PrimitiveType.LINE||!p.controls().isEmpty())return false;double length=p.value("length") instanceof SkillVfxModel.Literal v?v.value():1;document.execute(AbilityVisualCommands.setControlPoints(id,List.of(new SkillVfxModel.Vec(0,0,0),new SkillVfxModel.Vec(0,0,Math.clamp(length,0,128)))));return true;}
    /** Applies the bounded model-backed edit used by the live 3D viewport. */
    public static SkillVfxModel.Primitive edit(SkillVfxModel.Primitive p,Handle h,double x,double y,boolean zMode){
        if(h.kind()==Handle.Kind.AXIS_X||h.kind()==Handle.Kind.AXIS_Y||h.kind()==Handle.Kind.AXIS_Z){var o=p.offset();var n=switch(h.kind()){case AXIS_X->new SkillVfxModel.Vec(clamp(o.x()+x),o.y(),o.z());case AXIS_Y->new SkillVfxModel.Vec(o.x(),clamp(o.y()+y),o.z());case AXIS_Z->new SkillVfxModel.Vec(o.x(),o.y(),clamp(o.z()+x));default->o;};return copy(p,n,p.values(),p.controls());}
        if(h.id().startsWith("control:")){int at=Integer.parseInt(h.id().substring(8));var cs=new ArrayList<>(p.controls());var c=cs.get(at);cs.set(at,zMode?new SkillVfxModel.Vec(c.x(),c.y(),clamp(c.z()+x)):new SkillVfxModel.Vec(clamp(c.x()+x),clamp(c.y()+y),c.z()));return copy(p,p.offset(),p.values(),cs);}
        String key=switch(h.kind()){case RADIUS->"radius";case HEIGHT->"height";case TURNS->"turns";default->"";};if(key.isBlank()||!(p.value(key) instanceof SkillVfxModel.Literal v))return p;double amount=h.kind()==Handle.Kind.HEIGHT?y:x;var values=new TreeMap<>(p.values());values.put(key,new SkillVfxModel.Literal(Math.clamp(v.value()+amount,.001,key.equals("turns")?32:128)));return copy(p,p.offset(),values,p.controls());
    }
    public static String value(SkillVfxModel.Primitive p,Handle h){
        if(h.id().startsWith("control:")){int at=Integer.parseInt(h.id().substring(8));if(at>=0&&at<p.controls().size()){var c=p.controls().get(at);return String.format(Locale.ROOT,"X %.2f / Y %.2f / Z %.2f",c.x(),c.y(),c.z());}}
        var o=p.offset();return switch(h.kind()){case AXIS_X->String.format(Locale.ROOT,"X %.2f",o.x());case AXIS_Y->String.format(Locale.ROOT,"Y %.2f",o.y());case AXIS_Z->String.format(Locale.ROOT,"Z %.2f",o.z());case RADIUS,HEIGHT,TURNS->{String k=switch(h.kind()){case RADIUS->"radius";case HEIGHT->"height";case TURNS->"turns";default->"";};yield p.value(k) instanceof SkillVfxModel.Literal v?String.format(Locale.ROOT,"%.2f",v.value()):"";}default->"";};
    }
    private static Optional<Double> literal(SkillVfxModel.Primitive p,String key){return p.value(key) instanceof SkillVfxModel.Literal v?Optional.of(v.value()):Optional.empty();}
    private static SkillVfxModel.Primitive copy(SkillVfxModel.Primitive p,SkillVfxModel.Vec o,Map<String,SkillVfxModel.Scalar> v,List<SkillVfxModel.Vec> c){return new SkillVfxModel.Primitive(p.id(),p.type(),p.delayTicks(),p.durationTicks(),p.argb(),p.width(),p.density(),p.seed(),o,p.yaw(),v,c,p.appearance());}
    private static double clamp(double n){return Math.clamp(n,-128,128);}
    private static Vec rotate(Vec v,double yaw){double c=Math.cos(yaw),s=Math.sin(yaw);return new Vec(v.x()*c+v.z()*s,v.y(),-v.x()*s+v.z()*c);}
    private static double dot(Vec a,Vec b){return a.x*b.x+a.y*b.y+a.z*b.z;}
    private static Vec cross(Vec a,Vec b){return new Vec(a.y*b.z-a.z*b.y,a.z*b.x-a.x*b.z,a.x*b.y-a.y*b.x);} private static Vec normalize(Vec a){double n=Math.sqrt(dot(a,a));if(n<1e-8)throw new IllegalArgumentException("basis");return a.scale(1/n);}

    /** Live draft updates are cheap; release adds exactly one already-applied command. */
    public static final class DragTransaction {
        private final AbilityVisualEditorDocument document; private final String id; private final SkillVfxModel.Primitive before; private final io.github.gyai.projects.editor.core.EditorSelection selection; private SkillVfxModel.Primitive current; private boolean finished;
        private DragTransaction(AbilityVisualEditorDocument document,String id,SkillVfxModel.Primitive before){this.document=document;this.id=id;this.before=before;this.current=before;this.selection=document.selection();}
        public static Optional<DragTransaction> begin(AbilityVisualEditorDocument document,String id){return SkillVfxMutation.current(document,id).map(p->new DragTransaction(document,id,p));}
        public boolean update(java.util.function.UnaryOperator<SkillVfxModel.Primitive> edit){if(finished)return false;SkillVfxModel.Primitive next=Objects.requireNonNull(edit.apply(current));document.replacePrimitive(id,next);current=next;return true;}
        public boolean release(){if(finished)return false;finished=true;if(current.equals(before))return false;document.recordAlreadyApplied(AbilityVisualCommands.alreadyApplied("3D drag",id,before,current,selection));return true;}
        public boolean cancel(){if(finished)return false;finished=true;document.replacePrimitive(id,before);document.restoreSelection(selection);return true;}
        public SkillVfxModel.Primitive current(){return current;}
    }
}
