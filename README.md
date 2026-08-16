# ProjectS Client (Fabric 26.1.2)

## Build

Open PowerShell in this `ProjectS-Client` folder, then run:

```powershell
.\gradlew.bat --version
.\gradlew.bat clean build -PskipAutoStart
```

Do not run `cd ProjectS-Client` if PowerShell already shows a path ending in `ProjectS-Client>`.

Requirements:
- Minecraft 26.1.2
- JDK 25
- Gradle Wrapper 9.5.1
- Fabric Loader 0.19.3
- Fabric API 0.155.2+26.1.2

Output:

```text
client-core\build\libs\projects-client-0.1.0.jar
devtools\build\libs\projects-devtools-0.1.0.jar
```

`editor-core` is nested in the DevTools artifact and is not manually installed.

## Developer Tools / 管理者用バランス調整

Install `projects-devtools` beside Client Core, then open ProjectS menu →
`Developer Tools` → `Balance` (or Mob Editor, UI Kit, and the editor frontend).
Balance requires the server-side `projects.dev` permission; the optional mod does not
grant permission.
変更はサーバー側で検証され、「適用」とファイルへの「保存」は別操作です。
