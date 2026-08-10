package io.github.gyai.projects.devtools;

import io.github.gyai.projects.client.beta.BetaDisplayDocument;
import io.github.gyai.projects.devtools.editor.DockLayout;
import io.github.gyai.projects.devtools.editor.EditorPanel;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public final class DevToolsBoundaryTest {
    public static void main(String[] args) throws Exception {
        try (InputStream resource = DevToolsBoundaryTest.class.getClassLoader().getResourceAsStream("fabric.mod.json")) {
            String json = new String(resource.readAllBytes(), StandardCharsets.UTF_8); assert json.contains("projects_devtools") && json.contains("projects_client");
        }
        assert BetaMobEditorV2ViewModel.panel(new BetaDisplayDocument(1, BetaDisplayDocument.Status.CONFLICT, "", Map.of(), List.of())).revisionConflict();
        dockBoundsHonorMinimaAndDegradeSafely();
        DockLayout layout = new DockLayout(new EditorPanel("tree", "Tree", 140, 100, true),new EditorPanel("inspector", "Inspector", 180, 120, true),new EditorPanel("preview", "Preview", 160, 90, true));
        assert layout.orderedPanels().stream().map(EditorPanel::id).toList().equals(List.of("tree","inspector","preview"));
        layout.split(DockLayout.Axis.HORIZONTAL); layout.resize(10, 700); assert layout.divider() >= 140; assert layout.bounds(0,0,700,400).getFirst().bounds().width() == layout.divider();
        layout.split(DockLayout.Axis.VERTICAL); layout.resize(999, 500); assert layout.divider() <= 380; assert layout.bounds(0,0,700,500).getFirst().bounds().height() == layout.divider();
        layout.setVisible("inspector", false); assert layout.bounds(0,0,700,500).size()==2; layout.collapse(true); assert layout.bounds(0,0,700,500).size()==1; layout.reset(); assert !layout.collapsed() && layout.axis()==DockLayout.Axis.HORIZONTAL && layout.panels().values().stream().allMatch(EditorPanel::visible);
        assert Class.forName("io.github.gyai.projects.client.ProjectSClient") != null;
        assert !io.github.gyai.projects.client.beta.BetaClientRuntime.isCapabilityEnabled(io.github.gyai.projects.client.beta.BetaProtocol.Capability.MOB_EDITOR_V2);
        io.github.gyai.projects.client.beta.BetaClientRuntime.enableCapability(io.github.gyai.projects.client.beta.BetaProtocol.Capability.MOB_EDITOR_V2);
        assert io.github.gyai.projects.client.beta.BetaClientRuntime.isCapabilityEnabled(io.github.gyai.projects.client.beta.BetaProtocol.Capability.MOB_EDITOR_V2);
        String initializer = java.nio.file.Files.readString(java.nio.file.Path.of("devtools/src/client/java/io/github/gyai/projects/devtools/ProjectSDevTools.java"));
        assert initializer.contains("enableCapability(BetaProtocol.Capability.MOB_EDITOR_V2)");
        String editor = java.nio.file.Files.readString(java.nio.file.Path.of("devtools/src/client/java/io/github/gyai/projects/devtools/ProjectSEditorScreen.java"));
        assert editor.contains("paintChrome(graphics);super.extractRenderState");
        assert !editor.substring(editor.indexOf("extractThemedForeground")).contains("graphics.fill(0,0,width,height");
    }
    private static void dockBoundsHonorMinimaAndDegradeSafely() {
        DockLayout layout = new DockLayout(new EditorPanel("tree","Tree",140,100,true),new EditorPanel("inspector","Inspector",180,100,true),new EditorPanel("preview","Preview",180,120,true),new EditorPanel("timeline","Timeline",180,80,true));
        layout.split(DockLayout.Axis.HORIZONTAL);
        assertInBounds(layout.bounds(0,0,480,306),0,0,480,306); // 640x360 editor content: two pixels short of trailing stack minima.
        List<DockLayout.PanelBounds> horizontalExact=layout.bounds(0,0,324,308); assertInBounds(horizontalExact,0,0,324,308); assertMinima(horizontalExact);
        assertInBounds(layout.bounds(0,0,323,307),0,0,323,307); assertInBounds(layout.bounds(0,0,3,2),0,0,3,2); assertInBounds(layout.bounds(0,0,0,0),0,0,0,0);
        layout.split(DockLayout.Axis.VERTICAL);
        List<DockLayout.PanelBounds> verticalExact=layout.bounds(0,0,548,224); assertInBounds(verticalExact,0,0,548,224); assertMinima(verticalExact);
        assertInBounds(layout.bounds(0,0,547,223),0,0,547,223); assertInBounds(layout.bounds(0,0,1,0),0,0,1,0);
    }
    private static void assertMinima(List<DockLayout.PanelBounds> panels) { for (DockLayout.PanelBounds placed : panels) { assert placed.bounds().width() >= placed.panel().minWidth(); assert placed.bounds().height() >= placed.panel().minHeight(); } }
    private static void assertInBounds(List<DockLayout.PanelBounds> panels,int x,int y,int width,int height) { for (DockLayout.PanelBounds placed : panels) { DockLayout.Rect box=placed.bounds(); assert box.width()>=0 && box.height()>=0; assert box.x()>=x && box.y()>=y; assert (long)box.x()+box.width() <= (long)x+width; assert (long)box.y()+box.height() <= (long)y+height; } }
}
