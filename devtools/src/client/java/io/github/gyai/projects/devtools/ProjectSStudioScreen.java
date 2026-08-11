package io.github.gyai.projects.devtools;

import io.github.gyai.projects.devtools.studio.ProjectSStudioWorkspace;
import io.github.gyai.projects.ui.runtime.UiCaptureCancelReason;
import io.github.gyai.projects.devtools.studio.tool.StudioToolId;
import io.github.gyai.projects.devtools.studio.viewport.StudioViewportController;
import io.github.gyai.projects.devtools.studio.viewport.StudioViewportSemanticDelegate;
import io.github.gyai.projects.minecraft.adapter.MinecraftUiInputAdapter;
import io.github.gyai.projects.minecraft.adapter.MinecraftUiScreenHost;
import io.github.gyai.projects.minecraft.adapter.studio.viewport.StudioViewportInputModel;
import io.github.gyai.projects.minecraft.adapter.studio.viewport.StudioViewportInputResult;
import io.github.gyai.projects.minecraft.adapter.studio.viewport.StudioViewportMode;
import io.github.gyai.projects.minecraft.adapter.studio.viewport.StudioViewportEscapeAction;
import io.github.gyai.projects.minecraft.adapter.studio.viewport.StudioViewportInputTarget;
import io.github.gyai.projects.ui.runtime.UiKeyAction;
import io.github.gyai.projects.ui.runtime.UiKeyEvent;
import io.github.gyai.projects.ui.runtime.UiPoint;
import io.github.gyai.projects.ui.runtime.UiPointerEvent;
import io.github.gyai.projects.ui.runtime.UiPointerType;
import io.github.gyai.projects.ui.runtime.UiTheme;
import io.github.gyai.projects.ui.runtime.UiEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.Objects;

/**
 * DevTools-owned ProjectS Studio screen.  The adapter host keeps the world
 * render path transparent and non-pausing; this boundary only routes input
 * and delegates viewport gestures to the existing player/camera semantics.
 */
public final class ProjectSStudioScreen extends MinecraftUiScreenHost {
    private final ProjectSStudioWorkspace workspace;
    private final StudioViewportInputModel viewportInputModel;
    private final StudioViewportController viewportController;
    private final MinecraftUiInputAdapter inputAdapter = new MinecraftUiInputAdapter();
    private String worldIdentity = "";
    private boolean lifecycleCleaned;

    public ProjectSStudioScreen(Screen parent) {
        this(parent, new ProjectSStudioWorkspace());
    }

    private ProjectSStudioScreen(Screen parent, ProjectSStudioWorkspace workspace) {
        super("ProjectS Studio", Objects.requireNonNull(workspace, "workspace").root(),
                UiTheme.light(), () -> Minecraft.getInstance().setScreen(parent));
        this.workspace = workspace;
        viewportInputModel = new StudioViewportInputModel(workspace.viewportBounds());
        viewportController = new StudioViewportController(uiInput(), viewportInputModel,
                new ExistingViewportSemantics());
        workspace.bindInput(uiInput())
                .setThemeChangeListener(this::setUiTheme)
                .setToolActivationListener(this::onToolActivated)
                .setOverlayOpenListener(ignored ->
                        viewportController.cancelPointerCapture(UiCaptureCancelReason.MODAL_OPEN))
                .setStateChangeListener(this::syncViewportInput);
        syncViewportInput();
    }

    public static void open(Screen parent) {
        Minecraft.getInstance().setScreen(new ProjectSStudioScreen(parent));
    }

    public ProjectSStudioWorkspace workspace() {
        return workspace;
    }

    public StudioViewportController viewportController() {
        return viewportController;
    }

    @Override
    protected void onUiLayout(int width, int height) {
        workspace.layout(width, height);
        syncViewportInput();
    }

    @Override
    protected void onUiThemeChanged(UiTheme nextTheme) {
        workspace.setTheme(nextTheme);
    }

