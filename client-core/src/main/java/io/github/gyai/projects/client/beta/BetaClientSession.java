package io.github.gyai.projects.client.beta;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

public final class BetaClientSession {
    private static final int PAYLOAD_VERSION = 1;
    private static final int MAX_TERMINAL_RESULTS = 128;
    private static final int MAX_PENDING_REQUESTS = 128;
    private UUID sessionId;
    private long revision;
    private Map<BetaProtocol.Capability, Integer> capabilities = Map.of();
    private final LinkedHashMap<UUID, BetaDisplayDocument> terminalResults =
            new LinkedHashMap<>(16, 0.75f, true);
    private final LinkedHashMap<UUID, BetaProtocol.Capability> pendingRequests =
            new LinkedHashMap<>(16, 0.75f, true);

    /** Test/pure-session convenience; runtime uses the explicit capability policy overload. */
    public synchronized byte[] accept(BetaProtocol.Advertisement advertisement) {
        return accept(advertisement, capability -> true);
    }

    public synchronized byte[] accept(BetaProtocol.Advertisement advertisement,
            Predicate<BetaProtocol.Capability> acknowledgementPolicy) {
        EnumMap<BetaProtocol.Capability, Integer> accepted =
                new EnumMap<>(BetaProtocol.Capability.class);
        for (BetaProtocol.Descriptor descriptor : advertisement.capabilities()) {
            if (descriptor.payloadVersion() == PAYLOAD_VERSION
                    && acknowledgementPolicy.test(descriptor.capability())) {
                accepted.put(descriptor.capability(), descriptor.payloadVersion());
            }
        }
        sessionId = advertisement.sessionId();
        revision = advertisement.revision();
        capabilities = Map.copyOf(accepted);
        pendingRequests.clear();
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
        BetaProtocol.Command command = new BetaProtocol.Command(
                new BetaProtocol.Envelope(BetaProtocol.Kind.COMMAND, capability,
                        PAYLOAD_VERSION, sessionId, payload),
                revision, targetRevision, requestId);
        if (pendingRequests.size() >= MAX_PENDING_REQUESTS) {
            pendingRequests.remove(pendingRequests.keySet().iterator().next());
        }
        pendingRequests.put(requestId, capability);
        return Optional.of(command);
    }

    public synchronized boolean recordTerminal(
            UUID requestId,
            BetaProtocol.Capability capability,
            BetaDisplayDocument result
    ) {
        if (requestId == null || capability == null || result == null
                || pendingRequests.get(requestId) != capability) {
            return false;
        }
        pendingRequests.remove(requestId);
        if (terminalResults.size() >= MAX_TERMINAL_RESULTS
                && !terminalResults.containsKey(requestId)) {
            terminalResults.remove(terminalResults.keySet().iterator().next());
        }
        terminalResults.put(requestId, result);
        return true;
    }

    public synchronized Optional<BetaDisplayDocument> terminal(UUID requestId) {
        return Optional.ofNullable(terminalResults.get(requestId));
    }

    public synchronized boolean oldServerFallback() {
        return sessionId == null;
    }

    public synchronized boolean acceptsState(BetaProtocol.Envelope envelope) {
        return envelope != null && envelope.kind() == BetaProtocol.Kind.STATE
                && sessionId != null && sessionId.equals(envelope.requestOrSessionId());
    }

    public synchronized int pendingRequestCount() {
        return pendingRequests.size();
    }

    public synchronized long revision() {
        return revision;
    }

    public synchronized void clear() {
        sessionId = null;
        revision = 0;
        capabilities = Map.of();
        pendingRequests.clear();
        terminalResults.clear();
    }
}
