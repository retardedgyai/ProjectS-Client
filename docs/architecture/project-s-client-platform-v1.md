# ProjectS Client Platform v1

ProjectS runs on managed Minecraft/Fabric rather than a Minecraft fork. A future ProjectS Launcher selects a Player Profile containing `projects_client`, or a Developer Profile containing both `projects_client` and optional `projects_devtools`.

`client-core` is the player Fabric mod: lifecycle, HUD, input, loadout, telegraphs, Ability VFX, themes, shared widgets and player Beta surfaces. `devtools` is a separate Fabric mod with a required exact `projects_client` dependency. Core never detects DevTools with FabricLoader; it offers a deterministic duplicate-safe menu contribution registry, which DevTools registers once.

Server permissions and validation remain authoritative. Installing DevTools neither grants a permission nor bypasses revision, protocol, or server-side validation. Core and DevTools are built as separate mod jars; editor-core is nested only in DevTools, never a third user-installed mod.

Mob Editor v1/v2 channels and their bytes are frozen. Registration, receivers, mutation UI, previews and fixtures belong to DevTools, so a core-only launch contains none. Shared Beta envelopes/session/runtime remain Core because player capabilities multiplex on the same channels. Core declines `MOB_EDITOR_V2`; DevTools opts in during initialization.
