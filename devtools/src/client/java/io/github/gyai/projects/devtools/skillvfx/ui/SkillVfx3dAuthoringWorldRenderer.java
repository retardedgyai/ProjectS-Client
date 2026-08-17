package io.github.gyai.projects.devtools.skillvfx.ui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.gyai.projects.client.AbilityVfxLocalPreview;
import io.github.gyai.projects.client.vfx.AbilityVfx;
import io.github.gyai.projects.devtools.SkillEditorScreen;
import io.github.gyai.projects.devtools.skillvfx.SkillVfxDirectAuthoring;
import io.github.gyai.projects.devtools.skillvfx.SkillVfxModel;
import io.github.gyai.projects.devtools.skillvfx.SkillVfxAnchorAuthoring;
import io.github.gyai.projects.devtools.skillvfx.MotionAuthoringPresentation;
import io.github.gyai.projects.devtools.skillvfx.SkillVfxVisualUxPresentation;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** DevTools-only guide/gizmo submitter, active only while the authoring Screen owns input. */
public final class SkillVfx3dAuthoringWorldRenderer {
    private SkillVfx3dAuthoringWorldRenderer() { }

    public static void register() {
        LevelRenderEvents.COLLECT_SUBMITS.register(SkillVfx3dAuthoringWorldRenderer::render);
    }

    private static void render(LevelRenderContext context) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return;
            CameraRenderState camera = context.levelState().cameraRenderState;
            if (camera == null || camera.pos == null) return;
            SkillEditorScreen owner;
            AbilityVfx.Frame frame;
            SkillVfxDirectAuthoring.MotionHandleTarget target;
            SkillVfxDirectAuthoring.AuthoringTool tool;
            if (mc.screen instanceof SkillVfx3dAuthoringScreen screen) {
                screen.updateCamera(camera);
                owner = screen.owner();
                frame = screen.frame();
                target = screen.motionHandleTarget();
                tool = screen.authoringTool();
            } else if (mc.screen instanceof SkillEditorScreen editor
                    && editor.embeddedAuthoringActive()) {
                editor.updateEmbeddedCamera(camera);
                owner = editor;
                frame = editor.embeddedAuthoringFrame();
                target = editor.embeddedMotionHandleTarget();
                tool = editor.embeddedAuthoringTool();
            } else return;

            SkillVfxModel.Primitive primitive = owner.authoringPrimitive();
            if (primitive == null || frame == null) return;
            SkillVfxModel.GameplayAction action = owner.authoringAction();
            double progress = owner.authoringProgress();
            MotionAuthoringPresentation.GuideState guide = MotionAuthoringPresentation.guide(primitive, frame, action, progress, AbilityVfx.Quality.HIGH);
            if (!guide.supported()) return;
            List<AbilityVfx.Command> commands = new ArrayList<>();
            var range = SkillVfxAnchorAuthoring.range(frame, action);
            var authoredAnchors = owner.authoringAnchors();
            var caster = authoredAnchors.stream().filter(a -> a.kind() == SkillVfxAnchorAuthoring.Kind.CASTER).map(SkillVfxAnchorAuthoring.Anchor::position).findFirst().orElse(null);
            var targetAnchor = authoredAnchors.stream().filter(a -> a.kind() == SkillVfxAnchorAuthoring.Kind.TARGET).map(SkillVfxAnchorAuthoring.Anchor::position).findFirst().orElse(null);
            var impactAnchor = authoredAnchors.stream().filter(a -> a.kind() == SkillVfxAnchorAuthoring.Kind.IMPACT).map(SkillVfxAnchorAuthoring.Anchor::position).findFirst().orElse(null);
            if (range != null && caster != null && targetAnchor != null && impactAnchor != null) {
                    commands.add(line(caster, targetAnchor, 90, 220, 255));
                    commands.add(line(targetAnchor, impactAnchor, 255, 190, 70));
                    addRangeRing(commands, frame, range.distance(), 90, 220, 255);
                    addAnchorMarker(commands, caster, 80, 220, 255, .16);
                    addAnchorMarker(commands, targetAnchor, 255, 190, 70, .16);
                    addAnchorMarker(commands, impactAnchor, 255, 80, 110, .10);
            }
            for (SkillVfxDirectAuthoring.PrimitiveTarget candidate : owner.authoringTargets()) {
                if (!candidate.primitive().id().equals(primitive.id())) add(commands,
                        SkillVfxDirectAuthoring.guide(candidate.primitive(), frame, candidate.action()),
                        new AbilityVfx.Color(90, 125, 115, 70), .01);
            }
            add(commands, guide.fullShape(), new AbilityVfx.Color(75, 220, 255, 105), .012);
            add(commands, guide.remaining(), new AbilityVfx.Color(105, 120, 165, 115), .014);
            add(commands, guide.visible(), new AbilityVfx.Color(255, 235, 80, 235), .02);
            add(commands, guide.trail(), new AbilityVfx.Color(255, 170, 70, 185), .018);
            add(commands, guide.directionArrow(), new AbilityVfx.Color(255, 100, 240, 240), .045);
            add(commands, guide.headMarker(), new AbilityVfx.Color(255, 80, 80, 255), .12);

