package io.github.gyai.projects.devtools.skillvfx;

import io.github.gyai.projects.client.vfx.MotionDirection;
import io.github.gyai.projects.client.vfx.MotionEasing;
import io.github.gyai.projects.client.vfx.MotionMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Pure visual vocabulary and bounded layout for the VFX editor.
 *
 * <p>This class contains no Minecraft or Fabric types.  It describes small
 * schematic marks which the client screen may translate to pixels, so the
 * picker, inspector and 3D status panel keep one visual language without
 * creating another geometry or Motion implementation.</p>
 */
public final class SkillVfxVisualUxPresentation {
    public enum GlyphKind { SHAPE, MOTION, DIRECTION, PHASE, TRAIL, EASING, APPEARANCE, HANDLE }

    public sealed interface Mark permits Mark.Segment, Mark.Dot, Mark.Box, Mark.Diamond, Mark.Arc {
        record Segment(double x1, double y1, double x2, double y2) implements Mark { }
        record Dot(double x, double y, double radius, boolean filled) implements Mark { }
        record Box(double left, double top, double right, double bottom, boolean filled) implements Mark { }
        record Diamond(double x, double y, double radius, boolean filled) implements Mark { }
        record Arc(double centerX, double centerY, double radius, double startDegrees, double sweepDegrees) implements Mark { }
    }

    public record Glyph(String key, String label, String tooltip, GlyphKind kind, int color, List<Mark> marks) {
        public Glyph {
            if (key == null || key.isBlank() || label == null || label.isBlank() || tooltip == null || tooltip.isBlank()) {
                throw new IllegalArgumentException("glyph text");
            }
            Objects.requireNonNull(kind);
            Objects.requireNonNull(marks);
            if (marks.isEmpty()) throw new IllegalArgumentException("glyph marks");
            marks = List.copyOf(marks);
        }
    }

    public record CardItem(String id, String label, String detail, String tooltip, Glyph glyph, String thumbnailKey) {
        public CardItem {
            if (id == null || id.isBlank() || label == null || label.isBlank() || detail == null || detail.isBlank()
                    || tooltip == null || tooltip.isBlank() || thumbnailKey == null || thumbnailKey.isBlank()) {
                throw new IllegalArgumentException("card text");
            }
            Objects.requireNonNull(glyph);
        }
    }

    public record Rect(int x, int y, int width, int height) {
        public Rect {
            if (width < 1 || height < 1) throw new IllegalArgumentException("rect");
        }
        public boolean within(Rect outer) {
            return x >= outer.x && y >= outer.y && x + width <= outer.x + outer.width && y + height <= outer.y + outer.height;
        }
        public boolean overlaps(Rect other) {
            return x < other.x + other.width && x + width > other.x && y < other.y + other.height && y + height > other.y;
        }
    }

    public record PickerCard(CardItem item, Rect bounds, Rect preview, Rect action) {
        public PickerCard {
            Objects.requireNonNull(item);
            if (!preview.within(bounds) || !action.within(bounds)) throw new IllegalArgumentException("picker card bounds");
        }
        public boolean readable() { return preview.width() >= 24 && preview.height() >= 24 && action.width() >= 80 && bounds.width() >= 150; }
    }

    public record PickerPage(Rect bounds, String title, List<PickerCard> cards, Rect previous, Rect next, Rect cancel,
                             int page, int pages) {
        public PickerPage {
            if (title == null || title.isBlank() || page < 0 || pages < 1 || page >= pages) throw new IllegalArgumentException("picker page");
            cards = List.copyOf(cards);
            if (!previous.within(bounds) || !next.within(bounds) || !cancel.within(bounds)) throw new IllegalArgumentException("picker controls");
        }
        public boolean withinScreen(Rect screen) {
            return bounds.within(screen) && previous.within(screen) && next.within(screen) && cancel.within(screen)
                    && cards.stream().allMatch(card -> card.bounds().within(bounds) && card.preview().within(screen)
                    && card.action().within(screen) && card.readable());
        }
    }

    public record Chip(String id, String label, Rect bounds, Glyph glyph, boolean active) {
        public Chip {
            if (id == null || id.isBlank() || label == null || label.isBlank()) throw new IllegalArgumentException("chip");
            Objects.requireNonNull(bounds);
            Objects.requireNonNull(glyph);
        }
        public boolean readable() { return bounds.width() >= 96 && bounds.height() >= 16; }
    }

