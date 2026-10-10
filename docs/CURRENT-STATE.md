# 当前状态：普通 Android 面板

唯一推荐目标：普通 Android 大屏播放，保留原有选片/UI/账号/弹幕/画质能力，同页展开收起、单一圆点进度条、松手后一次 seek、等比 cover 裁切。当前产品运行时已隔离 Meta/XR；历史影院源码保留追溯，不恢复影院入口。

当前预发布为 [v0.1.0-rc.7 / code 7](https://github.com/znbsf/VRBiliBili/releases/tag/v0.1.0-rc.7)，精确构建源码 `1568ef4b0fc99306f184e5560dacd65a33426e0f`。main 通过正常快进纳入代码，后续文档仅补记实际结果。rc.7 禁用上游自动更新，手动入口只开本项目发布页。

16 项组件测试、范围静态分析、正式构建与 code 7 Quest 保数据升级通过。实际正确鼠标/触屏拖动、返回 0:17 续播、暂停 0:30 自然休眠后正常唤醒恢复、手动更新 URL 已验证。旧无按键 mouse 注入失败属于测试输入问题，没有为此改写生产播放器。真实窗口像素已取得，但 cover 会裁去字幕/水印边缘。

优先下一步：在不回到影院开发的前提下，验收实体手柄/手势、完整播放中休眠循环、典型比例与字幕可读性、长时音画同步。若必须完整显示字幕/边缘，需单独决定是否提供 contain/cover 切换；当前不擅改既定 cover 行为。上述缺口完成前保持 prerelease。

历史保留：rc.6 固定 `cde0af04f800c54b86f468e15f26ed185b9ff3be`；code 5 原始改动固化于 `1cff53e633a6f734e4bbae28dc3a05b4ded23a89`。main 的双亲正常合并保留原恢复代码与 rc.6 普通面板改动，详见 [MAIN-INTEGRATION](MAIN-INTEGRATION.md)。旧工作树、分支和发布附件不删除、不覆盖。

[本轮 rc.7 证据与限制](RC7-VALIDATION.md) · [可复用 Quest 测试](QUEST-TESTING.md) · [历史 rc.6 验收](QUEST-RC6-ACCEPTANCE.md) · [发布记录](RELEASE.md) · [依赖](DEPENDENCIES.md) · [源码交付](SOURCE-DELIVERY.md)
