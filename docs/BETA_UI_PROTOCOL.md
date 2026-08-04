# Beta UI protocol and client foundation

This branch starts from `origin/agent/boss-telegraphs` so the current Telegraph,
Mob Editor v1, monster HUD, and ProjectS theme/UI lineage remains intact. It does
not modify or include the unrelated uncommitted UI work in the original client
working directory.

The client implements aggregate protocol version `1` on the additive channels
`projects:beta_caps_v1`, `projects:beta_ack_v1`, `projects:beta_state_v1`, and
`projects:beta_command_v1`. It supports payload version `1` for the canonical
HUD, Party, elements, equipment, crafting, enhancement, and Mob Editor v2
capabilities. Existing channel constants and payloads are unchanged.

The handshake accepts only exact payload versions and sends a deterministic,
bounded acknowledgement. Decoders reject unsupported versions, unknown
capabilities/opcodes, invalid UTF-8, malformed lengths, duplicates, excessive
lists/maps, trailing bytes, non-finite display numbers, and packets above the
8 KiB handshake or 32 KiB normal limit. Incoming state is revision-gated and
display-only. Commands receive fresh idempotency request IDs and include the
advertised player-session revision and target/content revision; the server
remains authoritative.

Ephemeral stores cover HUD, Party, element target overlay, equipment detail,
crafting, enhancement, and Mob Editor v2. View models and management screens
show loading, error, unsupported, revision-conflict, terminal, and retry-forbidden
states. Enhancement can display unavailable balance data without inventing
probabilities or costs. Disconnect and reconnect clear all session, terminal,
and display state. With no advertisement, the client remains in old-server
fallback and the existing UI continues to run.

Automated tests cover constants shared with the server contract, deterministic
handshake/command bytes, malformed/fuzz input, exact capability versions,
revision ordering, duplicate fields, bounded terminal results, unsupported
servers, cleanup, and UI error/conflict behavior. The feature is not deployed;
Minecraft is not launched. Rollback removes the additive payload registrations
while retaining every existing fallback and channel.
