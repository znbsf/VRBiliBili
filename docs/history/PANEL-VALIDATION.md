> 历史快照：保留原始版本、结果与经验；不构成当前目标或操作授权。旧“只读/不推送/不发布”、设备状态和源码-only 限制仅适用于记录当时阶段。当前状态见 [CURRENT-STATE](../CURRENT-STATE.md)；历史 pass 不自动等于当前 pass。

# code 6 面板验收

> 2026-10-10 实机后续：正式 rc.6 已保数据安装，部分播放、返回续播、离头入睡后正常唤醒检查通过。详细边界见 [实机记录](QUEST-RC6-ACCEPTANCE.md)；下文先前 unauthorized 状态为发布时历史快照。

## 本轮已执行的离线检查

- 11 项 Flutter 组件用例：真实拖动过程中不被解码器 position 更新抢回；松手只提交一次 seek；未知时长禁用；位置越界钳制；切换 CID 丢弃未完成拖动；16:9、4:3、竖屏、超宽在 640×360 与 500×500 中等比例 cover。
- 隔离后的 Release Kotlin 编译通过；PanelRecoveryRegression 编译通过。发布附件记录最终构建、分析与产物检查结果。
- 历史纯 Dart 17 项与截图工具 20 项曾在本轮 review 通过，仅证明其辅助逻辑；不是 code 6 播放证据。

## 设备缺口

Quest 当前 unauthorized，本轮未安装、清数据、改变感应器或运行设备用例。未完成：真实 B 站选片、网络/画质变化、拖动落点与音画同步、返回列表/重启续播、自然睡眠恢复、手柄射线/手势、长期播放、裁切字幕/边缘体验。

## 面板专用测试入口

`PanelRecoveryRegression` 源自 main cba23e5 的面板恢复检查，去掉影院路径，保留新 CID/目录占用预检、线程安全日志、生命周期电源记录、双 CID 隔离与 loaded 后 3.5 秒内首次解码位置检查，防止从零播放追上目标位置冒充续播。

仅在授权设备上使用自有 fixture；包必须为 debug 0.1.0 / code 6。运行前选未占用的奇数 firstCid（910041000009–099）和唯一数字 fixtureGroup。显式参数必需，不接受无效 CID 回退到默认值。prepare 仅创建新目录，拒绝覆盖已有进度/fixture；panelSeed 保存不同 A/B 进度；panelRestart 必须是新进程且不重新种入进度。lifecycle 覆盖主动后台/前台的暂停状态，不等于自然头显休眠。

构建 AndroidTest APK 后入口为 `com.example.piliplus.PanelRecoveryRegression`，阶段为 prepare、panelSeed、panelRestart、lifecycle。只对该测试包/应用启动新进程，不重启 adb 服务、不卸载、不清数据、不持续唤醒或伪造佩戴。测试 APK 不作为用户 Release 附件。
