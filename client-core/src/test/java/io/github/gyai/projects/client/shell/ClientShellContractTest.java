package io.github.gyai.projects.client.shell;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Source contract guards for the formal shell boundary and visual mapping. */
public final class ClientShellContractTest {
    public static void main(String[] args) throws Exception {
        Path project = locateProjectRoot();
        Path shellSource = project.resolve("client-core/src/client/java/io/github/gyai/projects/client/ProjectSMenuScreen.java");
        Path rootSource = project.resolve("client-core/src/client/java/io/github/gyai/projects/client/ClientShellRoot.java");
        Path managerSource = project.resolve("client-core/src/client/java/io/github/gyai/projects/client/ProjectSScreenManager.java");
        String screen = Files.readString(shellSource);
        String root = Files.readString(rootSource);
        String manager = Files.readString(managerSource);
        String shellCatalog = Files.readString(project.resolve(
                "ui-runtime/src/main/java/io/github/gyai/projects/ui/runtime/icon/ShellIconCatalog.java"));
        String palette = Files.readString(project.resolve(
                "client-core/src/main/java/io/github/gyai/projects/client/shell/ClientShellPalette.java"));
        require(screen, "extends MinecraftUiScreenHost");
        require(screen, "ProjectSMenuScreen(Screen parent)");
        require(screen, "MinecraftUiRuntimeResources.currentOrNull()");
        require(screen, "clientShellVisualsReady()");
        require(manager, "ProjectSMenuScreen.openIfReady(client, screen)");
        require(root, "UiDrawList");
        require(root, "UiNode");
        String shellIconSources = root + shellCatalog;
        for (String icon : List.of("BRAND", "HOME", "LIBRARY", "SLIDERS", "CHEVRON_RIGHT",
                "CHEVRON_DOWN", "ARROW_RIGHT", "PLAY", "MONITOR", "SERVER", "CHECK", "LOADER",
                "CLOSE", "RETRY", "WARNING", "EYE", "SPARKLE", "INFO", "SHIELD", "LAYERS",
                "KEYBOARD")) {
            require(shellIconSources, "IconKey." + icon);
        }
        assert !root.contains("IconKey.ACCOUNT") : "unapproved account icon must not enter the 21-key atlas";
        List<String> shellSources = new java.util.ArrayList<>();
        shellSources.add(screen);
        shellSources.add(root);
        try (var paths = Files.walk(project.resolve("client-core/src/main/java/io/github/gyai/projects/client/shell"))) {
            paths.filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> {
                        try {
                            shellSources.add(Files.readString(path));
                        } catch (IOException exception) {
                            throw new IllegalStateException(exception);
                        }
                    });
        }
        for (String forbidden : List.of(
                "net.minecraft.client.gui.Font", "net.minecraft.client.gui.components.Button",
                "graphics.text(font", "→", "✓", "⚠", "★", "ProjectSUiKitPilotScreen",
                "HTMLElement", "document.", "window.", "<svg")) {
            for (String source : shellSources) assert !source.contains(forbidden) : forbidden;
        }

        for (String token : List.of("#0a0f0f", "#131b1a", "#192120", "#1d2827",
                "#dce8e6", "#a2adac", "#6d7876", "#9bd0cc", "#0d4845", "#fa746f",
                "CONTAINER_ALPHA = .82", "SUBTLE_ALPHA = .55", "ROUNDING_SCALE = 1.15",
                "SPACING_SCALE = .9", "TRANSITION_MILLIS = 200")) require(palette, token);

    }

    private static Path locateProjectRoot() throws IOException {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("client-core/build.gradle"))) return current;
            current = current.getParent();
        }
        throw new IOException("ProjectS-Client root not found");
    }

    private static void require(String source, String token) {
        assert source.contains(token) : "missing contract token: " + token;
    }
}
