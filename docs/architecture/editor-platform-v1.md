# Editor Platform v1

`editor-core` is Java 25 and dependency-free. It has no Minecraft, Fabric, LWJGL, ProjectS UI or game-definition imports. Its v0.1 contracts are generic documents, reversible commands, linear history with branch invalidation/saved baseline/reset, selection, dirty state, validation, and ordered duplicate-safe property schemas.

Minecraft DevTools is the first frontend; a future Desktop Content Studio may use the same core. Existing Mob/Ability/VFX definitions remain source of truth—editor UI/layout is never saved into them. Frequent visual redesign therefore does not change definition data or wire protocols.

The DevTools editor shell uses existing ProjectS theme/modal/toast/dropdown/interaction facilities. Dock v0.1 has stable panel ids, visibility, horizontal/vertical split, bounded resizing, collapse and reset. It intentionally excludes full drag docking. No VFX or Skill editor content is introduced by this foundation.
