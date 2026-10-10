# 当前状态：普通 Android 面板

唯一推荐目标是普通 Android 大屏播放：保留既有界面、同页展开/收起、单一进度条、松手后一次 seek、保持比例的 cover 裁切。Meta/XR 运行时从当前产品构建隔离；保留旧源码供历史追溯，不恢复影院入口。

main 已通过正常双亲合并接入 rc.6 功能，保留原 main 的恢复修复、测试与交接证据。分叉原因与逐项处理见 [MAIN-INTEGRATION](MAIN-INTEGRATION.md)。

公开预发布仍为 `v0.1.0-rc.6`，精确源码提交 `cde0af04f800c54b86f468e15f26ed185b9ff3be`；不能把后续 main 构建称为这一旧 APK。code 5 原始修改保存在 `1cff53e633a6f734e4bbae28dc3a05b4ded23a89`。

rc.6 本地执行过的 11 项 widget 测试被错误忽略规则漏出 Git/source ZIP。本次 main 修正忽略规则并补入同一测试源码；已发布 tag 和附件不重写。

Quest 仍为 unauthorized，code 6 实机验收未完成。下一步按 [验收矩阵](PANEL-VALIDATION.md) 在用户授权的 Quest 上验证选片网络播放、实际拖动与返回续播、比例裁切、自然休眠恢复及长时间播放。本轮不安装、不绕过设备授权。

[依赖审计](DEPENDENCIES.md) · [发布记录](RELEASE.md) · [源码交付](SOURCE-DELIVERY.md)
