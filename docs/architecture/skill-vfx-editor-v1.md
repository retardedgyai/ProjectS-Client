# Skill / VFX Editor v0.1

DevTools owns the frontend, v1 editor channels, document and view models. Editor Core is Java-base-only command/history, dirty, selection, validation and property-schema support. Client Core exposes only a generic local cue store and composes it with the existing sampler and renderer.

The fixed responsive shell contains visual tree, preview, inspector and VFX timeline. The inspector derives all primitive slots from one property-schema mapping. Gameplay remains an ordered, read-only snapshot. Edits are commands on a working document; the last received or applied authority snapshot is its dirty baseline.

Apply Session and Revert Session are temporary, revision/fingerprint-guarded server requests: no client persistence or gameplay mutation exists. Preview is an isolated local cue using the production sampler/renderer budget and is cleared on stop, close, world change, disconnect and connection reset. Future Desktop Content Studio or persistence can reuse the editor-core document/schema, but cannot make the client authoritative.

The delivered DevTools screen is componentized around header, gameplay projection, visual tree, schema inspector, preview and timeline views. It uses themed controls only: catalog cycling and capability-gated session actions, confirmation modals for destructive refresh/revert/close actions, and terminal status toasts. Gameplay remains ordered/read-only. The visual tree owns only selection while all mutations are document commands, including emission and primitive add/remove/duplicate/move.

The inspector pages deterministic schema rows so every common/type-specific field and Bézier control point remains reachable on the minimum layout. Scalar mode preserves its tagged gameplay source (including `RADIUS`) until explicitly changed; invalid text remains local feedback and does not replace a valid document value. The frontend layout supports panel visibility/reset and clamped divider dragging, with a compact two-row header at 640px.

Preview authorization comes from the received state. A permitted preview uses the actual player frame and a separate local virtual playhead; play/pause/seek/restart/speed and quality therefore affect the renderer, without changing network cue timing. Local and network commands still share the single renderer frame budget.
