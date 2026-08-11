package io.github.gyai.projects.ui.runtime;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

/** Pure nested clip calculation. Adapters decide how the resulting clips reach a GPU API. */
public final class UiClipStack {
    private final Deque<UiRect> clips = new ArrayDeque<>();

    public UiRect push(UiRect requested) {
        if (requested == null) throw new NullPointerException("requested");
        UiRect effective = clips.isEmpty() ? requested : clips.peek().intersection(requested);
        clips.push(effective);
        return effective;
    }

    public UiRect pop() {
        if (clips.isEmpty()) throw new IllegalStateException("Cannot pop an empty clip stack");
        return clips.pop();
    }

    public Optional<UiRect> current() { return Optional.ofNullable(clips.peek()); }
    public int depth() { return clips.size(); }
    public void clear() { clips.clear(); }
}
