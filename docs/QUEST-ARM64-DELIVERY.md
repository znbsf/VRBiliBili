# Quest ARM64 本地交付：2026-10-04

最终候选 `2.1.5-quest.20261004.4+2026100404` 已以保留数据的方式覆盖安装到真实 Quest 3，设备 APK 与本地候选 SHA256 一致。应用包名为 `io.github.vrbilibili.quest.debug`，本轮保留既有调试签名与应用 UID，不是商店签名的正式发行包。

基础源码是 `cc7c8734bbcf7f0b50a192c58eaf6996e15e7a2d`；编译时还包含 22 个文件的交付差异，补丁 SHA256 为 `10c9aabbb4bdb96419f25ae75aafa2c20f583172bd0271a717b5eac3a988a5e4`。关于页显示基础提交、完整补丁哈希和构建标签 `Q4 cc7c8734+10c9aabb`，主侧栏也显示短标签。本候选在提交前构建，不能把所有内容说成仅来自基础 main。精确源文件哈希及结果见 [构建身份记录](quest-build-identity.json)。

APK：`141225486` 字节，ABI 仅 `arm64-v8a`；SHA256：`c9c90955d5b03aefea18fd8ffc348c6cf3aa7277981df3190bebee0c10f73f31`。

## 实测结果

| 项目 | 结果 |
| --- | --- |
| 签名一致、保留数据覆盖安装、设备 APK 哈希一致 | 通过 |
| 真实 Quest MediaKit 320×180 H.264/AAC 解码 | 未通过／未完成，不能推定通过 |
| 正确 CID、新会话的 CinemaActivity 场景／首帧／音视频计数 | 未通过／未完成，不能推定通过 |
| 真实影院回调退出，Flutter 暂停并等待进度保存 | 未通过／未完成，不能推定通过 |
| 两个独立 CID 的进度不互相覆盖 | 未通过／未完成，不能推定通过 |
| 新应用进程保留数据并恢复两个 CID | 未通过／未完成，不能推定通过 |

Q4 `seed` 的最终观察：正确 CID 的媒体处于 `loaded`、`processing=false`，识别为 320×180／60.010 秒，但 `playing=false`、`rawPlaying=false`、位置 0，未满足自动播放断言；`restart` 因前置阶段未通过而未执行。睡眠／窗口／焦点条件造成的环境阻碍与产品回归原因尚不能完全区分，不把该观察夸大为已确定的播放器代码缺陷。

独立历史证据：Q1 在真实 Quest 上 MediaKit 解码 320×180，时长 60.010 秒、位置 416ms、`rawPlaying=true`；其正确 CID、新会话的真实影院记录为场景／首帧成立、视频 13 帧、音频 66 缓冲、`playing=true`。之后停在 5091ms，54 视频帧／227 音频缓冲，`trackingAligned=false`。这些是短时真实证据，不能替代最终 Q4 的完整验收。产品 Dart 源码逐字节相同；后续影院源码仅增加调试证据字段，未修改其焦点暂停逻辑。

影院断言同时核对 CID、新会话 ID、进入后的单调采样时间、场景、首帧及视频／音频计数，拒绝残留状态。暂停／退出／前进 10 秒通过真实原生 HUD 的回调执行，不注入伪播放状态。若设备自然掉焦已经暂停，会单独记录，不能当作暂停按钮操作已通过。普通 UI 动作必要时使用正常唤醒键；影院播放轮询不持续唤醒、不改追踪或传感器行为。

`prepare`／`seed`／`restart` 分进程执行，只有 `prepare` 写入全新的自有测试路径，并先检查两个 Hive 键不存在。后续阶段保留数据、不重新播种。每轮使用新的 CID／标题／截图文件名，既有账号、缓存、旧测试数据不清空、不覆盖。原始 60 秒素材由本项目生成，仅在测试 APK 内。

本轮没有证明长期佩戴时的稳定播放、头显双眼画面、追踪对齐、手柄射线、舒适度、整机重启／断电恢复、登录或在线 DASH 画质切换。应用进程重启不等于整机重启。Android Activity 窗口截图不等于 XR 合成器图像；截图不可用独立记录，不用模拟器替代头显结果。

## 修复与回归

已合入九文件恢复补丁：安全读取坏类型／负值进度；保存时固定 CID 并返回 Future；影院退出等待真实写入后恢复暂停 Flutter；进入时拒绝未加载／处理中／CID 不一致；跳过坏下载元数据、非法路径标签及缺失／空媒体文件；下载卡片进度限制在合理范围。新增真机测试、明确 ARM64 依赖打包过滤、构建身份和 SDK 原始通知。

既有纯 Dart 交接回归 17 项通过；九个改动 Dart 文件分析 0 错误、0 警告、2 条 info。后续候选的产品 Dart 文件逐字节保持一致。五个模拟器真实应用进程恢复阶段此前通过，包括旧类型、负值、截断 Hive 尾帧和坏下载目录，参见 [恢复验证](ISOLATED-RECOVERY-VALIDATION.md)。历史 HTML 的四个真实 Chrome 测试输入逐字节未改变，复用其既有浏览器结果；没有把 DOM 或 mock 测试称为真实头显验收。

## 复验与发行边界

固定版本及 `pubspec.lock`，使用已安装、来源明确的 Flutter/JDK/Android SDK，先 `flutter pub get --offline --enforce-lockfile`。直接 Gradle 构建需 `-Ptarget-platform=android-arm64` 和 `-I tools/quest-arm64.gradle`；测试 APK 通过 `tools/quest-preview-regression.gradle` 选择 `QuestHardwareRecoveryRegression`。编译时传入本页身份记录中的 `pili.name/code/hash/time`、`vr.patch/label`；本机密钥配置留在私有路径，不在仓库中。

覆盖安装前核对 `apksigner verify --print-certs`、包名、ABI、版本和现有签名。只在既有已授权连接上使用 `adb install -r`；不卸载／清空数据，不改配对、网络、权限或系统追踪。人工续验需佩戴头显、处理系统追踪提示后确认当前版本，使用新的专用 CID 再运行三个阶段，避免重用已保存进度的测试 CID；先正常退出影院，再仅重启此应用进程。

本次没有创建 tag 或正式 GitHub Release，也没有上传 SDK 组合 APK。许可证组合缺少肯定放行依据，详见 [APK 分发核查](BINARY-DISTRIBUTION-REVIEW.md)。SDK 原始 `LICENSE`／`notices.html` 已打包并逐字节核验；它们不解决 GPL 组合许可，原生播放器匹配源码／补丁／构建脚本和修改／重链接义务仍需落实。去除 SDK 会失去沉浸影院，属于产品取舍，未自行实施。
