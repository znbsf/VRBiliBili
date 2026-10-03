import 'package:flutter/services.dart';
import 'package:PiliPlus/http/browser_ua.dart';
import 'package:PiliPlus/models/common/video/video_quality.dart';
import 'package:PiliPlus/pages/danmaku/controller.dart';
import 'package:PiliPlus/pages/video/controller.dart';
import 'package:PiliPlus/plugin/pl_player/models/data_status.dart';

abstract final class CinemaPlayer {
  static const channel = MethodChannel('vrbilibili/cinema');
  static bool active = false;

  static Future<void> open(VideoDetailController detail) async {
    final player = detail.plPlayerController;
    if (active || player.processing || player.dataSource.videoSource.isEmpty) {
      throw PlatformException(code: 'loading', message: '请等待视频加载完成');
    }
    active = true;
    PlDanmakuController? danmaku;
    Map<String, dynamic> snapshot() => {
      'video': player.dataSource.videoSource,
      'audio': player.dataSource.audioSource,
      'cid': player.cid,
      'positionMs': player.positionInMilliseconds,
      'headers': {'User-Agent': BrowserUa.pc, 'Referer': 'https://www.bilibili.com/'},
    };
    try {
      await player.pause();
      channel.setMethodCallHandler((call) async {
        switch (call.method) {
          case 'catalog':
            return {'qualities': [for (final id in detail.data.dash?.video?.map((v) => v.id).toSet() ?? <int>{})
              {'id': id, 'title': VideoQuality.fromCode(id).desc,
                'selected': detail.currentVideoQa.value?.code == id}]};
          case 'quality':
            final id = (call.arguments as Map)['id'] as int;
            player.cacheVideoQa = id;
            detail.currentVideoQa.value = VideoQuality.fromCode(id);
            final autoPlay = detail.autoPlay;
            detail.autoPlay = false;
            try {
              detail.updatePlayer();
              await Future<void>.delayed(const Duration(milliseconds: 100));
              final deadline = DateTime.now().add(const Duration(seconds: 25));
              while (player.processing || detail.isQuerying) {
                if (DateTime.now().isAfter(deadline)) throw PlatformException(code: 'timeout');
                await Future<void>.delayed(const Duration(milliseconds: 100));
              }
              await player.pause();
              if (player.dataStatus.value != DataStatus.loaded) throw PlatformException(code: 'media');
            } finally {
              detail.autoPlay = autoPlay;
            }
            return snapshot();
          case 'danmaku':
            final cid = player.cid;
            if (cid == null) return {'items': []};
            danmaku ??= PlDanmakuController(cid, player, player.isFileSource);
            final pos = (call.arguments as Map)['positionMs'] as int;
            await danmaku!.queryDanmaku(pos ~/ 360000);
            return {'items': [for (var t = pos ~/ 100 * 100; t < pos + 1000; t += 100)
              for (final e in danmaku!.getCurrentDanmaku(t) ?? [])
                {'time': e.progress, 'text': e.content, 'color': e.color}]};
          case 'position':
            if ((call.arguments as Map)['cid'] == player.cid) {
              detail.playedTime = Duration(milliseconds: (call.arguments as Map)['positionMs'] as int);
              detail.cacheLocalProgress();
            }
            return null;
          default: throw MissingPluginException();
        }
      });
      final result = await channel.invokeMapMethod<String, dynamic>('open', snapshot());
      if (result?['positionMs'] case final int position when result?['cid'] == player.cid) {
        await player.seek(Duration(milliseconds: position), isSeek: false);
        detail.playedTime = Duration(milliseconds: position);
        detail.cacheLocalProgress();
      }
    } finally {
      // Never leave two audible players after a failed handoff or a return.
      await player.pause();
      channel.setMethodCallHandler(null);
      danmaku?.dispose();
      active = false;
    }
  }
}
