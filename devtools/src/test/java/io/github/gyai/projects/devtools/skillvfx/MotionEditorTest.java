package io.github.gyai.projects.devtools.skillvfx;

import io.github.gyai.projects.client.vfx.AbilityVfx;
import io.github.gyai.projects.client.vfx.AbilityVfxLocalPreviewStore;
import io.github.gyai.projects.client.vfx.MotionDirection;
import io.github.gyai.projects.client.vfx.MotionEasing;
import io.github.gyai.projects.client.vfx.MotionMode;
import io.github.gyai.projects.client.vfx.MotionSpec;

import java.util.List;
import java.util.UUID;

/** Pure PHASE 10-14 Motion editor, guide, handle, preview, and timeline coverage. */
public final class MotionEditorTest {
    public static void main(String[] args) {
        labelsAndTooltips();
        capabilityMatrix();
        inspectorAndHistory();
        motionChoiceCycles();
        guidePresentation();
        productionPathParity();
        previewMotionSync();
        directHandles();
        timelineOverlay();
        compactReachability();
        System.out.println("Motion editor presentation tests passed");
    }

    private static void labelsAndTooltips() {
        assert MotionAuthoringPresentation.label(MotionMode.STATIC).equals("静止");
        assert MotionAuthoringPresentation.label(MotionMode.REVEAL).equals("徐々に表示");
        assert MotionAuthoringPresentation.label(MotionMode.TRAVEL).equals("軌道移動");
        assert MotionAuthoringPresentation.label(MotionDirection.FORWARD).equals("正方向");
        assert MotionAuthoringPresentation.label(MotionDirection.REVERSE).equals("逆方向");
        assert MotionAuthoringPresentation.label(MotionEasing.EASE_IN_OUT).equals("ゆっくり開始・終了");
        assert MotionAuthoringPresentation.categoryLabel().equals("【動き】");
        var revealChoice = MotionAuthoringPresentation.modes(SkillVfxModel.PrimitiveType.LINE, MotionSpec.LEGACY_DEFAULT).stream()
                .filter(value -> value.value() == MotionMode.REVEAL).findFirst().orElseThrow();
        assert revealChoice.tooltip().equals("VFXの形を始点側から順番に表示します。");
        var travelChoice = MotionAuthoringPresentation.modes(SkillVfxModel.PrimitiveType.LINE, MotionSpec.LEGACY_DEFAULT).stream()
                .filter(value -> value.value() == MotionMode.TRAVEL).findFirst().orElseThrow();
        assert travelChoice.tooltip().equals("VFXの形に沿って表示位置を移動させます。");
        assert MotionAuthoringPresentation.directions(SkillVfxModel.PrimitiveType.LINE, MotionSpec.LEGACY_DEFAULT).stream()
                .allMatch(value -> value.tooltip().equals("軌道をどちら向きに進むか指定します。"));
        assert MotionAuthoringPresentation.modeTooltip(SkillVfxModel.PrimitiveType.LINE, MotionSpec.LEGACY_DEFAULT)
                .equals("VFXの形を始点側から順番に表示します。");
        assert MotionAuthoringPresentation.modeTooltip(SkillVfxModel.PrimitiveType.LINE,
                new MotionSpec(MotionMode.TRAVEL, MotionDirection.FORWARD, MotionEasing.LINEAR, 0, .25))
                .equals("VFXの形に沿って表示位置を移動させます。");
        assert MotionAuthoringPresentation.modeTooltip(SkillVfxModel.PrimitiveType.LINE,
                new MotionSpec(MotionMode.STATIC, MotionDirection.FORWARD, MotionEasing.LINEAR, 0, 0))
                .equals("動作の種類を指定します。");
        assert MotionAuthoringPresentation.properties(SkillVfxModel.defaults("line", SkillVfxModel.PrimitiveType.LINE), true).stream()
                .filter(value -> value.id().equals("motionPhase")).findFirst().orElseThrow().help()
                .equals("再生開始時点ですでにどこまで進んでいるか指定します。");
        var fallbackProperties = MotionAuthoringPresentation.properties(SkillVfxModel.defaults("point", SkillVfxModel.PrimitiveType.POINT), false);
        assert fallbackProperties.getFirst().id().equals("motionCategory") && fallbackProperties.getFirst().label().equals("【動き】");
        assert fallbackProperties.get(1).help().equals(MotionAuthoringPresentation.unsupportedMessage());
        var travel = withMotion(SkillVfxModel.defaults("line", SkillVfxModel.PrimitiveType.LINE), new MotionSpec(MotionMode.TRAVEL, MotionDirection.FORWARD, MotionEasing.LINEAR, .25, .25));
        var properties = MotionAuthoringPresentation.properties(travel, true);
        assert properties.stream().map(MotionAuthoringPresentation.Property::label).toList()
                .equals(List.of("【動き】", "動作", "方向", "開始位置", "イージング", "軌跡の長さ"));
        assert properties.stream().filter(value -> !value.id().equals("motionCategory"))
                .allMatch(value -> !value.help().isBlank());
        assert properties.stream().anyMatch(value -> value.help().contains("後ろに残る"));
        var fallback = SkillEditorUiController.inspectorProperties(travel, true, false);
        assert fallback.stream().anyMatch(value -> value.id().equals("motionUnsupported"));
        assert fallback.stream().noneMatch(value -> value.id().equals("motionMode"));
        assert fallback.stream().anyMatch(value -> value.id().equals("motionCategory") && value.label().equals("【動き】"));
    }

