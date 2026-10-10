> 历史快照：保留原始版本、结果与经验；不构成当前目标或操作授权。旧“只读/不推送/不发布”、设备状态和源码-only 限制仅适用于记录当时阶段。当前状态见 [CURRENT-STATE](../CURRENT-STATE.md)；历史 pass 不自动等于当前 pass。

# VR 应用与效果参考图册

更新：2026-09-23。用于 PiliPlus → Quest 3 空间界面设计。

本轮沿视频影院、空间工作区、三维效果与手势、空间信息四条主线，用 Exa 发起 15 次查询，累计 96 条网页结果，按 URL 去重为 95 个页面；另做图片检索和原站媒体链接定位。最终筛选 **25 个应用/工具、12 张参考图片（含 1 张动图）、10 个演示与教程入口**。部分应用此前已提到，本轮补充了效果或图片来源。

图片来自产品官网、开发者商店、项目案例或第一手体验文章，逐图标注。官方展示图也可能经过宣传编排，不等同于本机画质；历史截图不代表当前版本按钮位置。浏览器控制连接不可用，本轮未做头显实测或视频逐帧检查。远程图片保留原站地址，没有下载镜像；如当前查看器不显示图片，可打开各图来源页。

## 最值得先看的方向

- **自由摆放与布局保存：Fluid。** 适合主屏、选集、弹幕、笔记的成组管理。
- **透明度与小型快捷栏：OVR Toolkit、XSOverlay。** 都是 PC VR 参考，交互可借鉴，运行时不能直接当作 Quest 插件。
- **视频周围的三维信息：MLB、NBA。** 主视频保留清晰中心，周边组件有各自任务。
- **动态弹幕效果：Figmin XR、Open Brush。** 看路径、拖尾、音频响应和对象分组，避免先做大而复杂的影院。
- **真机设计验证：ShapesXR、Figmin XR。** ShapesXR 偏界面布局与交互原型，Figmin 偏三维物件和效果试验。

以下“用于 PiliPlus”的内容是本项目的设计建议，不是声称原应用已经实现 B 站弹幕。

## 参考图片

### 1. Fluid：把网页和工具摆成工作区

平台/版本：Quest，图为历史商店展示。

