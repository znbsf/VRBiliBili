# 0.1.0-rc.7 / code 7 普通面板待发布候选

尚未创建远端 rc.7；待授权确认后发布为 prerelease。拟定 tag、现有源码 ZIP 与 APK 内嵌构建提交固定 `1568ef4b0fc99306f184e5560dacd65a33426e0f`。正式包名/签名延续，versionCode 7。禁用上游自动检查，手动入口仅指向本项目。

APK SHA256 `da179d37ec4e41296424983ca47733825bd35ec357392cdc440c80d214a427f4`，26,234,610 字节。附件同时提供源码 ZIP、原生依赖源码 ZIP、validation.json 与 SHA256SUMS。设备安装后的 APK 与附件完全一致。

本轮 16 项组件测试、静态分析、正式构建、保数据覆盖、真实拖动、返回续播、暂停后自然休眠恢复及手动发布入口通过。取得真实应用窗口像素；cover 裁切边缘与物理交互等缺口仍存在，见 [RC7-VALIDATION](RC7-VALIDATION.md)。不宣称稳定版或 CI 通过。

---

# 0.1.0-rc.6 / code 6 普通面板预发布

版本名 0.1.0，versionCode 6，ARM64，包名 io.github.vrbilibili.quest。使用原正式签名流程，保留覆盖升级的数据兼容性；未访问私钥内容，未生成新签名身份。

下载与身份以 [Release](https://github.com/znbsf/VRBiliBili/releases/tag/v0.1.0-rc.6) 的 APK、SHA256SUMS、validation.json 与 source ZIP 为准。源码包由构建提交的 Git archive 生成，不是随手打包脏工作区。原生后端源码材料与独立 manifest 一并提供。

本版是普通 Android 面板，取消 Meta/XR 构建依赖、保留原 UI/账号/弹幕/画质与同页展开；拖动进度仅在松手时 seek。没有沉浸影院、空间回正、弧形屏幕、OpenXR 跨头显功能。

rc.6 发布当时设备 unauthorized，未将 code 5 历史结果转写成 code 6 通过。后续补充实机结果见 [QUEST-RC6-ACCEPTANCE](QUEST-RC6-ACCEPTANCE.md)，并非发布当时已完成。原发布范围保留在 [PANEL-VALIDATION](PANEL-VALIDATION.md)。没有根目录 GitHub Actions 工作流时，不宣称 CI 通过；嵌套在 clients/piliplus/.github 的上游工作流不会自动作为本仓库 CI。

## 历史对应

| 候选 | 对应 | 状态 |
| --- | --- | --- |
| rc.4 / code 4 | 63fa4a8；APK SHA256 078b1d27da5aa16399b8a50b0e3f77645a6c6d9ce19294eff43501146a505b1b | 历史已发布，包含 Meta/XR，不代表当前架构 |
| code 5 | 63fa4a8 后四个工作区代码改动，已固化为 1cff53e；APK SHA256 b9f9b6a75fd2a4f35376d7ed9cf8f66f2fcebb88da77e66d76861267ae6925ef | 本地历史覆盖安装记录，无独立公开 Release |
| rc.6 / code 6 | cde0af04f800c54b86f468e15f26ed185b9ff3be | 历史预发布；后续实机结果见 rc.6 验收记录 |
| rc.7 / code 7 | 1568ef4b0fc99306f184e5560dacd65a33426e0f | 本地待发布候选；本轮真实拖动、续播、暂停休眠与更新入口验证通过，仍有明确验收边界 |

旧 code 1–4 详细记录保留在 Git 历史与 Q4-INTEGRATION、QUEST-CONFIRM 文档中。旧签名成功或 SDK notices 完整不能代替分发/体验验证。
