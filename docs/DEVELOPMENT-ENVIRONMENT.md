# 当前环境与身份参数

这是环境映射，不是操作授权；单次事实在 [RC7-VALIDATION](RC7-VALIDATION.md)，方法在 [QUEST-TESTING](QUEST-TESTING.md)，唯一状态入口为 [CURRENT-STATE](CURRENT-STATE.md)。

| 项 | 当前构建基线 |
| --- | --- |
| 产品目录 | clients/piliplus，Flutter/media-kit 普通 Android 面板 |
| 工具链 | Flutter 3.47.5 / Dart 3.13.4、Java17、Android SDK37；沿用本机已隔离工具链，不自动安装 |
| 正式包 | io.github.vrbilibili.quest；ARM64；versionName0.1.0 / versionCode7 |
| 当前构建源码 | 1568ef4b0fc99306f184e5560dacd65a33426e0f；后续文档不重新构建 APK |
| 设备目标 | 本次是 Quest3；每次必须现场核对序列/型号及归属，不把静态参数当永久占用权 |
| 构建入口 | tools/Build-QuestApp.ps1；正式签名缺失则拒绝 release，不回退 debug |
| 源/依赖 | pubspec.lock、dependency-audit.json、native-source-manifest.json；锁定开源来源及许可证 |

个人工具路径、ADB 序列、签名配置与原始日志保留本地，不写入公开配置。`.tools/`、当前依赖缓存、`.local/release-signing/` 和客户端签名配置保持原位且未读凭据。历史 Unity/Meta 示例不属于当前 APK 构建依赖，也不是未来自动恢复影院的依据。

仓库保留旧浏览器原型源码及测试以追溯经验；它们不代表当前 Android 客户端验收。Node/已有浏览器相关要求按对应工具说明执行，不为文档清理安装新工具。
