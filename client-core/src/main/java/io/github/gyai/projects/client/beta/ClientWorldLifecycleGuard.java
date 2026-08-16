package io.github.gyai.projects.client.beta;

import java.util.Objects;

/** Tracks client world identity and reports transitions that must clear status projections. */
public final class ClientWorldLifecycleGuard {
    private Object worldIdentity;
    private boolean hasWorld;

    /** Returns true only when an established world changes or is unloaded. */
    public synchronized boolean observe(Object currentWorldIdentity) {
        if (currentWorldIdentity == null) {
            boolean changed = hasWorld;
            worldIdentity = null;
            hasWorld = false;
            return changed;
        }
        if (!hasWorld) {
            worldIdentity = currentWorldIdentity;
            hasWorld = true;
            return false;
        }
        if (Objects.equals(worldIdentity, currentWorldIdentity)) {
            return false;
        }
        worldIdentity = currentWorldIdentity;
        return true;
    }

    public synchronized void reset() {
        worldIdentity = null;
        hasWorld = false;
    }
}
