# 第三方来源与许可索引

本索引记录已有来源与保留的许可文本，不重新授予其他权利人的许可，也不把整个仓库改为 MIT。公开源码与发布包含第三方二进制的 APK 分别核对。

| 范围 | 来源和许可 | 许可或证据 |
| --- | --- | --- |
| PiliPlus 客户端及继承 assets | 固定 bggRGjQaUbCoE/PiliPlus c102a6115c7ac040f6a0c6a1653944b81b82dcb4；GPLv3，保留子目录例外 | [客户端 LICENSE](../clients/piliplus/LICENSE)、[60 文件来源与 SHA256](client-asset-provenance.json) |
| material_ui | 1.4.0 及上游匹配 Material 补丁；保留 BSD | [LICENSE](../clients/piliplus/vendor/material_ui/LICENSE) |
| webview Android fork | 固定 0bfa46dfff87f0d9e9d5e13cbd5c4a7c7310f8c9；Apache2 | [LICENSE](../clients/piliplus/vendor/webview/LICENSE) |
| account_manager | MIT | [LICENSE](../clients/piliplus/lib/utils/accounts/account_manager/LICENSE) |
| Anime4K shader | MIT，优先适用其子目录文本 | [LICENSE](../clients/piliplus/assets/shaders/LICENSE) |
| media-kit fork | 固定 73771ec38176be2d984a3049c28177bce23b54a0；保留各包及原生后端许可 | [锁文件](../clients/piliplus/pubspec.lock)，缓存 jar 未加入 Git |
| AndroidX Media3 | ExoPlayer 1.5.1；Apache2 | [版本 LICENSE](https://github.com/androidx/media/blob/1.5.1/LICENSE) |
| Meta Spatial SDK | core/toolkit/vr 0.14.0；MPT SDK Agreement，并非 MIT | [包许可](https://developers.meta.com/vr/documentation/spatial-sdk/spatial-sdk-packages/)、[MPT 条款](https://developers.meta.com/vr/licenses/oculussdk/) |
| Pillow 测试工具依赖 | 12.3.0；MIT-CMU（HPND） | [LICENSE](https://github.com/python-pillow/Pillow/blob/12.3.0/LICENSE)、[固定依赖](../tools/requirements-quest-test.txt) |

当前 Git 索引没有 AAR、SO、JAR、DLL 或 APK；Meta SDK 仅以 Maven 坐标引用。影院贴图和几何由 CinemaActivity.kt 的 Bitmap/Canvas/RadialGradient 及 SceneMesh 程序生成，没有打包 Meta 示例的 GLB、GLTF、GLXF、纹理或样例视频。本地官方示例研究副本不属于公开仓库。

这份源码许可索引不作组合 APK 发行兼容性结论。将来发布含 MPT SDK 的二进制时，须核对其 1.2.8 开源约束边界、1.3 再发行通知与 GPL 客户端的实际发行义务，并收集 Media3、media-kit 和其他运行时依赖通知。当前源码引用不直接等同于再分发 MPT 二进制。

参考链接不授予远端图片或视频的再发行权。仓库保存的是来源链接与自制示意原型；[测试媒体说明](../tests/fixtures/README.md) 记录本地探针的合成色块和音频，未复制电影或用户视频。

仓库自身原型、工具和文档不因本索引被自动改用另一种许可证。客户端 GPL 与各 vendored 许可原文继续保留，具体文本优先于本摘要。
