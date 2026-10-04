# APK 分发核查：2026-10-04

当前组合 APK 暂不公开分发。源码提交与本地构建、私人设备测试不代表第三方二进制分发已经获准。本页是基于固定依赖、原文条款和链接结构的审查记录，不是版权人授权或法律结论。

## 尚未解决的组合许可

客户端继承 [PiliPlus 固定提交 c102a611](https://github.com/bggRGjQaUbCoE/PiliPlus/tree/c102a6115c7ac040f6a0c6a1653944b81b82dcb4) 的 GPLv3 代码，许可原文保留在 [clients/piliplus/LICENSE](../clients/piliplus/LICENSE)。GPLv3 §5(c)、§6 和 §12 涉及组合作品、对应源码和无法同时满足义务时的分发限制。

`android/app/build.gradle.kts` 直接依赖 Meta Spatial SDK core、toolkit、vr 0.14.0；`CinemaActivity` 继承 SDK 的 `AppSystemActivity`，由同进程的 `CinemaSession` 连接 Flutter。三个固定 AAR/POM 的许可均指向 [Meta Platform Technologies SDK License Agreement](https://developers.meta.com/vr/licenses/oculussdk/)（2022-10-25）。§1.1／§1.1.1 对指定应用分发授予有条件的许可，§1.2.8 限制令 SDK 受开源许可约束的使用或再分发；§1.3 还有通知保留要求。

现有材料未包含相关版权人的有效链接例外，也未证明这些随应用打包的 SDK 是 GPL System Libraries 或独立聚合。因此，尚无足够依据放行组合 APK。示例项目的 MIT、用户确认或免责声明不能替代第三方许可。获得有效许可依据或改变组合方式属于后续需明确决策的工作；本轮没有接受新协议、改用另一产品架构或上传 SDK 二进制。

## 原生播放器及通知交付

Dart `media-kit` 包装层的 MIT 不能覆盖原生 libmpv／FFmpeg。固定 ARM64 JAR SHA256 为 `98df6410375cc7a4be7e6eff56f9ccd88fa52678973cc23bcf7e934ab8c8682d`，对应 [构建提交 8e50ecc0](https://github.com/My-Responsitories/libmpv-android-video-build/tree/8e50ecc027cd2443a3ef9b1a87991d37f1acdc41)（release `20260906`）。脚本固定 mpv 0.41.0、FFmpeg 9.0.1；mpv 使用 `-Dgpl=false`，FFmpeg 使用 `--disable-gpl --enable-version3`，支持 LGPL 构建路线的判断。

二进制正式分发前仍需核验各原生组件许可、匹配源码／补丁／构建脚本、修改及重链接要求，并检查最终 APK 内实际保留的通知。参考 [mpv Copyright](https://github.com/mpv-player/mpv/blob/v0.41.0/Copyright)、[FFmpeg LICENSE](https://github.com/FFmpeg/FFmpeg/blob/n9.0.1/LICENSE.md)。三个 Meta AAR 各含 `LICENSE` 和 80,504 字节的 `notices.html`；仓库的链接索引或一般依赖清单不能证明最终 APK 已保留它们。

既有继承资产来源记录见 [资产溯源](client-asset-provenance.json)，新增 60 秒色块／音调回归素材由本项目生成，来源和哈希见 [fixture 说明](../tests/fixtures/README.md)。未发现这些素材构成新增分发阻断项。

## 本轮已补齐的通知

三个固定 SDK AAR 的 `LICENSE` 与 `notices.html` 经逐字节比较相同，因此仅合并重复副本，原文保留在 `clients/piliplus/android/app/src/main/assets/third_party/meta-spatial-sdk-0.14.0/`，并附来源和哈希。最终 Q4 APK 内两份文本与每个 AAR 的原文都逐字节一致；没有把它们改为 GPL。该项已经完成，组合许可与原生播放器对应源码义务仍保持待决。

MPT §3.2 对相应第三方软件／内容设有其许可优先的条款，但现有通知未提供将本项目使用的 `AppSystemActivity`／`VRFeature` 等核心 API 整体转授为 MIT/GPL 的依据。[官方包说明](https://developers.meta.com/vr/documentation/spatial-sdk/spatial-sdk-packages/) 将 Spatial SDK 包归于 MPT；[官方示例 README](https://github.com/meta-quest/Meta-Spatial-SDK-Samples/blob/main/README.md) 也区分 MIT 示例代码与 MPT SDK／部分支持素材。

保持现有功能架构的最小路径是获得覆盖相关版权人的有效额外许可／链接例外，或对本项目实际组合取得可靠的兼容性确认，再完成其余二进制交付义务。这不会直接要求改变功能。GPL §7 允许版权人授予例外，不等于本项目已经获得例外；同进程或分包本身也不够作最终法律分类。未发现有效公开补充授权，不代表断言所有 GPL/Meta 组合都不兼容。
