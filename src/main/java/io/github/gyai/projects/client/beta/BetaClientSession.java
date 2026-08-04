package io.github.gyai.projects.client.beta;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class BetaClientSession {
    private static final int PAYLOAD_VERSION = 1;
    private static final int MAX_TERMINAL_RESULTS = 128;
    private UUID sessionId;
    private long revision;
    private Map<BetaProtocol.Capability, Integer> capabilities = Map.of();
    private final LinkedHashMap<UUID, BetaDisplayDocument> terminalResults =
            new LinkedHashMap<>(16, 0.75f, true);

    public synchronized byte[] accept(BetaProtocol.Advertisement advertisement) {
        EnumMap<BetaProtocol.Capability, Integer> accepted =
                new EnumMap<>(BetaProtocol.Capability.class);
        for (BetaProtocol.Descriptor descriptor : advertisement.capabilities()) {
            if (descriptor.payloadVersion() == PAYLOAD_VERSION) {
                accepted.put(descriptor.capability(), descriptor.payloadVersion());
            }
        }
        sessionId = advertisement.sessionId();
        revision = advertisement.revision();
        capabilities = Map.copyOf(accepted);
        terminalResults.clear();
        List<BetaProtocol.Descriptor> acknowledgement = accepted.entrySet().stream()
                .map(entry -> new BetaProtocol.Descriptor(entry.getKey(), entry.getValue()))
                .toList();
        return BetaProtocol.encodeAcknowledgement(advertisement, acknowledgement);
    }

    public synchronized boolean supports(BetaProtocol.Capability capability) {
        return sessionId != null && capabilities.getOrDefault(capability, -1) == PAYLOAD_VERSION;
    }

    public synchronized Optional<BetaProtocol.Command> command(
            BetaProtocol.Capability capability,
            long targetRevision,
            byte[] payload
    ) {
        if (!supports(capability) || targetRevision < 0) return Optional.empty();
        UUID requestId = UUID.randomUUID();
        return Optional.of(new BetaProtocol.Command(
                new BetaProtocol.Envelope(BetaProtocol.Kind.COMMAND, capability,
                        PAYLOAD_VERSION, sessionId, payload),
                revision, targetRevision, requestId));
    }

    public synchronized void recordTerminal(UUID requestId, BetaDisplayDocument result) {
        if (requestId == null || result == null) return;
        if (terminalResults.size() >= MAX_TERMINAL_RESULTS
                && !terminalResults.containsKey(requestId)) {
            terminalResults.remove(terminalResults.keySet().iterator().next());
        }
        terminalResults.put(requestId, result);
    }

    public synchronized Optional<BetaDisplayDocument> terminal(UUID requestId) {
        return Optional.ofNullable(terminalResults.get(requestId));
    }

    public synchronized boolean oldServerFallback() {
        return sessionId == null;
    }

    public synchronized long revision() {
        return revision;
    }

    public synchronized void clear() {
        sessionId = null;
        revision = 0;
        capabilities = Map.of();
        terminalResults.clear();
    }
}
