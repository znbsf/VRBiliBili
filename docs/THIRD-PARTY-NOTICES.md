# Third-party notices: ordinary panel

Current runtime: Flutter/PiliPlus (client GPLv3), material_ui (BSD), webview
(Apache-2.0), media-kit Dart packages (MIT), libmpv/FFmpeg and native dependencies
under their individual licenses. Exact locked repositories are in
[dependency-audit.json](dependency-audit.json); corresponding native source
snapshots and original licenses are described in [SOURCE-DELIVERY.md](SOURCE-DELIVERY.md).

The client LICENSE and vendor licenses remain unchanged. Account manager and
Anime4K subdirectory licenses retain their original scope. Inherited assets
remain documented in [client-asset-provenance.json](client-asset-provenance.json).
Self-generated test fixtures retain [their provenance](../tests/fixtures/README.md).

Meta Spatial SDK and cinema-only Media3 are historical source references, not
current runtime dependencies. Their old source/notices remain for traceability;
the build excludes their code and Meta notices from the APK. Do not apply the
former GPL/Meta combined-binary assessment to code 6 without inspecting the
actual APK. Conversely, removing Meta does not erase other source obligations.

APK assets/third_party/open-panel contains DART-LICENSES.txt, NATIVE-LICENSES.txt
and PiliPlus-GPL-3.0.txt. Upstream Flutter license screens continue to expose
package licenses. Repository-source visibility and a pre-release label do not
by themselves satisfy a license; original component texts govern.
