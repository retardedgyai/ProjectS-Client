# Ability VFX Runtime v1

`projects:ability_vfx_v1` is a cosmetic, clientbound-only channel. Invalid packets, invalid known primitives, packet trailing bytes, and unsupported protocol versions are dropped without disconnecting. Unknown framed primitive types/versions are skipped exactly by their u16 payload length.

The anchor frame uses origin plus **+Z forward, +Y up, +X right**. Local offset and yaw are applied before the deterministic anchor transform. No Minecraft type occurs in the protocol, store, sampler, or render policy.

Primitive slots after common fields are `size,radius,length,height,angle,startAngle,sweepAngle,turns`. POINT uses size (its rendered point width); LINE uses length and optionally exactly two controls; ARC uses radius/start/sweep; CIRCLE uses radius/start (full 2π); CONE uses length/angle and emits apex-to-rim rays; SPIRAL uses radius/height/turns; SPHERE uses radius; WAVE uses radius/length/height; BEZIER uses 3–4 controls; BURST uses radius/count and emits deterministic origin-to-endpoint rays. Every unused scalar is exactly zero. Controls and count are empty/zero except LINE controls, BEZIER controls, and BURST count.

Quality only scales density (LOW/MEDIUM/HIGH); it cannot change identity or random shape. Seeds are deterministic. Limits are 512 samples per primitive, 2048 per cue, and 8192 submitted samples per frame. Cues farther than 96 blocks are skipped; frustum checks apply in the renderer.

The store gates positive sequence within a server session, dedupes cue IDs, caps active cues at 64, expires by cue lifetime, and removes same-cast nonterminal cues when CANCEL or EXPIRE arrives. Session replacement retires the old session so late packets cannot resurrect it. World/null-level/dimension clearing removes active cues but preserves sequence/session protection; explicit JOIN and DISCONNECT reset all connection state. This runtime is independent of Telegraph and gameplay.
