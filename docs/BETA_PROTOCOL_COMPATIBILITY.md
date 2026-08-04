# Beta protocol v1 compatibility

This Client branch remains based on
`4daaafe0b234400636536be326e6c98dc4d616ce` and is validated against the
integrated ProjectS Server commit:

```text
3652747a9e1faedbd8e0b6323a248bbfa1a17ac2
```

The canonical manifest is `docs/protocol/beta-protocol-v1.json`:

```text
SHA-256 49d37172e5f5a95207876b328b52bf0d0a1a04aa6ec9a6f2e9f0bca8aa8937ac
```

The golden vectors are
`src/test/resources/protocol/beta-protocol-v1-vectors.json`:

```text
SHA-256 dde5a2e27d46e548b03abbc4f991c7542cf4b4f2d2a0a08c69bf292b3ec3bf1a
```

Both files are byte-identical to the Server copies, UTF-8, LF-only and
newline-terminated. `BetaProtocolCompatibilityTest` verifies the source
constants against the manifest and uses production Client codecs to decode the
Server vectors and encode acknowledgement/command vectors byte-for-byte.

The compatibility suite also verifies old-server fallback, unknown capability
and trailing-byte rejection, delayed old-session state rejection, terminal
result exactly-once handling, unavailable-balance display states and the
50-entry Mob Editor v2 list-page bound.

No automatic connection, feature activation, deployment or Minecraft launch
is included. Rollback removes the additive Beta payload registrations while
retaining all existing channels and old-server fallback behavior.
