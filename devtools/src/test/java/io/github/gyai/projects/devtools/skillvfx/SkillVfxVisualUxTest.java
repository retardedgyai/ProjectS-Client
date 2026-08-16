package io.github.gyai.projects.devtools.skillvfx;

import io.github.gyai.projects.client.vfx.MotionDirection;
import io.github.gyai.projects.client.vfx.MotionEasing;
import io.github.gyai.projects.client.vfx.MotionMode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;

/** Pure visual UX contract checks; no Minecraft client is started. */
public final class SkillVfxVisualUxTest {
    public static void main(String[] args) {
        shapeGlyphsAreDistinct();
        motionGlyphsAndInspectorMapping();
        appearanceCardsAreCatalogBacked();
        pickerIsReadableAndReachableAt640();
        authoringOverlayIsCompactAndNonOverlapping();
        sourceBoundaryIsPure();
    }

    private static void shapeGlyphsAreDistinct() {
        var glyphs = List.of(SkillVfxModel.PrimitiveType.values()).stream()
                .map(SkillVfxVisualUxPresentation::shapeGlyph).toList();
        assert glyphs.size() == 10;
        assert glyphs.stream().allMatch(g -> !g.marks().isEmpty() && !g.label().isBlank() && !g.tooltip().isBlank());
        assert new HashSet<>(glyphs.stream().map(SkillVfxVisualUxPresentation.Glyph::key).toList()).size() == 10;
        assert glyphs.stream().map(SkillVfxVisualUxPresentation.Glyph::key).distinct().count() == 10;
        assert SkillVfxVisualUxPresentation.shapeGlyph(SkillVfxModel.PrimitiveType.SPIRAL).marks().stream()
                .allMatch(mark -> mark instanceof SkillVfxVisualUxPresentation.Mark.Arc);
    }

    private static void motionGlyphsAndInspectorMapping() {
        assert SkillVfxVisualUxPresentation.motionGlyph(MotionMode.STATIC).kind() == SkillVfxVisualUxPresentation.GlyphKind.MOTION;
        assert SkillVfxVisualUxPresentation.motionGlyph(MotionMode.REVEAL).tooltip().contains("始点側");
        assert SkillVfxVisualUxPresentation.motionGlyph(MotionMode.TRAVEL).tooltip().contains("移動");
        assert !SkillVfxVisualUxPresentation.directionGlyph(MotionDirection.FORWARD).key().equals(SkillVfxVisualUxPresentation.directionGlyph(MotionDirection.REVERSE).key());
        assert !SkillVfxVisualUxPresentation.phaseGlyph().marks().isEmpty();
        assert !SkillVfxVisualUxPresentation.trailGlyph().marks().isEmpty();
        for (var easing : MotionEasing.values()) assert !SkillVfxVisualUxPresentation.easingGlyph(easing).marks().isEmpty();
        var primitive = SkillVfxModel.defaults("visual-ux", SkillVfxModel.PrimitiveType.SPIRAL);
        for (String field : List.of("type", "appearance", "motionCategory", "motionMode", "motionDirection", "motionPhase", "motionEasing")) {
            assert SkillVfxVisualUxPresentation.inspectorGlyph(field, primitive) != null : field;
        }
        assert SkillVfxVisualUxPresentation.inspectorGlyph("motionTrail", primitive.withMotion(MotionAuthoringPresentation.canonical(primitive.type(), MotionMode.TRAVEL, primitive.motion()))) != null;
    }

    private static void appearanceCardsAreCatalogBacked() {
        var cards = SkillVfxVisualUxPresentation.appearanceCards();
        assert cards.size() == SkillVfxAuthoring.appearances().size();
        assert cards.stream().map(SkillVfxVisualUxPresentation.CardItem::id).distinct().count() == cards.size();
        assert cards.stream().allMatch(card -> !card.thumbnailKey().isBlank() && !card.glyph().marks().isEmpty() && !card.detail().isBlank());
        assert cards.stream().map(SkillVfxVisualUxPresentation.CardItem::thumbnailKey).distinct().count() == cards.size();
    }

    private static void pickerIsReadableAndReachableAt640() {
        var screen = new SkillVfxVisualUxPresentation.Rect(0, 0, 640, 360);
        var primitiveItems = SkillVfxVisualUxPresentation.primitiveCards();
        var appearanceItems = SkillVfxVisualUxPresentation.appearanceCards();
        var primitivePage = SkillVfxVisualUxPresentation.pickerPage(640, 360, "形を選択", primitiveItems, 0);
        assert primitivePage.withinScreen(screen) && primitivePage.cards().size() == 4;
        assert primitivePage.cards().stream().allMatch(SkillVfxVisualUxPresentation.PickerCard::readable);
        int primitivePages = primitivePage.pages();
        for (int page = 0; page < primitivePages; page++) {
            var current = SkillVfxVisualUxPresentation.pickerPage(640, 360, "形を選択", primitiveItems, page);
            assert current.withinScreen(screen);
        }
        int appearancePages = Math.max(1, (appearanceItems.size() + 3) / 4);
        for (int page = 0; page < appearancePages; page++) {
            var current = SkillVfxVisualUxPresentation.pickerPage(640, 360, "見た目を選択", appearanceItems, page);
            assert current.withinScreen(screen);
        }
    }

