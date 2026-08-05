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
