package io.github.gyai.projects.client;

import com.mojang.logging.LogUtils;
import io.github.gyai.projects.client.shell.ClientShellModel;
import io.github.gyai.projects.client.shell.ClientShellNarration;
import io.github.gyai.projects.client.shell.SampleClientShellDataSource;
import io.github.gyai.projects.client.menu.ProjectSMenuExtension;
import io.github.gyai.projects.client.menu.ProjectSMenuExtensions;
import io.github.gyai.projects.minecraft.adapter.MinecraftUiRenderProfile;
import io.github.gyai.projects.minecraft.adapter.MinecraftUiRuntimeResources;
import io.github.gyai.projects.minecraft.adapter.MinecraftUiScreenHost;
import io.github.gyai.projects.ui.runtime.UiAccessibilityMetadata;
import io.github.gyai.projects.ui.runtime.UiKeyEvent;
import io.github.gyai.projects.ui.runtime.UiTheme;
import io.github.gyai.projects.ui.runtime.UiNode;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;

import java.util.Optional;
import org.slf4j.Logger;

/** ProjectS Client Shell; the legacy parent constructor remains the inventory entry signature. */
public final class ProjectSMenuScreen extends MinecraftUiScreenHost {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Screen parent;
    private final ClientShellRoot shell;
    private final ClientShellNarration narration = new ClientShellNarration();

    public ProjectSMenuScreen(Screen parent) {
        this(parent, createShell());
    }

    static void openIfReady(Minecraft client, Screen parent) {
        if (client == null) return;
        ProjectSMenuExtension extension = ProjectSMenuExtensions.entries().stream()
                .filter(entry -> entry.enabled().getAsBoolean())
                .findFirst()
                .orElse(null);
        if (extension != null) {
            extension.action().accept(parent);
            return;
        }
        MinecraftUiRuntimeResources resources = MinecraftUiRuntimeResources.currentOrNull();
        if (resources == null) {
            LOGGER.warn("ProjectS menu did not open: UI runtime resources are unavailable");
            return;
        }
        if (!resources.clientShellVisualsReady()) {
            LOGGER.warn("ProjectS menu did not open: {}",
                    resources.clientShellReadinessReport());
            return;
        }
        client.setScreen(new ProjectSMenuScreen(parent));
    }

    private ProjectSMenuScreen(Screen parent, ClientShellRoot shell) {
        super("ProjectS Client Hub", shell, UiTheme.dark(), null);
        this.parent = parent;
        this.shell = shell;
        shell.bindFocusRestoration(
                () -> uiInput().focus().current().map(UiNode::id).orElse(null),
                this::restoreFocusById);
        shell.bindFocusReset(() -> {
            uiInput().focus().clearFocus();
            uiInput().focus().traverseForward();
        });
        shell.bindAccessibilityChanged(this::consumeAccessibility);
    }

    private static ClientShellRoot createShell() {
        ClientShellModel model = new ClientShellModel(new SampleClientShellDataSource());
        return new ClientShellRoot(model, shellVisualsReady());
    }

    private static boolean shellVisualsReady() {
        try {
            MinecraftUiRuntimeResources resources = MinecraftUiRuntimeResources.currentOrNull();
            return resources != null && resources.clientShellVisualsReady();
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    @Override
    public MinecraftUiRenderProfile uiRenderProfile() {
        return MinecraftUiRenderProfile.CAELESTIA_SHELL;
    }

    @Override
    protected void onUiLayout(int width, int height) {
        shell.layout(width, height);
        if (shell.layoutSnapshot() != null && uiInput().focus().current().isEmpty()) {
            uiInput().focus().traverseForward();
        }
        consumeAccessibility();
    }

    @Override
    public void tick() {
        super.tick();
        shell.tick(uiTimeMillis());
        consumeAccessibility();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                   float tickProgress) {
        super.extractRenderState(graphics, mouseX, mouseY, tickProgress);
        consumeAccessibility();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event != null && event.key() == UiKeyEvent.KEY_ESCAPE) {
            if (!shell.model().state().isHome()) {
                shell.dispatch(io.github.gyai.projects.client.shell.ClientShellAction.escapeToHome());
            } else {
                onClose();
            }
            return true;
        }
        return super.keyPressed(event);
    }

    private void restoreFocusById(String id) {
        if (id == null || id.isBlank()) return;
        findById(uiTree().root(), id).ifPresent(node -> uiInput().focus().requestFocus(node));
    }

    private void consumeAccessibility() {
        if (!shell.visualsReadyForChild()) return;
        UiAccessibilityMetadata metadata = uiTree().root().accessibility();
        Optional<UiNode> focused = uiInput().focus().current();
        if (focused.isPresent() && focused.get().accessibility().focused()) {
            String focusLabel = focused.get().accessibility().label();
            if (!focusLabel.isBlank()) {
                metadata = metadata.withValue(metadata.value() + ". Focused: " + focusLabel);
            }
        }
        narration.offer(metadata, uiTimeMillis()).ifPresent(this::speak);
        narration.flush(uiTimeMillis()).ifPresent(this::speak);
    }

    private void speak(String message) {
        try {
            if (minecraft != null && minecraft.getNarrator() != null) {
                minecraft.getNarrator().saySystemNow(net.minecraft.network.chat.Component.literal(message));
            }
        } catch (RuntimeException ignored) {
            // Narration is best effort; the pure metadata and focus contract remains intact.
        }
    }

    private static Optional<UiNode> findById(UiNode node, String id) {
        if (node.id().equals(id)) return Optional.of(node);
        for (UiNode child : node.children()) {
            Optional<UiNode> found = findById(child, id);
            if (found.isPresent()) return found;
        }
        return Optional.empty();
    }

    @Override
    public void onClose() {
        uiInput().onScreenClosed();
        if (minecraft == null || parent == null) super.onClose();
        else minecraft.setScreen(parent);
    }
}
