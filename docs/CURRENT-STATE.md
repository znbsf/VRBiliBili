# 当前状态：普通 Android 面板

本文件是唯一当前状态入口。核验日期 2026-10-10；后续接手先核对实际源码、APK、设备身份与远端状态，不能仅凭旧 pass 沿用。

## 目标、代码与交付

唯一目标是 Quest 普通 Android 大屏 B 站客户端：保留选片/账号/弹幕/画质，同页展开收起、单一圆点进度条、松手一次 seek、等比 cover。Meta/XR 运行时已从当前产品隔离，旧影院源码保留追溯，不恢复旧影院入口。

[rc.7 / Android code7](https://github.com/znbsf/VRBiliBili/releases/tag/v0.1.0-rc.7) 已公开为 prerelease；tag、APK 内嵌提交和源 ZIP 固定 `1568ef4b0fc99306f184e5560dacd65a33426e0f`。main 已正常纳入代码，后续真实截图与文档整理不改变应用程序树，也不代表另一个 APK。正式包名 `io.github.vrbilibili.quest`，原正式签名延续。rc.6 tag/附件保持不变。

code7 禁用 PiliPlus 上游自动更新，手动入口与旧下载调用只打开本项目 releases；不自动下载/安装，也不在失败时回退上游。

## 验证适用性

| 层 | 本轮结果 | 适用性与边界 |
| --- | --- | --- |
| 组件/分析/构建 | pass | current：16 项测试、改动范围分析及正式构建；对应 1568ef4 |
| 包与已安装身份 | pass | current：ARM64/非 debug/原签名/无 XR；设备 code7 APK 与附件哈希相同 |
| 真实播放与输入 | pass | current：正确 primary mouse、touchscreen、暂停、返回 0:17 续播；不是实体手柄/手势验收 |
| 暂停后自然休眠 | pass | current：0:30 → 实际 Asleep → 普通唤醒 0:30 → 播放继续；不等于佩戴唤醒或播放中循环 |
| 更新入口 | pass | current：设备 VIEW intent 指向本项目；无上游弹窗；错误不回退由组件测试覆盖 |
| 分发 | pass | current：五附件下载哈希与源码身份核验，独立 prerelease；rc6 保持原身份 |
| 窗口像素 | pass（有限范围） | current：1375×900 真实应用窗口；cover 会裁去部分字幕/水印边缘 |
| 物理交互、长时音画、所有比例、佩戴唤醒、播放中休眠 | not_run | needs_recheck：完成前不能宣称稳定版 |
| GitHub Actions | not_run | 未配置根目录 CI；本轮 0 runs，不宣称 CI 通过 |

本轮精确数值见 [RC7-VALIDATION](RC7-VALIDATION.md)。旧 rc4/5/6 结果保留其历史版本，不继承为 code7 通过。后续程序、输入或环境变化时，仅已证明不受影响的证据可保持 current；其余标 needs_recheck。

## 本次阶段范围

最初只读整理阶段的“不要推送/发布/设备测试”已被本次用户后续明确授权覆盖：推进解耦、测试、正常推送 main、独立 rc7 prerelease、公开选定真实播放截图、发布后可恢复清理。两次审批拒绝仅是当时授权证据不足的历史事件；之后正常审批接受并完成推送发布。它们不是现在的仓库禁令，也不是未来自动授权。

本次仍保留：不改审批/安全规则、不读凭据、不清用户数据、不绕过 Guardian、不覆盖 rc6、不丢分歧代码、不操作其它设备或项目。未来任务按当时用户授权和实际目标执行。

## 优先下一步

先验收实体手柄/手势、完整播放中休眠循环、典型比例与字幕可读性、长时音画同步。若要求完整显示画面/字幕边缘，再决定是否增加 contain/cover 切换；不擅改既定 cover 行为。保持 prerelease。

[可复用测试方法](QUEST-TESTING.md) · [环境与身份参数](DEVELOPMENT-ENVIRONMENT.md) · [发布记录](RELEASE.md) · [依赖审计](DEPENDENCIES.md) · [源码交付](SOURCE-DELIVERY.md) · [历史目录](history/README.md) · [清理与恢复说明](CLEANUP-20261010.md)
