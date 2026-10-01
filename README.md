# VRBiliBili：PiliPlus 空间版原型

以 [PiliPlus](https://github.com/bggRGjQaUbCoE/PiliPlus) 为第三方客户端基础，探索 Quest 3 空间大屏、多组件摆放与可选空间弹幕。当前仓库包含设计、可点击的桌面原型与验证工具，尚无 PiliPlus 业务源码副本、业务 APK 或 Quest 验收结果。

阶段顺序为功能/UI 原型评审 → 头显等比例与输入验证 → 实施。首版围绕发现/搜索、详情、空间播放、稍后再看与历史续播；主视频始终不透明，最多两个辅助面板。河流弹幕是有条件试用，跳出/朗读为实验预览。最终平台路线和舒适度须由后续验证决定。

## 打开原型

直接打开 [prototype-preview.html](prototype-preview.html)，无需联网或安装依赖。内容、播放进度、登录流程与画质均为虚构示意，不连接 B 站或真实账号，不实现视频解码、XR 或空间音频。跨刷新保存依赖浏览器允许本地存储。

[piliplus-v1-prototype.html](piliplus-v1-prototype.html) 是可编辑源片段；修改后执行 `node tools/Build-PrototypePreview.cjs` 重建独立预览。仓库保留预览作为可直接打开的评审交付。

## 验证

静态检查及 DOM 检查需要 Node；真实浏览器工具需要 **Node >=22** 与已安装的 Chrome/Edge，也可用 `--browser` 指定其他 Chromium 路径。先安装仅用于 DOM 验证的固定依赖：

```powershell
cd tools/prototype
npm ci --ignore-scripts
cd ../..
node tools/Build-PrototypePreview.cjs
node prototype-check.cjs
node prototype-flow-check.cjs
node tools/prototype-browser-check.mjs --output captures/core-review
node tools/prototype-robustness-check.mjs --output captures/robustness-review
```

浏览器测试创建隔离配置与单文件本机服务器，完成后关闭本次浏览器，不复用用户窗口。测试包含真实鼠标、Escape、拖动、刷新与浏览器重开；部分设置组合使用原生 DOM input/change 事件。测试只模拟桌面流程，不能替代用户体验或 Quest 验收。结果与原始截图生成到被忽略的 `captures/`。

当前本地验证及覆盖说明见 [验证记录](docs/VALIDATION.md) 和 [去除本机路径的结果摘要](docs/validation-results.json)。仓库初始基线尚未配置 CI 工作流，不把本地测试称为远端 CI。

## 设计资料

| 文档 | 内容 |
| --- | --- |
| [V1-FUNCTIONAL-UI-DESIGN.md](V1-FUNCTIONAL-UI-DESIGN.md) | 首版范围、完整观看流程、窗口规则与验收门槛 |
| [V1-PROTOTYPE-REVIEW.md](V1-PROTOTYPE-REVIEW.md) | 原型覆盖、已修复问题与复查路径 |
| [PILIPLUS-SPATIAL-DESIGN.md](PILIPLUS-SPATIAL-DESIGN.md) | 空间设计与实现候选，不能自动扩大 V1 |
| [QUEST3-TEST-PLAN.md](QUEST3-TEST-PLAN.md) | 头显播放、输入、恢复和空间组件验收矩阵 |
| [VR-UI-REFERENCES.md](VR-UI-REFERENCES.md) | 实际演示时间点、官方说明与设计拆解 |
| [VR-REFERENCE-GALLERY.md](VR-REFERENCE-GALLERY.md) | VR 应用图册、效果图和演示入口 |
| [INITIAL-RESEARCH.md](INITIAL-RESEARCH.md) | 选定 PiliPlus 前的历史调研；旧候选建议不代表当前选择 |
| [开发环境边界](docs/DEVELOPMENT-ENVIRONMENT.md) | 工具验证概况与仍需设备/许可的事项 |
| [调研快照](docs/research-snapshot.json) | 历史上游版本与来源，不代表实时状态 |

本机安装目录、官方示例副本、完整机器环境记录、原始检索/测试日志、备份、APK 和交接包保留在本地，不纳入仓库。技术验证启动后再固定 PiliPlus 上游提交与真实依赖；目前未选定最终空间实现路线。