    private static void capabilityMatrix() {
        var legacy = MotionSpec.LEGACY_DEFAULT;
        for (var type : SkillVfxModel.PrimitiveType.values()) {
            var choices = MotionAuthoringPresentation.modes(type, legacy);
            assert choices.stream().filter(value -> value.value() == MotionMode.STATIC || value.value() == MotionMode.REVEAL).allMatch(MotionAuthoringPresentation.Choice::enabled);
            var travel = new MotionSpec(MotionMode.TRAVEL, MotionDirection.FORWARD, MotionEasing.LINEAR, 0, .25);
            assert choices.stream().filter(value -> value.value() == MotionMode.TRAVEL).findFirst().orElseThrow().enabled() == travel.supports(AbilityVfx.Type.valueOf(type.name()));
        }
        var pointReveal = MotionAuthoringPresentation.directions(SkillVfxModel.PrimitiveType.POINT, legacy);
        assert pointReveal.stream().filter(value -> value.value() == MotionDirection.REVERSE).findFirst().orElseThrow().enabled() == false;
        var lineReveal = MotionAuthoringPresentation.directions(SkillVfxModel.PrimitiveType.LINE, legacy);
        assert lineReveal.stream().filter(value -> value.value() == MotionDirection.REVERSE).findFirst().orElseThrow().enabled();
        var staticValue = MotionAuthoringPresentation.mode(legacy, SkillVfxModel.PrimitiveType.SPHERE, MotionMode.STATIC);
        assert staticValue.equals(new MotionSpec(MotionMode.STATIC, MotionDirection.FORWARD, MotionEasing.LINEAR, 0, 0));
        var travelValue = MotionAuthoringPresentation.mode(legacy, SkillVfxModel.PrimitiveType.LINE, MotionMode.TRAVEL);
        assert travelValue.mode() == MotionMode.TRAVEL && travelValue.trailFraction() == .25;
        assert MotionAuthoringPresentation.mode(legacy, SkillVfxModel.PrimitiveType.POINT, MotionMode.TRAVEL).equals(legacy);
    }