            AbilityVfx.Vec origin = frame.world(new AbilityVfx.Vec(
                    primitive.offset().x(), primitive.offset().y(), primitive.offset().z()));
            if (origin != null && tool == SkillVfxDirectAuthoring.AuthoringTool.MOVE) {
                commands.add(line(origin, frame.world(new AbilityVfx.Vec(
                        primitive.offset().x() + 1, primitive.offset().y(), primitive.offset().z())), 255, 80, 80));
                commands.add(line(origin, frame.world(new AbilityVfx.Vec(
                        primitive.offset().x(), primitive.offset().y() + 1, primitive.offset().z())), 80, 255, 100));
                commands.add(line(origin, frame.world(new AbilityVfx.Vec(
                        primitive.offset().x(), primitive.offset().y(), primitive.offset().z() + 1)), 80, 130, 255));
            }
            List<SkillVfxDirectAuthoring.Handle> handles = SkillVfxDirectAuthoring.toolHandles(
                    primitive, frame, action, progress, target, tool);
            for (SkillVfxDirectAuthoring.Handle handle : handles) {
                AbilityVfx.Vec at = new AbilityVfx.Vec(
                        handle.position().x(), handle.position().y(), handle.position().z());
                addHandleMarker(commands, at, SkillVfxVisualUxPresentation.handleVisual(handle.kind()));
            }
            if (primitive.type() == SkillVfxModel.PrimitiveType.BEZIER) {
                List<SkillVfxDirectAuthoring.Handle> controls = handles.stream()
                        .filter(h -> h.id().startsWith("control:"))
                        .sorted(Comparator.comparingInt(h -> Integer.parseInt(h.id().substring(8))))
                        .toList();
                for (int i = 1; i < controls.size(); i++) {
                    SkillVfxDirectAuthoring.Vec a = controls.get(i - 1).position();
                    SkillVfxDirectAuthoring.Vec b = controls.get(i).position();
                    commands.add(line(new AbilityVfx.Vec(a.x(), a.y(), a.z()),
                            new AbilityVfx.Vec(b.x(), b.y(), b.z()), 255, 130, 220));
                }
            }
            if (commands.isEmpty()) return;

