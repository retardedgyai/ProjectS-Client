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
to this Track. The compatible Server integration base is
`2a991aa6ba3afc0b59ebd1f0874c00b195ec84cd`; the Client base is
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