![Fluid：把网页和工具摆成工作区](https://d16qp92u5x17m8.cloudfront.net/22580.jpegtzin1703460998.jpeg?quality=80&type=jpg&width=1920)

[图片来源](https://www.altlabvr.com/fluid)。商店展示图经 AltLab 页面转引；窗口功能另由 Fluid 官网确认。

看什么：看视频、文档、网页如何围绕用户摆放，窗口下方如何保留控制入口。

用于 PiliPlus：对应主视频、选集、弹幕/评论、笔记；保存整组布局。

### 2. OVR Toolkit：透明度、固定与输入控制

平台/版本：PC VR / SteamVR，不能作为 Quest 独立应用插件。

![OVR Toolkit：透明度、固定与输入控制](https://shared.cloudflare.steamstatic.com/store_item_assets/steam/apps/1068820/ss_3c0ca86a0f44ad053d65e5508fc8f1151077ea58.1920x1080.jpg?t=1737846873)

[图片来源](https://store.steampowered.com/app/1068820/OVR_Toolkit__Desktop_Overlay/)。开发者 Steam 商店截图。

看什么：窗口设置与内容并排；不透明度、固定和输入控制都有明确入口。

用于 PiliPlus：辅助面板调整背景时保持文字清晰；锁定位置与锁定输入应分开。

### 3. XSOverlay：观看状态与布局编辑分开

平台/版本：PC VR / SteamVR。

![XSOverlay：观看状态与布局编辑分开](https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/1173510/ss_a99842aad6fd4c5bc89453e52b8003511ed162b3.1920x1080.jpg?t=1698417988)

[图片来源](https://store.steampowered.com/app/1173510/XSOverlay/)。开发者 Steam 商店截图，图中界面属于既有版本。

看什么：主内容后方保留空间环境，窗口属性用独立面板调整。

用于 PiliPlus：平时专心观看，进入摆放模式后才出现位置、透明度、附着方式。

### 4. Figmin XR：三维内容也有自己的上下文菜单

平台/版本：跨 XR 平台；图片不作为特定设备画质证明。

![Figmin XR：三维内容也有自己的上下文菜单](https://www.figmin.com/assets/3D-model-search.jpg)

[图片来源](https://www.figmin.com/)。Figmin 官网发布的产品图片。

看什么：模型预览、内容搜索和操作面板与现实空间同时存在。

用于 PiliPlus：把立体表情和精选弹幕当成有状态的对象，点选后再展开控制。

### 5. Figmin XR：桌面级场景、立体物件与装饰

平台/版本：图片来自 Viveport 开发者商店页面。

![Figmin XR：桌面级场景、立体物件与装饰](https://assets-global.viveport.com/vr_developer_published_assets/mobileapp/cb9d162a-343a-450f-83a4-453e8f7a7a70/image/image_1_v1675364424.png)

[图片来源](https://www.viveport.com/apps/cb9d162a-343a-450f-83a4-453e8f7a7a70)。开发者商店展示图。

看什么：局部小场景置于桌面，空间内容不必铺满整个视野。

用于 PiliPlus：在主屏下方设置小范围弹幕舞台，限制体积和活动边界。

### 6. Open Brush：空间笔触与发光效果

平台/版本：Quest / PC VR 等；作品示例不代表各平台性能一致。

![Open Brush：空间笔触与发光效果](https://openbrush.app/assets/carousel/1.png)

[图片来源](https://openbrush.app/)。官网 Made with Open Brush 作品展示；具体音频响应需看教程。

看什么：三维笔触本身可以成为可见的空间对象，而非贴在视频上的平面纹理。

用于 PiliPlus：河流轨道、短暂尾迹与装饰粒子；弹幕文字本体避免过度发光。

### 7. ShapesXR：在头显里试摆多个 UI 面板

平台/版本：空间设计工具；图为 Prisms 项目案例原型。

![ShapesXR：在头显里试摆多个 UI 面板](https://cdn.prod.website-files.com/615dc84364c59159d42e46c6/625e8a09a9635169fc05774b_Prisms1.gif)

[图片来源](https://www.shapesxr.com/case-studies/designing-spatial-learning-experiences)。ShapesXR 官网案例动图，展示原型而非最终播放器。

看什么：多个页面以真实空间关系排列，设计者能站到使用者的位置判断布局。

用于 PiliPlus：把现有 PiliPlus 草案放进 Quest，以 1:1 尺寸检查距离、字号与转头负担。

### 8. NBA：主画面和副画面的清晰层级

平台/版本：Vision Pro；不要据此假设 Quest 有眼动输入。

![NBA：主画面和副画面的清晰层级](https://www.apple.com/newsroom/images/2024/02/apple-announces-more-than-600-new-apps-built-for-apple-vision-pro/article/Apple-Vision-Pro-app-experiences-NBA-video-Multiview_big.jpg.large_2x.jpg)

[图片来源](https://www.apple.com/newsroom/2024/02/apple-announces-more-than-600-new-apps-built-for-apple-vision-pro/)。Apple 官方产品展示图，2024 年发布。

看什么：一块主屏配合较小副屏，切换内容和声音有明确操作位置。

用于 PiliPlus：可先把副屏用于选集预览和详情，而不是首版就同时解码多路视频。

### 9. MLB：视频旁边生长出三维信息

平台/版本：图为 Vision Pro 体验；当前官网另列 Quest / Android XR，布局需分别核对。

![MLB：视频旁边生长出三维信息](https://img.mlbstatic.com/mlb-images/image/upload/t_16x9/t_w1536/mlb/nignn16m8uoy7aebsab6.jpg)

[图片来源](https://www.mlb.com/news/apple-vision-pro-mlb-gameday-review)。MLB 官方体验文章的应用画面。

看什么：视频、数据面板和三维球场共存，三维内容传达信息而非单纯装饰。

用于 PiliPlus：可启发章节节点、热度分布、讲解对象或弹幕轨道；需要明确数据来源。

### 10. Disney+：主题环境与安静的观看位置

平台/版本：Vision Pro，2024 年体验文章。

![Disney+：主题环境与安静的观看位置](https://www.laughingplace.com/uploads/2024/05/disney-plus-apple-vision-pro-environment-stark-tow-2.jpg)

[图片来源](https://www.laughingplace.com/w/disney-entertainment/review-disney-plus-on-apple-vision-pro/)。第一手体验文章中的环境截图；主题环境功能由 Disney 官方确认。

看什么：环境提供氛围与空间归属，观看位置有稳定朝向。

用于 PiliPlus：为 PiliPlus 设计自己的安静主题空间；播放时减少环境动画和声音。

### 11. 4XVR：透明视频与普通半透明面板不同

平台/版本：官方透视视频教程，具体平台与版本需实测。

![4XVR：透明视频与普通半透明面板不同](https://14803101.s21i.faiusr.com/2/1/ABUIABACGAAglYqxxQYo8MGmuQQwhAc45wQ.jpg)

[图片来源](https://www.4xvr.net/h-nd-449.html)。4XVR 官方教程设置截图。

看什么：专门的 alpha / 色键与对象距离控制，用于准备好的透明内容。

用于 PiliPlus：透明表情动画可采用带遮罩的素材；普通投稿的人物抠图是另一个问题。

### 12. Hand Physics Lab：触碰反馈要明确

平台/版本：历史 Quest 演示；当前产品另有更新。

![Hand Physics Lab：触碰反馈要明确](https://img.itch.zone/aW1hZ2UvNjA1NDQ3LzQ0MjU2NjAucG5n/original/Xm9jf9.png)

[图片来源](https://holonautic.itch.io/hand-physics-lab)。开发者 itch.io 页面截图。

看什么：手指靠近、按下按钮与反馈灯之间的关系非常直观。

用于 PiliPlus：抓取条、按钮和滚动区域采用不同反馈，减少误把点击当拖动。

## 动态效果：直接看这些演示

静态图能说明布局，运动路径、拖尾、声画同步与手部反馈更适合看视频。本表只给已找到的原始链接，不编造没有核对过的时间点。

| 演示 | 重点 | 证据范围 |
| --- | --- | --- |
| [Fluid 官方入门](https://www.youtube.com/watch?v=nZRvivzFAZo) | Omni box、手势射线、透视开关和窗口操作 | 官方频道；取得部分文字稿 |
| [Figmin：音频响应笔刷](https://www.youtube.com/watch?v=3BzIYels15A) | 视频音频驱动笔触，适合音乐区外围光效 | 官方频道；取得文字稿 |
| [Figmin：运动编辑器](https://www.youtube.com/watch?v=eB0_7LZJYd4) | 录制物件路径，循环/往返，移动整条运动路径 | 官方频道；取得文字稿 |
| [Figmin：灯光编辑器](https://www.youtube.com/watch?v=3r5dbIEmw1I) | 场景光、物体光、映射房间内的模拟染色 | 官方频道；取得文字稿 |
| [Figmin：对象分组](https://www.youtube.com/watch?v=fWgsLCOTVVk) | 多个对象成为一个层级，整体移动、旋转和缩放 | 官方频道；取得文字稿 |
| [Figmin：拖尾编辑器](https://www.youtube.com/watch?v=vNtHkawjvpc) | 轨迹生成、消散、颜色、尺寸、生命周期 | 官方频道；取得文字稿 |
| [Open Brush：音频响应教学](https://www.youtube.com/watch?v=noGPupdXbmc) | 笔刷如何随声音变化；结合官方笔刷文档观看 | 取得标题；本次未取得完整文字稿 |
| [Hand Physics Lab 发布预告](https://www.youtube.com/watch?v=w-w7omxJuOk) | 约 1 分 10 秒，手指触碰、抓取与物理操作 | Holonautic 官方频道；历史版本预告 |
| [MLB App for Android XR](https://www.mlb.com/video/mlb-app-now-available-for-android-xr) | 视频、球场和轨迹的组合，注意平台不同 | MLB 官方演示，2025-10-22 |
| [Apple：设计沉浸环境](https://developer.apple.com/videos/play/wwdc2026/234/) | 环境目的、参考采集、光照与运动；用于设计方法 | WWDC26 官方课程，visionOS 专题 |

## 25 个应用/工具索引

平台列是本次参考范围，不是完整兼容性或购买清单。功能可能因版本与设备不同。

### 空间窗口与工作区

| 应用 | 参考平台 | 借鉴点 | 用于 PiliPlus |
| --- | --- | --- | --- |
| [Fluid](https://fluid.so/) | Quest 独立运行；可选桌面串流 | 网页窗口自由摆放、成组保存为 Spaces、混合现实与环境切换 | 将视频、选集、弹幕、笔记保存为一个布局，切换用途时整组恢复。 |
| [Immersed](https://immersed.com/) | Quest 等头显 + 电脑 | 多显示器围绕用户布置，个人工作区与共享空间 | 研究侧面板角度与正文阅读；不要照搬长时间办公的满屏密度。 |
| [Virtual Desktop](https://www.vrdesktop.net/) | Quest + 电脑；另有其他平台 | 多显示器、视频观看环境、桌面与 VR 切换 | 研究主副屏关系及环境切换，历史版本说明只用于功能起点。 |
| [Ethereal Planes](https://ethereal.glass/features/per-window-streaming) | Quest + Windows/macOS | 按单个桌面应用窗口生成独立空间面板 | 有助于理解组件窗口与整张桌面镜像的区别；开发者性能宣传未实测。 |
| [XSOverlay](https://store.steampowered.com/app/1173510/XSOverlay/) | PC VR / SteamVR | 布局模式、窗口锁定、输入锁定、透明度、附着方式 | 编辑模式与观看模式分离；透明面板的输入锁定要可见。 |
| [OVR Toolkit](https://store.steampowered.com/app/1068820/OVR_Toolkit__Desktop_Overlay/) | PC VR / SteamVR | 腕部快捷栏、窗口不透明度、曲率、固定与显隐 | 把暂停、音量、弹幕开关收进小型快捷栏；参考交互，不当作 Quest 原生插件。 |
| [Desktop+](https://store.steampowered.com/app/1494460/Desktop/) | PC VR / SteamVR | 上下文多叠层、窗口附着到控制器、快捷操作 | 研究小工具组件与主屏分离、面板按用途出现的方式。 |

### 视频与影院

| 应用 | 参考平台 | 借鉴点 | 用于 PiliPlus |
| --- | --- | --- | --- |
| [Bigscreen](https://www.bigscreenvr.com/software/) | Quest / PC VR | 社交影院、不同观看环境、随画面变化的影院灯光 | 影院模式与观看氛围；灯光只做低强度辅助，合看另列功能。 |
| [Moon VR Player](https://moonvrplayer.com/) | Quest 等；此处看 Quest 产品 | 透视观看、屏幕位置/姿态、环境反光效果 | 单独调整主屏尺寸、距离、俯仰和环境亮度。 |
| [SKYBOX](https://skybox.xyz/) | Quest / PC VR 等 | 媒体库、影院环境、字幕音轨与屏幕参数 | 低干扰播放控制、从选片到观看的流程。 |
| [4XVR](https://www.4xvr.net/h-nd-449.html) | Quest 等 | 透视视频设置、alpha / 色键、对象距离 | 研究透明素材的设置与反馈；普通视频整体透明与抠出人物是两种能力。 |
| [HereSphere](https://heresphere.com/) | Quest / PC VR | 投影校正、时间标签、多平面视频及音频焦点 | 借鉴章节标记和高级设置分层；不将其高密度菜单作为首屏。 |
| [DeoVR](https://deovr.com/blog/59-how-to-use-the-new-alpha-channel-feature-for-perfect-passthrough) | Quest / PC VR 等 | 专门准备的 alpha / 色键视频在透视环境中呈现 | 可启发有透明背景的表情或角色素材；不等同于普通投稿自动重建三维人物。 |
| [CineUltra](https://apps.apple.com/us/app/cineultra-immersive-cinema/id6478853637) | Vision Pro | 媒体库与沉浸影院之间的切换 | 研究内容浏览与影院观看的层级；算法转换画质没有在本轮评测。 |
| [Screenlit](https://apps.apple.com/us/app/screenlit/id6499478407) | Vision Pro | 虚拟媒体货架、可调曲面窗口、影院/虚空环境 | 把稍后再看做成可选内容架；深度转换作为独立实验。 |

### 三维效果、交互与原型工具

| 应用 | 参考平台 | 借鉴点 | 用于 PiliPlus |
| --- | --- | --- | --- |
| [Figmin XR](https://www.figmin.com/) | Quest / Vision Pro / PC VR 等 | 混放视频、文字、模型；分组、运动路径、拖尾、音频响应、灯光 | 最适合验证空间弹幕、立体表情、成组拖动和视频周边场景。 |
| [Open Brush](https://openbrush.app/) | Quest / PC VR 等 | 三维笔触、发光、粒子与部分音频响应笔刷 | 借鉴河流轨道、尾迹与短时氛围效果；文字本体保持清晰。 |
| [ShapesXR](https://www.shapesxr.com/) | Quest 等 XR 设备；以当前商店为准 | 空间 UI 原型、交互触发、1:1 场景试摆、组件层级 | 先在头显里比较字号、位置、距离和面板数量，再进入播放器改造。 |
| [First Hand](https://developers.meta.com/horizon/blog/introducing-first-hand/) | Quest | Meta 官方手势示例：抓取、触碰、开关和虚拟 UI | 明确抓取条与点击区，观察按压/悬停/捏合反馈。 |
| [Hand Physics Lab](https://www.holonautic.com/hand-physics-lab) | Quest；另有 Vision Pro 版 | 按钮、抓取、双手物理操作、机械键盘 | 借鉴明确触碰反馈，不必把日常视频操作做成复杂物理任务。 |
| [Passthrough Windows](https://sidequestvr.com/app/10522/passthrough-windows) | 历史 Quest 应用；商店列 Quest 2 / Pro，Quest 3 待核对 | 把墙面区域变成可看见其他风景的空间窗口 | 启发小型风景窗、门户式主题背景；先测试空间定位和观看舒适度。 |

### 视频与空间信息结合

| 应用 | 参考平台 | 借鉴点 | 用于 PiliPlus |
| --- | --- | --- | --- |
| [NBA](https://support.watch.nba.com/hc/en-us/articles/20891550821783-Apple-Vision-Pro) | 此处参考 Vision Pro 版 | 主视频加多个副视频、统计面板与内容切换 | 借鉴主次层级、选中态和声音焦点；首版仍先一个播放会话。 |
| [MLB App on XR](https://www.mlb.com/apps/mlb-app-on-xr) | 官网列出 Quest、Vision Pro、Android XR；图示为特定版本 | 视频、统计面板、三维球场与轨迹联动 | 内容之外显示结构化信息；可启发章节、热度或互动的空间表示。 |
| [Disney+](https://thewaltdisneycompany.com/news/disney-on-apple-vision-pro-ushers-in-a-new-era-of-storytelling-innovation-and-immersive-entertainment-2/) | 此处参考 Vision Pro 版 | 带环境动画和声音的主题影院 | 按观看主题选择安静环境，播放时降低背景存在感。 |
| [Apple TV](https://www.apple.com/apple-vision-pro/) | 此处参考 Vision Pro 版 | 主画面与副画面的 Multiview、影院和环境结合 | 研究聚焦主屏、快速切换副屏与整组缩放。 |

## 对现有设计的具体补充

| 可以试的效果 | 参考 | 建议的第一次实验 |
| --- | --- | --- |
| 整组工作区 | Fluid、Figmin 分组 | 视频和两个面板成组拖动，保存/恢复布局 |
| 从侧栏拆出组件 | XSOverlay、OVR Toolkit | 选集/弹幕拖出后独立摆放，收回后保留状态 |
| 腕部快捷栏 | OVR Toolkit | 只放暂停、音量、弹幕开关；不要求持续举手阅读 |
| 屏幕环境染色 | Moon、Bigscreen、Figmin Lighting | 低强度慢变化的外围颜色，保持视频与字幕可读 |
| 河流与拖尾 | Figmin Motion + Trail、Open Brush | 文字沿有限路径走；少量装饰拖尾，离开后及时清除 |
| 立体反应表情 | Figmin | 一次一个小物件，留在主屏附近，自动退场 |
| 音乐响应 | Figmin Audio Reactive | 只让外围光效随节奏变化，文字不跟着剧烈跳动 |
| 空间章节 / 热度节点 | MLB 的三维信息组织 | 在主屏下方显示少量可点击时间节点，点击仍调用原播放逻辑 |
| 风景窗 / 门户背景 | Passthrough Windows | 先试一个局部风景区域，保留现实环境感 |
| 准备好的透明动画 | 4XVR、DeoVR | 使用有 alpha/遮罩的素材；与普通视频透明度分开设置 |

“房间染色”是在头显画面里模拟光照，不会让物理房间真的发光。MLB 的三维球场依赖专门数据，不应据此推断普通 B 站视频能自动重建同类三维场景。音频响应效果与弹幕 TTS 朗读也是两条独立能力。

## 下一轮更有价值的验证

先在 ShapesXR 中用占位画面摆出主屏、选集、弹幕三块面板，实测尺寸、距离、手势命中和转头负担；同时用 Figmin 尝试一条短路径、一个文字对象和少量拖尾，比较开启/关闭后的干扰。这个阶段只验证设计，不承担 B 站登录、网络播放或正式分发。

确认空间组合值得保留后，再回到 PiliPlus + 原生空间宿主的技术探针。主播放稳定、恢复与字幕清晰仍按 [测试清单](QUEST3-TEST-PLAN.md) 验收，不因效果新奇放宽。

结构化条目与搜索审计见 catalog.json（原始检索记录仅本地保留，不纳入仓库）、search-audit.json（原始检索记录仅本地保留，不纳入仓库）；上一轮参考见 [VR-UI-REFERENCES.md](VR-UI-REFERENCES.md)。
