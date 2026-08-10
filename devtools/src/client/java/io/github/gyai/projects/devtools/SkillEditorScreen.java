package io.github.gyai.projects.devtools;

import io.github.gyai.projects.client.AbilityVfxLocalPreview;
import io.github.gyai.projects.client.vfx.AbilityVfx;
import io.github.gyai.projects.client.ui.render.ProjectSUiDraw;
import io.github.gyai.projects.client.ui.screen.ProjectSThemedScreen;
import io.github.gyai.projects.client.ui.theme.ProjectSThemeManager;
import io.github.gyai.projects.client.ui.widget.ProjectSButton;
import io.github.gyai.projects.client.ui.widget.ProjectSModal;
import io.github.gyai.projects.client.ui.widget.ProjectSNumberField;
import io.github.gyai.projects.client.ui.widget.ProjectSTextField;
import io.github.gyai.projects.client.ui.widget.ProjectSToast;
import io.github.gyai.projects.devtools.skillvfx.*;
import io.github.gyai.projects.devtools.skillvfx.ui.SkillVfxMinecraftPreviewController;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import java.util.*;

/** Interactive, DevTools-only Skill/VFX authoring surface.  The server stays authoritative. */
public final class SkillEditorScreen extends ProjectSThemedScreen {
    private final Screen parent;
    private final SkillEditorLayout layout=new SkillEditorLayout();
    private final SkillGameplayPanel gameplayPanel=new SkillGameplayPanel();
    private final SkillVisualTreePanel treePanel=new SkillVisualTreePanel();
    private final SkillVfxPreviewController preview=new SkillVfxPreviewController();
    private boolean visual=true, draggingLeft, draggingRight, draggingBottom;
    private SkillVfxModel.Hook hook=SkillVfxModel.Hook.CAST;
    private String emissionId, primitiveId, localError="";
    private long observedRevision=-1; private int headerControlY=8,inspectorPage,treeOffset; private boolean treeActions;