    public record AuthoringOverlay(Rect bounds, Rect title, List<Chip> chips, List<Rect> controls, Rect directOperation,
                                   Rect feedback, boolean directVisible) {
        public AuthoringOverlay {
            Objects.requireNonNull(title);
            chips = List.copyOf(chips);
            controls = List.copyOf(controls);
            if (!title.within(bounds) || !directOperation.within(bounds) || !feedback.within(bounds)
                    || chips.stream().anyMatch(chip -> !chip.bounds().within(bounds))
                    || controls.stream().anyMatch(control -> !control.within(bounds))) {
                throw new IllegalArgumentException("authoring overlay bounds");
            }
        }
        public boolean hasOverlaps() {
            ArrayList<Rect> all = new ArrayList<>();
            all.add(title);
            all.addAll(chips.stream().map(Chip::bounds).toList());
            all.addAll(controls);
            all.add(directOperation);
            all.add(feedback);
            for (int i = 0; i < all.size(); i++) for (int j = i + 1; j < all.size(); j++) if (all.get(i).overlaps(all.get(j))) return true;
            return false;
        }
    }

    public enum Marker { CROSS, DIAMOND, BAR }
    public record DirectHandleVisual(String id, String label, String tooltip, int color, Marker marker) {
        public DirectHandleVisual {
            if (id == null || id.isBlank() || label == null || label.isBlank() || tooltip == null || tooltip.isBlank()) throw new IllegalArgumentException("handle visual");
            Objects.requireNonNull(marker);
        }
    }

    private static final int WHITE = 0xFFE7EDF2;
    private static final int CYAN = 0xFF52D6F2;
    private static final int ORANGE = 0xFFFFA34A;
    private static final int BLUE = 0xFF86B7FF;
    private static final int GREEN = 0xFF8AD7A0;
    private static final int FLAME = 0xFFFF7B45;
    private static final int SOUL = 0xFF62C9D7;

    private SkillVfxVisualUxPresentation() { }

    public static Glyph shapeGlyph(SkillVfxModel.PrimitiveType type) {
        Objects.requireNonNull(type);
        return switch (type) {
            case POINT -> glyph("shape:point", "点", "一点の印", GlyphKind.SHAPE, CYAN, new Mark.Dot(.5, .5, .16, true));
            case LINE -> glyph("shape:line", "直線", "始点と終点を結ぶ線", GlyphKind.SHAPE, CYAN,
                    new Mark.Segment(.16, .72, .84, .28), new Mark.Dot(.16, .72, .09, true), new Mark.Dot(.84, .28, .09, true));
            case ARC -> glyph("shape:arc", "円弧", "円周の一部", GlyphKind.SHAPE, CYAN,
                    new Mark.Arc(.5, .62, .32, 200, 140), new Mark.Dot(.2, .46, .07, true), new Mark.Dot(.8, .46, .07, true));
            case CIRCLE -> glyph("shape:circle", "円", "閉じた円形", GlyphKind.SHAPE, CYAN,
                    new Mark.Arc(.5, .5, .36, 0, 360), new Mark.Dot(.5, .14, .07, true));
            case CONE -> glyph("shape:cone", "円錐", "前方へ広がる形", GlyphKind.SHAPE, CYAN,
                    new Mark.Segment(.23, .22, .23, .78), new Mark.Segment(.23, .22, .82, .5), new Mark.Segment(.23, .78, .82, .5), new Mark.Arc(.82, .5, .16, 90, 180));
            case SPIRAL -> glyph("shape:spiral", "螺旋", "回転する軌道", GlyphKind.SHAPE, CYAN,
                    new Mark.Arc(.5, .5, .12, 0, 260), new Mark.Arc(.5, .5, .22, 80, 260), new Mark.Arc(.5, .5, .32, 160, 260));
            case SPHERE -> glyph("shape:sphere", "球", "球面の形", GlyphKind.SHAPE, CYAN,
                    new Mark.Arc(.5, .5, .34, 0, 360), new Mark.Arc(.5, .5, .18, 0, 360), new Mark.Segment(.16, .5, .84, .5));
            case WAVE -> glyph("shape:wave", "波", "前方へ進む波形", GlyphKind.SHAPE, CYAN,
                    new Mark.Segment(.12, .56, .28, .32), new Mark.Segment(.28, .32, .44, .68), new Mark.Segment(.44, .68, .60, .32), new Mark.Segment(.60, .32, .76, .68), new Mark.Segment(.76, .68, .88, .5));
            case BEZIER -> glyph("shape:bezier", "ベジェ曲線", "制御点を通る曲線", GlyphKind.SHAPE, CYAN,
                    new Mark.Segment(.16, .72, .36, .22), new Mark.Segment(.36, .22, .64, .22), new Mark.Segment(.64, .22, .84, .72), new Mark.Dot(.16, .72, .07, true), new Mark.Dot(.5, .22, .07, true), new Mark.Dot(.84, .72, .07, true));
            case BURST -> glyph("shape:burst", "放射", "中心から放射", GlyphKind.SHAPE, CYAN,
                    new Mark.Segment(.5, .5, .5, .12), new Mark.Segment(.5, .5, .82, .3), new Mark.Segment(.5, .5, .86, .62), new Mark.Segment(.5, .5, .68, .86), new Mark.Segment(.5, .5, .28, .86), new Mark.Segment(.5, .5, .14, .62), new Mark.Segment(.5, .5, .18, .3));
        };
    }

