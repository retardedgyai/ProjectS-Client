package io.github.gyai.projects.devtools.skillvfx.ui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.gyai.projects.client.AbilityVfxLocalPreview;
import io.github.gyai.projects.client.vfx.AbilityVfx;
import io.github.gyai.projects.devtools.skillvfx.SkillVfxDirectAuthoring;
import io.github.gyai.projects.devtools.skillvfx.SkillVfxModel;
import io.github.gyai.projects.devtools.skillvfx.MotionAuthoringPresentation;
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
            if (!(mc.screen instanceof SkillVfx3dAuthoringScreen screen)
                    || mc.player == null || mc.level == null) return;
            CameraRenderState camera = context.levelState().cameraRenderState;
            if (camera == null || camera.pos == null) return;
            screen.updateCamera(camera);

            SkillVfxModel.Primitive primitive = screen.owner().authoringPrimitive();
            AbilityVfx.Frame frame = screen.frame();
            if (primitive == null || frame == null) return;
            SkillVfxModel.GameplayAction action = screen.owner().authoringAction();
            double progress = screen.owner().authoringProgress();
            MotionAuthoringPresentation.GuideState guide = MotionAuthoringPresentation.guide(primitive, frame, action, progress, AbilityVfx.Quality.HIGH);
            if (!guide.supported()) return;
            List<AbilityVfx.Command> commands = new ArrayList<>();
            add(commands, guide.fullShape(), new AbilityVfx.Color(75, 220, 255, 105), .012);
            add(commands, guide.remaining(), new AbilityVfx.Color(105, 120, 165, 115), .014);
            add(commands, guide.visible(), new AbilityVfx.Color(255, 235, 80, 235), .02);
            add(commands, guide.trail(), new AbilityVfx.Color(255, 170, 70, 185), .018);
            add(commands, guide.directionArrow(), new AbilityVfx.Color(255, 100, 240, 240), .045);
            add(commands, guide.headMarker(), new AbilityVfx.Color(255, 80, 80, 255), .12);

            AbilityVfx.Vec origin = frame.world(new AbilityVfx.Vec(
                    primitive.offset().x(), primitive.offset().y(), primitive.offset().z()));
            if (origin != null) {
                commands.add(line(origin, frame.world(new AbilityVfx.Vec(
                        primitive.offset().x() + 1, primitive.offset().y(), primitive.offset().z())), 255, 80, 80));
                commands.add(line(origin, frame.world(new AbilityVfx.Vec(
                        primitive.offset().x(), primitive.offset().y() + 1, primitive.offset().z())), 80, 255, 100));
                commands.add(line(origin, frame.world(new AbilityVfx.Vec(
                        primitive.offset().x(), primitive.offset().y(), primitive.offset().z() + 1)), 80, 130, 255));
            }
            List<SkillVfxDirectAuthoring.Handle> handles = SkillVfxDirectAuthoring.selectableMotionHandles(
                    primitive, frame, action, progress, screen.motionHandleTarget());
            for (SkillVfxDirectAuthoring.Handle handle : handles) {
                AbilityVfx.Vec at = new AbilityVfx.Vec(
                        handle.position().x(), handle.position().y(), handle.position().z());
                int r = 255;
                int g = handle.kind().name().startsWith("AXIS") ? 220 : 180;
                int b = handle.kind().name().startsWith("AXIS") ? 80 : 255;
                commands.add(line(at.add(new AbilityVfx.Vec(-.08, 0, 0)),
                        at.add(new AbilityVfx.Vec(.08, 0, 0)), r, g, b));
                commands.add(line(at.add(new AbilityVfx.Vec(0, -.08, 0)),
                        at.add(new AbilityVfx.Vec(0, .08, 0)), r, g, b));
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
