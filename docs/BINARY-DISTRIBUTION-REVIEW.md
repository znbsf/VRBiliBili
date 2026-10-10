# 当前二进制分发材料

code 6 普通面板不再构建 Meta Spatial SDK 与影院专用 Media3；最终依赖图、APK native libraries、DEX 和 manifest 的核验见 Release validation.json。旧 rc.4 的 GPL/Meta 组合问题属于历史候选，不能说其曾经得到许可确认；也不能把移除该 SDK 当作其他依赖义务自动完成。

客户端 GPLv3 与各 vendor 许可继续保留。media-kit 的 Dart MIT 不替代 libmpv/FFmpeg 及其静态依赖的许可证。发布同时提供固定应用源码、原生构建脚本/补丁、版本锁定源码材料、许可证原文与重建说明。详见 [SOURCE-DELIVERY](SOURCE-DELIVERY.md)。本文件是工程交付清单，不替代权利人授权或法律意见。

预发布标签只表达未完成 code 6 真机验收，不减轻对应源码义务。私钥、账号、原始设备日志、家庭透视截图和本机绝对路径不进入附件。历史完整评审在 Git 63fa4a8 中可查。