    public SkillEditorScreen(Screen parent){super(Component.literal("Skill Editor"));this.parent=parent;}
    @Override protected void init(){ rebuildWidgets(); if(SkillEditorClientState.document()==null&&!SkillEditorClientState.pending()&&!SkillEditorClientState.selectedAbility().isBlank())SkillEditorClientState.fetch(SkillEditorClientState.selectedAbility()); }
    @Override protected void rebuildWidgets(){
        clearWidgets(); labels.clear(); var doc=SkillEditorClientState.document();
        buildHeader(doc);
        if(!visual){buildGameplay(doc);return;}
        var b=layout.bounds(width,height); buildTree(doc,b.tree()); buildPreview(doc,b.preview()); buildInspector(doc,b.inspector()); buildTimeline(doc,b.timeline());
    }
    private void buildHeader(AbilityVisualEditorDocument doc){
        boolean has=SkillEditorClientState.catalog().stream().anyMatch(x->x.abilityId().equals(SkillEditorClientState.selectedAbility())&&x.hasVisual());
        var cap=SkillEditorUiController.header(doc,SkillEditorClientState.pending(),SkillEditorClientState.connected(),SkillEditorClientState.permitted()&&!SkillEditorClientState.refreshRequired()&&!SkillEditorClientState.sessionApplyDenied(),has);
        int x=8; add(button(x,8,130,abilityLabel(),this::cycleAbility)); x+=134;
        add(button(x,8,70,"Gameplay",()->{visual=false;rebuildWidgets();}).selected(!visual)); x+=74;
        add(button(x,8,58,"Visual",()->{visual=true;rebuildWidgets();}).selected(visual));
        headerControlY=width<800?32:8; int right=width-8; right=place(right,52,"Close",this::onClose,true); right=place(right,66,"Revert",this::confirmRevert,cap.revert()); right=place(right,92,"Apply Session",this::apply,cap.apply()); right=place(right,64,"Refresh",this::confirmRefresh,cap.refresh()); right=place(right,50,"Redo",()->{if(doc!=null)doc.redo();rebuildWidgets();},cap.redo()); place(right,50,"Undo",()->{if(doc!=null)doc.undo();rebuildWidgets();},cap.undo());
        if(visual&&width>=800){int panelX=8;add(button(panelX,32,58,layout.treeVisible()?"Hide tree":"Show tree",()->{layout.visibleTree(!layout.treeVisible());rebuildWidgets();}));panelX+=62;add(button(panelX,32,70,layout.previewVisible()?"Hide preview":"Show preview",()->{layout.visiblePreview(!layout.previewVisible());rebuildWidgets();}));panelX+=74;add(button(panelX,32,76,layout.inspectorVisible()?"Hide fields":"Show fields",()->{layout.visibleInspector(!layout.inspectorVisible());rebuildWidgets();}));panelX+=80;add(button(panelX,32,72,layout.timelineVisible()?"Hide timeline":"Show timeline",()->{layout.visibleTimeline(!layout.timelineVisible());rebuildWidgets();}));panelX+=76;add(button(panelX,32,48,"Reset",()->{layout.reset();rebuildWidgets();}));}
    }
    private int place(int right,int width,String label,Runnable action,boolean active){ right-=width;var b=button(right,headerControlY,width,label,action);b.active=active;add(b);return right-4; }
    private String abilityLabel(){var catalog=SkillEditorClientState.catalog();if(catalog.isEmpty())return SkillEditorClientState.pending()?"Loading abilities…":"No ability";String id=SkillEditorClientState.selectedAbility();return catalog.stream().filter(x->x.abilityId().equals(id)).findFirst().map(SkillVfxEditorProtocol.CatalogItem::displayName).orElse("Select ability");}
    private void cycleAbility(){var catalog=SkillEditorClientState.catalog();if(catalog.isEmpty()||SkillEditorClientState.pending())return;int at=0;for(int i=0;i<catalog.size();i++)if(catalog.get(i).abilityId().equals(SkillEditorClientState.selectedAbility()))at=i;SkillEditorClientState.fetch(catalog.get((at+1)%catalog.size()).abilityId());}
    private void buildGameplay(AbilityVisualEditorDocument doc){
        int y=56; for(int i=0;i<gameplayPanel.actions(doc).size()&&y<height-22;i++){var action=gameplayPanel.actions(doc).get(i); final int selected=i;var b=button(10,y,190,(i+1)+". "+action.type(),()->{gameplayPanel.select(selected,doc);rebuildWidgets();});b.selected(i==gameplayPanel.selected());add(b);if(i==gameplayPanel.selected()){int[] fy={y};drawLater(action.description(),214,fy[0]);fy[0]+=14;action.fields().forEach((key,value)->{drawLater("  "+key+": "+value,214,fy[0]);fy[0]+=14;});}y+=24;}
    }
    /* Foreground labels are retained separately so the gameplay inspector stays read-only and widget-free. */
    private final List<Label> labels=new ArrayList<>(); private record Label(String text,int x,int y) { }
    private void drawLater(String text,int x,int y){labels.add(new Label(text,x,y));}
    private void buildTree(AbilityVisualEditorDocument doc,SkillEditorLayout.Rect r){
        int x=r.x()+6,w=Math.max(70,r.width()-12),small=Math.max(30,(w-8)/2),top=r.y()+4;
        add(button(x,top,small,treeActions?"Tree":"Actions",()->{treeActions=!treeActions;rebuildWidgets();}));
        if(treeActions){buildTreeActions(doc,x,r.y()+28,small);return;}
        var previous=button(x+small+4,top,Math.max(28,(w-small-8)/2),"‹",()->{treeOffset=Math.max(0,treeOffset-2);rebuildWidgets();}); previous.active=treeOffset>0;add(previous);
        var next=button(x+small+8+Math.max(28,(w-small-8)/2),top,Math.max(28,(w-small-8)/2),"›",()->{treeOffset+=2;rebuildWidgets();});add(next);
        int y=r.y()+28; for(int index=0;index<treePanel.hooks().size();index++){var value=treePanel.hooks().get(index);boolean editable=treePanel.editable(value);int column=index%2,row=index/2;var b=button(x+column*(small+4),y+row*22,small,value==SkillVfxModel.Hook.TRAVEL?"TRAVEL":value.name(),()->{if(editable){hook=value;emissionId=null;primitiveId=null;treeOffset=0;preview.hook(value);rebuildWidgets();}});b.active=editable;b.selected(value==hook);add(b);}
        if(doc==null)return; List<TreeNode> nodes=new ArrayList<>();for(var e:doc.visual().emissions(hook)){nodes.add(new TreeNode(e.id(),"• "+e.id(),false));for(var p:e.primitives())nodes.add(new TreeNode(p.id(),p.type()+" "+p.id(),true));}int yNodes=y+66,nodeRows=layout.treeNodeRows(r);for(int i=treeOffset;i<nodes.size()&&i<treeOffset+nodeRows;i++){var node=nodes.get(i);var b=button(x+(node.primitive()?10:0),yNodes+(i-treeOffset)*20,w-(node.primitive()?10:0),node.label(),()->{emissionId=node.primitive()?findEmission(doc,node.id()):node.id();primitiveId=node.primitive()?node.id():null;if(node.primitive())doc.select("primitive",node.id());rebuildWidgets();});b.selected(node.id().equals(node.primitive()?primitiveId:emissionId));add(b);}
    }
    private record TreeNode(String id,String label,boolean primitive) { }
    private String findEmission(AbilityVisualEditorDocument doc,String primitive){for(var e:doc.visual().emissions(hook))if(e.primitives().stream().anyMatch(p->p.id().equals(primitive)))return e.id();return null;}
    private void buildTreeActions(AbilityVisualEditorDocument doc,int x,int y,int column){
        if(layout.treeActionRows(layout.bounds(width,height).tree())<5)return;
        boolean editable=doc!=null&&treePanel.editable(hook);var selected=editable?emission(doc):null;var primitive=editable?primitive(doc):null;
        add(button(x,y,column,"+ Emission",this::addEmission));
        var action=button(x+column+4,y,column,selected==null?"Action":actionBindingLabel(doc,selected),this::cycleEmissionAction);action.active=selected!=null;add(action);
        var add=button(x,y+22,column,"+ Primitive",this::addPrimitive);add.active=selected!=null;add(add);var duplicate=button(x+column+4,y+22,column,"Duplicate",this::duplicatePrimitive);duplicate.active=primitive!=null;add(duplicate);
        var remove=button(x,y+44,column,"Remove primitive",this::removePrimitive);remove.active=primitive!=null;add(remove);var removeEmission=button(x+column+4,y+44,column,"Remove emission",this::removeEmission);removeEmission.active=selected!=null;add(removeEmission);
        var up=button(x,y+66,column,"Move Up",()->movePrimitive(-1));up.active=canMovePrimitive(doc,-1);add(up);var down=button(x+column+4,y+66,column,"Move Down",()->movePrimitive(1));down.active=canMovePrimitive(doc,1);add(down);
        var emissionUp=button(x,y+88,column,"Emission Up",()->moveEmission(-1));emissionUp.active=canMoveEmission(doc,-1);add(emissionUp);var emissionDown=button(x+column+4,y+88,column,"Emission Down",()->moveEmission(1));emissionDown.active=canMoveEmission(doc,1);add(emissionDown);
    }
    private void buildPreview(AbilityVisualEditorDocument doc,SkillEditorLayout.Rect r){
        int x=r.x()+6,y=r.y()+26,w=Math.max(44,r.width()-12),allowed=SkillEditorClientState.previewAllowed()?1:0;var play=button(x,y,Math.min(58,w),preview.timeline().playing()?"Pause":"Play",this::play);play.active=allowed==1;add(play);add(button(x+62,y,Math.min(48,w),"Stop",this::stopPreview));var restart=button(x+114,y,Math.min(58,w),"Restart",this::restart);restart.active=allowed==1;add(restart);y+=24;add(button(x,y,Math.min(66,w),preview.timeline().loop()?"Loop: On":"Loop: Off",()->{preview.loop();rebuildWidgets();}));add(button(x+70,y,Math.min(76,w),"Speed "+preview.timeline().multiplier()+"x",()->{preview.cycleSpeed();rebuildWidgets();}));y+=24;add(button(x,y,Math.min(80,w),"Quality "+preview.quality(),()->{preview.cycleQuality();AbilityVfxLocalPreview.quality(toCoreQuality());rebuildWidgets();}));add(button(x+84,y,Math.min(100,w),preview.anchor()==SkillVfxPreviewController.Anchor.PLAYER?"Anchor Player":"Anchor Forward 3m",()->{preview.cycleAnchor();rebuildWidgets();}));y+=25;drawLater("Hook: "+hook+(allowed==1?"":"  Preview permission denied")+(localError.isBlank()?"":"  "+localError),x,y);
    }
    private void buildInspector(AbilityVisualEditorDocument doc,SkillEditorLayout.Rect r){
        int x=r.x()+6,y=r.y()+28,w=Math.max(72,r.width()-12);
        var p=primitive(doc);
        if(p==null){drawLater("Select a primitive to edit its schema fields.",x,y);return;}
        var fields=AbilityVisualPropertySchemas.descriptors(p.type()); int pageSize=(p.type()==SkillVfxModel.PrimitiveType.LINE||p.type()==SkillVfxModel.PrimitiveType.BEZIER)&&layout.bezierControlPage(r)?1:layout.inspectorPageSize(r), pages=Math.max(1,(fields.size()+pageSize-1)/pageSize); inspectorPage=Math.clamp(inspectorPage,0,pages-1); int first=inspectorPage*pageSize,last=Math.min(fields.size(),first+pageSize);
        drawLater(p.type()+" — schema fields "+(first+1)+"-"+last+" / "+fields.size(),x,y-15);
        var previous=button(x+r.width()-110,r.y()+4,48,"Prev",()->{inspectorPage=Math.max(0,inspectorPage-1);rebuildWidgets();});previous.active=inspectorPage>0;add(previous);
        var nextPage=button(x+r.width()-58,r.y()+4,48,"Next",()->{inspectorPage=Math.min(pages-1,inspectorPage+1);rebuildWidgets();});nextPage.active=inspectorPage+1<pages;add(nextPage);
        for(var descriptor:fields.subList(first,last)){
            String field=descriptor.id(), fieldLabel=descriptor.displayName();
            Object value=doc.selectedValue(field);
            if(field.equals("argb")){
                var box=new ProjectSTextField(font,x,y,w,18,Component.literal("Color ARGB"),Component.literal("#AARRGGBB"));
                box.setValue(SkillEditorUiController.argb(p.argb()));
                box.setResponder(raw->{var parsed=SkillEditorUiController.argb(raw);box.error(Component.literal(parsed.error()));if(parsed.valid())setPrimitive(p.id(),withArgb(p,parsed.value()));});
                add(box);y+=30;continue;
            }
            if(field.equals("controlPoints")){
                var controls=p.controls();
                if(p.type()==SkillVfxModel.PrimitiveType.LINE&&controls.isEmpty()){
                    add(button(x,y,Math.min(120,w),"Set LINE A/B",()->setControlPoints(doc,p,List.of(new SkillVfxModel.Vec(0,0,0),new SkillVfxModel.Vec(0,0,1)))));y+=30;continue;
                }
                if(p.type()==SkillVfxModel.PrimitiveType.BEZIER){
                    if(controls.size()==3)add(button(x,y,Math.min(120,w),"Add fourth point",()->{var nextControls=new ArrayList<>(p.controls());nextControls.add(p.controls().getLast());setControlPoints(doc,p,nextControls);}));
                    if(controls.size()==4)add(button(x,y,Math.min(120,w),"Remove fourth",()->setControlPoints(doc,p,p.controls().subList(0,3))));
                    y+=22;
                }
                for(int point=0;point<controls.size()&&y<r.y()+r.height()-25;point++){
                    for(int axis=0;axis<3;axis++){
                        final int pi=point,ai=axis;
                        double n=axis==0?controls.get(point).x():axis==1?controls.get(point).y():controls.get(point).z();
                        add(number(x+axis*(w/3),y,Math.max(25,w/3-2),"P"+(point+1)+" "+"XYZ".charAt(axis),n,v->setControlCoordinate(doc,p,pi,ai,v)));
                    } y+=30;
                } continue;
            }
            if(field.equals("count")){
                double count=value instanceof SkillVfxModel.Literal literal?Math.clamp(Math.rint(literal.value()),1,64):8;
                add(new ProjectSNumberField(font,x,y,w,18,Component.literal(fieldLabel),count,1,64,1,0,"",v->writeDescriptor(descriptor,doc,new SkillVfxModel.Literal((int)v))));
                drawLater("Burst count is an integral literal (1–64).",x,y+19);y+=30;continue;
            }
            if(isScalar(field)){
                SkillVfxModel.Scalar scalar=value instanceof SkillVfxModel.Scalar s?s:new SkillVfxModel.Literal(0);
                var mode=SkillEditorUiController.mode(scalar);
                var mb=button(x,y,Math.min(95,w),mode==SkillEditorUiController.ScalarMode.LITERAL?"Literal":"From Gameplay",()->{
                    SkillVfxModel.ActionField source=scalar instanceof SkillVfxModel.FromGameplay f?f.field():SkillVfxModel.ActionField.RADIUS;
                    setPrimitive(p.id(),p.withValue(field,SkillEditorUiController.scalar(mode==SkillEditorUiController.ScalarMode.LITERAL?SkillEditorUiController.ScalarMode.FROM_GAMEPLAY:SkillEditorUiController.ScalarMode.LITERAL,scalar instanceof SkillVfxModel.Literal l?l.value():0,source)));
                }); add(mb);
                if(mode==SkillEditorUiController.ScalarMode.FROM_GAMEPLAY){
                    var source=(SkillVfxModel.FromGameplay)scalar;
                    add(button(x+98,y,Math.max(40,w-98),source.field().name(),()->{
                        var all=SkillVfxModel.ActionField.values();
                        setPrimitive(p.id(),p.withValue(field,new SkillVfxModel.FromGameplay(all[(source.field().ordinal()+1)%all.length])));
                    }));
                } else {
                    add(number(x+98,y,Math.max(40,w-98),fieldLabel,((SkillVfxModel.Literal)scalar).value(),
                            v -> setPrimitive(p.id(), p.withValue(field, new SkillVfxModel.Literal(v)))));
                }
                y+=30;continue;
            }
            double number=numberValue(p,field,value);
            add(number(x,y,w,fieldLabel,number,v->{writeDescriptor(descriptor,doc,v);localError="";}));drawLater(AbilityVisualPropertySchemas.description(field),x,y+19);y+=30;
        }
        y+=2;
    }
    private void buildTimeline(AbilityVisualEditorDocument doc,SkillEditorLayout.Rect r){
        int x=r.x()+6,y=r.y()+25,w=Math.max(20,r.width()-12);if(doc==null)return;var bars=preview.timeline().bars(doc.visual(),hook);int duration=Math.max(1,bars.stream().mapToInt(SkillVfxTimeline.Bar::end).max().orElse(20));for(int lane=0;lane<bars.size()&&lane<3;lane++){var bar=bars.get(lane);int bx=x+(int)(bar.start()/(double)duration*w),bw=Math.max(2,(int)((bar.end()-bar.start())/(double)duration*w));drawLater(bar.id(),x,y+lane*14);/* bar is drawn in foreground using theme tokens */labels.add(new Label("["+"=".repeat(Math.min(24,Math.max(1,bw/5)))+"]",bx,y+lane*14));}drawLater("Playhead "+String.format(Locale.ROOT,"%.1f",preview.timeline().ticks())+" / "+duration+" ticks",x,y+48);
    }
    private ProjectSButton button(int x,int y,int w,String label,Runnable action){return new ProjectSButton(x,y,Math.max(1,w),20,Component.literal(label),ProjectSButton.Kind.SECONDARY,action);}
    @SuppressWarnings({"rawtypes","unchecked"}) private static void writeDescriptor(io.github.gyai.projects.editor.core.PropertyDescriptor descriptor,AbilityVisualEditorDocument document,Object value){descriptor.write(document,value);}
    private ProjectSNumberField number(int x,int y,int w,String label,double value,java.util.function.DoubleConsumer action){return new ProjectSNumberField(font,x,y,Math.max(25,w),18,Component.literal(label),value,-30_000_000,30_000_000,.1,3,"",action);}
    private void add(ProjectSButton button){addRenderableWidget(button);} private void add(ProjectSTextField field){addRenderableWidget(field);} private void add(ProjectSNumberField field){addRenderableWidget(field);}
    private AbilityVisualEditorDocument document(){return SkillEditorClientState.document();} private SkillVfxModel.Emission emission(AbilityVisualEditorDocument doc){return doc==null||emissionId==null?null:doc.visual().emissions(hook).stream().filter(e->e.id().equals(emissionId)).findFirst().orElse(null);} private SkillVfxModel.Primitive primitive(AbilityVisualEditorDocument doc){if(doc==null||primitiveId==null)return null;for(var e:doc.visual().emissions(hook))if(e.id().equals(emissionId))for(var p:e.primitives())if(p.id().equals(primitiveId))return p;return null;}
    private static boolean isScalar(String field){return !Set.of("delayTicks","durationTicks","argb","opacity","width","density","seed","offsetX","offsetY","offsetZ","yaw","controlPoints","count").contains(field);}
    private static double numberValue(SkillVfxModel.Primitive p,String field,Object ignored){return switch(field){case "delayTicks"->p.delayTicks();case "durationTicks"->p.durationTicks();case "opacity"->(p.argb()>>>24)&255;case "width"->p.width();case "density"->p.density();case "seed"->p.seed();case "offsetX"->p.offset().x();case "offsetY"->p.offset().y();case "offsetZ"->p.offset().z();case "yaw"->p.yaw();default->0;};}
    private static SkillVfxModel.Primitive withArgb(SkillVfxModel.Primitive p,int argb){return new SkillVfxModel.Primitive(p.id(),p.type(),p.delayTicks(),p.durationTicks(),argb,p.width(),p.density(),p.seed(),p.offset(),p.yaw(),p.values(),p.controls());}
    private static SkillVfxModel.Primitive withNumber(SkillVfxModel.Primitive p,String f,double v){int delay=f.equals("delayTicks")?Math.max(0,(int)v):p.delayTicks(),duration=f.equals("durationTicks")?Math.clamp((int)v,1,1200):p.durationTicks(),density=f.equals("density")?Math.clamp((int)v,1,256):p.density();int argb=f.equals("opacity")?((Math.clamp((int)v,0,255)<<24)|(p.argb()&0x00FFFFFF)):p.argb();double width=f.equals("width")?Math.max(.001,v):p.width();long seed=f.equals("seed")?(long)v:p.seed();SkillVfxModel.Vec offset=new SkillVfxModel.Vec(f.equals("offsetX")?v:p.offset().x(),f.equals("offsetY")?v:p.offset().y(),f.equals("offsetZ")?v:p.offset().z());return new SkillVfxModel.Primitive(p.id(),p.type(),delay,duration,argb,width,density,seed,offset,f.equals("yaw")?v:p.yaw(),p.values(),p.controls());}
    private void setControlCoordinate(AbilityVisualEditorDocument doc,SkillVfxModel.Primitive p,int point,int axis,double value){var controls=new ArrayList<>(p.controls());var old=controls.get(point);controls.set(point,new SkillVfxModel.Vec(axis==0?value:old.x(),axis==1?value:old.y(),axis==2?value:old.z()));setControlPoints(doc,p,controls);}
    @SuppressWarnings({"rawtypes","unchecked"}) private static void setControlPoints(AbilityVisualEditorDocument document,SkillVfxModel.Primitive primitive,List<SkillVfxModel.Vec> controls){io.github.gyai.projects.editor.core.PropertyDescriptor descriptor=AbilityVisualPropertySchemas.schema(primitive.type()).property("controlPoints");descriptor.write(document,controls);}
    private void setPrimitive(String id,SkillVfxModel.Primitive replacement){var d=document();if(d!=null){d.execute(AbilityVisualCommands.replacePrimitive(id,replacement));localError="";}}
    private String actionBindingLabel(AbilityVisualEditorDocument doc,SkillVfxModel.Emission emission){if(emission.actionIndex()<0)return "Action: Unbound";var actions=doc.baseline().gameplay();return emission.actionIndex()<actions.size()?"Action "+emission.actionIndex()+": "+actions.get(emission.actionIndex()).type():"Action: invalid";} private void cycleEmissionAction(){var d=document();var selected=emission(d);if(d==null||selected==null)return;int limit=d.baseline().gameplay().size(),next=selected.actionIndex()+1; if(next>=limit)next=-1;d.execute(AbilityVisualCommands.setEmissionActionIndex(hook,selected.id(),next));rebuildWidgets();}
    private void addEmission(){var d=document();if(d==null)return;Set<String> used=new HashSet<>();for(var e:d.visual().emissions(hook))used.add(e.id());var id=SkillVfxModel.nextId(used,"emission");d.execute(AbilityVisualCommands.addEmission(hook,new SkillVfxModel.Emission(id,-1,List.of())));emissionId=id;rebuildWidgets();}
    private void addPrimitive(){var d=document();if(d==null||emissionId==null)return;Set<String> used=new HashSet<>();for(var h:d.visual().hooks())for(var e:h.emissions())for(var p:e.primitives())used.add(p.id());SkillVfxModel.PrimitiveType type=primitive(d)==null?SkillVfxModel.PrimitiveType.POINT:SkillVfxModel.PrimitiveType.values()[(primitive(d).type().ordinal()+1)%SkillVfxModel.PrimitiveType.values().length];var p=SkillVfxModel.defaults(SkillVfxModel.nextId(used,type.name()),type);d.execute(AbilityVisualCommands.add(hook,emissionId,p));primitiveId=p.id();d.select("primitive",primitiveId);rebuildWidgets();}
    private void duplicatePrimitive(){var d=document();if(d!=null&&primitiveId!=null&&emissionId!=null){d.execute(AbilityVisualCommands.duplicate(hook,emissionId,primitiveId));rebuildWidgets();}} private void duplicateEmission(){var d=document();if(d!=null&&emissionId!=null){d.execute(AbilityVisualCommands.duplicateEmission(hook,emissionId));rebuildWidgets();}} private void removeEmission(){var d=document();if(d!=null&&emissionId!=null){d.execute(AbilityVisualCommands.removeEmission(hook,emissionId));emissionId=null;primitiveId=null;d.select("emission",null);rebuildWidgets();}}
    private void removePrimitive(){var d=document();if(d!=null&&primitiveId!=null){d.execute(AbilityVisualCommands.remove(primitiveId));primitiveId=null;d.select("primitive",null);rebuildWidgets();}}
    private void movePrimitive(int delta){var d=document();if(d==null||primitiveId==null||emissionId==null)return;var e=d.visual().emissions(hook).stream().filter(x->x.id().equals(emissionId)).findFirst().orElse(null);if(e==null)return;int at=0;for(int i=0;i<e.primitives().size();i++)if(e.primitives().get(i).id().equals(primitiveId))at=i;d.execute(AbilityVisualCommands.movePrimitive(hook,emissionId,primitiveId,at+delta));rebuildWidgets();}
    private void moveEmission(int delta){var d=document();if(d==null||emissionId==null)return;var es=d.visual().emissions(hook);int at=0;for(int i=0;i<es.size();i++)if(es.get(i).id().equals(emissionId))at=i;d.execute(AbilityVisualCommands.moveEmission(hook,emissionId,at+delta));rebuildWidgets();}
    private boolean canMovePrimitive(AbilityVisualEditorDocument d,int delta){var e=emission(d);if(e==null||primitiveId==null)return false;for(int i=0;i<e.primitives().size();i++)if(e.primitives().get(i).id().equals(primitiveId))return i+delta>=0&&i+delta<e.primitives().size();return false;}
    private boolean canMoveEmission(AbilityVisualEditorDocument d,int delta){if(d==null||emissionId==null)return false;var emissions=d.visual().emissions(hook);for(int i=0;i<emissions.size();i++)if(emissions.get(i).id().equals(emissionId))return i+delta>=0&&i+delta<emissions.size();return false;}
    private AbilityVfx.Quality toCoreQuality(){return AbilityVfx.Quality.valueOf(preview.quality().name());}
    private void play(){if(!SkillEditorClientState.previewAllowed()){localError="The server did not permit local preview.";rebuildWidgets();return;}if(preview.timeline().playing()){preview.pause();AbilityVfxLocalPreview.pause();rebuildWidgets();return;}if(AbilityVfxLocalPreview.preview().isPresent()){preview.play();AbilityVfxLocalPreview.play();rebuildWidgets();return;}var result=SkillVfxMinecraftPreviewController.play(document(),preview);localError=result.message();if(result.valid()){preview.play();AbilityVfxLocalPreview.quality(toCoreQuality());}rebuildWidgets();} private void stopPreview(){preview.stop();SkillVfxMinecraftPreviewController.stop();rebuildWidgets();}private void restart(){if(!SkillEditorClientState.previewAllowed()){localError="The server did not permit local preview.";rebuildWidgets();return;}stopPreview();play();preview.restart();AbilityVfxLocalPreview.restart();}
    private void apply(){if(SkillEditorClientState.apply())toasts.show(ProjectSToast.Kind.INFO,Component.literal("Skill Editor"),Component.literal("Applying temporary dev override…"),2500);else localError="Apply is unavailable until the visual is valid and the server is ready.";}
    private void confirmRefresh(){var d=document();if(d!=null&&d.dirty())modal.open(Component.literal("Refresh server state?"),Component.literal("Refreshing discards this local visual draft."),Component.literal("Refresh"),SkillEditorClientState::refresh,Component.literal("Cancel"),()->{},ProjectSModal.PrimaryKind.DANGER,true);else SkillEditorClientState.refresh();}
    private void confirmRevert(){modal.open(Component.literal("Revert session override?"),Component.literal("This asks the server to replace the draft with its baseline visual."),Component.literal("Revert"),SkillEditorClientState::revert,Component.literal("Cancel"),()->{},ProjectSModal.PrimaryKind.DANGER,true);}
    @Override public void tick(){super.tick();var d=document();if(preview.timeline().playing()&&d!=null){int duration=Math.max(1,preview.timeline().bars(d.visual(),hook).stream().mapToInt(SkillVfxTimeline.Bar::end).max().orElse(20));preview.timeline().advance(1,duration);AbilityVfxLocalPreview.seek(preview.timeline().ticks());if(preview.timeline().playing())AbilityVfxLocalPreview.play();}if(observedRevision!=SkillEditorClientState.updateRevision()){observedRevision=SkillEditorClientState.updateRevision();var f=SkillEditorClientState.feedback();if(f!=SkillEditorController.Feedback.NONE){ProjectSToast.Kind kind=switch(f){case APPLIED,REVERTED->ProjectSToast.Kind.SUCCESS;case STALE,CONFLICT,REFRESH_REQUIRED->ProjectSToast.Kind.WARNING;default->ProjectSToast.Kind.ERROR;};toasts.show(kind,Component.literal("Skill Editor"),Component.literal(SkillEditorUiController.feedback(f,SkillEditorClientState.feedbackMessage())),4500);}rebuildWidgets();}}
    @Override public void extractRenderState(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float tickProgress){var t=ProjectSThemeManager.get().activeTheme().tokens();graphics.fill(0,0,width,height,t.background());var d=document();if(width>=800)graphics.text(font,(d!=null&&d.dirty()?"● Unsaved visual changes":"● Server baseline")+"  Apply Session is a dev override cleared by server restart.",8,34,t.textMuted(),false);if(!visual)graphics.text(font,"Gameplay editing comes in later version",8,width>=800?48:56,t.textPrimary(),false);else{var b=layout.bounds(width,height);panel(graphics,b.tree(),"Visual tree");panel(graphics,b.preview(),"Preview");panel(graphics,b.inspector(),"Inspector");panel(graphics,b.timeline(),"VFX timeline");}super.extractRenderState(graphics,mouseX,mouseY,tickProgress);for(var label:labels)graphics.text(font,label.text(),label.x(),label.y(),t.textSecondary(),false);}
    private void panel(GuiGraphicsExtractor graphics,SkillEditorLayout.Rect r,String title){if(r.width()<=0||r.height()<=0)return;var t=ProjectSThemeManager.get().activeTheme().tokens();ProjectSUiDraw.cutPanel(graphics,r.x(),r.y(),r.width(),r.height(),5,t.surface(),t.borderCard());graphics.text(font,title,r.x()+6,r.y()+8,t.textPrimary(),false);}
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick){if(super.mouseClicked(event,doubleClick))return true;var b=layout.bounds(width,height);if(visual&&event.button()==0){if(Math.abs(event.x()-(b.tree().x()+b.tree().width()))<5&&b.tree().width()>0){draggingLeft=true;return true;}if(Math.abs(event.x()-b.inspector().x())<5&&b.inspector().width()>0){draggingRight=true;return true;}if(Math.abs(event.y()-b.timeline().y())<5&&b.timeline().height()>0){draggingBottom=true;return true;}if(b.timeline().x()<=event.x()&&event.x()<b.timeline().x()+b.timeline().width()&&b.timeline().y()<=event.y()&&event.y()<b.timeline().y()+b.timeline().height()){var d=document();int duration=d==null?20:Math.max(1,preview.timeline().bars(d.visual(),hook).stream().mapToInt(SkillVfxTimeline.Bar::end).max().orElse(20));double seek=(event.x()-b.timeline().x())/(double)Math.max(1,b.timeline().width())*duration;preview.timeline().seek(seek);AbilityVfxLocalPreview.seek(seek);return true;}}return false;}
    @Override public boolean mouseDragged(MouseButtonEvent event,double deltaX,double deltaY){if(super.mouseDragged(event,deltaX,deltaY))return true;if(draggingLeft)layout.dragLeft((int)event.x(),width);else if(draggingRight)layout.dragRight((int)event.x(),width);else if(draggingBottom)layout.dragBottom((int)event.y(),height);else return false;rebuildWidgets();return true;}
    @Override public boolean mouseReleased(MouseButtonEvent event){if(draggingLeft||draggingRight||draggingBottom){draggingLeft=draggingRight=draggingBottom=false;return true;}return super.mouseReleased(event);}
    @Override public void onClose(){if(document()!=null&&document().dirty()){modal.open(Component.literal("Discard local visual changes?"),Component.literal("These unsent DevTools edits will be lost."),Component.literal("Discard"),this::closeEditor,Component.literal("Cancel"),()->{},ProjectSModal.PrimaryKind.DANGER,true);return;}closeEditor();}
    private void closeEditor(){preview.stop();AbilityVfxLocalPreview.closeEditor();minecraft.setScreen(parent);}
}