    private static void inspectorAndHistory() {
        var p = withMotion(SkillVfxModel.defaults("line", SkillVfxModel.PrimitiveType.LINE), new MotionSpec(MotionMode.REVEAL, MotionDirection.FORWARD, MotionEasing.LINEAR, 0, 0));
        var visual = visual(p);
        var document = new AbilityVisualEditorDocument(snapshot(visual));
        var modeMotion = MotionAuthoringPresentation.mode(p.motion(), p.type(), MotionMode.TRAVEL);
        document.execute(AbilityVisualCommands.setMotion(p.id(), modeMotion));
        assert modeMotion.trailFraction() == .25 && primitive(document, p.id()).motion().equals(modeMotion);
        assert document.history().size() == 1 && document.undo() && primitive(document, p.id()).motion().equals(p.motion());
        assert document.redo() && primitive(document, p.id()).motion().equals(modeMotion);

        var phaseMotion = MotionAuthoringPresentation.phase(modeMotion, p.type(), .75);
        document.execute(AbilityVisualCommands.setMotion(p.id(), phaseMotion));
        assert document.history().size() == 2 && primitive(document, p.id()).motion().equals(phaseMotion);
        assert document.undo() && primitive(document, p.id()).motion().equals(modeMotion);
        assert document.redo() && primitive(document, p.id()).motion().equals(phaseMotion);

        var directionMotion = MotionAuthoringPresentation.direction(phaseMotion, p.type(), MotionDirection.REVERSE);
        document.execute(AbilityVisualCommands.setMotion(p.id(), directionMotion));
        assert document.history().size() == 3 && primitive(document, p.id()).motion().equals(directionMotion);
        assert document.undo() && primitive(document, p.id()).motion().equals(phaseMotion);
        assert document.redo() && primitive(document, p.id()).motion().equals(directionMotion);

        var easingMotion = MotionAuthoringPresentation.easing(directionMotion, p.type(), MotionEasing.EASE_OUT);
        document.execute(AbilityVisualCommands.setMotion(p.id(), easingMotion));
        assert document.history().size() == 4 && primitive(document, p.id()).motion().equals(easingMotion);
        assert document.undo() && primitive(document, p.id()).motion().equals(directionMotion);
        assert document.redo() && primitive(document, p.id()).motion().equals(easingMotion);

        var trailMotion = MotionAuthoringPresentation.trail(easingMotion, p.type(), .5);
        document.execute(AbilityVisualCommands.setMotion(p.id(), trailMotion));
        assert document.history().size() == 5 && primitive(document, p.id()).motion().equals(trailMotion);
        assert document.undo() && primitive(document, p.id()).motion().equals(easingMotion);
        assert document.redo() && primitive(document, p.id()).motion().equals(trailMotion);
        assert MotionAuthoringPresentation.toggleDirection(primitive(document, p.id()).motion(), p.type()).direction() == MotionDirection.FORWARD;
        assert MotionAuthoringPresentation.toggleDirection(MotionSpec.LEGACY_DEFAULT, SkillVfxModel.PrimitiveType.POINT).equals(MotionSpec.LEGACY_DEFAULT);
    }

    private static void motionChoiceCycles() {
        var p = SkillVfxModel.defaults("cycle-line", SkillVfxModel.PrimitiveType.LINE);
        var document = new AbilityVisualEditorDocument(snapshot(visual(p)));
        document.execute(AbilityVisualCommands.setMotion(p.id(), MotionAuthoringPresentation.nextDirection(p.type(), primitive(document, p.id()).motion())));
        assert primitive(document, p.id()).motion().direction() == MotionDirection.REVERSE && document.history().size() == 1;
        document.execute(AbilityVisualCommands.setMotion(p.id(), MotionAuthoringPresentation.nextDirection(p.type(), primitive(document, p.id()).motion())));
        assert primitive(document, p.id()).motion().direction() == MotionDirection.FORWARD && document.history().size() == 2;
        assert document.undo() && primitive(document, p.id()).motion().direction() == MotionDirection.REVERSE;
        assert document.redo() && primitive(document, p.id()).motion().direction() == MotionDirection.FORWARD;

        MotionEasing[] expected = {MotionEasing.EASE_IN, MotionEasing.EASE_OUT, MotionEasing.EASE_IN_OUT, MotionEasing.LINEAR};
        for (int i = 0; i < expected.length; i++) {
            final int at = i;
            document.execute(AbilityVisualCommands.setMotion(p.id(), MotionAuthoringPresentation.nextEasing(p.type(), primitive(document, p.id()).motion())));
            assert primitive(document, p.id()).motion().easing() == expected[at] && document.history().size() == 3 + at;
        }
        assert document.undo() && primitive(document, p.id()).motion().easing() == MotionEasing.EASE_IN_OUT;
        assert document.redo() && primitive(document, p.id()).motion().easing() == MotionEasing.LINEAR;
    }

