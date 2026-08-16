package io.github.gyai.projects.devtools.studio.tool;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Pure ordered tool-state model.  It contains no platform adapter dependency;
 * a host can replace the context or active tool and rebuild its
 * presentation without copying editor document logic.
 */
public final class StudioToolPaletteModel {
    private StudioToolContextState contextState;
    private StudioToolId activeTool;

    public StudioToolPaletteModel() {
        this(StudioToolContextState.none(), StudioToolId.SELECT);
    }

    public StudioToolPaletteModel(StudioToolContext context, StudioToolId activeTool) {
        this(StudioToolContextState.of(context), activeTool);
    }

    public StudioToolPaletteModel(StudioToolContextState contextState, StudioToolId activeTool) {
        this.contextState = Objects.requireNonNull(contextState, "contextState");
        this.activeTool = activeTool;
    }

    public StudioToolContextState contextState() { return contextState; }

    public StudioToolContext context() { return contextState.context(); }

    public Optional<StudioToolId> activeTool() { return Optional.ofNullable(activeTool); }

    public StudioToolPaletteModel setContext(StudioToolContext context) {
        return setContextState(StudioToolContextState.of(context));
    }

    public StudioToolPaletteModel setContextState(StudioToolContextState next) {
        contextState = Objects.requireNonNull(next, "contextState");
        if (activeTool != null && !isEnabled(activeTool)) activeTool = null;
        return this;
    }

    public StudioToolPaletteModel setActiveTool(StudioToolId next) {
        if (next != null && !isEnabled(next)) throw new IllegalArgumentException("active tool disabled");
        activeTool = next;
        return this;
    }

    public List<StudioToolEntry> entries() {
        ArrayList<StudioToolEntry> result = new ArrayList<>(StudioToolId.values().length);
        for (StudioToolId id : StudioToolId.values()) {
            boolean enabled = isEnabled(id);
            result.add(new StudioToolEntry(id, enabled, enabled && id == activeTool));
        }
        return List.copyOf(result);
    }

    public Optional<StudioToolEntry> entry(StudioToolId id) {
        if (id == null) return Optional.empty();
        boolean enabled = isEnabled(id);
        return Optional.of(new StudioToolEntry(id, enabled, enabled && id == activeTool));
    }

    public boolean isEnabled(StudioToolId id) {
        if (id == null) return false;
        StudioToolContext context = contextState.context();
        return switch (id) {
            case SELECT, ADD -> true;
            case DUPLICATE -> contextState.canDuplicate();
            case DELETE -> contextState.canDelete();
            case MOVE, ROTATE, SCALE -> contextState.directEditing();
            case SHAPE -> contextState.directEditing()
                    && (context == StudioToolContext.PRIMITIVE || context == StudioToolContext.HANDLE);
            case MOTION -> contextState.directEditing()
                    && (context == StudioToolContext.PRIMITIVE
                    || context == StudioToolContext.EMISSION || context == StudioToolContext.HANDLE);
            case PHASE, TRAIL -> contextState.directEditing()
                    && (context == StudioToolContext.PRIMITIVE || context == StudioToolContext.EMISSION);
        };
    }

    public boolean isActive(StudioToolId id) {
        return id != null && id == activeTool && isEnabled(id);
    }
}
