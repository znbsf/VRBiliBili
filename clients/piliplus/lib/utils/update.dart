import 'package:PiliPlus/quest/release_channel.dart';
import 'package:PiliPlus/utils/page_utils.dart';

abstract final class Update {
  static const _channel = ReleaseChannel(PageUtils.launchURL);

  // Automatic upstream checks are intentionally disabled for this fork.
  // Manual requests always open the project's release page, including fallback.
  static Future<void> checkUpdate([bool isAuto = true]) =>
      _channel.check(automatic: isAuto);

  static Future<void> onDownload(Map data, {String? ext}) =>
      _channel.download(data);
}