    private static void guidePresentation() {
        AbilityVfx.Frame frame = frame();
        var spiral = withMotion(SkillVfxModel.defaults("spiral", SkillVfxModel.PrimitiveType.SPIRAL), new MotionSpec(MotionMode.TRAVEL, MotionDirection.REVERSE, MotionEasing.EASE_IN_OUT, .25, .5));
        var guide = MotionAuthoringPresentation.guide(spiral, frame, null, .5, AbilityVfx.Quality.HIGH);
        assert guide.supported() && guide.plan().sampleStart() > guide.plan().sampleEnd();
        assert !guide.fullShape().isEmpty() && !guide.visible().isEmpty() && !guide.trail().isEmpty();
        assert guide.headPosition() != null && guide.trailPosition() != null && !guide.directionArrow().isEmpty() && !guide.headMarker().isEmpty();
        var circle = withMotion(SkillVfxModel.defaults("circle", SkillVfxModel.PrimitiveType.CIRCLE), new MotionSpec(MotionMode.REVEAL, MotionDirection.REVERSE, MotionEasing.LINEAR, 0, 0));
        var circleGuide = MotionAuthoringPresentation.guide(circle, frame, null, 1, AbilityVfx.Quality.HIGH);
        assert circleGuide.supported() && !circleGuide.directionArrow().isEmpty();
        assert !new MotionSpec(MotionMode.TRAVEL, MotionDirection.FORWARD, MotionEasing.LINEAR, 0, .25).supports(AbilityVfx.Type.POINT);
        var point = SkillVfxDirectAuthoring.worldHandles(SkillVfxModel.defaults("point", SkillVfxModel.PrimitiveType.POINT), frame, null, .5);
        assert point.stream().noneMatch(value -> value.kind() == SkillVfxDirectAuthoring.Handle.Kind.PHASE || value.kind() == SkillVfxDirectAuthoring.Handle.Kind.TRAIL);
        assert SkillVfxDirectAuthoring.worldHandles(spiral, frame, null, .5).stream()
                .noneMatch(value -> value.kind() == SkillVfxDirectAuthoring.Handle.Kind.DIRECTION);
    }

    private static void productionPathParity() {
        var motion = new MotionSpec(MotionMode.TRAVEL, MotionDirection.REVERSE, MotionEasing.EASE_OUT, .25, .5);
        var source = withMotion(SkillVfxModel.defaults("parity", SkillVfxModel.PrimitiveType.SPIRAL), motion);
        var visual = visual(source);
        var snapshot = snapshot(visual);
        var frame = frame();
        double progress = .6;
        var remoteCue = SkillVfxPreviewBuilder.build(snapshot, visual, SkillVfxModel.Hook.CAST, frame,
                UUID.randomUUID(), "minecraft:overworld", 0).cue();
        var localCue = SkillVfxPreviewBuilder.build(snapshot, visual, SkillVfxModel.Hook.CAST, frame,
                UUID.randomUUID(), "minecraft:overworld", 0).cue();
        var remote = remoteCue.primitives().getFirst();
        var local = localCue.primitives().getFirst();
        var remotePlan = io.github.gyai.projects.client.vfx.AbilityVfxMotionPlanner.plan(remote.motion(), progress);
        var remoteCommands = AbilityVfx.sample(remote, frame, remotePlan, AbilityVfx.Quality.HIGH);
        var localCommands = AbilityVfx.sample(local, frame,
                io.github.gyai.projects.client.vfx.AbilityVfxMotionPlanner.plan(local.motion(), progress), AbilityVfx.Quality.HIGH);
        var directCommands = SkillVfxDirectAuthoring.guide(source, frame, null, progress);
        assert remoteCommands.equals(localCommands) && remoteCommands.equals(directCommands);

        var particleSource = source.withAppearance(SkillVfxModel.Appearance.particle("minecraft:flame"));
        var particleVisual = visual(particleSource);
        var particleCue = SkillVfxPreviewBuilder.build(snapshot(particleVisual), particleVisual, SkillVfxModel.Hook.CAST,
                frame, UUID.randomUUID(), "minecraft:overworld", 0).cue();
        var particlePrimitive = particleCue.primitives().getFirst();
        var particlePlan = io.github.gyai.projects.client.vfx.AbilityVfxParticlePlanner.plan(particleCue,
                value -> true, value -> progress, AbilityVfx.Quality.HIGH, 0, 0);
        var particleCommands = AbilityVfx.sample(particlePrimitive, frame,
                io.github.gyai.projects.client.vfx.AbilityVfxMotionPlanner.plan(particlePrimitive.motion(), progress), AbilityVfx.Quality.HIGH);
        assert particlePlan.spawns().size() == particleCommands.size();
        for (int i = 0; i < particlePlan.spawns().size(); i++) {
            var command = particleCommands.get(i);
            assert particlePlan.spawns().get(i).at().equals(command.b() == null ? command.a() : command.b());
        }
    }

