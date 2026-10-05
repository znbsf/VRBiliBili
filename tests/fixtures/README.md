# 原创真实播放测试素材

`playback-h264-aac.mp4`：6秒，240×136，12fps，H.264 视频与 AAC 音频。内容仅为交替的纯色画面和程序生成的440Hz低音量正弦波，无外部视频、音乐、账号或个人信息。文件约82KB，供加载/解码/恢复技术验证，不用于画质或舒适度判断。

生成代码：`tools/generate-playback-fixture.py`。本轮使用本机已有 Blender 4.1.1（签名有效），只运行单线程后台2D序列编码。正常浏览器测试无需 Blender、ffmpeg、下载或安装软件。

如需重新生成，使用已安装且来源可信的 Blender，在仓库根目录执行：

```text
blender --background --factory-startup --disable-autoexec --threads 1 --python tools/generate-playback-fixture.py -- captures/fixture.mp4
```

代码生成的同等素材可能因编码器版本含不同二进制元数据；自动化根据实际帧、伴音、时长和恢复行为验收。默认回归使用当前已提交素材，其 SHA256 记录于 `docs/playback-validation.json`。

## Quest 恢复回归长片段

`clients/piliplus/android/app/src/androidTest/assets/quest-recovery-owned-60s.mp4` 为本项目原创的 60 秒、320×180、12fps H.264/AAC 测试素材，仅包含交替色块和生成的 440Hz 低音量音调，无外部影像、音乐或账号数据。随仓库 GPL-3.0 许可分发，仅打包进测试 APK。生成源码为 `tools/generate-quest-recovery-fixture.py`，使用已有 Blender 4.1；不自动安装或下载工具。

固定文件 SHA256：`e0f0bc8a1555d225038cf35a7393d92da44bf6603183b8b96e65d4fc1a0a4b48`。真实设备恢复回归使用长片段，避免启动 XR 所需时间耗尽原有 6 秒素材。
