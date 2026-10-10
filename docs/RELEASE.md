# code 7 / rc.7 候选

正式包名和签名延续，versionCode 7。停用上游自动更新；手动更新指向本项目。旧 rc.6 tag/附件保持不变。新版本构建身份、哈希、实机与尚未通过项目以新 Release 验证附件为准。见 [RC7-VALIDATION](RC7-VALIDATION.md)。

---

# 0.1.0-rc.6 / code 6 普通面板预发布

版本名 0.1.0，versionCode 6，ARM64，包名 io.github.vrbilibili.quest。使用原正式签名流程，保留覆盖升级的数据兼容性；未访问私钥内容，未生成新签名身份。

下载与身份以 [Release](https://github.com/znbsf/VRBiliBili/releases/tag/v0.1.0-rc.6) 的 APK、SHA256SUMS、validation.json 与 source ZIP 为准。源码包由构建提交的 Git archive 生成，不是随手打包脏工作区。原生后端源码材料与独立 manifest 一并提供。

本版是普通 Android 面板，取消 Meta/XR 构建依赖、保留原 UI/账号/弹幕/画质与同页展开；拖动进度仅在松手时 seek。没有沉浸影院、空间回正、弧形屏幕、OpenXR 跨头显功能。

仍为预发布：本轮设备 unauthorized，未将 code 5 的历史真机结果转写成 code 6 通过。详见 [验收范围](PANEL-VALIDATION.md)。没有根目录 GitHub Actions 工作流时，不宣称 CI 通过；嵌套在 clients/piliplus/.github 的上游工作流不会自动作为本仓库 CI。

## 历史对应

| 候选 | 对应 | 状态 |
| --- | --- | --- |
| rc.4 / code 4 | 63fa4a8；APK SHA256 078b1d27da5aa16399b8a50b0e3f77645a6c6d9ce19294eff43501146a505b1b | 历史已发布，包含 Meta/XR，不代表当前架构 |
| code 5 | 63fa4a8 后四个工作区代码改动，已固化为 1cff53e；APK SHA256 b9f9b6a75fd2a4f35376d7ed9cf8f66f2fcebb88da77e66d76861267ae6925ef | 本地历史覆盖安装记录，无独立公开 Release |
| code 6 | 发布附件记录完整构建提交 | 本轮开源普通面板候选，未做真机复测 |

旧 code 1–4 详细记录保留在 Git 历史与 Q4-INTEGRATION、QUEST-CONFIRM 文档中。旧签名成功或 SDK notices 完整不能代替分发/体验验证。
