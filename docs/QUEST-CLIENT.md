# Quest 基础 PiliPlus 移植

2026-10-03 用户实际使用后反馈：详情留白、播放控制隐藏、缺少明显返回入口、按钮过小；随后明确要求砍掉空间视频路线，先完善基础 PiliPlus。

## 当前实现

- 继续使用原 PiliPlus / media-kit 播放链路，在 Quest 普通 Android 大屏窗口内运行。
- 旧 SpatialPlayerActivity、多面板和摆放系统已移除。后续按用户新要求增加独立 CinemaActivity，仅负责固定银幕的全屏影院。Meta Spatial SDK 0.14.0 和 Media3 1.5.1 用于这一入口。
- Quest 专用详情布局：左侧视频与常驻进度/播放控制，右侧简介、评论与可用播放列表。
- 顶部仅保留返回上页、返回主页；播放、快退、快进、弹幕、画质、设置和全屏影院位于底部同一工具栏。Quest 选中视频后自动播放。
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

Android instrumentation `QuestUiDriver` 检查真实选片、常驻图标控制、播放器解码尺寸/媒体时间、自动开播、暂停、前进 10 秒、直接画质/弹幕入口、影院解码及返回进度、设置、返回上页/主页，并从设备生成主页、详情、画质、设置及影院/纯画面截图。测试不进行账号登录、不发布互动内容。

```powershell
cd clients/piliplus/android
./gradlew.bat :app:assembleDebugAndroidTest --no-daemon -Ptarget-platform=android-arm64
# 回仓库根目录
adb -s QUEST_SERIAL install -r clients/piliplus/build/app/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s QUEST_SERIAL shell am instrument -w io.github.vrbilibili.quest.debug.test/com.example.piliplus.QuestUiDriver
```

完整日志及实机 UI 图保留在忽略的 `reports/quest-cinema-20261003/`。旧空间播放器的记录只描述旧 APK，不能作为新版验证结果。实际佩戴的易用性仍以用户反馈为准。

## 全屏影院

播放栏“全屏影院”进入原生沉浸式 Activity，调用 `scene.enablePassthrough(false)`。默认使用固定银幕和简洁的三维舞台、侧墙、暗光布景；“纯画面”隐藏全部布景。播放时操作栏 3.5 秒后隐藏，点击屏幕唤出，暂停和打开画质选择时保持可见。可从栏内切换弹幕/画质、重新居中或退出全屏。影院默认关闭弹幕，避免挡住画面。

普通播放器先暂停，再移交当前媒体与进度。返回后同步进度并保持暂停，避免两个播放器同时发声。签名 URL 与请求头仅保存在内存；不持久化账户凭据。影院布局不会读取旧空间面板位置。

场景具有真实双目几何深度，地面柔光目前是静态近似，没有实现视频驱动的模糊反射，也不将普通 2D 视频转换成 3D。自动测试的影院控制使用调试命令；实际手柄/手势命中和佩戴舒适度仍需人工检查。

依据：[Meta 普通窗口透视边界](https://developers.meta.com/vr/essentials/horizon-os-passthrough/)、[官方媒体播放器示例](https://developers.meta.com/vr/documentation/spatial-sdk/spatial-sdk-sample-mediaplayer/)。

影院等待有效头显定位后按当前视线居中；未佩戴/追踪接口返回默认姿态时保留初始位置，不将默认姿态用于重新摆放。当前自动测试中定位返回默认值，实戴居中与手柄命中仍待用户确认。

完整回归与截图（在已安装 app 和 instrumentation APK 后）：

```powershell
python tools/Test-QuestCinema.py --serial QUEST_SERIAL --adb C:/path/to/adb.exe --metavr C:/path/to/metavr.exe --output reports/quest-cinema-20261003/final
```

Android UiAutomation 只用于普通窗口截图；影院截图使用 metavr 的 metacam 通道，避免把 Android 黑屏误当作 OpenXR 画面。场景关闭时停止轮询并释放 Media3，防止关闭后继续读取定位句柄。