    public static Glyph motionGlyph(MotionMode mode) {
        Objects.requireNonNull(mode);
        return switch (mode) {
            case STATIC -> glyph("motion:static", "静止", "形全体を静止表示", GlyphKind.MOTION, WHITE,
                    new Mark.Box(.2, .2, .8, .8, false), new Mark.Segment(.35, .5, .65, .5));
            case REVEAL -> glyph("motion:reveal", "徐々に表示", "始点側から順番に表示", GlyphKind.MOTION, BLUE,
                    new Mark.Segment(.16, .65, .55, .35), new Mark.Segment(.62, .3, .82, .18), new Mark.Dot(.55, .35, .1, true));
            case TRAVEL -> glyph("motion:travel", "軌道移動", "形に沿って表示位置を移動", GlyphKind.MOTION, ORANGE,
                    new Mark.Segment(.15, .5, .85, .5), new Mark.Segment(.3, .4, .15, .5), new Mark.Segment(.3, .6, .15, .5), new Mark.Box(.55, .4, .7, .6, true));
        };
    }

    public static Glyph directionGlyph(MotionDirection direction) {
        Objects.requireNonNull(direction);
        boolean forward = direction == MotionDirection.FORWARD;
        double start = forward ? .2 : .8, end = forward ? .8 : .2;
        double tip = forward ? .8 : .2;
        return glyph("direction:" + direction.name().toLowerCase(), forward ? "正方向" : "逆方向", "軌道をどちら向きに進むか", GlyphKind.DIRECTION, WHITE,
                new Mark.Segment(start, .5, end, .5), new Mark.Segment(tip, .5, tip + (forward ? -.18 : .18), .34), new Mark.Segment(tip, .5, tip + (forward ? -.18 : .18), .66));
    }

    public static Glyph phaseGlyph() {
        return glyph("phase", "開始位置", "再生開始時の位置", GlyphKind.PHASE, CYAN,
                new Mark.Segment(.16, .62, .84, .38), new Mark.Diamond(.32, .56, .12, true));
    }

    public static Glyph trailGlyph() {
        return glyph("trail", "軌跡の長さ", "移動する光の後ろに残る範囲", GlyphKind.TRAIL, ORANGE,
                new Mark.Segment(.2, .5, .82, .5), new Mark.Box(.28, .38, .55, .62, true), new Mark.Diamond(.82, .5, .1, true));
    }

    public static Glyph easingGlyph(MotionEasing easing) {
        Objects.requireNonNull(easing);
        List<Mark> marks = switch (easing) {
            case LINEAR -> List.of(new Mark.Segment(.16, .78, .84, .22));
            case EASE_IN -> List.of(new Mark.Segment(.16, .78, .35, .74), new Mark.Segment(.35, .74, .84, .22));
            case EASE_OUT -> List.of(new Mark.Segment(.16, .78, .65, .26), new Mark.Segment(.65, .26, .84, .22));
            case EASE_IN_OUT -> List.of(new Mark.Segment(.16, .78, .42, .62), new Mark.Segment(.42, .62, .58, .38), new Mark.Segment(.58, .38, .84, .22));
        };
        return glyph("easing:" + easing.name().toLowerCase(), MotionAuthoringPresentation.label(easing), "動きの速さの変化", GlyphKind.EASING, GREEN, marks.toArray(Mark[]::new));
    }

