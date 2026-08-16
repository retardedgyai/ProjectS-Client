package io.github.gyai.projects.minecraft.adapter.component;

import io.github.gyai.projects.ui.runtime.IconKey;
import io.github.gyai.projects.ui.runtime.IconSpec;
import io.github.gyai.projects.ui.runtime.UiRenderCommand;
import io.github.gyai.projects.ui.runtime.UiRect;
import io.github.gyai.projects.ui.runtime.UiTheme;
import io.github.gyai.projects.ui.runtime.UiTree;
import io.github.gyai.projects.ui.runtime.UiNode;
import io.github.gyai.projects.ui.runtime.component.GlassPanel;
import io.github.gyai.projects.ui.runtime.component.IconButton;
import io.github.gyai.projects.ui.runtime.component.Separator;
import io.github.gyai.projects.ui.runtime.icon.IconState;
import io.github.gyai.projects.ui.runtime.icon.IconStyle;

/** Non-GUI adapter seam test; no Minecraft screen, widget, or renderer is constructed. */
public final class MinecraftLiquidGlassComponentAdapterTest {
    public static void main(String[] args) {
        UiNode root = new UiNode("root", new UiRect(0, 0, 160, 90));
        GlassPanel panel = new GlassPanel("panel", new UiRect(8, 8, 120, 60)).setTitle("Panel");
        panel.addChild(new Separator("line", new UiRect(8, 32, 100, 1)));
        root.addChild(panel);
        var drawList = new MinecraftLiquidGlassComponentAdapter().build(new UiTree(root), UiTheme.light());
        if (drawList.clipDepth() != 0) throw new AssertionError("draw list clips balanced");
        if (drawList.commands().stream().noneMatch(command -> command instanceof UiRenderCommand.RoundedSurface)) {
            throw new AssertionError("adapter preserves tier 1 surface commands");
        }
        iconStatesReachTheDrawCommand();
        System.out.println("MINECRAFT_COMPONENT_ADAPTER_TEST_PASS: pure tree to draw-list seam");
    }

    private static void iconStatesReachTheDrawCommand() {
        UiTheme theme = UiTheme.dark();
        IconState[] states = {IconState.NORMAL, IconState.HOVERED, IconState.PRESSED,
                IconState.DISABLED, IconState.SELECTED, IconState.FOCUSED};
        for (IconState expected : states) {
            UiNode root = new UiNode("icon-root", new UiRect(0, 0, 80, 80));
            IconButton button = new IconButton("icon-button", new UiRect(10, 10, 32, 32),
                    IconSpec.procedural(IconKey.SETTINGS, "settings"), "Settings");
            switch (expected) {
                case HOVERED -> button.setHovered(true);
                case PRESSED -> button.setPressed(true);
                case DISABLED -> button.setEnabled(false);
                case SELECTED -> button.setSelected(true);
                case FOCUSED -> button.setFocused(true);
                case NORMAL -> { }
            }
            root.addChild(button);
            var drawList = new MinecraftLiquidGlassComponentAdapter().build(new UiTree(root), theme);
            UiRenderCommand.StatefulIcon command = drawList.commands().stream()
                    .filter(candidate -> candidate instanceof UiRenderCommand.StatefulIcon)
                    .map(candidate -> (UiRenderCommand.StatefulIcon) candidate)
                    .findFirst().orElseThrow(() -> new AssertionError("missing stateful icon command"));
            if (command.state() != expected) throw new AssertionError("icon state mapping: " + expected);
            var expectedTint = IconStyle.resolve(button.icon(), expected, theme).resolvedTint();
            if (!command.tint().equals(expectedTint)) throw new AssertionError("icon tint mapping: " + expected);
        }
    }
}