    private static void authoringOverlayIsCompactAndNonOverlapping() {
        var primitive = SkillVfxModel.defaults("overlay", SkillVfxModel.PrimitiveType.SPIRAL);
        var readablePrimitive = SkillVfxModel.defaults("overlay-readable", SkillVfxModel.PrimitiveType.BEZIER)
                .withAppearance(SkillVfxModel.Appearance.particle("minecraft:enchanted_hit"));
        var overlay = SkillVfxVisualUxPresentation.authoringOverlay(640, 360, readablePrimitive, SkillVfxDirectAuthoring.MotionHandleTarget.PHASE, "短い状態");
        var screen = new SkillVfxVisualUxPresentation.Rect(0, 0, 640, 360);
        assert overlay.bounds().within(screen) && !overlay.hasOverlaps();
        assert overlay.chips().size() == 4 && overlay.controls().size() == 4;
        assert overlay.directVisible();
        assert overlay.chips().stream().allMatch(SkillVfxVisualUxPresentation.Chip::readable);
        assert overlay.chips().getFirst().label().contains("ベジェ曲線");
        assert overlay.chips().get(1).label().contains("徐々に表示");
        assert overlay.chips().get(2).label().contains("正方向");
        assert overlay.chips().get(3).label().contains("攻撃");
        assert overlay.controls().getFirst().y() >= 68 && overlay.directOperation().y() >= 164 && overlay.feedback().y() >= 188;
        assert !SkillVfxVisualUxPresentation.authoringOverlay(640, 360, primitive.withMotion(new io.github.gyai.projects.client.vfx.MotionSpec(MotionMode.STATIC, MotionDirection.FORWARD, MotionEasing.LINEAR, 0, 0)), SkillVfxDirectAuthoring.MotionHandleTarget.PHASE, "").directVisible();
        var phase = SkillVfxVisualUxPresentation.handleVisual(SkillVfxDirectAuthoring.Handle.Kind.PHASE);
        var trail = SkillVfxVisualUxPresentation.handleVisual(SkillVfxDirectAuthoring.Handle.Kind.TRAIL);
        var shape = SkillVfxVisualUxPresentation.handleVisual(SkillVfxDirectAuthoring.Handle.Kind.POINT);
        assert phase.marker() == SkillVfxVisualUxPresentation.Marker.DIAMOND;
        assert trail.marker() == SkillVfxVisualUxPresentation.Marker.BAR;
        assert shape.marker() == SkillVfxVisualUxPresentation.Marker.CROSS;
        assert phase.color() != trail.color() && trail.color() != shape.color();
        var phaseHandle = new SkillVfxDirectAuthoring.Handle("motion:phase", "開始位置", new SkillVfxDirectAuthoring.Vec(0, 0, 0), SkillVfxDirectAuthoring.Handle.Kind.PHASE);
        var shapeHandle = new SkillVfxDirectAuthoring.Handle("offsetX", "移動 X", new SkillVfxDirectAuthoring.Vec(0, 0, 0), SkillVfxDirectAuthoring.Handle.Kind.AXIS_X);
        assert SkillVfxVisualUxPresentation.hoverContext(phaseHandle).equals("開始位置 をドラッグ");
        assert SkillVfxVisualUxPresentation.hoverContext(shapeHandle).equals("移動 X をドラッグ");
        var hud = SkillVfxWorldPreviewHudLayout.layout(196, value -> value.codePointCount(0, value.length()) * 8,
                "VFX プレビュー", "発動 / 螺旋 / プレイヤー", "P 再生   R 最初から   E 戻る");
        assert hud.lines().size() <= 3 && hud.width() <= 202 && hud.height() <= 42;
    }

    private static void sourceBoundaryIsPure() {
        try {
            Path root = Path.of("").toAbsolutePath();
            String source = Files.readString(root.resolve("devtools/src/main/java/io/github/gyai/projects/devtools/skillvfx/SkillVfxVisualUxPresentation.java"));
            String screen = Files.readString(root.resolve("devtools/src/client/java/io/github/gyai/projects/devtools/skillvfx/ui/SkillVfx3dAuthoringScreen.java"));
            assert !source.contains("net.minecraft") && !source.contains("net.fabricmc");
            assert source.contains("primitiveCards()") && source.contains("appearanceCards()") && source.contains("authoringOverlay");
            assert screen.contains("Mark.Arc") && screen.contains("chipArc") && screen.contains("sweepDegrees");
            assert screen.contains("hoverContext(hover)") && !screen.contains("targetVisual.label()+\" をドラッグ\"");
        } catch (java.io.IOException error) {
            throw new AssertionError(error);
        }
    }
}
