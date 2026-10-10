> 历史快照：保留原始版本、结果与经验；不构成当前目标或操作授权。旧“只读/不推送/不发布”、设备状态和源码-only 限制仅适用于记录当时阶段。当前状态见 [CURRENT-STATE](../CURRENT-STATE.md)；历史 pass 不自动等于当前 pass。

# 独立真实媒体技术探针

这是与现有交互原型、正式 PiliPlus UI 解耦的可丢弃可行性验证；不决定最终客户端或 XR 路线。它使用浏览器 HTMLMediaElement 实际解码本地合法视频或 HTTP(S) 直接媒体，验证单一会话的恢复规则。

## 使用

在仓库根目录用 Node 启动仅绑定本机的服务器：

```powershell
node tools/serve-playback-probe.mjs
```

打开打印出的本机地址，选择自己有权播放的视频，或输入支持匿名 CORS 的直接媒体地址。点击播放/暂停、拖动进度；网络恢复后点击重试。关闭/刷新后重新选择同一素材可恢复进度，保持暂停。浏览器不支持某编码时显示错误，不声称所有编码或链接可用。

从文件系统直接双击 HTML 可能因模块/CORS 限制无法运行，使用上述本机服务器。既有离线 `prototype-preview.html` 仍可直接双击。

## 实现边界

- 状态来自 loadedmetadata、playing、waiting、timeupdate、pause、ended、error 和 offline/online。进度由实际 currentTime 获取，没有模拟播放计时。
- 一个 video 元素与一个会话。换素材先释放旧 URL/资源；异步来源选择按序号保护；过期 play Promise 不覆盖新素材。
- 手动重试、加载超时、离线暂停；网络恢复不自动发声。重试保留进度与用户播放/暂停意图，浏览器要求交互时回到可手动播放状态。
- 本地进度只保留最多20个摘要指纹/时间，不存 URL、文件名、Blob、媒体内容或凭据；损坏/不可用存储不阻断本次播放。
- 文件指纹使用大小与首尾各最多64KB，避免整段大视频载入内存。它不是全文件内容哈希，特殊的首尾相同/中段不同文件可能碰撞。URL 参数变化视为另一素材，尚不处理签名地址续期后的内容身份关联。

## 自动化

Node >=22 与已安装 Chromium；无需下载浏览器或额外 npm 依赖：

```powershell
node tools/playback-probe-check.mjs --output captures/playback-probe
```

自动化在独立静音 headless Chrome 里，解码仓库内82KB的原创 H.264/AAC 素材（6秒、240×136、12fps 的颜色变化与正弦音）。通过条件包含真实视频帧、伴音解码、媒体时钟、拖动、HTTP 失败、断网/恢复、重选素材、刷新及关闭/重开浏览器。原始日志/截图位于被忽略的 captures，不进入提交。微型素材只验证流程，不是1080p/30fps或硬件性能验收。

素材生成脚本是 `tools/generate-playback-fixture.py`。本轮只使用已有且签名有效的 Blender 4.1.1，一线程后台2D序列编码，没有3D场景渲染或新软件安装。正常测试直接读取已有素材，**不依赖 Blender**；生成方法见 [素材说明](../../tests/fixtures/README.md)。

验证摘要在 [playback-validation.json](../playback-validation.json)，不包含本机路径。headless 下音频输出被静音以免干扰用户；音频解码字节证明伴音解码，不能替代真人听感和音画同步检查。

## 尚未完成

没有 B 站 API/扫码/账号会话、DASH 分离轨道/请求头、DRM、真实 media-kit 解码、外部空间 Surface、XR 或 Quest。用户评审与头显输入/舒适度仍是正式客户端开发门槛。上游接入事实见 [播放基线](PILIPLUS-PLAYBACK-BASELINE.md)。测试通过不表示应用已发布或已经完成 Quest 验收。
