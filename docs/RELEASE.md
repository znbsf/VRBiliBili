# Quest 0.1.0-rc.4 预发布版

## 分发状态

当前 code 4 按维护者要求作为预发布版提供；下面 code 1–3 的本地交付记录保留为历史。GPL / Meta Spatial SDK 组合分发仍未解决，见 [分发审查](BINARY-DISTRIBUTION-REVIEW.md)。SDK notices 已补入不代表兼容性审查通过；不得以正式签名、安装成功或 OpenXR loader 存在宣称可以公开发布或已纯 OpenXR 迁移。

## 发布身份

- 当前版本：0.1.0，versionCode 4，ARM64；Git 标签 v0.1.0-rc.4。
- 包名：io.github.vrbilibili.quest，与 .debug 包并存。
- 影院使用 Meta Spatial SDK 0.14.0 / OpenXR；不支持 OpenVR / SteamVR。
- 状态：GitHub 预发布候选；未发布商店，设备交互验收未完成。

## 签名

本机首次创建项目专用 RSA 3072 位 PKCS12 密钥，保存在 `.local/release-signing/vrbilibili-release.p12`。`clients/piliplus/android/key.properties` 保存本地签名配置，两者均被 Git 忽略，不得提交或放入发布包。后续更新必须复用此密钥；用户应将密钥与配置存入自己的加密备份。

外部构建者创建自己的私有 key.properties，字段为 storeFile、storePassword、keyAlias、keyPassword。storeFile 建议使用正斜杠绝对路径。没有该文件时 Release 构建直接失败，不允许悄悄回退为 Android debug 签名。Debug 构建继续使用独立 debug 密钥。

```powershell
./tools/Build-QuestApp.ps1 -Mode release -TargetPlatform android-arm64 -BuildName 0.1.0 -BuildNumber 4
```

首次安装 Release 不迁移开发版账号；开发版数据原地保留。不要卸载开发版来解决签名差异。

## 验收门槛

- 验证包名、版本、ARM64、不可调试标记、签名证书与 OpenXR loader。
- Release 真实启动、选片、播放、影院进入/退出及休眠恢复。
- 图标默认无文字，实际手柄/手势悬停显示提示，菜单文字正常。
- 检查长时间播放、空间舒适度、网络失败与恢复。
- README 图片必须标明实际采集版本，不能用旧截图证明新构建通过。

具体构建哈希、签名指纹及验收结果另存 release-validation.json。Debug 的回归结果不能替代 Release 验收。

## 2026-10-05 本地交付

目录：`reports/quest-release-20261005/release/`（被 Git 忽略）。

- `VRBiliBili-0.1.0-quest-arm64.apk`：专用密钥签名，不可调试，仅 ARM64，已独立安装到 Quest 3。
- `VRBiliBili-0.1.0-source.zip`：对应工作区源码快照，不含密钥、账号、构建缓存和 reports。
- `SHA256SUMS.txt`：上述产物的 SHA-256。
- `signature.txt`：APK 签名验证输出。

APK SHA-256：`169dbc595d9dedc19ae62bfc768057174feea9dfcbb5001d71999af20fcf8dd7`。
签名证书 SHA-256：`965bac5fc009f98059d68213a6e7a6a29a27676e1e2d017b8302a62aa95599f9`。

本轮系统显示“需使用控制器”，继续按钮不可用，启动后的真实交互验收被挡住；不将安装成功当成播放成功。README 保留已验证开发版的真实内容截图并标明版本差异。Release 的悬停、手柄、休眠恢复和长时间播放仍为待验收，当前仅交付候选包。

## Q4 修复合入候选（code 2）

2026-10-05 新候选已构建、正式签名、安装并核对设备哈希。版本 0.1.0 / code 2；产物位于 `reports/quest-release-20261005/q4-merged-v2/`。上文 code 1 为保留的历史候选。XR 验收仍未完成。详见 [本轮报告](Q4-INTEGRATION-20261005.md)。

## code 3 混淆修复候选

已安装并核对哈希，详见 Q4-INTEGRATION-20261005.md。code 2 实测影院崩溃，code 3 修复构建已完成但遇追踪丢失，影院仍待复测；不可公开发布。

## code 4 预发布

[下载](https://github.com/znbsf/VRBiliBili/releases/tag/v0.1.0-rc.4)。APK SHA256：`078b1d27da5aa16399b8a50b0e3f77645a6c6d9ce19294eff43501146a505b1b`。附件提供当前提交的源码包和 SHA256SUMS.txt。普通播放及影院连续显示有实机证据，影院退出、手柄交互与进度交接尚未完成验收；见 [复核记录](QUEST-CONFIRM-20261005.md)。

公开截图为已检查的 2026-10-04 开发版应用画面，不含家庭透视背景。后续工作见 [纯 OpenXR 迁移](OPENXR-MIGRATION.md)。
