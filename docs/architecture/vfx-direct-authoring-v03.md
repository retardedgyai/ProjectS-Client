# VFX Direct Authoring v0.3

`SkillVfx3dAuthoringScreen` is a transparent, non-pausing DevTools screen. It retains the original `SkillEditorScreen`, its `AbilityVisualEditorDocument`, selection, history, timeline and playback controller; return simply restores that same screen object and performs no fetch.

`SkillVfxDirectAuthoring` is frontend-independent Java. Its guide is exactly `AbilityVfx.sample(convert(selectedWorkingPrimitive), frame, 1, HIGH)`. Handles are annotations: all primitives have XYZ transform axes; SPIRAL exposes literal radius/height/turns, LINE exposes both endpoints, and BEZIER exposes all three/four controls. Gameplay-bound scalar values are displayed but not invented as draggable constants.

Dragging updates only the existing working document. Release records one `EditorHistory.recordAlreadyApplied` command; Escape restores the captured primitive and selection without history. The local preview cue is replaced through a Client Core seam that preserves playhead, playing state and quality.

Current motion capability is **INSUFFICIENT** for authored traversal. The runtime samples normalized progress/reveal only; it has no model fields for direction, travel, phase, authored speed, or easing. v0.3 therefore highlights only existing sampled progress and introduces no Server/model/wire change. A future minimal platform-neutral extension would need an explicit traversal mode, direction and easing/progress fields, followed by a separately versioned Server/runtime protocol decision.

| Primitive | Existing editable fields | Guide/progress | Direct handles |
|---|---|---|---|
| POINT | size, offset, yaw | sampled point | XYZ transform |
| LINE | length, controls, offset, yaw | sampled line/reveal | XYZ transform, A/B XYZ |
| ARC | radius, start/sweep, offset, yaw | sampled arc/reveal | XYZ transform, radius |
| CIRCLE | radius, start angle, offset, yaw | sampled circle/reveal | XYZ transform, radius |
| CONE | length, angle, offset, yaw | sampled rays/expansion | XYZ transform |
| SPIRAL | radius, height, turns, offset, yaw | sampled spiral/reveal | XYZ transform, radius/height/turns |
| SPHERE | radius, offset, yaw | sampled surface/reveal | XYZ transform, radius |
| WAVE | radius, length, height, offset, yaw | sampled wave/reveal | XYZ transform, radius |
| BEZIER | controls, offset, yaw | sampled curve/reveal | XYZ transform, every 3/4-point control XYZ |
| BURST | radius, count, offset, yaw | sampled rays/expansion | XYZ transform, radius |

Picking consumes the same per-frame `CameraRenderState` used by world rendering: interpolated/third-person camera position, orientation-derived right/up/forward axes, and the active projection matrix focal length in GUI units. Drag motion is projected onto those rendered axes (or the selected XY/Z control-point plane), so camera rotation does not change the edited model axis. A vertical camera remains valid because the orientation supplies camera up directly.

The immutable authoring cue frame remains the sampling and handle anchor after Alt-look. Length-only LINE data can be materialized into deterministic A/B controls as one undoable command; this changes only the working draft. BEZIER draws its existing control polygon as an editor annotation in addition to the production-sampled curve. These annotations never become runtime VFX or hit geometry.
