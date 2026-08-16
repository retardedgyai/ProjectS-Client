package io.github.gyai.projects.client;

public final class TelegraphRevisionGate {
    private long lastDetonatedRevision = Long.MIN_VALUE;
    private long lastCancelledRevision = Long.MIN_VALUE;
    private long lastLockFlashRevision = Long.MIN_VALUE;
    private long particleRevision = Long.MIN_VALUE;

    public boolean beginDetonation(long revision) {
        if (revision <= lastDetonatedRevision) {
            return false;
        }
        lastDetonatedRevision = revision;
        return true;
    }

    public boolean beginCancellation(long revision) {
        if (revision <= lastCancelledRevision) {
            return false;
        }
        lastCancelledRevision = revision;
        return true;
    }

    public boolean beginLockFlash(long revision) {
        if (revision <= lastLockFlashRevision) {
            return false;
        }
        lastLockFlashRevision = revision;
        return true;
    }

    public boolean consumeDetonationParticles() {
        if (lastDetonatedRevision == Long.MIN_VALUE
                || particleRevision
                == lastDetonatedRevision) {
            return false;
        }
        particleRevision = lastDetonatedRevision;
        return true;
    }
}