    private static void directHandles() {
        var line = withMotion(new SkillVfxModel.Primitive("line", SkillVfxModel.PrimitiveType.LINE, 0, 20, 0xFFFFFFFF, .1, 8, 0, new SkillVfxModel.Vec(0, 0, 0), 0, java.util.Map.of("length", new SkillVfxModel.Literal(1)), List.of(new SkillVfxModel.Vec(0, 0, 1), new SkillVfxModel.Vec(1, 0, 1))), new MotionSpec(MotionMode.TRAVEL, MotionDirection.FORWARD, MotionEasing.LINEAR, .25, .25));
        var frame = frame();
        var projection = SkillVfxDirectAuthoring.fromBasis(new SkillVfxDirectAuthoring.Vec(1, 0, 0), new SkillVfxDirectAuthoring.Vec(0, 1, 0), new SkillVfxDirectAuthoring.Vec(0, 0, 1), new SkillVfxDirectAuthoring.Vec(0, 0, -5), 100);
        var handles = SkillVfxDirectAuthoring.worldHandles(line, frame, null, .5);
        var phase = handles.stream().filter(value -> value.kind() == SkillVfxDirectAuthoring.Handle.Kind.PHASE).findFirst().orElseThrow();
        var trail = handles.stream().filter(value -> value.kind() == SkillVfxDirectAuthoring.Handle.Kind.TRAIL).findFirst().orElseThrow();
        var end = projection.project(new SkillVfxDirectAuthoring.Vec(1, 0, 1)).orElseThrow();
        var movedPhase = SkillVfxDirectAuthoring.editAtMouse(line, phase, frame, null, .5, projection, end.x(), end.y());
        assert movedPhase.motion().phase() > .9;
        var start = projection.project(new SkillVfxDirectAuthoring.Vec(0, 0, 1)).orElseThrow();
        var movedTrail = SkillVfxDirectAuthoring.editAtMouse(line, trail, frame, null, .5, projection, start.x(), start.y());
        assert movedTrail.motion().trailFraction() >= .49;
        var document = new AbilityVisualEditorDocument(snapshot(visual(line)));
        var tx = SkillVfxDirectAuthoring.DragTransaction.begin(document, "line").orElseThrow();
        assert tx.update(value -> value.withMotion(movedPhase.motion()));
        assert tx.release() && document.history().size() == 1;
        assert document.undo() && primitive(document, "line").motion().equals(line.motion());
        var cancelled = SkillVfxDirectAuthoring.DragTransaction.begin(document, "line").orElseThrow();
        cancelled.update(value -> value.withMotion(movedTrail.motion()));
        assert cancelled.cancel() && document.history().size() == 1 && primitive(document, "line").motion().equals(line.motion());
        handleSelectionTargets(line, frame, projection);
    }

