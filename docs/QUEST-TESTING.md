# Quest 测试：先复用正常唤醒，再判断实际阻塞

看到 `Asleep` 不等于必须请用户佩戴。先核对目标 Quest 的序列/型号及现有 ADB server；用户授权测试后，在同一设备上执行正常唤醒和应用启动，并验证结果：

```powershell
adb -s SERIAL shell input keyevent KEYCODE_WAKEUP
adb -s SERIAL shell am start -n io.github.vrbilibili.quest/com.example.piliplus.MainActivity
adb -s SERIAL shell dumpsys power
metavr -d SERIAL window focus --json
```

将唤醒、启动、采集安排在一个短操作内。离头自动休眠可能让分开的命令失去有效窗口；命令返回零、窗口 visible 或焦点记录都不单独证明实际像素/输入/播放。

本机历史任务已经使用过以下 Meta CLI 临时开发测试方式。本轮也确认能唤醒，但它可能触发 Guardian 房间定位提示。仅在用户授权这种临时测试时使用，先记原值、限时、最后恢复并读回；不能关闭 Guardian、锁屏或鉴权来继续。

```powershell
metavr -d SERIAL device proximity --status --json
metavr -d SERIAL device get-property --json
metavr -d SERIAL device proximity --disable --duration-ms 120000
# bounded test window
metavr -d SERIAL device proximity --enable
metavr -d SERIAL device proximity --status --json
metavr -d SERIAL device get-property --json
```

本轮原值/恢复值：enabled=true、prox_override=DISABLED；disable_guardian、disable_dialogs、disable_autosleep、set_proximity_close 均 false。不得把临时覆盖期间的结果当成自然休眠或佩戴/手柄验收。

普通 ADB 坐标点击/鼠标拖动在当前离头面板上未证明有效。旧测试使用 `UiAutomation` 的真实 accessibility `ACTION_CLICK`；本轮独立无权限 instrumentation helper 成功操作正式 rc.6 的视频卡片、播放/暂停、返回、大屏和前进按钮，不替换正式 APK、不读取账户数据库。节点动作不能冒充手柄射线或真实拖动。

若正常唤醒已成功，报告真正剩余阻塞，例如 Guardian 房间定位占据焦点、无有效应用像素、输入没有改变进度；不要再把问题概括成“无权限/必须佩戴”。


## rc.7 补充：真实拖动和应用窗口像素

`adb shell input mouse swipe` 不带 BUTTON_PRIMARY，不能据此判定 slider 故障。用 source MOUSE 且 DOWN/MOVE buttons=1、UP buttons=0 的完整系统事件，或 touchscreen swipe；保留 Window.Callback 的 source/buttons/time 与松开前后位置证据。

`-PpanelAcceptance=true` 生成 release-signed 独立 androidTest，目标是未改动正式 APK。`PanelAcceptanceProbe` 捕获目标 Activity Window 的真实像素，避免把黑色/透视 adb screencap 当作应用布局。正式产品不打入 test runner。只在授权设备执行，使用正常 KEYCODE_WAKEUP，不关闭 Guardian/安全功能。自然睡眠验证须停止唤醒循环并实际读到 Asleep；暂停循环和播放中循环分别报告。

本轮具体数值和限制见 [RC7-VALIDATION](RC7-VALIDATION.md)。原始截图/日志留本地，不作为公开发行附件。
