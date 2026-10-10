/// Fork releases are reviewed downloads, never upstream binary replacements.
class ReleaseChannel {
  const ReleaseChannel(this.openPage);
  final Future<void> Function(String) openPage;
  static const releasesUrl = 'https://github.com/znbsf/VRBiliBili/releases';

  Future<void> check({bool automatic = true}) async {
    if (!automatic) await openPage(releasesUrl);
  }

  // Ignore release/asset URLs supplied by stale upstream dialogs or callers.
  Future<void> download(Object? release) => check(automatic: false);
}
