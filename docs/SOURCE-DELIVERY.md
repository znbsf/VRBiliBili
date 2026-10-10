# Source delivery and rebuilding

Release attachments pair a Git-archive application source ZIP with the exact
commit embedded in the APK, SHA256SUMS and validation.json. The source includes
the client GPL license, local vendor sources/licenses, Flutter patch files,
locked package revisions and Android build scripts. Build from the clean commit
using Flutter 3.47.5 prepared with tools/Prepare-QuestFlutter.py, Java 17 and
Android SDK 37. tools/Build-QuestApp.ps1 records source identity and requires
your own private signing configuration; that configuration is never distributed.

Native-source materials include the fixed libmpv Android builder, mpv 0.41.0,
FFmpeg 9.0.1, enabled native dependencies, pinned submodule snapshots, the mpv
patch and original license texts. See [native-source-manifest.json](native-source-manifest.json)
for exact archives and revisions and BUILD-AND-LICENSES.md inside the source
materials for safe reconstruction/relink instructions. The media-kit MIT Dart
license does not replace the native component licenses. The app keeps those
license texts in assets/third_party/open-panel and ships source materials
alongside the binary.

The packaged libmpv is compared byte-for-byte to the pinned upstream default
JAR. We did not rebuild that third-party binary bit-for-bit. The upstream helper
and native-event-loop source pins were reconstructed from its public history;
that limitation is stated in the materials rather than called reproducible.
Android SDK/NDK and other system toolchains are not redistributed.

The app source ZIP is generated from Git, not from a live directory: no signing
keys, credentials, account files, machine paths, build caches or private logs.
Old cinema source is retained but excluded from the product build, not shipped
as a proprietary SDK binary. Locked Dart sources remain available from the
public repositories/registries recorded in dependency-audit.json.
