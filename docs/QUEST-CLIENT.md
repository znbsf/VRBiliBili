# Quest 基础 PiliPlus 移植

2026-10-03 用户实际使用后反馈：详情留白、播放控制隐藏、缺少明显返回入口、按钮过小；随后明确要求砍掉空间视频路线，先完善基础 PiliPlus。

## 当前实现

- 继续使用原 PiliPlus / media-kit 播放链路，在 Quest 普通 Android 大屏窗口内运行。
- 移除空间播放按钮、独立 Spatial Activity、Meta Spatial SDK/Media3 依赖、透视/手部跟踪声明及相应调试入口。
- Quest 专用详情布局：左侧视频与常驻进度/播放控制，右侧简介、评论与可用播放列表。
- 顶部常驻返回上页、返回主页与放大画面；播放、快退、快进和设置集中在底部工具栏。放大模式收起详情，仍可直接导航和控制。
- 导航与播放图标保留 52 dp 点击区域，视觉图标为 26 dp；原默认 UI 缩放从 1.15 调到 1.3，保留用户更大的自定义缩放。
- 播放设置包含实际可用画质、倍速与弹幕开关；继续保留上游完整业务和播放器功能。
- 同包名覆盖升级，保留登录/历史等应用数据。空间布局文件仅成为无引用历史数据，不读取或影响播放器。

视觉采用深灰底色、粉色播放强调和紧凑图标工具栏。此前查阅的 B 站与 YouTube 功能说明不是视觉设计稿，不将其称为本界面的设计来源。页面本身是 Flutter 应用 UI，并未嵌入视频网站。

## 构建与测试

固定工具链继续使用 Flutter 3.47.5 私有补丁 SDK、Java 17、Gradle 8.14.5、AGP 8.11.1、Kotlin 2.2.21。源码来源和许可证见客户端内的 VRBILIBILI-UPSTREAM.md。

```powershell
python tools/Prepare-QuestFlutter.py --source C:/path/to/unmodified/flutter
./tools/Build-QuestApp.ps1 -Jdk C:/path/to/jdk17 -AndroidSdk C:/path/to/Android/Sdk
# 若私有 SDK 已准备好，不重复运行 Prepare；可通过 -FlutterSdk 指定现有副本。
adb -s QUEST_SERIAL install -r clients/piliplus/build/app/outputs/flutter-apk/app-debug.apk
```

包名保持 `io.github.vrbilibili.quest.debug`，标签 VRBiliBili。本仓库禁用常驻 Gradle daemon，构建完毕由单次构建进程自行退出。

Android instrumentation `QuestUiDriver` 检查真实选片、常驻图标控制、播放器解码尺寸/媒体时间、暂停、前进 10 秒、放大/详情、设置、返回上页/主页，并从设备生成主页、详情、放大画面和设置截图。测试不进行账号登录、不发布互动内容。

```powershell
cd clients/piliplus/android
./gradlew.bat :app:assembleDebugAndroidTest --no-daemon -Ptarget-platform=android-arm64
# 回仓库根目录
adb -s QUEST_SERIAL install -r clients/piliplus/build/app/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s QUEST_SERIAL shell am instrument -w io.github.vrbilibili.quest.debug.test/com.example.piliplus.QuestUiDriver
```

完整日志及实机 UI 图保留在忽略的 `reports/quest-ui-redesign-20261003/`。旧空间播放器的记录只描述旧 APK，不能作为新版验证结果。实际佩戴的易用性仍以用户反馈为准。