    public static AppearancePreview appearancePreview(SkillVfxModel.Appearance appearance) {
        Objects.requireNonNull(appearance);
        String id = appearance.id();
        int color = switch (id) {
            case "projects:debug_quad" -> WHITE;
            case "minecraft:flame" -> FLAME;
            case "minecraft:soul", "minecraft:soul_fire_flame" -> SOUL;
            case "minecraft:cloud", "minecraft:ash" -> 0xFFB9C4D0;
            case "minecraft:crit", "minecraft:enchanted_hit" -> 0xFFFFE27A;
            case "minecraft:end_rod" -> 0xFFD8F3FF;
            case "minecraft:firework" -> 0xFFE58BFF;
            default -> 0xFFC7D7FF;
        };
        String motif = switch (id) {
            case "projects:debug_quad" -> "四角";
            case "minecraft:flame" -> "炎";
            case "minecraft:soul", "minecraft:soul_fire_flame" -> "魂";
            case "minecraft:cloud" -> "雲";
            case "minecraft:ash" -> "灰";
            case "minecraft:crit" -> "閃光";
            case "minecraft:enchanted_hit" -> "攻撃";
            case "minecraft:end_rod" -> "光";
            case "minecraft:firework" -> "花火";
            default -> "粒子";
        };
        List<Mark> marks = appearance.kind().name().equals("DEBUG_QUAD")
                ? List.of(new Mark.Box(.18, .18, .82, .82, false), new Mark.Segment(.28, .28, .72, .72))
                : List.of(new Mark.Dot(.5, .5, .18, true), new Mark.Dot(.33, .36, .07, true), new Mark.Dot(.68, .3, .06, true), new Mark.Arc(.5, .5, .31, 210, 220));
        Glyph glyph = glyph("appearance:" + id, motif, "見た目: " + SkillVfxAuthoring.particleLabel(id), GlyphKind.APPEARANCE, color, marks.toArray(Mark[]::new));
        return new AppearancePreview(glyph, "projects:appearance/" + id, motif, color);
    }

    public record AppearancePreview(Glyph glyph, String thumbnailKey, String motif, int color) {
        public AppearancePreview { Objects.requireNonNull(glyph); }
    }

    public static List<CardItem> primitiveCards() {
        return SkillVfxAuthoring.primitiveChoices().stream().map(choice -> new CardItem(
                choice.type().name(), choice.label(), primitiveDetail(choice.type()), "形: " + choice.description(), shapeGlyph(choice.type()), "projects:shape/" + choice.type().name().toLowerCase())).toList();
    }

    public static List<CardItem> appearanceCards() {
        return SkillVfxAuthoring.appearances().stream().map(choice -> {
            AppearancePreview preview = appearancePreview(choice.appearance());
            return new CardItem(choice.appearance().id(), choice.label(), preview.motif(), "見た目: " + choice.label(), preview.glyph(), preview.thumbnailKey());
        }).toList();
    }

    public static PickerPage pickerPage(int screenWidth, int screenHeight, String title, List<CardItem> items, int requestedPage) {
        Rect screen = new Rect(0, 0, Math.max(1, screenWidth), Math.max(1, screenHeight));
        int width = Math.min(500, Math.max(320, screen.width() - 16));
        width = Math.min(width, screen.width() - 16);
        int height = Math.min(294, Math.max(1, screen.height() - 56));
        Rect bounds = new Rect(8, 56, Math.max(1, width), height);
        int pages = Math.max(1, (items.size() + 3) / 4);
        int page = Math.clamp(requestedPage, 0, pages - 1);
        int cardWidth = Math.max(1, (bounds.width() - 12) / 2);
        int cardHeight = Math.min(94, Math.max(58, (bounds.height() - 54) / 2));
        ArrayList<PickerCard> cards = new ArrayList<>();
        int first = page * 4;
        for (int index = first; index < Math.min(first + 4, items.size()); index++) {
            int slot = index - first;
            int column = slot % 2;
            int row = slot / 2;
            int x = bounds.x() + column * (cardWidth + 8);
            int y = bounds.y() + 24 + row * (cardHeight + 10);
            Rect cardBounds = new Rect(x, y, cardWidth, cardHeight);
            Rect preview = new Rect(x + 6, y + 8, Math.min(42, cardWidth - 12), Math.min(42, cardHeight - 28));
            Rect action = new Rect(x + 6, y + cardHeight - 20, Math.max(80, cardWidth - 12), 16);
            cards.add(new PickerCard(items.get(index), cardBounds, preview, action));
        }
        int controlsY = bounds.y() + bounds.height() - 22;
        return new PickerPage(bounds, title, cards, new Rect(bounds.x(), controlsY, 52, 18), new Rect(bounds.x() + 56, controlsY, 52, 18), new Rect(bounds.x() + 112, controlsY, 72, 18), page, pages);
    }

    public static Glyph inspectorGlyph(String field, SkillVfxModel.Primitive primitive) {
        if (field == null || primitive == null) return null;
        return switch (field) {
            case "type", "shape" -> shapeGlyph(primitive.type());
            case "appearance" -> appearancePreview(primitive.appearance()).glyph();
            case "motionCategory", "motionMode" -> motionGlyph(primitive.motion().mode());
            case "motionDirection" -> directionGlyph(primitive.motion().direction());
            case "motionPhase" -> phaseGlyph();
            case "motionTrail" -> trailGlyph();
            case "motionEasing" -> easingGlyph(primitive.motion().easing());
            default -> null;
        };
    }

