package io.github.gyai.projects.client.ui.mobeditor;

/**
 * One-shot client correlation for a duplicate draft request.
 *
 * <p>An in-progress response keeps the correlation pending. Every terminal response consumes it:
 * only a permitted, supported, non-conflicted successful response whose detail ID matches can
 * succeed; every other terminal response fails and clears the correlation.</p>
 */
public final class DuplicateRequestCorrelation {
    private String pendingId = "";

    public void begin(String id) {
        pendingId = id == null ? "" : id;
    }

    /**
     * Consumes a matching successful terminal result exactly once.
     *
     * @return true only when this response is the pending duplicate's accepted result
     */
    public boolean consumeSuccessful(boolean supported, boolean permitted, boolean success,
            boolean revisionConflict, String message, String detailId) {
        if (pendingId.isBlank() || inProgress(message)) return false;
        boolean accepted = supported && permitted && success && !revisionConflict
                && pendingId.equals(detailId);
        clear();
        return accepted;
    }

    public boolean pending() {
        return !pendingId.isBlank();
    }

    public void clear() {
        pendingId = "";
    }

    private static boolean inProgress(String message) {
        return message != null && message.endsWith("中...");
    }
}
