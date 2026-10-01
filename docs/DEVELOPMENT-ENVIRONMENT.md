# 开发环境范围

记录日期：2026-10-01。这里只保留可迁移的工具与验证边界；个人机器安装路径、安装日志和本机启动脚本不纳入仓库。

桌面原型需要 Node 与已有 Chromium。DOM 验证依赖固定为 linkedom 0.18.12；真实浏览器工具使用 Node >=22 的内置 WebSocket，无需下载浏览器。测试工具通过 `--browser` 或 `VRBILI_BROWSER` 接收现有浏览器路径。

客户端业务基础已选定 PiliPlus/Flutter；当前仓库仍是设计与原型，没有业务源码集成。先评审流程，再用头显确认尺度、输入与舒适度，之后选择一个实现路线做单业务面板与独立视频输出的技术验证。

| 已有工具验证记录 | 结果与限制 |
| --- | --- |
| Flutter 3.47.5 / Dart 3.13.4、Android/JDK17 | 已有电脑端 ARM64 示例构建记录；不代表业务客户端完成 |
| Meta Spatial SDK 0.14.0 官方示例 | Starter/MediaPlayer 示例构建已验证；空间业务集成和设备体验未完成 |
| Unity 6.3 LTS 6000.3.25f1 与 Android 工具链 | 文件与工具执行已核验；有效许可和项目功能验证仍待处理 |
| Spatial/XR Simulator | 组件准备记录存在；实际运行和图形验收仍待完成 |
| Quest、Meta 账号、ADB 部署 | 不属于本次桌面仓库基线验证 |

官方入口：[Meta 平台与工具](https://developers.meta.com/horizon/discover/platforms/)、[Spatial Simulator](https://developers.meta.com/horizon/documentation/android-apps/spatial-sim-overview/)、[Layout SDK](https://developers.meta.com/horizon/documentation/android-apps/meta-vr-layout-sdk/)。这些工具候选不构成最终产品路线决定。

机器相关 `.local/`、`.tools/`、完整 `QUEST-DEVELOPMENT-ENVIRONMENT.md` 和启动脚本保留在本地并被忽略。仓库不会自动安装 SDK、引擎、启动设备或登录账号。
