# 普通面板依赖审计（2026-10-10）

基于 pubspec.lock、实际解析 package_config、各包许可证、GitHub 仓库元数据和 Gradle releaseRuntimeClasspath，而非按名称猜测。

| 类别 | 实际对象 | 处理 |
| --- | --- | --- |
| 封闭 SDK | Meta Spatial SDK 0.14.0 core/toolkit/vr 及传递 isdk | 从运行时依赖移除；Cinema Kotlin 与旧测试源码显式不参与编译；manifest 不再声明影院、XR 功能或 Oculus native library |
| 影院专用开源播放器 | AndroidX Media3 1.5.1 | 普通面板不用，移除直接依赖，不称为封闭 SDK |
| 普通播放开放实现 | Flutter + media-kit 固定 fork 73771ec38176be2d984a3049c28177bce23b54a0 | 保留已有播放器、账号和内容解析；没有重启沉浸功能，也没有把功能删除伪称为等价 OpenXR 替换 |
| 本地源码 overrides | vendor/material_ui、vendor/webview | 来源分别为 Flutter Material 补丁与 webview fork，带 BSD / Apache-2.0 原文；保留，不是私有二进制 |
| Git 包 | 19 个独立仓库；锁文件共 252 包 | 本轮 GitHub API 全部返回 public；包级许可证已读。仓库 API 的 license=null/NOASSERTION 不等于无许可证，须看具体包文本 |
| Maven/包源 | Google Maven、Maven Central、Gradle Plugin Portal、pub.dev、公开 GitHub fork | 未发现需要私有包源凭据的声明。离线缓存解析不能保证未来网络一直可用 |
| 原生后端 | libmpv/FFmpeg + 字体、字幕和解码库 | 开源但有独立许可证/源码义务，不能用 Dart MIT 替代；见源码交付 |

机器可读 [审计清单](dependency-audit.json) 保留包版本、固定 revision 和仓库可见性，不包含本机缓存路径。Flutter 子包的许可证继承 SDK 顶层 LICENSE。Dart 包许可证汇总与客户端 GPL 原文打包到 APK 的 third_party/open-panel。

对最终 APK 还须核对：ARM64、包名/版本/正式签名、无 Cinema Activity/XR 权限、无 Meta/OpenXR native library、DEX 无 com.meta.spatial 类、实际 libmpv 与固定 JAR 一致。仅依赖文本变更不能证明 APK 已干净，结果在发布 validation.json。

旧影院 Kotlin、Dart、测试和 Meta 通知源码保留为历史，不在当前运行时使用。构建排除不是“所有历史文件已删除”，也不是“全功能纯 OpenXR 迁移”。
