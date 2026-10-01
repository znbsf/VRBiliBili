# PiliPlus 真实播放接入基线

核查日期：2026-10-01。上游固定为 [PiliPlus c102a611](https://github.com/bggRGjQaUbCoE/PiliPlus/tree/c102a6115c7ac040f6a0c6a1653944b81b82dcb4)，不是移动分支引用。选取 pubspec.yaml、pubspec.lock、播放器控制器及 LICENSE，经 GitHub API 获取后逐文件校验 Git blob SHA1；完整清单和 SHA256 见 [机器可读基线](piliplus-playback-baseline.json)。本轮未复制完整业务仓库或构建 PiliPlus。

## 实际依赖

pubspec.lock 将 media_kit、media_kit_video、Android/Windows 视频库、通用视频库和 native event loop 等 Git 依赖锁定到 [media-kit fork 73771ec](https://github.com/My-Responsitories/media-kit/tree/73771ec38176be2d984a3049c28177bce23b54a0)。其中 media_kit 是 1.1.11，media_kit_video 是 1.2.5，Android 视频库是 1.3.7；其他平台 hosted 依赖另有校验和。不能用这些版本号替代实际 fork SHA。

历史研究曾单独读取同一个 fork，但未确认它就是最终锁定版本；本轮通过当前 PiliPlus lockfile 完成该关联，没有把历史快照当成新证据。

## 已有实现与接入边界

以下函数定位均对应上述精确上游 [controller.dart](https://github.com/bggRGjQaUbCoE/PiliPlus/blob/c102a6115c7ac040f6a0c6a1653944b81b82dcb4/lib/plugin/pl_player/controller.dart)：

| 链路 | 已有代码事实 | 空间接入必须保留的语义 |
| --- | --- | --- |
| setDataSource（583行） | 设置 loading，保留初始化 seekTo、autoplay、内容标识；异常转 error | 加载和内容身份独立于面板显示，不用 UI 计时伪装媒体进度 |
| _createVideoController（767行） | 复用/初始化单个 Player；音视频分轨经 EDL 组合；Media.start=seekTo，先 play:false | 保留请求上下文与分轨，组件开关不创建第二个有声播放器 |
| refreshPlayer（831行） | 非本地素材的当前 Media 复制实际 position 后重新 open；当前代码 play:true | 恢复策略必须核验用户暂停意图；该调用点是待验证风险，不是本轮确认的上游缺陷 |
| position/duration/buffer/error streams（947–1036行） | 实际媒体时间、缓冲、错误监听；已有网络错误节流和延迟重试 | 复用媒体事实与既有处理，避免再实现一套模拟业务状态 |
| dispose（1538行起） | 监听、控制器及播放器释放路径已存在 | 空间宿主重建和关闭必须对应释放与唯一会话 |

精确 fork 的 [VideoOutput.java](https://github.com/My-Responsitories/media-kit/blob/73771ec38176be2d984a3049c28177bce23b54a0/media_kit_video/android/src/main/java/com/alexmercerind/media_kit_video/VideoOutput.java) 创建 Flutter SurfaceTextureEntry 和 Surface；销毁时释放 texture/surface。核查未证明已有任意外部空间 Surface 注入接口。正式接入应先做一个业务面板与一个视频输出的探针，不据本轮 HTML 解码结果声称 Flutter/空间输出可用。

## 本轮已推进 / 尚缺

- 已推进：精确依赖与源码证据；独立桌面真实媒体探针的加载、失败、离线、重试和进度恢复，详见 [技术探针](REAL-PLAYBACK-PROBE.md)。
- 已存在：PiliPlus 原客户端的解析/播放基础及部分重试；本机官方 MediaPlayerSample 构建。本轮没有重复重写或把样例构建当成业务集成。
- 尚缺：本项目中的 PiliPlus 客户端集成、请求头/分轨/媒体地址刷新适配、账号权益、真实 media-kit/空间 Surface 验证、Quest 输入与舒适度。仍按 [阶段计划](阶段推进计划.md) 的人工门槛推进。

依赖文档不包含 token、Cookie 或实际签名媒体地址；本地获取的源码仅作为核查证据保留，不随探针上传。
