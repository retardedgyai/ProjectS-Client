package io.github.gyai.projects.client.beta;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

public final class BetaUiStateStores {
    private final EnumMap<BetaProtocol.Capability, RevisionedStore> stores =
            new EnumMap<>(BetaProtocol.Capability.class);

    public BetaUiStateStores() {
        for (BetaProtocol.Capability capability : BetaProtocol.Capability.values()) {
            stores.put(capability, new RevisionedStore());
        }
    }

    public synchronized boolean receive(BetaProtocol.Envelope envelope, BetaClientSession session) {
        if (envelope == null || session == null || envelope.payloadVersion() != 1
                || !session.supports(envelope.capability())) return false;
        try {
            BetaDisplayDocument document = BetaDisplayDocumentCodec.decode(envelope.payload());
            if (envelope.capability() == BetaProtocol.Capability.MOB_EDITOR_V2
                    && document.entries().size()
                    > BetaProtocol.MOB_EDITOR_LIST_PAGE_MAX_ENTRIES) {
                return false;
            }
            if (envelope.kind() == BetaProtocol.Kind.COMMAND_RESULT) {
                return session.recordTerminal(
                        envelope.requestOrSessionId(), envelope.capability(), document);
            }
            if (!session.acceptsState(envelope)) return false;
            return stores.get(envelope.capability()).receive(document);
        } catch (IOException exception) {
            stores.get(envelope.capability()).failure(exception.getMessage());
            return false;
        }
    }

    public synchronized BetaDisplayDocument hud() { return value(BetaProtocol.Capability.HUD); }
    public synchronized BetaDisplayDocument party() { return value(BetaProtocol.Capability.PARTY); }
    public synchronized BetaDisplayDocument elements() { return value(BetaProtocol.Capability.ELEMENTS); }
    public synchronized BetaDisplayDocument equipment() { return value(BetaProtocol.Capability.EQUIPMENT); }
    public synchronized BetaDisplayDocument crafting() { return value(BetaProtocol.Capability.CRAFTING); }
    public synchronized BetaDisplayDocument enhancement() { return value(BetaProtocol.Capability.ENHANCEMENT); }
    public synchronized BetaDisplayDocument mobEditorV2() { return value(BetaProtocol.Capability.MOB_EDITOR_V2); }

    public synchronized Map<BetaProtocol.Capability, BetaDisplayDocument> snapshot() {
        EnumMap<BetaProtocol.Capability, BetaDisplayDocument> copy =
                new EnumMap<>(BetaProtocol.Capability.class);
        stores.forEach((key, value) -> copy.put(key, value.value));
        return Map.copyOf(copy);
    }

    public synchronized void clear() {
        stores.values().forEach(RevisionedStore::clear);
    }

    private BetaDisplayDocument value(BetaProtocol.Capability capability) {
        return stores.get(capability).value;
    }

    private static final class RevisionedStore {
        private BetaDisplayDocument value = BetaDisplayDocument.loading();

        private boolean receive(BetaDisplayDocument replacement) {
            if (replacement.revision() < value.revision()) return false;
            value = replacement;
            return true;
        }

        private void failure(String detail) {
            value = new BetaDisplayDocument(value.revision(), BetaDisplayDocument.Status.ERROR,
                    detail == null ? "不正な状態を受信しました" : detail, Map.of(), java.util.List.of());
        }

        private void clear() {
            value = BetaDisplayDocument.loading();
        }
    }
}
