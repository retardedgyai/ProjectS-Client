# VFX Appearance / Particle Runtime Foundation v0.1

Shape remains the `AbilityVfx` primitive geometry. Appearance is an independent, validated value:
`projects:debug_quad` uses the existing frame renderer, while the nine catalogued Minecraft particle IDs are emitted only from the END_CLIENT_TICK dispatcher.

The runtime packet remains `projects:ability_vfx_v1` with outer version `1`. A legacy primitive header (`1`) has its byte-identical body and defaults to debug quads. Header `2` appends the particle appearance kind and stable id; unknown primitive headers and supported-header unknown particle ids are skipped, whereas malformed known v2 payloads reject the cue.

DevTools owns both editor payload channel generations. It chooses `skill_editor_req_v2` before the first catalog request when available, locks that choice for the connection, and otherwise keeps the v1 fallback. The v2 envelope wraps canonical v1 bytes with document-order appearance tables. Core contains neither editor codec nor editor channels.

Particle work is bounded to 64 samples per primitive, 256 per cue, and 512 globally per tick. Quality and Minecraft particle status scale the pure policy before the Minecraft-only dispatcher resolves the explicit particle switch. World preview enters the same Core local-preview queue; it does not introduce a second renderer or particle path.
