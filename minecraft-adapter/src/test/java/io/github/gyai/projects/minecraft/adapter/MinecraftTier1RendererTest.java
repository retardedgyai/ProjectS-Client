package io.github.gyai.projects.minecraft.adapter;

import io.github.gyai.projects.ui.runtime.UiColor;
import io.github.gyai.projects.ui.runtime.UiDrawList;
import io.github.gyai.projects.ui.runtime.UiGradientSpan;
import io.github.gyai.projects.ui.runtime.UiRasterSpan;
import io.github.gyai.projects.ui.runtime.UiRect;
import io.github.gyai.projects.ui.runtime.UiRenderCommand;
import io.github.gyai.projects.ui.runtime.UiRoundedRaster;

import java.util.List;

/** Pure Tier 1 shape evidence; it never constructs a Minecraft renderer. */
public final class MinecraftTier1RendererTest {
    public static void main(String[] args) {
        UiRect rect = new UiRect(0, 0, 32, 20);
        List<UiRasterSpan> fill = UiRoundedRaster.fillSpans(rect, 6);
        List<UiGradientSpan> gradientPlan = UiRoundedRaster.gradientSpans(rect, 6);
        List<UiRasterSpan> border = UiRoundedRaster.borderSpans(rect, 6, 1);

        check(!fill.isEmpty(), "rounded fill must contain spans");
        check(gradientPlan.size() == fill.size(), "gradient plan must cover each rounded row once");
        for (int index = 0; index < gradientPlan.size(); index++) {
            check(gradientPlan.get(index).span().equals(fill.get(index)), "gradient span coverage order");
            check(index == 0 || gradientPlan.get(index - 1).span().y() < gradientPlan.get(index).span().y(),
                    "gradient rows must be visited exactly once");
            check(gradientPlan.get(index).amount() >= 0 && gradientPlan.get(index).amount() <= 1,
                    "gradient amount must be normalized");
        }
        check(fill.get(0).left() > 0 && fill.get(0).right() < 32,
                "top fill corner must remain cut");
        check(fill.get(fill.size() - 1).left() > 0 && fill.get(fill.size() - 1).right() < 32,
                "bottom fill corner must remain cut");
        check(border.stream().noneMatch(span -> span.y() == 0 && span.left() == 0 && span.right() == 32),
                "border corner must not become a rectangle");
        check(border.stream().allMatch(span -> span.left() >= 0 && span.right() <= 32
                && span.right() > span.left()), "border spans stay inside the rounded bounds");

        UiDrawList drawList = new UiDrawList();
        drawList.gradient(rect, 6, UiColor.hex("#ffffff"), UiColor.hex("#000000"));
        UiRenderCommand.Gradient gradient = (UiRenderCommand.Gradient) drawList.commands().get(0);
        check(gradient.radius() == 6, "gradient command carries the rounded radius");
        try {
            MinecraftScissorStackTest.run();
        } catch (Exception error) {
            throw new AssertionError(error);
        }
        System.out.println("TIER1_RENDERER_TEST_PASS: fill gradient border share rounded corner spans and one-pass gradient plan");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
