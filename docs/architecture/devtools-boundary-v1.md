# DevTools Boundary v1

DevTools owns balance payloads/state/tuning UI, Mob Editor v1/v2 payloads and screens, UI Kit, diagnostics, the editor frontend, and the Developer Tools submenu. It maps balance values into Core's read-only skill-description store; Core falls back to `SkillCatalog` without DevTools.

DevTools resources live under `assets/projects_devtools`; Core resources are not copied. It has no shaded Core output. The Gradle dependency uses Core's `namedElements` plus client split-source output, while `editor-core` is Loom-included once. Deployment copies both mod jars only to the developer test profile and safely removes stale versioned copies before closing the old client/Launcher and reopening the Launcher; automated verification uses `-PskipAutoStart`.
