# Quest UI 录屏参考实施

## 当前公开版本状态

本文件后续章节记录不同迭代的历史证据。其中“最终 APK”和第二轮实机通过仅对应各章节记录的版本，不能作为最新图标控件迭代已通过的证明。最新 `quest-client-validation.json` 保留 `journeyPassed=false`：头显旅程被系统追踪提示阻塞；本次无人值守整合没有唤醒或操作头显，也没有替代用户判断舒适度、控制器射线和 XR 观感。

公开版本标为实验性源码，仅发布源码。隔离模拟器的离线 Flutter/MediaKit → 原生 Preview → 返回测试与历史网络视频、头显 XR 测试属于不同覆盖范围；进程内缓存检查不证明应用重启后的持久化。不发布包含 Meta SDK 的组合 APK。

参考：用户提供的 YouTube VR 实机录屏，212.34 秒；全片审阅记录位于 reports/youtube-reference-20261004/REVIEW.md。

## 已实现

- Quest 专用统一视觉主题：中性深色、粉色强调、白色主文字、灰色次级文字；统一卡片、菜单、对话框、列表和图标命中尺寸。保持原 PiliPlus 功能和数据来源。
- 首页：左侧固定首页/动态/我的导航，顶部品牌、搜索、头像，主体发现标题、搜索及分类。
- 搜索：圆角大搜索框、左右内容留白，不自动弹出头显键盘。
- 我的：媒体库标题与更大的历史、收藏等快捷入口。
- 详情：保留真实简介/评论/选集；增加信息栏收起/展开和搜索；播放控件保留弹幕、画质、设置、全屏直接入口。
- 原生影院：矢量图标、突出的播放暂停键、快退/快进、弹幕选中状态、画质菜单、环境菜单、屏幕尺寸菜单、居中及退出。标题与控件一起显隐，播放进度使用细条。
- 观看环境：默认深色影院、浅色空间、纯画面、混合现实。移除旧墙壁/台阶/粉色条，使用连续渐变纹理的地面柔光与环境背景。
- 屏幕：小/中/大；视频与交互面板同时缩放，保留视频原比例。

## 范围与限制

频道、收藏、动态等页面共用主题和控件改进，内容结构仍由 PiliPlus 提供。本次没有实现 YouTube 的弧形屏幕、浏览时视频小窗和多侧栏沉浸目录；没有恢复旧空间多窗口路线。地面光晕是静态纹理，不是实时视频反射。纯画面隐藏背景与弹幕，离开纯画面后恢复原弹幕开关状态。

## 验证

Dart 定向分析无错误/警告，三个风格提示；APK 编译通过。实机覆盖安装保留账号和历史。
首次设备旅程被 Quest 系统“正在寻找房间中的位置/无法检测移动”提示遮挡，未到达应用卡片，因此失败，不能作为界面或播放通过证据。后续实际结果以新设备旅程日志为准。

自动旅程新增：首页、搜索、动态、个人媒体库截图；详情信息栏收放；环境菜单、尺寸菜单截图；尺寸与环境状态检查。原生操作仍通过调试命令触发；实际射线命中、舒适度与混合现实显示效果需佩戴复核。测试失败不再导出可能来自旧版本的应用截图。

最终 APK SHA-256：`34759bffd10dfc313209308923df184f2e4bd4857301c436b2dc851cc4ea7519`。设备 base.apk 的 SHA-256 已独立读回一致。最终设备测试仍被系统追踪提示遮挡，无新截图被接纳；详见 `reports/quest-system-ui-20261004/final/journey.txt`。

## 2026-10-04 模拟器验证

已构建安装 x86_64 版本到 Meta Spatial Simulator 207，自动面板旅程通过。真实 1920×1080 视频播放、暂停、跳转、返回主页通过；首页、搜索、未登录媒体库/动态、详情、宽屏、画质及设置已有截图。首页初始截图早于封面下载，后续 home-loaded.png 确认封面正常。

影院控件提取为 CinemaHud、CinemaMenu、CinemaControl，头显与模拟器预览共用实现。模拟器预览使用真实 Media3 播放器，验证了视频解码、环境/尺寸菜单及退出返回。环境与尺寸仅验证菜单，不渲染 XR 场景；弹幕渲染、透视、空间居中、地面柔光和手柄射线未被此测试验证。截图外侧客厅属于模拟器背景。

证据：reports/quest-simulator-20261004/panel-journey.txt、同目录截图及 VRBiliBili-simulator-x64.apk。哈希与设备安装包独立读回一致，详见 quest-client-validation.json 的 simulator 字段。本轮未更新头显安装；原 ARM APK 与追踪遮挡记录属于上一轮证据。

复现：使用 tools/Build-QuestApp.ps1 -TargetPlatform android-x64 构建模拟器版；测试 APK 也应以 -Ptarget-platform=android-x64 构建。覆盖安装到 emulator-5554 后，运行 adb -s emulator-5554 shell am instrument -w -e scope panel io.github.vrbilibili.quest.debug.test/com.example.piliplus.QuestUiDriver。不要将 x64 APK 安装到 Quest。

官方模拟器范围：https://developers.meta.com/vr/documentation/android-apps/spatial-sim-overview/

## 2026-10-04 Quest 3 实机复测与修复

本轮覆盖安装最新 ARM APK，保留账号与数据。首轮实机发现柔光地面被 setAlbedoColor 覆盖成灰色平面，以及退出沉浸 Activity 后二维面板未恢复。移除地面纹理的颜色覆盖，并在沉浸 Activity 销毁后延迟恢复 MainActivity。

最终自动旅程 passed=true：点选自动播放、1920×1080 解码、暂停/前进、画质菜单、影院内真实切源、暗色影院默认关闭透视、控件自动隐藏、环境/尺寸状态、纯画面、退出保留进度、再次进入、返回上页/主页均通过。最终截图确认地面渐变柔光和纯画面；影院测试通过调试命令触发，不能据此声称手柄射线和舒适度已验收。透视只验证选择状态，未拍摄相机画面。静态柔光不是实时视频反射；弧形屏幕、浏览小窗仍未实现。

证据与已安装 APK：reports/quest-device-retest-20261004/return-fix/；此前两轮失败证据保留在上级及 fixed/。最终 APK 与设备 base.apk 哈希独立读回一致，记录于 quest-client-validation.json。测试后 automation_disable 广播成功；没有修改已有 StayOn 设置，无 Java/Dart 构建进程残留。

## 2026-10-04 第二轮实机迭代

改善影院标题、时间和按钮文字在亮画面上的对比度；纯画面隐藏弹幕并保留开关偏好；修复简介栏中长 UP 主名称和统计文字横向溢出。修改的 Flutter 文件定向分析 No issues found。

最终 Quest 3 旅程通过：普通播放实际 3840×2160 解码、影院切源、通过真实 View.performClick 回调操作环境/尺寸/暂停/快进/退出、纯画面弹幕隐藏与返回恢复、退出保留进度、重新进入及返回主页。performClick 不是手柄射线测试。最终标题/控件截图已目视检查。证据和已安装 APK 位于 reports/quest-iteration2-20261004/focus-retry/，哈希与设备独立读回一致。

本轮保留了启动失败与超时记录：部分 instrumentation 启动焦点落入 Horizon FocusPlaceholderActivity，明确恢复面板焦点后才完成旅程，不能把失败归为通过，也尚未确认冷启动焦点问题完全解决。测试后临时唤醒关闭，账号数据保留。
