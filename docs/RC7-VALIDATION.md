# rc.7 / Android code 7：本轮验证与边界

构建、APK 内嵌提交和 release tag 固定为 `1568ef4b0fc99306f184e5560dacd65a33426e0f`。后续 main 文档提交补记结果，不是重新构建的 APK。rc.6 tag 和附件保持不变。

## 变更

禁用 PiliPlus 上游自动更新检查；设置“VRBiliBili 发布与更新”、关于页手动检查和旧下载调用只打开 `https://github.com/znbsf/VRBiliBili/releases`。忽略传入的上游 asset URL，浏览器失败也不回退。保留开源署名与许可证；不自动下载/安装 APK，也不提供应用内验签。版本为 0.1.0 / code 7，使用原正式包名、正式签名。

## 拖动根因与回归

旧 `adb shell input mouse swipe` 的原生窗口事件为 source=8194、buttonState=0。没有按住鼠标主键，不是有效鼠标拖动。未改动的 rc.6 上，正确 primary-button mouse 在松开前保持 0:04，松开后到 0:34；触屏可从 0:34 到 0:51。不能把旧注入失败写成播放器缺陷。

code 7 的 release-signed androidTest 再次记录 Activity 原生 Window.Callback：无按键 mouse 保持 0:04；按住主键的 mouse 松开前为 0:04，松开后到 7:55 / 17:50；touchscreen 从 7:55 到 11:53。生产 seek 和 cover 代码没有为测试改写。

测试 APK 通过 `-PpanelAcceptance=true` 明确选择 release 目标并用相同正式签名；主产品 APK 不含测试 runner。默认 debug 恢复 runner 保持可用。测试仅用普通 KEYCODE_WAKEUP；code 7 验收没有接近传感器覆盖，也没有改 Guardian、对话框、安全或休眠设置。

## 本轮 code 7 证据

| 检查 | 实际结果 |
| --- | --- |
| 组件测试与静态分析 | 16 项通过；改动范围分析无问题；正式 release 构建成功 |
| 包检查 | ARM64、非 debug、正式包名、原签名；APK 无 Meta/XR 运行时与测试 runner；许可证存在 |
| 保数据升级 | code 6 → 7，install -r 成功；首次安装时间保留；拉回设备 base APK 与发布 APK SHA256 完全一致 |
| 选片播放/暂停/拖动 | 网络视频实际播放；暂停稳定；原生正确 mouse 与 touchscreen 拖动通过 |
| 返回续播 | 同一视频 BV1YLeJ6YECh 在 0:17 返回，再打开首个位置 0:17，随后继续推进 |
| 自然休眠 | 暂停 0:30 后真实 Asleep；普通唤醒后仍为 0:30；点击播放从 0:32 继续 |
| 更新通道 | 未出现上游更新弹窗；手动入口实际打开本项目 releases 的浏览器 VIEW intent；失败无上游回退由组件测试覆盖 |
| 应用像素 | release-signed instrumentation 捕获真实 Activity 窗口 1375×900 的面板/展开/拖动后截图 |
| 设备收尾 | proximity enabled、override DISABLED；Guardian/dialog/autosleep/proximity-close 覆盖均 false |

APK `VRBiliBili-0.1.0-7-quest-arm64.apk`：26,234,610 字节，SHA256 `da179d37ec4e41296424983ca47733825bd35ec357392cdc440c80d214a427f4`。签名证书 SHA256 `965bac5fc009f98059d68213a6e7a6a29a27676e1e2d017b8302a62aa95599f9`。

## 仍未通过的范围

真实窗口像素说明等比 cover 填满区域，也能看到部分水印和字幕边缘被裁掉。保留当前 cover 目标，不宣称全画幅字幕完整。四种源比例×两种区域的几何组件测试不等于全部比例实机验收。

未验收实体手柄/手势、主观音画同步、长时舒适度、所有源比例、实际佩戴唤醒、完整播放中休眠循环。自然休眠结果只覆盖暂停后离头睡眠与普通 ADB 唤醒。没有根目录 CI 工作流，不宣称 GitHub Actions 通过。因此 rc.7 仍是 prerelease。

发布附件保留精确源码、原生依赖源码、validation.json 和 SHA256SUMS；本地原始 UI、序列号和窗口截图不随公开附件分发。原生源码/清单支持核查与重建，不承诺逐字节可复现。
