package io.github.gyai.projects.client.shell;

import java.util.List;

/** Pure reducer/data-source acceptance checks for the five reviewable states and shell actions. */
public final class ClientShellReducerTest {
    public static void main(String[] args) {
        ClientShellSnapshot initial = ClientShellSnapshot.initial();
        assert initial.isHome();

        for (ClientShellState state : ClientShellState.values()) {
            ClientShellSnapshot reviewed = ClientShellReducer.reduce(initial, ClientShellAction.review(state));
            assert reviewed.page() == ClientShellPage.HOME : state;
            assert reviewed.state() == state : state;
            assert reviewed.reviewMode() : state;
        }

        assert ClientShellReducer.reduce(initial, ClientShellAction.playPreview()).state()
                == ClientShellState.LAUNCHING;
        assert ClientShellReducer.reduce(initial, ClientShellAction.openClientPreview()).state()
                == ClientShellState.CONNECTING;
        assert ClientShellReducer.reduce(initial, ClientShellAction.previewError()).state()
                == ClientShellState.RECOVERABLE_ERROR;

        ClientShellSnapshot connected = ClientShellReducer.reduce(
                initial, ClientShellAction.review(ClientShellState.CONNECTED));
        ClientShellSnapshot opened = ClientShellReducer.reduce(
                connected, ClientShellAction.openClientPreview());
        assert opened.clientPreviewOpen();
        assert opened.state() == ClientShellState.CONNECTED;

        ClientShellSnapshot library = ClientShellReducer.reduce(initial, ClientShellAction.navigateLibrary());
        assert library.page() == ClientShellPage.LIBRARY;
        assert ClientShellReducer.reduce(library, ClientShellAction.navigateHome()).isHome();
        assert ClientShellReducer.reduce(initial, ClientShellAction.openAccount()).page()
                == ClientShellPage.SETTINGS;
        assert ClientShellReducer.reduce(connected, ClientShellAction.escapeToHome()).isHome();

        ClientShellDataSnapshot data = new SampleClientShellDataSource().snapshot();
        assert data.profiles().size() == 2;
        assert data.profile("horizon").name().equals("Horizon Realm");
        try {
            data.profiles().add(data.firstProfile());
            throw new AssertionError("data source snapshot must be immutable");
        } catch (UnsupportedOperationException expected) {
            // expected
        }

        ClientShellDataSource replacement = () -> new ClientShellDataSnapshot(
                "future_account", "future account", "Future session", "Ready", "Local",
                List.of(data.firstProfile()));
        ClientShellModel model = new ClientShellModel(replacement);
        assert model.data().accountName().equals("future_account");
        model.dispatch(ClientShellAction.selectProfile("horizon"));
        assert model.state().profileId().equals("horizon");

        automaticTimeline(replacement);
        manualAndRetryTimelines(replacement);

        ClientShellAccessibility narration = ClientShellAccessibility.from(
                ClientShellSnapshot.initial(), data);
        assert narration.label().equals("ProjectS Client Hub");
        assert narration.value().contains("Horizon Realm");
        assert narration.focusLabels().contains("Play preview");

        ClientShellTreeTest.run();
    }

    private static void automaticTimeline(ClientShellDataSource source) {
        ClientShellModel model = new ClientShellModel(source);
        model.dispatch(ClientShellAction.playPreview());
        for (long now = 50; now <= 550; now += 50) {
            model.tick(now);
            if (now < 200) {
                assert model.state().state() == ClientShellState.LAUNCHING : now;
                assert model.state().launchStep() == 1 : now;
            } else if (now < 400) {
                assert model.state().state() == ClientShellState.LAUNCHING : now;
                assert model.state().launchStep() == 2 : now;
            } else {
                assert model.state().state() == ClientShellState.LAUNCHING : now;
                assert model.state().launchStep() == 3 : now;
            }
        }
        model.tick(600);
        assert model.state().state() == ClientShellState.CONNECTING;
        assert model.state().connectionStep() == 2;
        model.tick(650);
        assert model.state().state() == ClientShellState.CONNECTING
                : "CONNECTING must survive the next 50ms host tick";
        model.tick(799);
        assert model.state().state() == ClientShellState.CONNECTING;
        model.tick(800);
        assert model.state().state() == ClientShellState.CONNECTED;

        ClientShellSnapshot launching = ClientShellReducer.reduce(
                ClientShellSnapshot.initial(), ClientShellAction.playPreview());
        assert ClientShellReducer.advance(launching, 599).state() == ClientShellState.LAUNCHING;
        assert ClientShellReducer.advance(launching, 600).state() == ClientShellState.CONNECTING;
        ClientShellSnapshot connecting = ClientShellReducer.advance(launching, 600);
        assert ClientShellReducer.advance(connecting, 199).state() == ClientShellState.CONNECTING;
        assert ClientShellReducer.advance(connecting, 200).state() == ClientShellState.CONNECTED;
    }

    private static void manualAndRetryTimelines(ClientShellDataSource source) {
        ClientShellModel model = new ClientShellModel(source);
        model.dispatch(ClientShellAction.review(ClientShellState.CONNECTING));
        model.tick(500);
        assert model.state().state() == ClientShellState.CONNECTING;
        assert model.state().reviewMode() : "manual review state must remain inert";

        model.dispatch(ClientShellAction.playPreview());
        model.tick(699);
        assert model.state().state() == ClientShellState.LAUNCHING;
        model.tick(700);
        assert model.state().launchStep() == 2 : "manual launch starts its own transition clock";

        model.dispatch(ClientShellAction.previewError());
        model.tick(1000);
        assert model.state().state() == ClientShellState.RECOVERABLE_ERROR;
        model.dispatch(ClientShellAction.retryPreview());
        model.tick(1199);
        assert model.state().state() == ClientShellState.LAUNCHING;
        model.tick(1200);
        assert model.state().launchStep() == 2 : "retry launch starts at dispatch time";

        model.dispatch(ClientShellAction.cancelPreview());
        model.tick(1500);
        assert model.state().isHome();
        model.dispatch(ClientShellAction.openClientPreview());
        model.tick(1699);
        assert model.state().state() == ClientShellState.CONNECTING;
        model.tick(1700);
        assert model.state().state() == ClientShellState.CONNECTED;
    }
}
