> 历史快照：保留原始版本、结果与经验；不构成当前目标或操作授权。旧“只读/不推送/不发布”、设备状态和源码-only 限制仅适用于记录当时阶段。当前状态见 [CURRENT-STATE](../CURRENT-STATE.md)；历史 pass 不自动等于当前 pass。

# 旧 OpenXR 迁移计划：暂停

当前产品方向是 [普通 Android 大屏](../CURRENT-STATE.md)。code 6 将未使用的 Meta/XR 链路隔离出运行时构建，使用现有 Flutter/media-kit 普通播放，不实现新 OpenXR 影院。

此前“移除 Meta 同时保留沉浸影院”的计划未完成，不因发布 rc.4 而成立；后续用户试用要求取消影院，已由 code 5/6 方向取代。历史设计和源码保留，不据此自行恢复入口。