    private static void handleSelectionTargets(SkillVfxModel.Primitive base, AbilityVfx.Frame frame,
                                                SkillVfxDirectAuthoring.Projection projection) {
        for (MotionDirection direction : MotionDirection.values()) {
            for (double phase : new double[]{0, 1}) {
                for (double trail : new double[]{0, 1}) {
                    var motion = new MotionSpec(MotionMode.TRAVEL, direction, MotionEasing.LINEAR, phase, trail);
                    var primitive = withMotion(base, motion);
                    for (var target : new SkillVfxDirectAuthoring.MotionHandleTarget[]{
                            SkillVfxDirectAuthoring.MotionHandleTarget.PHASE,
                            SkillVfxDirectAuthoring.MotionHandleTarget.TRAIL}) {
                        var handles = SkillVfxDirectAuthoring.selectableMotionHandles(primitive, frame, null, 0,
                                target);
                        var expectedKind = target == SkillVfxDirectAuthoring.MotionHandleTarget.PHASE
                                ? SkillVfxDirectAuthoring.Handle.Kind.PHASE : SkillVfxDirectAuthoring.Handle.Kind.TRAIL;
                        var expected = handles.stream().filter(value -> value.kind() == expectedKind).findFirst().orElseThrow();
                        var point = projection.project(expected.position()).orElseThrow();
                        var picked = SkillVfxDirectAuthoring.pick(handles, projection, point.x(), point.y(), 2).orElseThrow();
                        assert picked.handle().kind() == expectedKind : "active motion target must win overlap";
                        assert handles.stream().noneMatch(value -> value.kind() == (expectedKind == SkillVfxDirectAuthoring.Handle.Kind.PHASE
                                ? SkillVfxDirectAuthoring.Handle.Kind.TRAIL : SkillVfxDirectAuthoring.Handle.Kind.PHASE));
                    }
                }
            }
        }
        var reveal = withMotion(base, new MotionSpec(MotionMode.REVEAL, MotionDirection.REVERSE, MotionEasing.LINEAR, .5, 0));
        var revealPhase = SkillVfxDirectAuthoring.selectableMotionHandles(reveal, frame, null, 0,
                SkillVfxDirectAuthoring.MotionHandleTarget.PHASE);
        assert revealPhase.stream().anyMatch(value -> value.kind() == SkillVfxDirectAuthoring.Handle.Kind.PHASE);
        assert revealPhase.stream().noneMatch(value -> value.kind() == SkillVfxDirectAuthoring.Handle.Kind.TRAIL);
        var revealTrail = SkillVfxDirectAuthoring.selectableMotionHandles(reveal, frame, null, 0,
                SkillVfxDirectAuthoring.MotionHandleTarget.TRAIL);
        assert revealTrail.stream().noneMatch(value -> value.kind() == SkillVfxDirectAuthoring.Handle.Kind.PHASE || value.kind() == SkillVfxDirectAuthoring.Handle.Kind.TRAIL);
        assert SkillVfxDirectAuthoring.worldHandles(reveal, frame, null, 0).stream()
                .noneMatch(value -> value.kind() == SkillVfxDirectAuthoring.Handle.Kind.DIRECTION);
    }

    private static void previewMotionSync() {
        var source = SkillVfxModel.defaults("preview-line", SkillVfxModel.PrimitiveType.LINE);
        var visual = visual(source);
        var snapshot = snapshot(visual);
        var frame = frame();
        var initial = SkillVfxPreviewBuilder.build(snapshot, visual, SkillVfxModel.Hook.CAST, frame, UUID.randomUUID(), "minecraft:overworld", 0);
        assert initial.valid();
        var store = new AbilityVfxLocalPreviewStore();
        assert store.begin(initial.cue(), 0);
        store.seek(7); store.pause();
        var motion = new MotionSpec(MotionMode.TRAVEL, MotionDirection.FORWARD, MotionEasing.EASE_OUT, .25, .5);
        var changed = visual.withHooks(List.of(new SkillVfxModel.HookBinding(SkillVfxModel.Hook.CAST, List.of(new SkillVfxModel.Emission("cast", -1, List.of(source.withMotion(motion)))))));
        var next = SkillVfxPreviewBuilder.build(snapshot, changed, SkillVfxModel.Hook.CAST, frame, UUID.randomUUID(), "minecraft:overworld", 0);
        assert next.valid() && store.replaceCuePreservingPlayback(next.cue());
        var preview = store.preview().orElseThrow();
        assert preview.tick() == 7 && !preview.playing() && preview.cue().primitives().getFirst().motion().equals(motion);
    }

