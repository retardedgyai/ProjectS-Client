package io.github.gyai.projects.client.shell;

import io.github.gyai.projects.ui.runtime.UiButton;
import io.github.gyai.projects.ui.runtime.UiNode;
import io.github.gyai.projects.ui.runtime.UiPoint;
import io.github.gyai.projects.ui.runtime.UiRect;
import io.github.gyai.projects.ui.runtime.UiAccessibilityRole;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** Tree-level control and reducer effects for every reachable home-flow snapshot. */
public final class ClientShellTreeTest {
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;

    private ClientShellTreeTest() { }

    public static void main(String[] args) {
        run();
    }

    static void run() {
        homeControlsDispatchExpectedEffects();
        launchingCancelDispatchesHome();
        connectingControlsDispatchExpectedEffects();
        connectedControlsDispatchExpectedEffects();
        errorControlsDispatchExpectedEffects();
    }

    private static void homeControlsDispatchExpectedEffects() {
        Harness profile = harness(ClientShellState.HOME);
        activate(profile, "home.profile");
        assert profile.model.state().profileId().equals("atelier");
        assertTreeContract(profile);

        Harness play = harness(ClientShellState.HOME);
        activate(play, "home.play.action");
        assert play.model.state().state() == ClientShellState.LAUNCHING;
        assertTreeContract(play);

        Harness openClient = harness(ClientShellState.HOME);
        activate(openClient, "home.open-client.action");
        assert openClient.model.state().state() == ClientShellState.CONNECTING;
        assertTreeContract(openClient);
    }

    private static void launchingCancelDispatchesHome() {
        Harness launching = harness(ClientShellState.LAUNCHING);
        activate(launching, "launch.cancel.action");
        assert launching.model.state().isHome();
        assertTreeContract(launching);
    }

    private static void connectingControlsDispatchExpectedEffects() {
        Harness openClient = harness(ClientShellState.CONNECTING);
        activate(openClient, "connecting.open-client.action");
        assert openClient.model.state().state() == ClientShellState.CONNECTING;
        assert openClient.model.state().announcement().contains("Opening");
        assertTreeContract(openClient);

        Harness error = harness(ClientShellState.CONNECTING);
        activate(error, "connecting.error.action");
        assert error.model.state().state() == ClientShellState.RECOVERABLE_ERROR;
        assertTreeContract(error);

        Harness cancel = harness(ClientShellState.CONNECTING);
        activate(cancel, "connecting.cancel.action");
        assert cancel.model.state().isHome();
        assertTreeContract(cancel);
    }

    private static void connectedControlsDispatchExpectedEffects() {
        Harness openClient = harness(ClientShellState.CONNECTED);
        activate(openClient, "connected.open-client.action");
        assert openClient.model.state().state() == ClientShellState.CONNECTED;
        assert openClient.model.state().clientPreviewOpen();
        assertTreeContract(openClient);

        Harness back = harness(ClientShellState.CONNECTED);
        activate(back, "connected.home.action");
        assert back.model.state().isHome();
        assertTreeContract(back);
    }

    private static void errorControlsDispatchExpectedEffects() {
        Harness retry = harness(ClientShellState.RECOVERABLE_ERROR);
        activate(retry, "error.retry.action");
        assert retry.model.state().state() == ClientShellState.LAUNCHING;
        assertTreeContract(retry);

        Harness openClient = harness(ClientShellState.RECOVERABLE_ERROR);
        activate(openClient, "error.open-client.action");
        assert openClient.model.state().state() == ClientShellState.CONNECTING;
        assertTreeContract(openClient);

        // The existing error design uses Back to hub as its cancel path.
        Harness back = harness(ClientShellState.RECOVERABLE_ERROR);
        activate(back, "error.home.action");
        assert back.model.state().isHome();
        assertTreeContract(back);
    }

    private static Harness harness(ClientShellState state) {
        ClientShellModel model = new ClientShellModel(new SampleClientShellDataSource());
        if (state != ClientShellState.HOME) {
            model.dispatch(ClientShellAction.review(state));
        }
        UiNode root = createRoot(model);
        Harness result = new Harness(model, root);
        assertTreeContract(result);
        return result;
    }

    private static UiNode createRoot(ClientShellModel model) {
        try {
            Class<?> rootClass = Class.forName("io.github.gyai.projects.client.ClientShellRoot");
            Constructor<?> constructor = rootClass.getDeclaredConstructor(ClientShellModel.class, boolean.class);
            constructor.setAccessible(true);
            UiNode root = (UiNode) constructor.newInstance(model, true);
            Method layout = rootClass.getDeclaredMethod("layout", int.class, int.class);
            layout.setAccessible(true);
            layout.invoke(root, WIDTH, HEIGHT);
            return root;
        } catch (ReflectiveOperationException exception) {
            Throwable cause = exception instanceof InvocationTargetException target
                    && target.getCause() != null ? target.getCause() : exception;
            throw new AssertionError("Client Shell root test setup failed", cause);
        }
    }

    private static void assertTreeContract(Harness harness) {
        ClientShellSnapshot state = harness.model.state();
        List<String> focusOrder = ClientShellLayout.compute(WIDTH, HEIGHT,
                state.page(), state.state()).focusOrder();
        for (String id : focusOrder) {
            List<UiNode> matches = findById(harness.root, id);
            assert matches.size() == 1 : id + " occurs " + matches.size() + " times";
            UiNode node = matches.getFirst();
            assert node instanceof UiButton : id + " is not a button";
            assert node.focusable() : id + " is not focusable";
            assert node.accessibility().role() == UiAccessibilityRole.BUTTON
                    : id + " is not accessible as a button";
            assert node.isEffectivelyInteractive(harness.root) : id + " is not interactive";
        }
    }

    private static void activate(Harness harness, String id) {
        List<UiNode> matches = findById(harness.root, id);
        assert matches.size() == 1 : id + " occurs " + matches.size() + " times";
        UiNode node = matches.getFirst();
        assert node instanceof UiButton : id + " is not actionable";
        UiButton button = (UiButton) node;
        UiRect bounds = button.globalBounds();
        UiPoint center = new UiPoint(bounds.x() + bounds.width() / 2,
                bounds.y() + bounds.height() / 2);
        assert button.pointerDown(center) : id + " rejected pointer down";
        assert button.pointerUp(center) : id + " rejected pointer up";
    }

    private static List<UiNode> findById(UiNode node, String id) {
        ArrayList<UiNode> matches = new ArrayList<>();
        if (node.id().equals(id)) matches.add(node);
        for (UiNode child : node.children()) matches.addAll(findById(child, id));
        return matches;
    }

    private record Harness(ClientShellModel model, UiNode root) { }
}
