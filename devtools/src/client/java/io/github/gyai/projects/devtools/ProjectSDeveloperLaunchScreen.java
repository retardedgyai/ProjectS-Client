package io.github.gyai.projects.devtools;

import io.github.gyai.projects.minecraft.adapter.MinecraftUiRenderProfile;
import io.github.gyai.projects.minecraft.adapter.MinecraftUiScreenHost;
import io.github.gyai.projects.ui.runtime.UiTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;

public final class ProjectSDeveloperLaunchScreen extends MinecraftUiScreenHost {
    private static final String LOCAL_SERVER = "127.0.0.1:25565";
    private final DeveloperLaunchRoot root;
    private boolean probeStarted;

    public ProjectSDeveloperLaunchScreen() {
        this(new DeveloperLaunchRoot());
    }

    private ProjectSDeveloperLaunchScreen(DeveloperLaunchRoot root) {
        super("ProjectS Developer Client", root,
                UiTheme.custom(io.github.gyai.projects.ui.runtime.UiThemeMode.DARK,
                        141, 215, 208), null);
        this.root = root;
        root.bindActions(this::connectLocal, this::openWorkspace, this::exitClient);
    }

    @Override
    protected MinecraftUiRenderProfile uiRenderProfile() {
        return MinecraftUiRenderProfile.CAELESTIA_SHELL;
    }

    @Override
    protected void init() {
        super.init();
        Minecraft client = Minecraft.getInstance();
        root.setEnvironment(client.getUser().getName(), "26.1.2");
        if (!probeStarted) {
            probeStarted = true;
            probeLocalServer();
        }
    }

    @Override
    protected void onUiLayout(int width, int height) {
        root.layout(width, height);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    private void connectLocal() {
        if (minecraft == null) return;
        ServerData data = new ServerData(
                "ProjectS Local", LOCAL_SERVER, ServerData.Type.OTHER);
        ConnectScreen.startConnecting(this, minecraft,
                ServerAddress.parseString(LOCAL_SERVER), data, false, null);
    }

    private void openWorkspace() {
        ProjectSDevToolsMenuScreen.open(this);
    }

    private void exitClient() {
        if (minecraft != null) minecraft.stop();
    }

    private void probeLocalServer() {
        root.setServerStatus(DeveloperLaunchRoot.ServerStatus.CHECKING);
        CompletableFuture.supplyAsync(() -> {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress("127.0.0.1", 25565), 600);
                return DeveloperLaunchRoot.ServerStatus.READY;
            } catch (Exception ignored) {
                return DeveloperLaunchRoot.ServerStatus.OFFLINE;
            }
        }).thenAccept(result -> {
            Minecraft client = Minecraft.getInstance();
            client.execute(() -> root.setServerStatus(result));
        });
    }
}
