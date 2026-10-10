# VRBiliBili

基于 [PiliPlus](https://github.com/bggRGjQaUbCoE/PiliPlus) 的 Quest 普通 Android 大屏 B 站客户端。保留 Flutter/media-kit 播放、弹幕与画质设置，在同一页面展开/收起信息栏，单一进度条保持原播放状态并在松手后 seek。视频等比 cover 裁切，不拉伸；不是沉浸影院或跨头显 OpenXR 实现。

## 当前本地待发布候选：0.1.0-rc.7 / Android code 7

截至本轮结束，rc.7 尚未推送或发布；远端最新仍为 rc.6。本地修复与五个发布附件已完成，推送受授权审批阻塞。

修复 fork 更新入口：停用 PiliPlus 上游自动检查，手动更新统一打开本项目发布页，不再提供上游 APK 或错误回退。播放器拖动与裁切实现保持原行为，新增正式签名实机事件和窗口截图验收。main 包含完整普通面板、XR 隔离与 seek 修复历史。

从 [项目发布页](https://github.com/znbsf/VRBiliBili/releases) 获取明确预发布 APK、匹配源码、原生依赖源码和验证清单。以附件 validation.json、SHA256SUMS 和标签对应源码为准；`v0.1.0-rc.6` 仍固定 `cde0af0`，后续 main/code7 不冒充旧包来源。

正式包名 `io.github.vrbilibili.quest`，ARM64，沿用正式签名。先核验哈希，再正常保数据覆盖；不要通过卸载或清数据绕过签名冲突：

```powershell
adb -s YOUR_QUEST_SERIAL install -r VRBiliBili-0.1.0-7-quest-arm64.apk
```

## 构建与验证

使用隔离的 Flutter 3.47.5 / Dart 3.13 工具链、Java 17 与 Android SDK 37。构建需要自己的本地签名配置，缺失时拒绝 release，不回退 debug 签名；签名材料不在源码附件中。

```powershell
./tools/Build-QuestApp.ps1 -Mode release -TargetPlatform android-arm64 -BuildName 0.1.0 -BuildNumber 7
```

[rc.7 变更与验证边界](docs/RC7-VALIDATION.md) · [rc.6 实机证据](docs/QUEST-RC6-ACCEPTANCE.md) · [可复用 Quest 唤醒/测试方法](docs/QUEST-TESTING.md) · [当前状态](docs/CURRENT-STATE.md)

设备上的窗口截图、原生触摸事件与组件几何测试有不同覆盖。没有实测的手柄/手势、主观音画同步、全比例裁切和长时舒适度不标为通过；临时佩戴模拟也不算自然休眠。最终本轮结果以各版本验证附件为准。

## 开源来源与许可

本项目非 Bilibili 或 Meta 官方客户端。继承 [PiliPlus GPLv3 许可证](clients/piliplus/LICENSE)，固定上游来源与修改见 [VRBILIBILI-UPSTREAM](clients/piliplus/VRBILIBILI-UPSTREAM.md)。旧影院源码仅供追溯，已排除于普通面板运行时构建。

[依赖审计](docs/DEPENDENCIES.md) · [第三方声明](docs/THIRD-PARTY-NOTICES.md) · [源码交付](docs/SOURCE-DELIVERY.md) · [分发核查](docs/BINARY-DISTRIBUTION-REVIEW.md) · [发布记录](docs/RELEASE.md)