    private static void timelineOverlay() {
        var line = withMotion(SkillVfxModel.defaults("line", SkillVfxModel.PrimitiveType.LINE), new MotionSpec(MotionMode.TRAVEL, MotionDirection.REVERSE, MotionEasing.LINEAR, .25, .5));
        var overlay = SkillVfxTimelinePresentation.motionOverlay(line, 10, 20, new SkillVfxTimelinePresentation.Rect(0, 0, 200, 24), 2, 8);
        assert overlay != null && overlay.travel() && overlay.reverse() && overlay.visible().width() > 0 && overlay.trail().height() > 0;
        assert overlay.phaseMarker().x() > overlay.track().x() && overlay.direction().fromX() > overlay.direction().toX();
        assert overlay.visible().x() + overlay.visible().width() <= overlay.track().x() + overlay.track().width();
        assert overlay.phaseMarker().x() + overlay.phaseMarker().width() <= overlay.track().x() + overlay.track().width();
        var reveal = withMotion(line, new MotionSpec(MotionMode.REVEAL, MotionDirection.FORWARD, MotionEasing.LINEAR, .25, 0));
        var revealOverlay = SkillVfxTimelinePresentation.motionOverlay(reveal, 0, 20, new SkillVfxTimelinePresentation.Rect(0, 0, 200, 24), 2, 8);
        assert !revealOverlay.travel() && revealOverlay.visible().width() > 0;
    }

    private static void compactReachability() {
        var layout = new SkillEditorLayout();
        var bounds = layout.bounds(640, 360);
        var properties = MotionAuthoringPresentation.properties(SkillVfxModel.defaults("line", SkillVfxModel.PrimitiveType.LINE), true)
                .stream().map(value -> new SkillEditorLayout.InspectorProperty(value.id(), value.label(), value.help(), value.inputHeight())).toList();
        var page = layout.inspectorPage(bounds.inspector(), properties, 0, value -> value.codePointCount(0, value.length()) * 9, 9);
        assert page.reachable() && page.entries().stream().allMatch(value -> value.field().input().within(layout.inspectorHeader(bounds.inspector()).content()));
        assert properties.getFirst().id().equals("motionCategory") && properties.getFirst().label().equals("【動き】");
        var line = SkillVfxModel.defaults("line", SkillVfxModel.PrimitiveType.LINE);
        var travel = withMotion(line, new MotionSpec(MotionMode.TRAVEL, MotionDirection.FORWARD, MotionEasing.LINEAR, 0, .25));
        var control = MotionAuthoringPresentation.motionHandleControl(640, travel, SkillVfxDirectAuthoring.MotionHandleTarget.TRAIL);
        assert control.visible() && control.enabled() && control.label().equals("直接操作: 軌跡") && control.within(640, 360);
        var revealControl = MotionAuthoringPresentation.motionHandleControl(640, line, SkillVfxDirectAuthoring.MotionHandleTarget.TRAIL);
        assert revealControl.visible() && !revealControl.enabled() && revealControl.label().equals("直接操作: 開始位置") && revealControl.within(640, 360);
        assert !MotionAuthoringPresentation.motionHandleControl(640, SkillVfxModel.defaults("point", SkillVfxModel.PrimitiveType.POINT), SkillVfxDirectAuthoring.MotionHandleTarget.PHASE).visible();
    }

    private static SkillVfxModel.Visual visual(SkillVfxModel.Primitive primitive) {
        return new SkillVfxModel.Visual("projects:vfx/motion-editor", List.of(new SkillVfxModel.HookBinding(SkillVfxModel.Hook.CAST, List.of(new SkillVfxModel.Emission("cast", -1, List.of(primitive))))));
    }

    private static SkillVfxModel.Snapshot snapshot(SkillVfxModel.Visual visual) {
        return new SkillVfxModel.Snapshot(UUID.randomUUID(), 1, "projects:motion-editor", "Motion Editor", visual.id(), "base", "effective", false, List.of(), visual);
    }

    private static SkillVfxModel.Primitive primitive(AbilityVisualEditorDocument document, String id) {
        return SkillVfxMutation.current(document, id).orElseThrow();
    }

    private static SkillVfxModel.Primitive withMotion(SkillVfxModel.Primitive primitive, MotionSpec motion) {
        return primitive.withMotion(motion);
    }

    private static AbilityVfx.Frame frame() {
        return new AbilityVfx.Frame(new AbilityVfx.Vec(0, 0, 0), new AbilityVfx.Vec(0, 0, 1), new AbilityVfx.Vec(0, 1, 0));
    }
}