    @Override
    public void tick() {
        super.tick();
        String nextWorldIdentity = currentWorldIdentity();
        if (!nextWorldIdentity.equals(worldIdentity)) {
            worldIdentity = nextWorldIdentity;
            workspace.closeThemePopup();
            workspace.closeAssetBrowser();
            viewportController.onWorldChanged(nextWorldIdentity);
            syncViewportInput();
        }
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        route(inputAdapter.move(mouseX, mouseY));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return route(inputAdapter.mouse(event, true)).handled();
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        return route(inputAdapter.drag(event)).handled();
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return route(inputAdapter.mouse(event, false)).handled();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        return route(inputAdapter.scroll(mouseX, mouseY, horizontal, vertical)).handled();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        StudioViewportInputResult result = route(inputAdapter.key(event, UiKeyAction.DOWN));
        if (event.key() == UiKeyEvent.KEY_ESCAPE) {
            return applyEscape(result);
        }
        return result.handled();
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        return route(inputAdapter.key(event, UiKeyAction.UP)).handled();
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return route(inputAdapter.text(event)).handled();
    }

    @Override
    public void onClose() {
        cleanupViewportLifecycle();
        super.onClose();
    }

    @Override
    public void removed() {
        cleanupViewportLifecycle();
        super.removed();
    }

    private StudioViewportInputResult route(UiEvent event) {
        if (event instanceof UiPointerEvent pointer
                && workspace.consumeOutsidePointerDown(pointer)) {
            return new StudioViewportInputResult(true, StudioViewportInputTarget.UI, false,
                    StudioViewportEscapeAction.NONE);
        }
        return viewportController.route(event);
    }

    private boolean applyEscape(StudioViewportInputResult result) {
        return switch (result.escapeAction()) {
            case CANCEL_DRAG, EXIT_TEMPORARY_MODE -> {
                yield true;
            }
            case CLOSE_POPUP -> {
                workspace.closeThemePopup();
                yield true;
            }
            case CLOSE_DRAWER -> {
                workspace.closeDrawer();
                yield true;
            }
            case CLOSE_STUDIO -> {
                onClose();
                yield true;
            }
            case NONE -> result.handled();
        };
    }

    private void syncViewportInput() {
        viewportInputModel.setViewportBounds(workspace.viewportBounds())
                .setUiPanels(workspace.inputPanelBounds())
                .setPopupBounds(workspace.themePopupBounds())
                .setPopupOpen(workspace.popupOpenForInput())
                .setDrawerBounds(workspace.drawerBarrierBounds())
                .setDrawerOpen(workspace.drawerOpenForInput());
    }

    private void onToolActivated(StudioToolId id) {
        if (id == StudioToolId.SELECT || id == StudioToolId.ADD
                || id == StudioToolId.DUPLICATE || id == StudioToolId.DELETE) {
            viewportController.state().exitTemporaryMode();
        } else if (viewportController.state().mode() == StudioViewportMode.UI_MODE) {
            viewportController.state().enterDirectEditMode();
        }
    }

    private void cleanupViewportLifecycle() {
        if (lifecycleCleaned) return;
        lifecycleCleaned = true;
        viewportController.onScreenClosed();
        workspace.onScreenClosed();
    }

    private String currentWorldIdentity() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return "";
        return client.level.dimension().identifier() + "@" + System.identityHashCode(client.level);
    }

    /** Existing world camera seam; no world/player/document object is retained. */
    private static final class ExistingViewportSemantics implements StudioViewportSemanticDelegate {
        @Override
        public boolean beginCameraGesture(UiPoint viewportLocalPoint,
                                          io.github.gyai.projects.ui.runtime.UiModifiers modifiers) {
            return Minecraft.getInstance().player != null;
        }

        @Override
        public void updateCameraGesture(double deltaX, double deltaY) {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.turn((float) deltaX, (float) deltaY);
            }
        }

        @Override
        public void endCameraGesture() { }

        @Override
        public void onViewportSessionCleanup() { }
    }
}
