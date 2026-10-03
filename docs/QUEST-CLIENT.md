# Quest 客户端

2026-10-03：从设计进入实际开发。该状态取代旧阶段文档中“尚未开发业务 APK”的描述；旧原型检查记录仍是历史记录。

## 使用

在 Quest 应用库的“未知来源”中启动 **VRBiliBili**。选择推荐视频或搜索视频，进入详情页后点击右上方 **空间播放**。首次加载需完成 B 站取流。

空间控制台提供暂停/播放、时间拖动、前后 10 秒、倍速、重试、返回内容页、音量、透视明暗与透视切换。返回内容页同步同一视频的时间并保持暂停，避免两套播放器同时出声。手柄/手势的实际舒适度与操作准确度仍需佩戴头显人工验收。

辅助面板最多两个：选集/画质/字幕、评论。编辑布局可以选择面板、移动、缩放、锁定或跟随主窗；支持专注/桌面/社交预设和保存各预设布局。“召回窗口”按当前头部水平方向重新放置整组窗口。主视频保持不透明，辅助背景透明度独立调整。普通弹幕可调密度/字号，与可用字幕叠加在视频前，暂停时使用媒体时间冻结。

浏览、搜索、扫码登录、历史和稍后再看沿用 PiliPlus；仅对本人有权限的资源取流。登录账号与受限资源未在自动化中操作。河流/跳出弹幕、朗读与空间音频仍为设计实验项，未将普通播放器伪称为这些能力。摆放模式会显示独立拖拽条，使用 SDK Grabbable 移动窗口；同一操作也提供按钮路径。真实手柄/手势拖拽仍待佩戴验收。

## 构建

工具：Windows、Git、Python 3、JDK 17、Android SDK 平台 37.0、Flutter 3.47.5。ARM64 最低 Android API 34，目标 Quest 3。所有路径在仓库内相对解析；代理仅为当前构建进程可选参数。

```powershell
python tools/Prepare-QuestFlutter.py --source C:/path/to/unmodified/flutter
./tools/Build-QuestApp.ps1 -Jdk C:/path/to/jdk17 -AndroidSdk C:/path/to/Android/Sdk
# 如本机已有可用代理，可额外指定 -ProxyHost 127.0.0.1 -ProxyPort 10646
```

准备脚本只复制到新的私有 SDK，再应用上游的 24 个框架补丁，不重置原 SDK、不改全局 Git 配置。已经准备好的 SDK 通过 `-FlutterSdk` 传给构建脚本。Windows 插件链接权限不足时使用生成目录中的目录联接。材料组件和 WebView 的补丁/短路径源码已随仓库保存，出处及许可证见客户端来源说明。

产物：`clients/piliplus/build/app/outputs/flutter-apk/app-debug.apk`。
开发包 ID：`io.github.vrbilibili.quest.debug`，标签 VRBiliBili；不会覆盖 PiliPlus 或 Meta 示例。使用开发签名，未作为商店发行包。安装请明确指定目标 Quest 序列号：

```powershell
adb -s QUEST_SERIAL install -r clients/piliplus/build/app/outputs/flutter-apk/app-debug.apk
```

## 自动化和证据

```powershell
python tools/quest-spatial-smoke.py --adb C:/path/to/adb.exe --serial QUEST_SERIAL --output reports/spatial-smoke
# 在配置好 JAVA_HOME/ANDROID_HOME 的命令行中：
cd clients/piliplus/android
./gradlew.bat :app:assembleDebugAndroidTest -Ptarget-platform=android-arm64
# 回仓库根目录，安装 androidTest APK，再运行：
adb -s QUEST_SERIAL install -r clients/piliplus/build/app/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s QUEST_SERIAL shell am instrument -w io.github.vrbilibili.quest.debug.test/com.example.piliplus.QuestUiDriver
```

独立测试片验证新会话、视频/音频解码、暂停、拖动、继续、缩放与返回时间。界面自动化通过实际 Flutter 可访问性控件选取公开推荐视频并点击空间播放，然后检查原生解码、辅助数据、画质切换和返回暂停状态。测试需要头显连接并保持唤醒；它不能替代佩戴视觉、手柄、手势、长时播放/散热验收。

测试片与入口仅存在 debug 构建。ADB 测试 Activity/Receiver 要求 `android.permission.DUMP`；发行构建不含它们。诊断文件仅 debug 写入应用私有目录，包含时间、媒体 CID、解码计数与数据条数，不含 Cookie、媒体地址或账号凭据。设备截图与完整测试日志保存到忽略的 `reports/`，不提交。

已确认结果：`reports/quest-native-20261003/spatial-clean/results.json` 为 7/7；`online-ui-journey-4.txt` 为真实 1920 宽视频与音频进入原生空间播放成功。`online-ui-journey-6.txt` 进一步通过实际画质切换、评论/选集数据加载，以及返回同一视频 3 秒处保持暂停。早期失败记录保留，不算通过。


## 本次最终交付

最终 ARM64 开发 APK 与 Quest 已安装 APK 的 SHA-256 一致：
`0bac08754767cb7265ca78f816c1cc37cd652ace9b1e234ed73fd914f7e686b1`。
结果摘要见 [quest-client-validation.json](quest-client-validation.json)。

- `spatial-final/results.json`：7 项通过、0 项失败。
- `online-ui-final.txt`：真实推荐选片、进入原生播放器、视频/音频解码、选集与评论数据、实际切换画质、同一视频返回 3 秒且暂停，全部通过。
- 修改的 Dart 文件静态分析：0 error、0 warning、1 条代码风格 info。
- 新增核心文件空白检查通过；整份上游副本仍含原有空白风格问题，未为此改写第三方源码。
- 已实现但尚无人工验收：手柄/手势拖拽、透视明暗与辅助透明度观感、字幕显示、长时间观看舒适度。登录/个人云端数据未使用真实账号测试。

测试通过不等于完整商店发布验收。本次交付可在头显启动的真实客户端开发包，后续人工意见应在此客户端上修订。
