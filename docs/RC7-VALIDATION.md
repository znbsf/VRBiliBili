# rc.7 / Android code 7：候选变更与验收边界

## 变更

更新渠道不再自动请求或下载 PiliPlus 上游包。设置中的入口改为“VRBiliBili 发布与更新”，手动检查、旧下载调用和错误路径只使用本项目 releases 页面。浏览器打开失败时不回退到其他项目。许可证、上游致谢和来源说明保留。

版本增加为 0.1.0 / code 7，沿用正式包名和签名。rc.6 tag 与旧 APK 不改写；新包精确源码提交与哈希以其 Release 的 validation.json 为准。

## 拖动诊断

不能把早期鼠标注入未改变进度认定为播放器缺陷。在未修改的 rc.6 正式 APK 上，release-signed instrumentation 记录到正常 touchscreen DOWN/MOVE/UP 进入原生窗口，真实进度从 0:04 跳到 21:05 / 31:37。已取得 1375×900 的正式应用窗口截图。同窗口对比确认 adb mouse swipe 事件 source=8194、buttonState=0，无主键按下而不 seek；touchscreen source=4098 的完整拖动从 0:04 到 29:42 / 44:32。播放器 seek 实现和 cover 实现没有为制造测试通过而改写。

新增测试 APK 通过 `-PpanelAcceptance=true` 显式选择 release 目标和原正式签名。它只包含在 androidTest 产物中，诊断正常系统 mouse/touchscreen 事件、按钮状态和进度，抓取目标 Activity 窗口像素，使用普通 KEYCODE_WAKEUP。测试输出在新建的应用专属子目录，不读取账户库，不改 Guardian/凭据/感应器。测试失败仍保留证据。

默认 debug instrumentation 与历史恢复 runner 保持可用。测试组件的 APK 不是对外发布的用户 APK。

## 验证与限制

- 16 项组件回归：拖动预览/单次提交/源切换、真实32px控制栏鼠标拖动、四种源比例×两种区域、四项更新渠道策略。改动范围静态分析通过。
- rc.6 真机普通播放、返回同片续播、自然离头入睡后 ADB 正常唤醒与暂停位置保持已记录；code 7 的实际复测结果另列于 Release validation.json，不自动继承旧版本结果。
- 本次取得的应用窗口像素可支持所测片段的布局观察，但不等于双眼 XR、手柄射线、音画主观同步、所有源比例或长时舒适度通过。四比例几何单测与真机截图是不同覆盖。
- 新包安装前后核验包名、versionCode、签名与首次安装时间；仅正常覆盖安装，签名不兼容时停止。
- 无根目录 CI 运行时不宣称 GitHub Actions 通过。
