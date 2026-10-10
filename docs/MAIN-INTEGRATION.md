# main 与 rc.6 整合记录（2026-10-10）

## 分叉与目标

共同祖先为 `796763a5e2ab2e68b84dea08ef2f00a692a51801`。整合前 main 为 `6d238556ddb7a31b2878f913d354132a32fe0e7f`，rc.6 为 `cde0af04f800c54b86f468e15f26ed185b9ff3be`，双方分别独有 4 与 3 个提交。rc.4/rc.6 发布线从共同祖先延续，复制过 main 的恢复代码，但没有合并 main 历史；此前 main 只更新了 README 发布入口。因此发布完成不等于功能已进入 main。

本次正常双亲合并同时保留两条历史，不改写 tag。当前唯一产品目标是普通 Android 面板，入口与构建配置采用 rc.6 已发布实现。

## 冲突处理

- 多数冲突仅为 CRLF/LF；逐文件确认等价后保留 main 原内容，包括 controller.dart、cinema_handoff.dart 与恢复回归测试。
- 当前 manifest、MainActivity、视频页面、build_config、pubspec、构建脚本及依赖/分发文档采用 rc.6，保持普通面板、单次 seek 和 XR 隔离。
- 保留 main 的增强版 QuestHardwareRecoveryRegression（仍排除于当前构建）、历史交接、截图、构建身份、fixture 工具，不恢复旧入口。
- 历史文档加上版本边界提示；README 与 CURRENT-STATE 反映合并后的当前方向。
- 修复 client .gitignore 错误测试例外，将实际执行过的 panel_widgets_test.dart 纳入 Git。该文件未被收入 rc.6 原源码 ZIP，这是本次补齐；不声称原 ZIP 已包含它。

## 发布身份与验证边界

rc.6 tag 仍固定 `cde0af0`；APK SHA256 为 `fe014f789e2c492d831d3bde9787ec5f35e304867a48e6a56fabd8da1fdcadf3`。后续 main commit 不替代旧包源码身份。运行时代码和当前构建配置与 rc.6 对比校验；本次合并后的测试、分析和编译结果在本地整合报告中保存。GitHub Actions 无可用运行记录不能算 CI 通过。设备 unauthorized，实机验收仍待完成。
