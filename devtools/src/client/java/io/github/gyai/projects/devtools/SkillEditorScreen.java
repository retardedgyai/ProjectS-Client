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
import net.minecraft.client.gui.components.Tooltip;
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
    private boolean draggingLeft, draggingRight, draggingBottom, displaySettings;
    private SkillVfxModel.Hook hook=SkillVfxModel.Hook.CAST;
    private String emissionId, primitiveId, localError="";
    private AbilityVisualEditorDocument selectionDocument;
    private ProjectSButton undoButton,redoButton,applyButton,revertButton;
    private long observedRevision=-1; private int headerControlY=8,inspectorPage,treeOffset; private boolean treeActions;

    public SkillEditorScreen(Screen parent){super(Component.literal("スキル VFX エディター"));this.parent=parent;}
    @Override protected void init(){ rebuildWidgets(); if(SkillEditorClientState.document()==null&&!SkillEditorClientState.pending()&&!SkillEditorClientState.selectedAbility().isBlank())SkillEditorClientState.fetch(SkillEditorClientState.selectedAbility()); }
    @Override protected void rebuildWidgets(){
        clearWidgets(); labels.clear(); var doc=SkillEditorClientState.document();
        if(doc!=selectionDocument){resolveSelection(doc);selectionDocument=doc;}else reconcileSelection(doc);
        buildHeader(doc);
        if(displaySettings){buildDisplaySettings();return;}
        var b=layout.bounds(width,height); buildTree(doc,b.tree()); buildPreview(doc,b.preview()); buildInspector(doc,b.inspector()); buildTimeline(doc,b.timeline());
    }
    private void resolveSelection(AbilityVisualEditorDocument doc){
        var selected=SkillVfxSelection.resolve(doc,null);
        hook=selected.hook(); emissionId=selected.emissionId(); primitiveId=selected.primitiveId(); preview.hook(hook);
        if(doc!=null&&selected.selected()&&!Objects.equals(doc.selection().primaryId(),primitiveId))doc.select("primitive",primitiveId);
    }
    private void reconcileSelection(AbilityVisualEditorDocument doc){
        if(doc==null)return;var selected=SkillVfxSelection.resolve(doc,new SkillVfxSelection.Value(hook,emissionId,primitiveId));
        hook=selected.hook();emissionId=selected.emissionId();primitiveId=selected.primitiveId();preview.hook(hook);
        if(selected.selected()&&!Objects.equals(doc.selection().primaryId(),primitiveId))doc.select("primitive",primitiveId);
        if(!selected.selected()&&doc.selection().primaryId()!=null)doc.select("tree",null);
    }
    private void buildHeader(AbilityVisualEditorDocument doc){
        boolean has=SkillEditorClientState.catalog().stream().anyMatch(x->x.abilityId().equals(SkillEditorClientState.selectedAbility())&&x.hasVisual());
        var cap=SkillEditorUiController.header(doc,SkillEditorClientState.pending(),SkillEditorClientState.connected(),SkillEditorClientState.permitted()&&!SkillEditorClientState.refreshRequired()&&!SkillEditorClientState.sessionApplyDenied(),has);
        int x=8; add(button(x,8,150,abilityLabel(),this::cycleAbility)); x+=154;
        add(button(x,8,72,"表示設定",()->{displaySettings=!displaySettings;rebuildWidgets();}).selected(displaySettings));
        headerControlY=width<760?32:8; int right=width-8; right=place(right,52,"閉じる",this::onClose,true,null); right=place(right,66,"変更を破棄",this::confirmRevert,cap.revert(),"サーバー上の一時反映を破棄し、基準の VFX に戻します。"); right=place(right,84,"変更を反映",this::apply,cap.apply(),"現在の下書きをこの開発セッションに一時反映します。サーバー再起動で解除されます。"); right=place(right,72,"再読み込み",this::confirmRefresh,cap.refresh(),"サーバーの最新状態を読み込みます。未反映の変更は失われます。"); right=place(right,58,"やり直す",()->{if(doc!=null)doc.redo();rebuildWidgets();},cap.redo(),null); place(right,58,"元に戻す",()->{if(doc!=null)doc.undo();rebuildWidgets();},cap.undo(),null);
    }
    private void buildDisplaySettings(){int x=8,y=56,w=Math.max(110,Math.min(180,(width-28)/2));add(button(x,y,w,layout.treeVisible()?"ツリーを隠す":"ツリーを表示",()->{layout.visibleTree(!layout.treeVisible());rebuildWidgets();}));add(button(x+w+4,y,w,layout.previewVisible()?"プレビューを隠す":"プレビューを表示",()->{layout.visiblePreview(!layout.previewVisible());rebuildWidgets();}));y+=24;add(button(x,y,w,layout.inspectorVisible()?"詳細を隠す":"詳細を表示",()->{layout.visibleInspector(!layout.inspectorVisible());rebuildWidgets();}));add(button(x+w+4,y,w,layout.timelineVisible()?"タイムラインを隠す":"タイムラインを表示",()->{layout.visibleTimeline(!layout.timelineVisible());rebuildWidgets();}));y+=24;add(button(x,y,Math.min(w,120),"リセット",()->{layout.reset();rebuildWidgets();}));drawLater("表示するパネルを切り替えます。もう一度「表示設定」で編集へ戻ります。",x,y+26);}
    private int place(int right,int width,String label,Runnable action,boolean active,String tip){ right-=width;var b=button(right,headerControlY,width,label,action);b.active=active;if(label.equals("元に戻す"))undoButton=b;if(label.equals("やり直す"))redoButton=b;if(label.equals("変更を反映"))applyButton=b;if(label.equals("変更を破棄"))revertButton=b;if(tip!=null)b.setTooltip(Tooltip.create(Component.literal(tip)));add(b);return right-4; }
    private void afterMutation(boolean rebuild){var doc=document();reconcileSelection(doc);if(doc!=null){boolean has=SkillEditorClientState.catalog().stream().anyMatch(x->x.abilityId().equals(SkillEditorClientState.selectedAbility())&&x.hasVisual());var cap=SkillEditorUiController.header(doc,SkillEditorClientState.pending(),SkillEditorClientState.connected(),SkillEditorClientState.permitted()&&!SkillEditorClientState.refreshRequired()&&!SkillEditorClientState.sessionApplyDenied(),has);if(undoButton!=null)undoButton.active=cap.undo();if(redoButton!=null)redoButton.active=cap.redo();if(applyButton!=null)applyButton.active=cap.apply();if(revertButton!=null)revertButton.active=cap.revert();}if(rebuild)rebuildWidgets();}
    private String abilityLabel(){var catalog=SkillEditorClientState.catalog();if(catalog.isEmpty())return SkillEditorClientState.pending()?"スキルを読み込み中…":"スキルなし";String id=SkillEditorClientState.selectedAbility();return catalog.stream().filter(x->x.abilityId().equals(id)).findFirst().map(SkillVfxEditorProtocol.CatalogItem::displayName).orElse("スキルを選択");}
    private void cycleAbility(){var catalog=SkillEditorClientState.catalog();if(catalog.isEmpty()||SkillEditorClientState.pending())return;int at=0;for(int i=0;i<catalog.size();i++)if(catalog.get(i).abilityId().equals(SkillEditorClientState.selectedAbility()))at=i;SkillEditorClientState.fetch(catalog.get((at+1)%catalog.size()).abilityId());}
    private void buildGameplay(AbilityVisualEditorDocument doc){
        int y=56; for(int i=0;i<gameplayPanel.actions(doc).size()&&y<height-22;i++){var action=gameplayPanel.actions(doc).get(i); final int selected=i;var b=button(10,y,190,(i+1)+". "+action.type(),()->{gameplayPanel.select(selected,doc);rebuildWidgets();});b.selected(i==gameplayPanel.selected());add(b);if(i==gameplayPanel.selected()){int[] fy={y};drawLater(action.description(),214,fy[0]);fy[0]+=14;action.fields().forEach((key,value)->{drawLater("  "+key+": "+value,214,fy[0]);fy[0]+=14;});}y+=24;}
    }
    /* Foreground labels are retained separately so the gameplay inspector stays read-only and widget-free. */
    private final List<Label> labels=new ArrayList<>(); private record Label(String text,int x,int y) { }
    private void drawLater(String text,int x,int y){labels.add(new Label(text,x,y));}
    private void buildTree(AbilityVisualEditorDocument doc,SkillEditorLayout.Rect r){
        if(r.width()<=0||r.height()<=0)return;
        int x=r.x()+6,w=Math.max(70,r.width()-12),small=Math.max(30,(w-8)/2),top=r.y()+4;
        add(button(x,top,small,treeActions?"ツリー":"編集操作",()->{treeActions=!treeActions;rebuildWidgets();}));
        if(treeActions){buildTreeActions(doc,x,r.y()+28,small);return;}
        var previous=button(x+small+4,top,Math.max(28,(w-small-8)/2),"‹",()->{treeOffset=Math.max(0,treeOffset-2);rebuildWidgets();}); previous.active=treeOffset>0;add(previous);
        var next=button(x+small+8+Math.max(28,(w-small-8)/2),top,Math.max(28,(w-small-8)/2),"›",()->{treeOffset+=2;rebuildWidgets();});add(next);
        int y=r.y()+28; for(int index=0;index<treePanel.hooks().size();index++){var value=treePanel.hooks().get(index);boolean editable=treePanel.editable(value);int column=index%2,row=index/2;var b=button(x+column*(small+4),y+row*22,small,SkillVfxDisplay.hook(value),()->{if(editable){hook=value;emissionId=null;primitiveId=null;treeOffset=0;preview.hook(value);if(doc!=null)doc.select("hook",null);rebuildWidgets();}});b.active=editable;b.selected(value==hook);add(b);}
        if(doc==null)return; List<TreeNode> nodes=new ArrayList<>();for(var e:doc.visual().emissions(hook)){nodes.add(new TreeNode(e.id(),"└ 発生: "+e.id(),false));for(var p:e.primitives())nodes.add(new TreeNode(p.id(),"   └ "+SkillVfxDisplay.primitive(p.type())+"  ("+p.id()+")",true));}int yNodes=y+66,nodeRows=layout.treeNodeRows(r);for(int i=treeOffset;i<nodes.size()&&i<treeOffset+nodeRows;i++){var node=nodes.get(i);var b=button(x+(node.primitive()?10:0),yNodes+(i-treeOffset)*20,w-(node.primitive()?10:0),node.label(),()->{emissionId=node.primitive()?findEmission(doc,node.id()):node.id();primitiveId=node.primitive()?node.id():null;doc.select(node.primitive()?"primitive":"emission",node.primitive()?node.id():null);rebuildWidgets();});b.selected(node.id().equals(node.primitive()?primitiveId:emissionId));add(b);}
    }
    private record TreeNode(String id,String label,boolean primitive) { }
    private String findEmission(AbilityVisualEditorDocument doc,String primitive){for(var e:doc.visual().emissions(hook))if(e.primitives().stream().anyMatch(p->p.id().equals(primitive)))return e.id();return null;}
    private void buildTreeActions(AbilityVisualEditorDocument doc,int x,int y,int column){
        if(layout.treeActionRows(layout.bounds(width,height).tree())<5)return;
        boolean editable=doc!=null&&treePanel.editable(hook);var selected=editable?emission(doc):null;var primitive=editable?primitive(doc):null;
        add(button(x,y,column,"+ 発生",this::addEmission));
        var action=button(x+column+4,y,column,selected==null?"アクション":actionBindingLabel(doc,selected),this::cycleEmissionAction);action.active=selected!=null;add(action);
        var add=button(x,y+22,column,"+ パーツ",this::addPrimitive);add.active=selected!=null;add(add);var duplicate=button(x+column+4,y+22,column,"複製",this::duplicatePrimitive);duplicate.active=primitive!=null;add(duplicate);
        var remove=button(x,y+44,column,"パーツを削除",this::removePrimitive);remove.active=primitive!=null;add(remove);var removeEmission=button(x+column+4,y+44,column,"発生を削除",this::removeEmission);removeEmission.active=selected!=null;add(removeEmission);
        var up=button(x,y+66,column,"上へ移動",()->movePrimitive(-1));up.active=canMovePrimitive(doc,-1);add(up);var down=button(x+column+4,y+66,column,"下へ移動",()->movePrimitive(1));down.active=canMovePrimitive(doc,1);add(down);
        var emissionUp=button(x,y+88,column,"発生を上へ",()->moveEmission(-1));emissionUp.active=canMoveEmission(doc,-1);add(emissionUp);var emissionDown=button(x+column+4,y+88,column,"発生を下へ",()->moveEmission(1));emissionDown.active=canMoveEmission(doc,1);add(emissionDown);
    }
    private void buildPreview(AbilityVisualEditorDocument doc,SkillEditorLayout.Rect r){
        if(r.width()<=0||r.height()<=0)return;
        int x=r.x()+6,y=r.y()+26,w=Math.max(44,r.width()-12),allowed=SkillEditorClientState.previewAllowed()?1:0;var play=button(x,y,Math.min(72,w),preview.timeline().playing()?"一時停止":"再生",this::play);play.active=allowed==1;add(play);add(button(x+76,y,Math.min(48,w),"停止",this::stopPreview));var restart=button(x+128,y,Math.min(68,w),"最初から",this::restart);restart.active=allowed==1;add(restart);y+=24;add(button(x,y,Math.min(72,w),preview.timeline().loop()?"ループ: オン":"ループ: オフ",()->{preview.loop();rebuildWidgets();}));add(button(x+76,y,Math.min(82,w),"再生速度 "+preview.timeline().multiplier()+"x",()->{preview.cycleSpeed();rebuildWidgets();}));y+=24;add(button(x,y,Math.min(76,w),"表示品質 "+SkillVfxDisplay.quality(preview.quality()),()->{preview.cycleQuality();AbilityVfxLocalPreview.quality(toCoreQuality());rebuildWidgets();}));var anchor=button(x+80,y,Math.min(100,w),"基準 "+SkillVfxDisplay.anchor(preview.anchor()),()->{preview.cycleAnchor();rebuildWidgets();});anchor.setTooltip(Tooltip.create(Component.literal("プレビューの基準位置を、プレイヤー位置または前方3mに切り替えます。")));add(anchor);y+=25;var p=primitive(doc);drawLater("現在: "+SkillVfxDisplay.hook(hook)+(p==null?"":" > "+SkillVfxDisplay.primitive(p.type()))+(allowed==1?"":"  プレビュー権限がありません")+(localError.isBlank()?"":"  "+localError),x,y);
    }
    private void buildInspector(AbilityVisualEditorDocument doc,SkillEditorLayout.Rect r){
        if(r.width()<=0||r.height()<=0)return;
        int x=r.x()+6,y=r.y()+28,w=Math.max(72,r.width()-12);
        var p=primitive(doc);
        if(p==null){drawLater("VFX パーツが選択されていません。左のツリーからパーツを選んでください。",x,y);drawLater("例: 発動時 > 発生 > 点",x,y+14);return;}
        var fields=AbilityVisualPropertySchemas.descriptors(p.type()); int pageSize=(p.type()==SkillVfxModel.PrimitiveType.LINE||p.type()==SkillVfxModel.PrimitiveType.BEZIER)&&layout.bezierControlPage(r)?1:layout.inspectorPageSize(r), pages=Math.max(1,(fields.size()+pageSize-1)/pageSize); inspectorPage=Math.clamp(inspectorPage,0,pages-1); int first=inspectorPage*pageSize,last=Math.min(fields.size(),first+pageSize);
        drawLater(layout.inspectorTitle(p.type(),first,last,fields.size(),r),x,y-15);
        var previous=button(x+r.width()-110,r.y()+4,48,"前へ",()->{inspectorPage=Math.max(0,inspectorPage-1);rebuildWidgets();});previous.active=inspectorPage>0;add(previous);
        var nextPage=button(x+r.width()-58,r.y()+4,48,"次へ",()->{inspectorPage=Math.min(pages-1,inspectorPage+1);rebuildWidgets();});nextPage.active=inspectorPage+1<pages;add(nextPage);
        for(var descriptor:fields.subList(first,last)){
            String field=descriptor.id(), fieldLabel=descriptor.displayName();
            Object value=doc.selectedValue(field);
            if(field.equals("argb")){
                var box=new ProjectSTextField(font,x,y,w,18,Component.literal("色 (ARGB)"),Component.literal("#AARRGGBB"));
                box.setValue(SkillEditorUiController.argb(p.argb()));
                box.setResponder(raw->{var parsed=SkillEditorUiController.argb(raw);box.error(Component.literal(parsed.error()));if(parsed.valid())setPrimitive(p.id(),current->withArgb(current,parsed.value()),false);});
                add(box);drawLater(AbilityVisualPropertySchemas.description(field),x,y+19);y+=30;continue;
            }
            if(field.equals("controlPoints")){
                var controls=p.controls();
                drawLater(AbilityVisualPropertySchemas.description(field),x,y);
                y+=14;
                if(p.type()==SkillVfxModel.PrimitiveType.LINE&&controls.isEmpty()){
                    add(button(x,y,Math.min(120,w),"始点・終点を設定",()->setControlPoints(doc,p.id(),current->List.of(new SkillVfxModel.Vec(0,0,0),new SkillVfxModel.Vec(0,0,1)),true)));y+=30;continue;
                }
                if(p.type()==SkillVfxModel.PrimitiveType.BEZIER){
                    if(controls.size()==3)add(button(x,y,Math.min(120,w),"制御点2を追加",()->setControlPoints(doc,p.id(),current->{var nextControls=new ArrayList<>(current.controls());nextControls.add(current.controls().getLast());return nextControls;},true)));
                    if(controls.size()==4)add(button(x,y,Math.min(120,w),"制御点2を削除",()->setControlPoints(doc,p.id(),current->List.of(current.controls().get(0),current.controls().get(1),current.controls().get(3)),true)));
                    y+=22;
                }
                for(int point=0;point<controls.size()&&y<r.y()+r.height()-25;point++){
                    for(int axis=0;axis<3;axis++){
                        final int pi=point,ai=axis;
                        double n=axis==0?controls.get(point).x():axis==1?controls.get(point).y():controls.get(point).z();
                        String pointName=p.type()==SkillVfxModel.PrimitiveType.LINE?(point==0?"始点":"終点"):(point==0?"始点":point==controls.size()-1?"終点":"制御点"+point);
                        add(number(x+axis*(w/3),y,Math.max(25,w/3-2),pointName+" "+"XYZ".charAt(axis),n,v->setControlCoordinate(doc,p.id(),pi,ai,v)));
                    } y+=30;
                } continue;
            }
            if(field.equals("count")){
                double count=value instanceof SkillVfxModel.Literal literal?Math.clamp(Math.rint(literal.value()),1,64):8;
                add(new ProjectSNumberField(font,x,y,w,18,Component.literal(fieldLabel),count,1,64,1,0,"",v->writeDescriptor(descriptor,doc,new SkillVfxModel.Literal((int)v))));
                drawLater("火花の数は 1〜64 の固定値です。",x,y+19);y+=30;continue;
            }
            if(isScalar(field)){
                SkillVfxModel.Scalar scalar=value instanceof SkillVfxModel.Scalar s?s:new SkillVfxModel.Literal(0);
                var mode=SkillEditorUiController.mode(scalar);
                var mb=button(x,y,Math.min(95,w),mode==SkillEditorUiController.ScalarMode.LITERAL?"固定値":"ゲームプレイ値",()->{
                    SkillVfxModel.ActionField source=scalar instanceof SkillVfxModel.FromGameplay f?f.field():SkillVfxModel.ActionField.RADIUS;
                    setPrimitive(p.id(),current->current.withValue(field,SkillEditorUiController.scalar(mode==SkillEditorUiController.ScalarMode.LITERAL?SkillEditorUiController.ScalarMode.FROM_GAMEPLAY:SkillEditorUiController.ScalarMode.LITERAL,scalar instanceof SkillVfxModel.Literal l?l.value():0,source)),true);
                }); add(mb);
                if(mode==SkillEditorUiController.ScalarMode.FROM_GAMEPLAY){
                    var source=(SkillVfxModel.FromGameplay)scalar;
                    add(button(x+98,y,Math.max(40,w-98),source.field().name(),()->{
                        var all=SkillVfxModel.ActionField.values();
                        setPrimitive(p.id(),current->current.withValue(field,new SkillVfxModel.FromGameplay(all[(source.field().ordinal()+1)%all.length])),true);
                    }));
                } else {
                    add(number(x+98,y,Math.max(40,w-98),fieldLabel,((SkillVfxModel.Literal)scalar).value(),
                            v -> setPrimitive(p.id(), current -> current.withValue(field, new SkillVfxModel.Literal(v)),false)));
                }
                drawLater(AbilityVisualPropertySchemas.description(field),x,y+19);y+=30;continue;
            }
            double number=numberValue(p,field,value);
            add(number(x,y,w,fieldLabel,number,v->{writeDescriptor(descriptor,doc,v);localError="";}));drawLater(AbilityVisualPropertySchemas.description(field),x,y+19);y+=30;
        }
        y+=2;
    }
    private void buildTimeline(AbilityVisualEditorDocument doc,SkillEditorLayout.Rect r){
        if(r.width()<=0||r.height()<=0||doc==null)return;var bars=preview.timeline().bars(doc.visual(),hook);int duration=Math.max(1,bars.stream().mapToInt(SkillVfxTimeline.Bar::end).max().orElse(20));
        var view=SkillVfxTimelinePresentation.layout(bars,duration,preview.timeline().ticks(),timelineViewport(r));
        for(var bar:view.bars())drawLater(bar.id()+"  "+bar.start()+"〜"+bar.end()+" tick",r.x()+6,bar.bounds().y());
        drawLater("開始 0 tick　　　　　　　　　　　　終了 "+view.endTick()+" tick",r.x()+6,r.y()+17);
    }
    private ProjectSButton button(int x,int y,int w,String label,Runnable action){return new ProjectSButton(x,y,Math.max(1,w),20,Component.literal(label),ProjectSButton.Kind.SECONDARY,action);}
    @SuppressWarnings({"rawtypes","unchecked"}) private void writeDescriptor(io.github.gyai.projects.editor.core.PropertyDescriptor descriptor,AbilityVisualEditorDocument document,Object value){descriptor.write(document,value);afterMutation(false);}
    private ProjectSNumberField number(int x,int y,int w,String label,double value,java.util.function.DoubleConsumer action){return new ProjectSNumberField(font,x,y,Math.max(25,w),18,Component.literal(label),value,-30_000_000,30_000_000,.1,3,"",action);}
    private void add(ProjectSButton button){addRenderableWidget(button);} private void add(ProjectSTextField field){addRenderableWidget(field);} private void add(ProjectSNumberField field){addRenderableWidget(field);}
    private AbilityVisualEditorDocument document(){return SkillEditorClientState.document();} private SkillVfxModel.Emission emission(AbilityVisualEditorDocument doc){return doc==null||emissionId==null?null:doc.visual().emissions(hook).stream().filter(e->e.id().equals(emissionId)).findFirst().orElse(null);} private SkillVfxModel.Primitive primitive(AbilityVisualEditorDocument doc){if(doc==null||primitiveId==null)return null;for(var e:doc.visual().emissions(hook))if(e.id().equals(emissionId))for(var p:e.primitives())if(p.id().equals(primitiveId))return p;return null;}
    private static boolean isScalar(String field){return !Set.of("delayTicks","durationTicks","argb","opacity","width","density","seed","offsetX","offsetY","offsetZ","yaw","controlPoints","count").contains(field);}
    private static double numberValue(SkillVfxModel.Primitive p,String field,Object ignored){return switch(field){case "delayTicks"->p.delayTicks();case "durationTicks"->p.durationTicks();case "opacity"->(p.argb()>>>24)&255;case "width"->p.width();case "density"->p.density();case "seed"->p.seed();case "offsetX"->p.offset().x();case "offsetY"->p.offset().y();case "offsetZ"->p.offset().z();case "yaw"->p.yaw();default->0;};}
    private static SkillVfxModel.Primitive withArgb(SkillVfxModel.Primitive p,int argb){return new SkillVfxModel.Primitive(p.id(),p.type(),p.delayTicks(),p.durationTicks(),argb,p.width(),p.density(),p.seed(),p.offset(),p.yaw(),p.values(),p.controls());}
    private static SkillVfxModel.Primitive withNumber(SkillVfxModel.Primitive p,String f,double v){int delay=f.equals("delayTicks")?Math.max(0,(int)v):p.delayTicks(),duration=f.equals("durationTicks")?Math.clamp((int)v,1,1200):p.durationTicks(),density=f.equals("density")?Math.clamp((int)v,1,256):p.density();int argb=f.equals("opacity")?((Math.clamp((int)v,0,255)<<24)|(p.argb()&0x00FFFFFF)):p.argb();double width=f.equals("width")?Math.max(.001,v):p.width();long seed=f.equals("seed")?(long)v:p.seed();SkillVfxModel.Vec offset=new SkillVfxModel.Vec(f.equals("offsetX")?v:p.offset().x(),f.equals("offsetY")?v:p.offset().y(),f.equals("offsetZ")?v:p.offset().z());return new SkillVfxModel.Primitive(p.id(),p.type(),delay,duration,argb,width,density,seed,offset,f.equals("yaw")?v:p.yaw(),p.values(),p.controls());}
    private void setControlCoordinate(AbilityVisualEditorDocument doc,String id,int point,int axis,double value){setControlPoints(doc,id,current->{var controls=new ArrayList<>(current.controls());var old=controls.get(point);controls.set(point,new SkillVfxModel.Vec(axis==0?value:old.x(),axis==1?value:old.y(),axis==2?value:old.z()));return controls;},false);}
    private void setControlPoints(AbilityVisualEditorDocument document,String id,java.util.function.Function<SkillVfxModel.Primitive,List<SkillVfxModel.Vec>> update,boolean rebuild){if(SkillVfxMutation.writeControls(document,id,update))afterMutation(rebuild);}
    private void setPrimitive(String id,java.util.function.UnaryOperator<SkillVfxModel.Primitive> update,boolean rebuild){var d=document();if(SkillVfxMutation.replace(d,id,update)){localError="";afterMutation(rebuild);}}
    private String actionBindingLabel(AbilityVisualEditorDocument doc,SkillVfxModel.Emission emission){if(emission.actionIndex()<0)return "アクション: 未設定";var actions=doc.baseline().gameplay();return emission.actionIndex()<actions.size()?"アクション "+emission.actionIndex()+": "+actions.get(emission.actionIndex()).type():"アクション: 無効";} private void cycleEmissionAction(){var d=document();var selected=emission(d);if(d==null||selected==null)return;int limit=d.baseline().gameplay().size(),next=selected.actionIndex()+1; if(next>=limit)next=-1;d.execute(AbilityVisualCommands.setEmissionActionIndex(hook,selected.id(),next));rebuildWidgets();}
    private void addEmission(){var d=document();if(d==null)return;Set<String> used=new HashSet<>();for(var e:d.visual().emissions(hook))used.add(e.id());var id=SkillVfxModel.nextId(used,"emission");d.execute(AbilityVisualCommands.addEmission(hook,new SkillVfxModel.Emission(id,-1,List.of())));emissionId=id;rebuildWidgets();}
    private void addPrimitive(){var d=document();if(d==null||emissionId==null)return;Set<String> used=new HashSet<>();for(var h:d.visual().hooks())for(var e:h.emissions())for(var p:e.primitives())used.add(p.id());SkillVfxModel.PrimitiveType type=primitive(d)==null?SkillVfxModel.PrimitiveType.POINT:SkillVfxModel.PrimitiveType.values()[(primitive(d).type().ordinal()+1)%SkillVfxModel.PrimitiveType.values().length];var p=SkillVfxModel.defaults(SkillVfxModel.nextId(used,type.name()),type);d.execute(AbilityVisualCommands.add(hook,emissionId,p));primitiveId=p.id();d.select("primitive",primitiveId);rebuildWidgets();}
    private void duplicatePrimitive(){var d=document();if(d!=null&&primitiveId!=null&&emissionId!=null){d.execute(AbilityVisualCommands.duplicate(hook,emissionId,primitiveId));rebuildWidgets();}} private void duplicateEmission(){var d=document();if(d!=null&&emissionId!=null){d.execute(AbilityVisualCommands.duplicateEmission(hook,emissionId));rebuildWidgets();}} private void removeEmission(){var d=document();if(d!=null&&emissionId!=null){d.execute(AbilityVisualCommands.removeEmission(hook,emissionId));emissionId=null;primitiveId=null;d.select("emission",null);rebuildWidgets();}}
    private void removePrimitive(){var d=document();if(d!=null&&primitiveId!=null){d.execute(AbilityVisualCommands.remove(primitiveId));primitiveId=null;d.select("primitive",null);rebuildWidgets();}}
    private void movePrimitive(int delta){var d=document();if(d==null||primitiveId==null||emissionId==null)return;var e=d.visual().emissions(hook).stream().filter(x->x.id().equals(emissionId)).findFirst().orElse(null);if(e==null)return;int at=0;for(int i=0;i<e.primitives().size();i++)if(e.primitives().get(i).id().equals(primitiveId))at=i;d.execute(AbilityVisualCommands.movePrimitive(hook,emissionId,primitiveId,at+delta));rebuildWidgets();}
    private void moveEmission(int delta){var d=document();if(d==null||emissionId==null)return;var es=d.visual().emissions(hook);int at=0;for(int i=0;i<es.size();i++)if(es.get(i).id().equals(emissionId))at=i;d.execute(AbilityVisualCommands.moveEmission(hook,emissionId,at+delta));rebuildWidgets();}
    private boolean canMovePrimitive(AbilityVisualEditorDocument d,int delta){var e=emission(d);if(e==null||primitiveId==null)return false;for(int i=0;i<e.primitives().size();i++)if(e.primitives().get(i).id().equals(primitiveId))return i+delta>=0&&i+delta<e.primitives().size();return false;}
    private boolean canMoveEmission(AbilityVisualEditorDocument d,int delta){if(d==null||emissionId==null)return false;var emissions=d.visual().emissions(hook);for(int i=0;i<emissions.size();i++)if(emissions.get(i).id().equals(emissionId))return i+delta>=0&&i+delta<emissions.size();return false;}
    private AbilityVfx.Quality toCoreQuality(){return AbilityVfx.Quality.valueOf(preview.quality().name());}
    private void play(){if(!SkillEditorClientState.previewAllowed()){localError="サーバーがローカルプレビューを許可していません。";rebuildWidgets();return;}if(preview.timeline().playing()){preview.pause();AbilityVfxLocalPreview.pause();rebuildWidgets();return;}if(AbilityVfxLocalPreview.preview().isPresent()){preview.play();AbilityVfxLocalPreview.play();rebuildWidgets();return;}var result=SkillVfxMinecraftPreviewController.play(document(),preview);localError=result.valid()?"":"ローカルプレビューを開始できません。";if(result.valid()){preview.play();AbilityVfxLocalPreview.quality(toCoreQuality());}rebuildWidgets();} private void stopPreview(){preview.stop();SkillVfxMinecraftPreviewController.stop();rebuildWidgets();}private void restart(){if(!SkillEditorClientState.previewAllowed()){localError="サーバーがローカルプレビューを許可していません。";rebuildWidgets();return;}stopPreview();play();preview.restart();AbilityVfxLocalPreview.restart();}
    private void apply(){if(SkillEditorClientState.apply())toasts.show(ProjectSToast.Kind.INFO,Component.literal("スキル VFX エディター"),Component.literal("開発セッションへ一時反映しています…"),2500);else localError="VFX が有効でサーバー準備完了になるまで反映できません。";}
    private void confirmRefresh(){var d=document();if(d!=null&&d.dirty())modal.open(Component.literal("サーバーの状態を再読み込みしますか？"),Component.literal("未反映のローカル VFX 下書きは失われます。"),Component.literal("再読み込み"),SkillEditorClientState::refresh,Component.literal("キャンセル"),()->{},ProjectSModal.PrimaryKind.DANGER,true);else SkillEditorClientState.refresh();}
    private void confirmRevert(){modal.open(Component.literal("一時反映を破棄しますか？"),Component.literal("サーバーに基準 VFX への復帰を要求します。"),Component.literal("変更を破棄"),SkillEditorClientState::revert,Component.literal("キャンセル"),()->{},ProjectSModal.PrimaryKind.DANGER,true);}
    @Override public void tick(){super.tick();var d=document();if(preview.timeline().playing()&&d!=null){int duration=Math.max(1,preview.timeline().bars(d.visual(),hook).stream().mapToInt(SkillVfxTimeline.Bar::end).max().orElse(20));preview.timeline().advance(1,duration);AbilityVfxLocalPreview.seek(preview.timeline().ticks());if(preview.timeline().playing())AbilityVfxLocalPreview.play();}if(observedRevision!=SkillEditorClientState.updateRevision()){observedRevision=SkillEditorClientState.updateRevision();var f=SkillEditorClientState.feedback();if(f!=SkillEditorController.Feedback.NONE){ProjectSToast.Kind kind=switch(f){case APPLIED,REVERTED->ProjectSToast.Kind.SUCCESS;case STALE,CONFLICT,REFRESH_REQUIRED->ProjectSToast.Kind.WARNING;default->ProjectSToast.Kind.ERROR;};toasts.show(kind,Component.literal("スキル VFX エディター"),Component.literal(SkillEditorUiController.feedback(f,SkillEditorClientState.feedbackMessage())),4500);}rebuildWidgets();}}
    @Override public void extractRenderState(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float tickProgress){var t=ProjectSThemeManager.get().activeTheme().tokens();graphics.fill(0,0,width,height,t.background());var d=document();int statusX=width<760?240:8,statusY=width<760?10:34;graphics.text(font,d!=null&&d.dirty()?"● 未反映の変更があります":"変更なし",statusX,statusY,t.textMuted(),false);if(!displaySettings){var b=layout.bounds(width,height);panel(graphics,b.tree(),"VFX ツリー");panel(graphics,b.preview(),"プレビュー");panel(graphics,b.inspector(),"詳細");panel(graphics,b.timeline(),"VFX タイムライン");drawTimeline(graphics,d,b.timeline(),t);}else graphics.text(font,"表示設定",8,48,t.textPrimary(),false);super.extractRenderState(graphics,mouseX,mouseY,tickProgress);for(var label:labels)graphics.text(font,label.text(),label.x(),label.y(),t.textSecondary(),false);}
    private void drawTimeline(GuiGraphicsExtractor graphics,AbilityVisualEditorDocument doc,SkillEditorLayout.Rect r,io.github.gyai.projects.client.ui.theme.ProjectSThemeTokens t){
        if(doc==null||r.width()<=0||r.height()<=0)return;var bars=preview.timeline().bars(doc.visual(),hook);int duration=Math.max(1,bars.stream().mapToInt(SkillVfxTimeline.Bar::end).max().orElse(20));
        var view=SkillVfxTimelinePresentation.layout(bars,duration,preview.timeline().ticks(),timelineViewport(r));
        for(var bar:view.bars())graphics.fill(bar.bounds().x(),bar.bounds().y(),bar.bounds().x()+bar.bounds().width(),bar.bounds().y()+bar.bounds().height(),t.accentPrimary());
        graphics.fill(view.playheadX(),r.y()+27,view.playheadX()+2,r.y()+Math.max(28,r.height()-5),t.danger());
    }
    private SkillVfxTimelinePresentation.Rect timelineViewport(SkillEditorLayout.Rect r){return new SkillVfxTimelinePresentation.Rect(r.x()+6,r.y()+30,Math.max(1,r.width()-12),Math.max(1,r.height()-48));}
    private void panel(GuiGraphicsExtractor graphics,SkillEditorLayout.Rect r,String title){if(r.width()<=0||r.height()<=0)return;var t=ProjectSThemeManager.get().activeTheme().tokens();ProjectSUiDraw.cutPanel(graphics,r.x(),r.y(),r.width(),r.height(),5,t.surface(),t.borderCard());graphics.text(font,title,r.x()+6,r.y()+8,t.textPrimary(),false);}
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick){if(super.mouseClicked(event,doubleClick))return true;var b=layout.bounds(width,height);if(event.button()==0){if(Math.abs(event.x()-(b.tree().x()+b.tree().width()))<5&&b.tree().width()>0){draggingLeft=true;return true;}if(Math.abs(event.x()-b.inspector().x())<5&&b.inspector().width()>0){draggingRight=true;return true;}if(Math.abs(event.y()-b.timeline().y())<5&&b.timeline().height()>0){draggingBottom=true;return true;}var viewport=timelineViewport(b.timeline());if(event.x()>=viewport.x()&&event.x()<viewport.x()+viewport.width()&&event.y()>=viewport.y()&&event.y()<viewport.y()+viewport.height()){var d=document();int duration=d==null?20:Math.max(1,preview.timeline().bars(d.visual(),hook).stream().mapToInt(SkillVfxTimeline.Bar::end).max().orElse(20));double seek=SkillVfxTimelinePresentation.seekTick(event.x(),duration,viewport);preview.timeline().seek(seek);AbilityVfxLocalPreview.seek(seek);return true;}}return false;}
    @Override public boolean mouseDragged(MouseButtonEvent event,double deltaX,double deltaY){if(super.mouseDragged(event,deltaX,deltaY))return true;if(draggingLeft)layout.dragLeft((int)event.x(),width);else if(draggingRight)layout.dragRight((int)event.x(),width);else if(draggingBottom)layout.dragBottom((int)event.y(),height);else return false;rebuildWidgets();return true;}
    @Override public boolean mouseReleased(MouseButtonEvent event){if(draggingLeft||draggingRight||draggingBottom){draggingLeft=draggingRight=draggingBottom=false;return true;}return super.mouseReleased(event);}
    @Override public void onClose(){if(document()!=null&&document().dirty()){modal.open(Component.literal("未反映の変更を破棄しますか？"),Component.literal("送信していない DevTools の変更は失われます。"),Component.literal("破棄して閉じる"),this::closeEditor,Component.literal("キャンセル"),()->{},ProjectSModal.PrimaryKind.DANGER,true);return;}closeEditor();}
    private void closeEditor(){preview.stop();AbilityVfxLocalPreview.closeEditor();minecraft.setScreen(parent);}
}
