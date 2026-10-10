> 历史快照：保留原始版本、结果与经验；不构成当前目标或操作授权。旧“只读/不推送/不发布”、设备状态和源码-only 限制仅适用于记录当时阶段。当前状态见 [CURRENT-STATE](../CURRENT-STATE.md)；历史 pass 不自动等于当前 pass。

# 2026-10-05 code 4 实机复核

本轮验证已有正式候选，不重新构建或安装，不清除账号数据。

- 在线 Quest 3 安装版本为 0.1.0 / versionCode 4。设备 base.apk SHA256 实读为 `078b1d27da5aa16399b8a50b0e3f77645a6c6d9ce19294eff43501146a505b1b`，与本地产物一致。
- 官方 CLI 启动 MainActivity 成功，首页加载真实内容。取消上游更新提示后，通过语义选择器选中视频。截图显示真实视频、暂停图标及 0:20 / 18:12 进度。本轮样本的控制栏紧贴画面；不据一个素材宣称所有比例通过。
- 点击“全屏影院”成功进入 CinemaActivity；连续 metacam 截图显示不同视频帧，确认影院实际显示并继续播放。本轮未复现 code 2 的进入影院崩溃。
- 首张影院截图下半部为大片灰色；后续截图未重现同样灰块，但银幕位于视野左侧。未确定是姿态、场景还是截图问题，不判定布局/居中验收通过。
- 官方 Android Back 输入后仍停留 CinemaActivity。ui actions 只枚举到系统控件，未能操作影院“退出”按钮；退出与进度交接未验证。此结果不证明真实手柄退出按钮失败。
- 未验证真实手柄/手势、声音、长时间稳定性、休眠恢复或完整 XR session 状态序列。Android 窗口焦点不等同于 XR 焦点。
- 以 120 秒自动到期的近距覆盖执行检查；结束显式恢复并实读 enabled=true、prox_override=DISABLED、autosleep_disabled=false。

证据保存于本地忽略目录 `reports/quest-confirm-20261005/`：initial-state.json、playback-actions.json、panel.png、cinema.png、cinema-second.png、returned.png、cinema-actions.json、cinema-log.txt。returned.png 是发送 Back 后的截图，实际仍为影院，不是返回成功证据。截图可能包含透视房间背景，不用于公开展示。

结论：code 4 的真实普通播放与影院进入/显示已获得新实机证据；完整影院往返和交互验收仍未完成。
