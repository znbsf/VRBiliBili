> 历史快照：保留原始版本、结果与经验；不构成当前目标或操作授权。旧“只读/不推送/不发布”、设备状态和源码-only 限制仅适用于记录当时阶段。当前状态见 [CURRENT-STATE](../CURRENT-STATE.md)；历史 pass 不自动等于当前 pass。

> 历史记录：以下验收和版本结论仅适用于文中旧构建。当前产品为普通 Android 面板；请先阅读 [CURRENT-STATE](../CURRENT-STATE.md)。旧影院源码和证据保留，不表示恢复影院入口或通过 code 6 实机验收。

# Quest ARM64 本地交付（2026-10-04）

Q4 debug候选 `2.1.5-quest.20261004.4+2026100404` 已保数据更新到真实Quest 3，APK仅arm64-v8a、UID10056。它沿用现有调试签名，未完成正式发行验收。

安装应用对应交付源码提交 [`d73f75251aed9866e909f3348bbc2f5d490541d8`](https://github.com/znbsf/VRBiliBili/commit/d73f75251aed9866e909f3348bbc2f5d490541d8)。APK编译时基础提交仍是 `cc7c8734bbcf7f0b50a192c58eaf6996e15e7a2d`，22文件补丁SHA256 `10c9aabbb4bdb96419f25ae75aafa2c20f583172bd0271a717b5eac3a988a5e4`，界面标签 `Q4 cc7c8734+10c9aabb`。后续main仅更新测试与文档/截图，不能将新的main提交说成APK已嵌入的基础提交。固定构建清单见 [身份记录](../quest-build-identity.json)。

APK141225486字节，SHA256 `c9c90955d5b03aefea18fd8ffc348c6cf3aa7277981df3190bebee0c10f73f31`。本轮只重编译和更新测试APK，产品源码、应用APK、UID和用户数据未变化。

## 实际验收

| 项目 | 结果 |
| --- | --- |
| 兼容签名、保数据更新、设备APK哈希 | 通过 |
| 真实Quest MediaKit离线解码 | 通过；320×180 H.264/AAC、60.010秒，实际进度及rawPlaying=true |
| 面板真实暂停/前进按钮和双CID隔离 | 通过；A保存10750ms，B21166ms，B不覆盖A |
| 面板新进程续播 | 通过；PID9536→10360，未重建种子，首次真实解码A11083/B21750ms，各在loaded后507ms观测 |
| 正确CID、新会话的真实CinemaActivity持续解码 | 未通过；新影院在系统休眠时暂停，无本次新会话帧证明 |
| 真实影院退出、返回暂停Flutter并持久化 | 未完成 |
| 影院返回后的重启续播、完整端到端 | 未完成；面板结果不能替代此路径 |
| 佩戴、双眼画面、手柄射线、跟踪对齐、舒适度 | 需要用户实机评审 |

原Q4首次seed停在loaded但0秒、不播放。补读同阶段系统日志证实：VrPowerManagerService在HEADSET_UNMOUNTED后进入睡眠，系统以sleep原因暂停MainActivity，VR runtime进入STANDBY。不能凭该结果推定具体Dart pause调用者，也未将其作为确定的产品autoplay缺陷改写播放策略。

拆开加载/播放后，R5的正确CID910041000009实际解码到500ms；点击“全屏影院”时interactive从true转false，新CinemaActivity和MainActivity均被系统sleep暂停。旧Q1 CID1/session状态被正确拒绝，未冒充新会话通过。历史Q1曾短时得到新会话13视频帧/66音频缓冲，随后5091ms暂停、trackingAligned=false；该历史不代表Q4完整验收。

本轮测试修复：真实Play按钮恢复；生命周期/电源/uptime记录；线程安全日志；新CID和目录占用预检；正确B缓存探测；退出后不读已销毁的页面通道；不将已消费并清空的defaultST判为失败。A/B通过真实按钮推进到明显不同的约10/20秒，续播要求首次解码在loaded后3.5秒内接近保存位置，防止从零播放误通过。restart保留现有素材和进度，不重新写种子。

自有CID910041000013/014在R7正常离页保存，R8仅在新进程读取并解码恢复；两段真实Pause按钮均执行，B操作后A缓存为11083ms。新的测试APKSHA256 `70d204e5ebbe3ff4979329db5a5c591ae43b59cecb9092d0a5693019ed3f168e`，runnerSHA256 `891d5abf0f66ae1b8d3ef9025719c10d1d32323c53f5e9e39d8e35a60af0d797`；与应用APK身份分开。

## 构建、数据与下一步

17项Dart交接回归此前通过；产品Dart未变，既有分析0错误/0警告/2个info。本轮离线AndroidTest构建通过、真机面板上述阶段通过。真实Chrome原型已完成的验证覆盖未变化；DOM、模拟器与真实头显结果分别记录。

工具链、pubspec.lock、Android ABI与 [构建环境](../DEVELOPMENT-ENVIRONMENT.md) 保持固定。测试由 `tools/quest-preview-regression.gradle` 选择 `QuestHardwareRecoveryRegression`，离线单工作线程构建。密钥、账户资料、签名机器私有路径不进入仓库。APK内SDK原始LICENSE/notices已逐字节核对；这不构成重新授权。

普通UI动作开始时可发一次正常唤醒键；不在影院轮询中持续唤醒、不修改接近传感器/追踪设置、不模拟佩戴。阶段间可等待系统自然睡眠再开始下一项正常UI动作，不设置保活。应用数据不清空、账户不登录/变更、网络/权限/配对不调整。自有测试片段仍留在缓存列表，便于交接；不删除用户内容。

继续影院验收的最小条件：用户戴上Quest，正常处理系统追踪提示并保持佩戴约2–3分钟，保留USB连接。确认Q4标识后使用新自有pair，例如预检未占用的910041000015/016、fixtureGroup8，依次prepare→seed→restart；每阶段新应用进程但保留数据，先通过真实影院退出再重启。已有CID1–14不重新seed。自动回调不能代替佩戴者对双眼画面/控制器/舒适度的评价。

README的Quest截图已更新为Q4 R5真实MainActivity窗口原图；不是XR合成器双眼图像。另两张模拟器图片保留明确标注。

未创建正式tag/Release或公开上传APK；[公开分发风险](../BINARY-DISTRIBUTION-REVIEW.md)仍待肯定依据及原生依赖对应源码/构建/relink义务。独立核验的结论是未解决风险，不能推广为所有GPL/Meta组合一律不允许。没有开始OpenXR架构替换，也未自行决定去掉影院。
