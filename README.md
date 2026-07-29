# ProjectS Client (Fabric 26.1.2)

## Build

Open PowerShell in this `ProjectS-Client` folder, then run:

```powershell
.\gradlew.bat --version
.\gradlew.bat build
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
build\libs\projects-client-0.1.0.jar
```

## 管理者用バランス調整

対応するProjectSサーバーへ接続すると、ProjectSメニューから
「バランス調整」を開けます。利用にはサーバー側の`projects.dev`権限が必要です。
変更はサーバー側で検証され、「適用」とファイルへの「保存」は別操作です。
