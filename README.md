# VRBiliBili

> 2026-10-10 实机后续：正式 rc.6 已保数据安装，部分播放、返回续播、离头入睡后正常唤醒检查通过。详细边界见 [实机记录](docs/QUEST-RC6-ACCEPTANCE.md)；下文先前 unauthorized 状态为发布时历史快照。

基于 [PiliPlus](https://github.com/bggRGjQaUbCoE/PiliPlus) 的 Quest 普通 Android 大屏 B 站客户端。使用 Flutter 界面与开源 media-kit 播放，在系统可调整窗口内选片、观看、拖动进度和展开/收起信息栏。

**0.1.0-rc.6 / Android code 6 是预发布候选。** 延续本地 code 5 的普通面板方向，不提供沉浸影院。不是 B 站或 Meta 官方客户端；不宣称支持 SteamVR 或其他头显。

main 已完整接入 rc.6 普通面板、XR 隔离及 seek 修复，并保留双方历史。发布 APK 精确对应 `cde0af0`；后续 main 补齐测试源码，详情见 [整合记录](docs/MAIN-INTEGRATION.md)。

## 本版变化

- Meta Spatial SDK core/toolkit/vr/isdk 与影院专用 Media3 不再进入运行时依赖；影院 Activity、XR 权限/功能与入口已脱离普通面板。
- 原 UI、弹幕、画质设置和单一控制栏保留。展开/收起在同一 Flutter 页面完成，不切换播放器。
- 拖动进度条时预览目标位置，松手后提交一次 seek，避免连续异步跳转互相覆盖；不主动改变暂停/播放状态。
- 视频保持比例、按可用区域裁切铺满。不同画面比例会裁掉边缘，不拉伸；编码在视频内部的黑边不自动消除。没有新增“完整显示”模式。

## 下载与保数据升级

从 [rc.6 预发布](https://github.com/znbsf/VRBiliBili/releases/tag/v0.1.0-rc.6) 获取 ARM64 APK、对应源码、原生播放器源码材料、验证记录和 SHA256SUMS。下载后先核对哈希。

包名仍为 `io.github.vrbilibili.quest`，沿用正式签名，versionCode 从旧候选递增到 6。使用 Android 覆盖安装保留该包的账号和数据：

```powershell
adb -s YOUR_QUEST_SERIAL install -r VRBiliBili-0.1.0-6-quest-arm64.apk
```

不要卸载或清数据来解决签名冲突。`.debug` 包的数据独立，正式包不会迁移其账号。该版本未发布商店。

## 验证范围

已做普通面板组件测试（拖动、切源、四种视频比例 × 两种窗口尺寸）、静态分析和 Android 编译。实际通过项、APK 哈希、构建提交和依赖/DEX/manifest 核验以 Release 的 `validation.json` 为准。

本轮 Quest 连接为 unauthorized，未在设备上安装或运行 code 6。选片网络播放、真实手柄/手势、解码器拖动落点、返回/重启续播、自然休眠恢复、长时间播放与裁切舒适度仍待 code 6 实机验收。code 5 与 Q4 的历史结果不等于 code 6 已通过。

## 构建与开源依赖

使用 Flutter 3.47.5 的独立补丁 SDK、Java 17、Android SDK 37 和锁文件。通过 `tools/Prepare-QuestFlutter.py` 在独立副本应用已有补丁；不要直接运行上游会改全局 Git 设置的 patch.ps1。

```powershell
./tools/Build-QuestApp.ps1 -Mode release -TargetPlatform android-arm64 -BuildName 0.1.0 -BuildNumber 6
```

Release 需要构建者自己的本地签名配置；缺失时失败，不回退 debug 签名。私钥、账号、构建缓存和本机记录不属于源码附件。构建脚本记录提交身份并拒绝带未提交改动的 Release 构建。

客户端保留 GPLv3；material_ui 为 BSD，webview 为 Apache-2.0，media-kit Dart 包为 MIT，原生 mpv/FFmpeg 及其依赖分别遵守自身许可。审计的 19 个 Git 依赖仓库均为公开仓库，两个本地 vendor 是有来源和许可证的开源源码。详见 [依赖审计](docs/DEPENDENCIES.md)、[源码与重建](docs/SOURCE-DELIVERY.md)、[第三方索引](docs/THIRD-PARTY-NOTICES.md)。

## 项目入口

[当前状态](docs/CURRENT-STATE.md) · [发布记录](docs/RELEASE.md) · [面板验收](docs/PANEL-VALIDATION.md) · [客户端来源](clients/piliplus/VRBILIBILI-UPSTREAM.md)

旧 Cinema 源码、实验测试、空间原型和截图留作历史，构建显式排除了影院代码和历史 Meta notices；不会自动恢复影院。rc.4 是历史 Meta/XR 版本，不能用来证明当前包的架构或体验。旧纯 OpenXR 迁移计划暂停。