            var collector = context.submitNodeCollector().order(18_100);
            PoseStack pose = context.poseStack();
            pose.pushPose();
            pose.translate(-camera.pos.x, -camera.pos.y, -camera.pos.z);
            collector.submitCustomGeometry(pose, RenderTypes.debugQuads(),
                    (matrix, vertices) -> commands.forEach(c -> submit(matrix, vertices, c)));
            pose.popPose();
        } catch (RuntimeException ignored) {
            // Editor guides must never affect gameplay rendering.
        }
    }

    private static AbilityVfx.Command line(AbilityVfx.Vec a, AbilityVfx.Vec b, int r, int g, int blue) {
        return new AbilityVfx.Command(a, b, new AbilityVfx.Color(r, g, blue, 220), .035);
    }

    private static void addHandleMarker(List<AbilityVfx.Command> commands, AbilityVfx.Vec at,
                                         SkillVfxVisualUxPresentation.DirectHandleVisual visual) {
        int argb = visual.color();
        int r = (argb >>> 16) & 255, g = (argb >>> 8) & 255, b = argb & 255;
        switch (visual.marker()) {
            case DIAMOND -> {
                commands.add(line(at.add(new AbilityVfx.Vec(-.1, 0, 0)), at.add(new AbilityVfx.Vec(0, .1, 0)), r, g, b));
                commands.add(line(at.add(new AbilityVfx.Vec(0, .1, 0)), at.add(new AbilityVfx.Vec(.1, 0, 0)), r, g, b));
                commands.add(line(at.add(new AbilityVfx.Vec(.1, 0, 0)), at.add(new AbilityVfx.Vec(0, -.1, 0)), r, g, b));
                commands.add(line(at.add(new AbilityVfx.Vec(0, -.1, 0)), at.add(new AbilityVfx.Vec(-.1, 0, 0)), r, g, b));
            }
            case BAR -> {
                commands.add(line(at.add(new AbilityVfx.Vec(-.12, 0, 0)), at.add(new AbilityVfx.Vec(.12, 0, 0)), r, g, b));
                commands.add(line(at.add(new AbilityVfx.Vec(-.12, -.05, 0)), at.add(new AbilityVfx.Vec(-.12, .05, 0)), r, g, b));
                commands.add(line(at.add(new AbilityVfx.Vec(.12, -.05, 0)), at.add(new AbilityVfx.Vec(.12, .05, 0)), r, g, b));
            }
            case CROSS -> {
                commands.add(line(at.add(new AbilityVfx.Vec(-.08, 0, 0)), at.add(new AbilityVfx.Vec(.08, 0, 0)), r, g, b));
                commands.add(line(at.add(new AbilityVfx.Vec(0, -.08, 0)), at.add(new AbilityVfx.Vec(0, .08, 0)), r, g, b));
            }
        }
    }

    private static void addAnchorMarker(List<AbilityVfx.Command> commands, AbilityVfx.Vec at,
                                        int r, int g, int b, double size) {
        commands.add(line(at.add(new AbilityVfx.Vec(-size, 0, 0)), at.add(new AbilityVfx.Vec(size, 0, 0)), r, g, b));
        commands.add(line(at.add(new AbilityVfx.Vec(0, -size, 0)), at.add(new AbilityVfx.Vec(0, size, 0)), r, g, b));
        commands.add(line(at.add(new AbilityVfx.Vec(0, 0, -size)), at.add(new AbilityVfx.Vec(0, 0, size)), r, g, b));
    }

    private static void addRangeRing(List<AbilityVfx.Command> commands, AbilityVfx.Frame frame,
                                     double radius, int r, int g, int b) {
        AbilityVfx.Vec previous = null;
        for (int i = 0; i <= 32; i++) {
            double angle = Math.PI * 2 * i / 32d;
            AbilityVfx.Vec point = frame.world(new AbilityVfx.Vec(Math.cos(angle) * radius, 0, Math.sin(angle) * radius));
            if (previous != null && point != null) commands.add(line(previous, point, r, g, b));
            previous = point;
        }
    }

    private static void add(List<AbilityVfx.Command> target, List<AbilityVfx.Command> source, AbilityVfx.Color color, double width) {
        for (AbilityVfx.Command command : source) {
            if (command.a() != null) target.add(new AbilityVfx.Command(command.a(), command.b(), color, Math.max(width, command.width())));
        }
    }

    private static void submit(PoseStack.Pose pose, VertexConsumer vertices, AbilityVfx.Command command) {
        if (command.a() == null) return;
        if (command.b() == null) {
            submitPoint(pose, vertices, command);
            return;
        }
        double dx = command.b().x() - command.a().x();
        double dz = command.b().z() - command.a().z();
        double length = Math.hypot(dx, dz);
        double sx = length < 1e-6 ? command.width() : -dz / length * command.width() / 2;
        double sz = length < 1e-6 ? 0 : dx / length * command.width() / 2;
        int color = (command.color().a() << 24) | (command.color().r() << 16)
                | (command.color().g() << 8) | command.color().b();
        vertices.addVertex(pose, (float) (command.a().x() + sx), (float) command.a().y(),
                (float) (command.a().z() + sz)).setColor(color);
        vertices.addVertex(pose, (float) (command.b().x() + sx), (float) command.b().y(),
                (float) (command.b().z() + sz)).setColor(color);
        vertices.addVertex(pose, (float) (command.b().x() - sx), (float) command.b().y(),
                (float) (command.b().z() - sz)).setColor(color);
        vertices.addVertex(pose, (float) (command.a().x() - sx), (float) command.a().y(),
                (float) (command.a().z() - sz)).setColor(color);
    }

    /** POINT commands have no endpoint; draw a small marker centered on the sampled position. */
    private static void submitPoint(PoseStack.Pose pose, VertexConsumer vertices, AbilityVfx.Command command) {
        double half = Math.clamp(command.width() * .1, .04, .12);
        int color = (command.color().a() << 24) | (command.color().r() << 16)
                | (command.color().g() << 8) | command.color().b();
        vertices.addVertex(pose, (float) (command.a().x() - half), (float) command.a().y(),
                (float) (command.a().z() - half)).setColor(color);
        vertices.addVertex(pose, (float) (command.a().x() + half), (float) command.a().y(),
                (float) (command.a().z() - half)).setColor(color);
        vertices.addVertex(pose, (float) (command.a().x() + half), (float) command.a().y(),
                (float) (command.a().z() + half)).setColor(color);
        vertices.addVertex(pose, (float) (command.a().x() - half), (float) command.a().y(),
                (float) (command.a().z() + half)).setColor(color);
    }
}
