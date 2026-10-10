> 历史快照：保留原始版本、结果与经验；不构成当前目标或操作授权。旧“只读/不推送/不发布”、设备状态和源码-only 限制仅适用于记录当时阶段。当前状态见 [CURRENT-STATE](../CURRENT-STATE.md)；历史 pass 不自动等于当前 pass。

# VR 视频界面参考与可复查时间点

检索日期：2026-09-22。资料用于 PiliPlus Quest 3 设计。优先采用官方说明、原作者视频与原作者设计文章。

2026-09-23 扩展：新增 [VR 应用与效果参考图册](VR-REFERENCE-GALLERY.md)，包含 25 个应用/工具、12 张图片（含动图）与 10 个演示入口。本页保留上一轮选片时间点与初始来源记录。

本轮 Exa 搜索请求共返回 31 个结果项，包含重复与未采用结果，不能理解为 31 个独立项目或完整审核。以下是筛选后实际影响设计的资料。视频核对了标题、章节及可取得文字稿；本次浏览器连接失败，未逐帧观看视频，也没有把作者录屏等同于本机实测。

## 1. 优先打开的实际使用视频

### Quest 上的 Netflix / 浏览器观看控制

[Bearski — Best Tips For Watching Quest 2, 3, 3s Netflix!](https://www.youtube.com/watch?v=o2m8iDRaJOc)

时长约 5:43。作者为展示浏览器操作，部分画面使用 YouTube；不能称为 Netflix 原生 VR 应用演示。

| 时间 | 内容 | 我们借鉴什么 |
| --- | --- | --- |
| [00:37](https://www.youtube.com/watch?v=o2m8iDRaJOc&t=37s) | Theater Mode | 一键进入大屏观看 |
| [01:15](https://www.youtube.com/watch?v=o2m8iDRaJOc&t=75s) | 隐藏 Meta 面板 | 观看时减少常驻 UI |
| [01:52](https://www.youtube.com/watch?v=o2m8iDRaJOc&t=112s) | 调整屏幕大小 | 抓取边角调整尺寸 |
| [02:12](https://www.youtube.com/watch?v=o2m8iDRaJOc&t=132s) | 第二浏览器窗口 | 内容与伴随面板分离 |
| [02:56](https://www.youtube.com/watch?v=o2m8iDRaJOc&t=176s) | 屏幕摆放 | 支持坐姿、半躺姿势重新放置 |
| [04:30](https://www.youtube.com/watch?v=o2m8iDRaJOc&t=270s) | 平面 / 曲面 | 曲率应是用户可选项 |
| [04:53](https://www.youtube.com/watch?v=o2m8iDRaJOc&t=293s) | 环境调暗 | 与视频和 UI 透明度分别控制 |

当前产品事实以 [Netflix 官方 Quest 指南](https://help.netflix.com/en/node/110502) 与 [支持的浏览器说明](https://help.netflix.com/en/node/30081) 为准。旧版本录屏不能用来确认当前 OS 的按钮位置。

### SKYBOX / Virtual Desktop / Bigscreen 实际对比

[The Construct — Best Quest 3 Cinema Experience?](https://www.youtube.com/watch?v=rJyAvHr7lm4)

2024-04-03，约 22:36。它展示真实观看流程，适合研究操作和空间关系；作为历史视频，不用来判断 2026 年的功能缺失或手势支持。

| 时间 | 内容 | 我们借鉴什么 |
| --- | --- | --- |
| [00:22](https://www.youtube.com/watch?v=rJyAvHr7lm4&t=22s) | SKYBOX | 内容选择到播放的切换 |
| [02:11](https://www.youtube.com/watch?v=rJyAvHr7lm4&t=131s) | 影院环境 | 环境服务于观看，避免装饰性干扰 |
| [03:11](https://www.youtube.com/watch?v=rJyAvHr7lm4&t=191s) | 移动屏幕 | 主屏位置调整需要直接、易找回 |
| [03:30](https://www.youtube.com/watch?v=rJyAvHr7lm4&t=210s) | 混合现实 | 同一视频支持透视与影院 |
| [06:56](https://www.youtube.com/watch?v=rJyAvHr7lm4&t=416s) | 观看参数 | 将屏幕尺寸与观看设置拆开 |
| [08:27](https://www.youtube.com/watch?v=rJyAvHr7lm4&t=507s) | Virtual Desktop | 桌面工作区与视频观看的关系 |
| [13:00](https://www.youtube.com/watch?v=rJyAvHr7lm4&t=780s) | Bigscreen | 社交影院参考，非首版功能承诺 |
| [17:55](https://www.youtube.com/watch?v=rJyAvHr7lm4&t=1075s) | 减少干扰 | 关闭陪伴元素、回归纯观看 |

## 2. 官方产品说明与设计拆解

| 资料与性质 | 读哪里 / 借鉴点 | 使用边界 |
| --- | --- | --- |
| [YouTube VR 控制说明](https://support.google.com/youtube/answer/7205134?hl=en)，官方 | Panel / Immersive 切换；控制栏显示；环境与 MR；曲面 | Quest 实际功能参考；具体入口需按当前设备版本核对 |
| [How YouTube Made the Jump to Spatial Computing](https://apps.apple.com/us/iphone/story/id1876711467)，Apple 刊载的 YouTube 产品与设计负责人访谈，2026-02-25 | 将控制、评论、互动拆成组件，适配紧凑窗口和影院；团队称其为 responsive postures | 已发布 Vision Pro 产品的设计经验，不能推导为 Quest 存在眼动输入 |
| [Moon VR — How to watch and adjust videos](https://moonvrplayer.com/blog/19/how-to-watch-and-adjust-videos)，官方使用文章，2023-05-06 | 手柄移动屏幕；分开调整大小、距离、位置和比例；重新居中 | 历史版本交互参考，按钮映射须实测 |
| [SKYBOX 官网](https://skybox.xyz/) 与 [官方商店页](https://www.meta.com/experiences/skybox-vr-video-player/2063931653705427/) | 曲面、屏幕调整、字幕、环境 | 核对产品声明；不以旧视频的“No Pinch”判断现在没有手势 |
| [Kevin Kwok — Spatial UI Design Concept for Apple Vision Pro](https://kevinnkwok.medium.com/spatial-ui-design-concept-for-apple-vision-pro-case-study-5c85f5031bf)，2023-07-27，作者概念设计 | Netflix 内容目录、详情、工具栏的前后层级；保留熟悉内容结构 | **非 Netflix 官方、非已发布界面**；其中激进扩展视野的提议不直接作为舒适度标准 |

## 3. 实现时需要读的工程资料

- [Meta Spatial SDK 官方示例库](https://github.com/meta-quest/Meta-Spatial-SDK-Samples)：先看 HybridSample、MediaPlayerSample；示例用于空间宿主与媒体路径验证，不是现成 PiliPlus 插件。
- [Media playback](https://developers.meta.com/horizon/documentation/spatial-sdk/spatial-sdk-media-playback/)：Surface 视频输出、可读取纹理与合成路径的限制。
- [Blend modes](https://developers.meta.com/horizon/documentation/spatial-sdk/spatial-sdk-blend-modes/)：透明材质、深度与排序，避免把整层透明当成无成本功能。
- [Media view display](https://developers.meta.com/horizon/documentation/spatial-sdk/media-view-display/)：面板打开、关闭、最小化、最大化等生命周期。
- [Comfort](https://developers.meta.com/horizon/design/comfort/)、[Hands UI](https://developers.meta.com/horizon/design/hands-ui-best-practices/)、[Hands 3D](https://developers.meta.com/horizon/design/hands-3d-best-practices/)：视野、字号、点击与抓取的区别。
- [Flutter multiple instances](https://docs.flutter.dev/add-to-app/multiple-flutters)：FlutterEngineGroup 与多个实例的状态/通信边界。

## 4. 下一轮真机参考拆解方法

同一个人、同一坐姿、同一 Quest OS，依次录制：打开视频 → 隐藏控制 → 移动/缩放/回中 → 展开伴随面板 → 调暗环境 → 暂停并切选集 → 摘戴恢复。每次只比较一个设计点，记录完成时间、误触、额外转头与主观阅读体验。

若录屏出现受保护内容黑屏，只记录允许显示的操作区域和作者说明，不把黑屏推断成真实观看失败。是否顺手、是否舒服、立体层次是否正确，需要佩戴者判断，二维录屏不能替代。
