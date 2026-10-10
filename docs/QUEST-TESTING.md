# Quest 测试 runbook：可复用方法

这里保留稳定操作原则；工具/环境见 [DEVELOPMENT-ENVIRONMENT](DEVELOPMENT-ENVIRONMENT.md)，某次结果见 [RC7-VALIDATION](RC7-VALIDATION.md)。本文件不授予设备或发布权限。

## 身份、保数据和恢复

先核对目标主机、Quest 序列/型号、ADB 状态与当前写入者，避免误用电视或其他线程设备。对齐源码提交、APK 内嵌提交、包名/versionCode/ABI/签名、设备已安装 APK 哈希。覆盖前后记录首次安装时间；用 `adb -s SERIAL install -r APK`，签名不符就停止，不能卸载/清数据绕过。不要读取签名私钥内容；利用正常签名流程与公开证书指纹核验。

## 正常唤醒优先

Asleep 不等于必须要求用户佩戴。已获设备测试授权时，先在一个短操作窗口内执行并观察：

```powershell
adb -s SERIAL shell input keyevent KEYCODE_WAKEUP
adb -s SERIAL shell am start -n io.github.vrbilibili.quest/com.example.piliplus.MainActivity
adb -s SERIAL shell dumpsys power
metavr -d SERIAL window focus --json
```

离头自动休眠可能让分开的命令失效；命令返回零、visible 或焦点记录都不单独证明有效输入、画面和播放。正常唤醒后若仍受阻，记录真实阻塞及窗口证据，不泛称“无权限”。

仅在明确授权且正常方式不足时使用有限临时 proximity 测试。先保存原值，限时，finally 恢复原状态并读回；不能关闭 Guardian、鉴权或安全对话框继续：

```powershell
metavr -d SERIAL device proximity --status --json
metavr -d SERIAL device get-property --json
metavr -d SERIAL device proximity --disable --duration-ms 120000
# bounded test only; if original state was normal enabled:
metavr -d SERIAL device proximity --enable
metavr -d SERIAL device proximity --status --json
metavr -d SERIAL device get-property --json
```

原状态不是正常 enabled 时不能盲目套用恢复命令。临时覆盖下的结果不证明自然休眠或佩戴感应。自然睡眠测试必须停止唤醒循环并实际观测 Asleep；暂停后、播放中、佩戴唤醒分别记录。

## 有效输入与画面

`adb shell input mouse swipe` 不带 BUTTON_PRIMARY（source=8194/buttons=0），不能作为按住拖动判据。使用 primary mouse 的 DOWN/MOVE buttons=1、UP buttons=0，或 source=4098 touchscreen；记录原生 Window.Callback 的 source/buttons/time、松开前后进度。点击可用真实 accessibility ACTION_CLICK，但不能冒充物理手柄或拖动。

`-PpanelAcceptance=true` 生成同正式签名的独立 release androidTest，目标为未改动正式 APK；`PanelAcceptanceProbe` 捕获 Activity Window 像素与真实系统输入。主 APK 必须不含 test runner。使用正常 KEYCODE_WAKEUP，不关闭安全机制。输出路径由每次 runner 返回，保存到独立本地证据目录；不要把旧目录当新证据。

黑色/透视 adb screencap、UI 节点树、模拟渲染不算真实应用视频像素。截图公开前核查账号头像、私人历史和 token；只公开用户授权的普通视频画面与控件。当前示例 [Quest3 code7 实图](images/quest3-code7-playback-20261010.png)；它不证明所有比例、完整字幕或主观音画同步。

## 分发一致性

保留不可变 tag→源码ZIP→APK内嵌提交对应，单独标明后续文档提交。核对公开证书、ABI、非 debug、生产包不含测试 runner、无当前排除的 XR 运行时。draft 上传完整五附件后实际下载核验 SHA256/大小/源 ZIP comment，再发布 prerelease；复核旧 tag/附件不变。没有根目录 workflow/runs 不能声称 CI 通过。
