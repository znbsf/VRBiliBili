import 'package:flutter_test/flutter_test.dart';
import 'package:PiliPlus/quest/release_channel.dart';

void main() {
  test('automatic checking does not open or request upstream releases', () async {
    final urls = <String>[];
    await ReleaseChannel((url) async { urls.add(url); }).check();
    expect(urls, isEmpty);
  });
  test('manual checking uses project releases including prereleases', () async {
    final urls = <String>[];
    await ReleaseChannel((url) async { urls.add(url); }).check(automatic: false);
    expect(urls, ['https://github.com/znbsf/VRBiliBili/releases']);
  });
  test('stale upstream asset and malformed payload cannot choose a download', () async {
    final urls = <String>[];
    final channel = ReleaseChannel((url) async { urls.add(url); });
    await channel.download({'assets': [{'browser_download_url': 'https://github.com/bggRGjQaUbCoE/PiliPlus/releases/download/latest/upstream.apk'}]});
    await channel.download(null);
    expect(urls, List.filled(2, 'https://github.com/znbsf/VRBiliBili/releases'));
  });
  test('browser failure never falls back to upstream or another package', () async {
    final urls = <String>[];
    final channel = ReleaseChannel((url) { urls.add(url); throw StateError('browser unavailable'); });
    await expectLater(channel.check(automatic: false), throwsStateError);
    expect(urls, ['https://github.com/znbsf/VRBiliBili/releases']);
  });
}
