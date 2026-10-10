# 2026-10-05 Q4 修复合入与无人佩戴验证推进

唯一写入者在原工作区合入 d73f752 相对 796763a 的恢复实现；保留已有 UI、签名和其他未提交修改。逐文件合并前副本与清单在 `.local/q4-integration-20261005/before`、`manifest.json`。没有重置、提交或修改其他 checkout。

已合入来源 CID 归属检查、交接租约关闭、质量切换及清理异常处理、多 CID 缓存读取/持久化、原生预览回调代次隔离和恢复测试入口。主分支 Q4 的硬件通过记录是历史证据，本次候选不继承其通过状态。

本地 17 项 Dart 交接测试、20 项截图校验测试通过。相关 Dart 静态检查无错误、两条 info。Release 构建成功（147.6 秒）；0.1.0 / versionCode 2 / io.github.vrbilibili.quest / ARM64 / 不可调试，沿用正式证书。已更新安装并读回同一 SHA256：f685c65bd4032286957c43ac055b14b7a514ec2fe73798e845062f2303a22815。旧 v1 产物保留；新产物在 reports/quest-release-20261005/q4-merged-v2/。

SDK LICENSE/notices 已合入且验证 APK 内字节相同，分发审查已引入本仓库。这不解决 GPL / Meta SDK 兼容性，仍仅本地私有；未公开 Release。当前仍为 Spatial SDK，不是纯 OpenXR 迁移。

## 无人佩戴诊断

官方 CLI 1.8 的 input 只暴露普通 key；ui 提供 Android 元素点击。没有把这些接口声明为 XR 控制器输入。官方环境文档区分二维 Spatial Simulator 与桌面 XR Simulator；TapeDeck 需要真实有效录制，预检成功不代表回放成功。

本轮实际读回：enabled=true、prox_override=DISABLED、power_state=STANDBY、autosleep_disabled=false；窗口焦点为 LaunchCheckControllerRequiredDialogActivity；TapeDeck 原型 pidof 返回 1 无进程。XR 焦点仍未知，不能从窗口焦点推断。官方 ui actions 在待机下失败：uiautomator 报告写入，但拉取文件不存在；没有据此点击未经确认的坐标。

新增 tools/Inspect-QuestState.py，每个子命令 15 秒超时，分别记录近距/电源、窗口、进程；XR 状态明确 unverified。已实际运行，收据在 .local/q4-integration-20261005/device-state.json。脚本不唤醒、不 cast、不覆盖近距。

## 下一轮入口与未验项

已一次性请求新增最多 120 秒官方近距覆盖和该窗口内桌面 cast/input，异常或结束立即恢复并读回；截至此记录尚未收到授权，因此本轮未执行覆盖或 cast，也未变更 Guardian/安全弹窗/追踪。授权不是控制器焦点或录制成功的保证。

授权后先在同一窗口重复只读诊断，再取桌面 UI/cast 证据；仅操作明确可用的正常应用入口，不绕过安全提示。若应用真正运行，必须记录 XR READY/VISIBLE/FOCUSED 和渲染/输入实际计数，再决定录制；只有存在有效 VRS 才能验证回放。现有独立原型和自动测试入口可继续工作，不能将等待控制器作为全部开发的终点。

新候选的真实播放、XR 显示、XR 输入、影院往返与休眠恢复仍未验；纯 OpenXR 完整产品迁移、有效 VRS/回放仍未完成。Manifest 已有 handtracking 声明不是已实现/已验手势交互证据，本轮没有新增此类声明来绕过启动门。

官方依据：https://developers.meta.com/vr/essentials/metavr-environment/

## 后续授权与离头实测（同日）

用户随后明确授权本任务不限次数/时长的开发验证。此前“待新增授权”已失效；测试采用可自动到期的近距覆盖，结束仍显式恢复，不关闭 Guardian 或安全弹窗。

离头实测取消旧启动请求（官方 input key back）后，二维 InspectActivity 得到进程并成功创建 OpenXR instance、查询 Oculus runtime / Meta Quest 3 system，xrResult=0。videoSurfaceCreated=false、framesSubmitted=0，因此不是会话/画面验收。官方 ui actions 可列出正式包 25 个元素；桌面点击选片后真实 B 站 BV1V9aq6CErU 已显示画面，截图进度 0:08 / 1:50。

点击全屏影院发现 code 2 的实际崩溃：Duplicate Systems / n1 already registered，发生在 Spatial SDK Activity.onCreate。已启用项目 ProGuard 文件并保留 com.meta.spatial 的类及成员，正在构建 code 3 复测。此时不能写影院通过。独立原型 12 个 instrumentation 契约再次通过，回执明确 xrSessionVerified=false。

证据仅本地 `.local/offhead-20261005/`，截图包含透视房间背景，不用于公开 README。官方开发输入功能见 https://developers.meta.com/vr/documentation/spatial-sdk/spatial-sdk-tooling-castinputforward/ ，CastInputForwardFeature 必须仅用于开发构建；目前尚未接入/实测，不宣称已可用。

## 本轮收尾实测结果

code 3 构建成功并已安装，设备与本地产物 SHA256 一致：f9c5d3d82edb81aa85007bbbc882eb93ce2c135092ad4f787c29e02ff5d2f12d。签名验证通过。R8 mapping 验证 SystemManager、ScaleSystem 保留原名称。规则最终为 keep,allowshrinking，第一轮全保留因未使用的可选 okhttp compat 引用失败，未使用 dontwarn 隐藏问题。

复测出现系统移动追踪丢失提示：“你的房间边界和大多数应用都无法运行，因为头戴设备目前无法检测你的移动情况。”没有选择继续不追踪或旅行模式。自动审批拒绝了发送 Back 后继续启动视频的组合操作（blocked by policy，无更详细理由），未换命令绕过。因此 code 3 的影院修复只有构建/安装/映射验证，实际影院仍待复测；code 2 已确认影院失败，不能沿用 unverified 掩盖失败。

结束显式恢复并读回 enabled=true / prox_override=DISABLED / autosleep_disabled=false。用户的持续开发授权仍有效，但不将追踪丢失或自动审批拒绝当成可以强行越过的提示。无需重新索取同一近距授权。下一次在追踪正常后复测 code 3；继续完善仅 Debug 的官方 CastInputForwardFeature 接入（本轮只核实官方文档，未接入）。原 Q4/账号数据未清除，未公开 Release。

## code 4：面板播放器比例自适应

用户截图显示上下人工黑边。原因是左侧 Expanded 强制占满行高，同时内部固定 16:9 并 Center；本次改为居中但收缩到内容高度的卡片，视频通过 Flexible 获取最大可用高度，按解码尺寸（元数据后备）等比限制在可用宽高内，控制栏紧贴画面。容器宽高比边界 9:16..2.4:1，极端素材仍 contain，不拉伸/裁切。静态检查无错误，仅原有 unnecessary_lambdas 提示。实际头显视觉复核待完成。

code 4 已构建并更新安装，设备 APK SHA256 与本地一致：078b1d27da5aa16399b8a50b0e3f77645a6c6d9ce19294eff43501146a505b1b。本次未唤醒头显或变更近距设置，视觉效果未冒充已复核。