    public static AuthoringOverlay authoringOverlay(int viewportWidth, int viewportHeight, SkillVfxModel.Primitive primitive,
                                                    SkillVfxDirectAuthoring.MotionHandleTarget target, String feedback) {
        int panelWidth = Math.min(262, Math.max(176, viewportWidth - 16));
        Rect bounds = new Rect(5, 5, panelWidth + 3, 208);
        Rect title = new Rect(8, 8, panelWidth, 12);
        int chipWidth = Math.max(72, (panelWidth - 12) / 2);
        ArrayList<Chip> chips = new ArrayList<>();
        SkillVfxModel.PrimitiveType type = primitive == null ? SkillVfxModel.PrimitiveType.POINT : primitive.type();
        MotionMode mode = primitive == null ? MotionMode.STATIC : primitive.motion().mode();
        MotionDirection direction = primitive == null ? MotionDirection.FORWARD : primitive.motion().direction();
        String appearance = primitive == null ? "見た目" : appearancePreview(primitive.appearance()).motif();
        chips.add(new Chip("shape", "形:" + SkillVfxDisplay.primitive(type), new Rect(8, 24, chipWidth, 18), shapeGlyph(type), false));
        chips.add(new Chip("motion", "動:" + MotionAuthoringPresentation.label(mode), new Rect(8 + chipWidth + 4, 24, chipWidth, 18), motionGlyph(mode), false));
        chips.add(new Chip("direction", "向:" + MotionAuthoringPresentation.label(direction), new Rect(8, 44, chipWidth, 18), directionGlyph(direction), false));
        chips.add(new Chip("appearance", "見:" + appearance, new Rect(8 + chipWidth + 4, 44, chipWidth, 18), primitive == null ? motionGlyph(MotionMode.STATIC) : appearancePreview(primitive.appearance()).glyph(), false));
        List<Rect> controls = List.of(new Rect(8, 68, Math.min(252, panelWidth), 20), new Rect(8, 92, Math.min(252, panelWidth), 20), new Rect(8, 116, Math.min(252, panelWidth), 20), new Rect(8, 140, Math.min(252, panelWidth), 20));
        Rect direct = new Rect(8, 164, Math.min(152, panelWidth), 20);
        Rect help = new Rect(8, 188, panelWidth, 12);
        boolean directVisible = primitive != null && MotionAuthoringPresentation.motionHandleControl(viewportWidth, primitive, target).visible();
        return new AuthoringOverlay(bounds, title, chips, controls, direct, help, directVisible);
    }

    public static DirectHandleVisual handleVisual(SkillVfxDirectAuthoring.Handle.Kind kind) {
        Objects.requireNonNull(kind);
        return switch (kind) {
            case PHASE -> new DirectHandleVisual("phase", "開始位置", "シアンの印をドラッグ", CYAN, Marker.DIAMOND);
            case TRAIL -> new DirectHandleVisual("trail", "軌跡", "オレンジの端をドラッグ", ORANGE, Marker.BAR);
            default -> new DirectHandleVisual("shape", "形", "形のハンドルをドラッグ", 0xFFD0D8E0, Marker.CROSS);
        };
    }

    /** Contextual hover text uses the hovered handle, while the direct target remains a separate chip. */
    public static String hoverContext(SkillVfxDirectAuthoring.Handle handle) {
        if (handle == null) return "";
        DirectHandleVisual visual = handleVisual(handle.kind());
        String label = handle.kind() == SkillVfxDirectAuthoring.Handle.Kind.PHASE
                || handle.kind() == SkillVfxDirectAuthoring.Handle.Kind.TRAIL ? visual.label() : handle.label();
        return label + " をドラッグ";
    }

    private static Glyph glyph(String key, String label, String tooltip, GlyphKind kind, int color, Mark... marks) {
        return new Glyph(key, label, tooltip, kind, color, List.of(marks));
    }

    private static String primitiveDetail(SkillVfxModel.PrimitiveType type) {
        return switch (type) {
            case POINT -> "一点の印";
            case LINE -> "始点と終点";
            case ARC -> "円周の一部";
            case CIRCLE -> "閉じた円形";
            case CONE -> "前方へ広がる";
            case SPIRAL -> "回転する軌道";
            case SPHERE -> "球面の形";
            case WAVE -> "進む波形";
            case BEZIER -> "制御点の曲線";
            case BURST -> "中心から放射";
        };
    }
}
