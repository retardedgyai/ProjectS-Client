# Activation Track 4 — Client runtime lifecycle

The existing Beta protocol payload registration, four channel identifiers,
capability acknowledgement, state stores, command dispatch, UI views, manifest,
and golden vectors are retained unchanged. This Track adds a pure connection
lifecycle adapter and connects it to Fabric JOIN and DISCONNECT callbacks.

States are `DISCONNECTED`, `WAITING_FOR_ADVERTISEMENT`, and `ACTIVE`. JOIN first
clears previous session/state and waits for a Server advertisement. Without an
advertisement the old-server fallback remains active, commands cannot be sent,
and Beta UI data remains unavailable. A valid advertisement activates the
existing negotiated session. A later advertisement clears pending and terminal
results through the existing connection state before accepting the new session.
DISCONNECT is idempotent and clears capability, request, terminal, and display
state.

The UI is not authoritative, no channel or payload ID was changed, automatic
connection was not enabled, and no Server/Client deployment or launch belongs
to this Track. The compatible Server integration SHA is
`6c20b167bb43063490e8bcac189dd5af8e343a87`; the Client base is
`27f2c5e4b535dee19c860b711cac9662606540ff`.

## Fire status display addendum

The authoritative `projects:elements` display document is projected into a
bounded, display-only Fire store. The Client never increments stacks, decides
detonation, advances decay, or sends Fire state back to the Server. The generic
protocol-v1 map carries the additional fields without changing the channel,
aggregate version, capability ID, codec, manifest, or golden-vector meaning.

For the matching target network entity, a ProjectS-drawn flame silhouette,
stack count, and fractional progress strip render immediately below the
Monster/Training Dummy HP bar. Stack increases pulse briefly, stack 9 uses a
warning color, a new Server detonation pulse revision flashes once, and the
post-detonation Server snapshot displays the residual three stacks. Decay is a
subdued color/down marker. No Minecraft fire overlay is used.

Zero stacks are hidden. Stale/equal revisions and old-session packets are
rejected; equal packets cannot restart a flash. Target replacement, expiry,
world/target UI clear, new advertisement, and disconnect clear the display.
Without a Server advertisement, the existing old-server fallback remains
hidden.

## Server publisher compatibility fixture

`fire-elements-server-publisher-v1.json` contains two byte-for-byte packets
captured from the integrated Server `ElementSnapshotProtocolPublisher` at
`6c20b167bb43063490e8bcac189dd5af8e343a87`. State A carries Fire 10 and a new
detonation pulse revision. State B advances the state revision, carries the
post-detonation Fire 3 residual state, and retains the pulse revision. The
Client test decodes the outer protocol envelope and the independent Elements
v1 payload decoder, passes the result through the existing revisioned state
store and Fire display projection, and verifies exactly-once flash behavior.

The canonical manifest SHA-256 remains
`49d37172e5f5a95207876b328b52bf0d0a1a04aa6ec9a6f2e9f0bca8aa8937ac` and the
golden vectors SHA-256 remains
`dde5a2e27d46e548b03abbc4f991c7542cf4b4f2d2a0a08c69bf292b3ec3bf1a`.
No deployment or Client launch was performed.
