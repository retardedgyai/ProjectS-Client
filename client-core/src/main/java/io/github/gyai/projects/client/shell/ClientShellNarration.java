package io.github.gyai.projects.client.shell;

import io.github.gyai.projects.ui.runtime.UiAccessibilityMetadata;

import java.util.Optional;

/**
 * Pure bounded/debounced narration gate. The screen boundary owns the platform narrator; this
 * class only decides which immutable metadata snapshot may be spoken and when.
 */
public final class ClientShellNarration {
    public static final long DEBOUNCE_MILLIS = ClientShellPalette.TRANSITION_MILLIS;

    private String lastSignature;
    private String pendingSignature;
    private String pendingMessage;
    private long lastSpokenAt = Long.MIN_VALUE;
    private long lastNow;

    public Optional<String> offer(UiAccessibilityMetadata metadata, long nowMillis) {
        requireTime(nowMillis);
        if (metadata == null) return Optional.empty();
        String signature = signature(metadata);
        if (signature.equals(lastSignature) || signature.equals(pendingSignature)) {
            return Optional.empty();
        }
        String message = message(metadata);
        if (canSpeak(nowMillis)) return speak(signature, message, nowMillis);
        pendingSignature = signature;
        pendingMessage = message;
        return Optional.empty();
    }

    /** Flushes the newest pending local feedback once the bounded quiet period has elapsed. */
    public Optional<String> flush(long nowMillis) {
        requireTime(nowMillis);
        if (pendingMessage == null || !canSpeak(nowMillis)) return Optional.empty();
        return speak(pendingSignature, pendingMessage, nowMillis);
    }

    public boolean hasPending() { return pendingMessage != null; }

    private Optional<String> speak(String signature, String message, long nowMillis) {
        lastSignature = signature;
        lastSpokenAt = nowMillis;
        pendingSignature = null;
        pendingMessage = null;
        return Optional.of(message);
    }

    private boolean canSpeak(long nowMillis) {
        return lastSpokenAt == Long.MIN_VALUE
                || nowMillis >= lastSpokenAt + Math.max(1, DEBOUNCE_MILLIS);
    }

    private void requireTime(long nowMillis) {
        if (nowMillis < 0 || nowMillis < lastNow) throw new IllegalArgumentException("narration time");
        lastNow = nowMillis;
    }

    private static String signature(UiAccessibilityMetadata metadata) {
        return metadata.role() + "|" + metadata.label() + "|" + metadata.value()
                + "|" + metadata.enabled() + "|" + metadata.selected();
    }

    private static String message(UiAccessibilityMetadata metadata) {
        String value = metadata.value().isBlank() ? "" : ". " + metadata.value();
        return metadata.label() + value;
    }
}
