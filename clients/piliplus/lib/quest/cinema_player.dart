import 'package:flutter/services.dart';
import 'package:PiliPlus/http/browser_ua.dart';
import 'package:PiliPlus/models/common/video/video_quality.dart';
import 'package:PiliPlus/pages/danmaku/controller.dart';
import 'package:PiliPlus/pages/video/controller.dart';
import 'package:PiliPlus/plugin/pl_player/models/data_status.dart';

import 'package:PiliPlus/quest/cinema_handoff.dart';

abstract final class CinemaPlayer {
  static const channel = MethodChannel('vrbilibili/cinema');
  static bool active = false;

  static Future<void> open(
    VideoDetailController detail, {
    String title = '',
  }) async {
    final player = detail.plPlayerController;
    final sessionCid = player.cid;
    bool ownsSource() => cinemaOwnsSource(
      sessionCid: sessionCid,
      playerCid: player.cid,
      detailCid: detail.cid.value,
      closed: detail.isClosed,
      querying: detail.isQuerying,
    );
    if (active ||
        !ownsSource() ||
        player.processing ||
        player.dataStatus.value != DataStatus.loaded ||
        player.dataSource.videoSource.isEmpty) {
      throw PlatformException(code: 'loading', message: '请等待视频加载完成');
    }
    active = true;
    PlDanmakuController? danmaku;
    CinemaFailure? primaryFailure;
    final lease = CinemaHandoffLease();
    final previousAutoPlay = detail.autoPlay;
    var qualityChanged = false;
    Map<String, dynamic> snapshot({bool selectedQuality = false}) => {
      'title': title,
      'video': selectedQuality
          ? detail.videoUrl
          : player.dataSource.videoSource,
      'audio': selectedQuality
          ? detail.audioUrl
          : player.dataSource.audioSource,
      'cid': player.cid,
      'positionMs': player.positionInMilliseconds,
      'headers': {
        'User-Agent': BrowserUa.pc,
        'Referer': 'https://www.bilibili.com/',
      },
    };
    try {
      await player.pause();
      if (!ownsSource()) throw PlatformException(code: 'closed');
      channel.setMethodCallHandler((call) async {
        if (!lease.open || !ownsSource()) {
          throw PlatformException(code: 'closed');
        }
        switch (call.method) {
          case 'catalog':
            return {
              'qualities': [
                for (final id
                    in detail.data.dash?.video?.map((v) => v.id).toSet() ??
                        <int>{})
                  {
                    'id': id,
                    'title': VideoQuality.fromCode(id).desc,
                    'selected': detail.currentVideoQa.value?.code == id,
                  },
              ],
            };
          case 'quality':
            final id = (call.arguments as Map)['id'] as int;
            if (!(detail.data.dash?.video?.any((v) => v.id == id) ?? false)) {
              throw PlatformException(code: 'quality');
            }
            final previousQuality = detail.currentVideoQa.value;
            detail.currentVideoQa.value = VideoQuality.fromCode(id);
            try {
              // No asynchronous Flutter reload can outlive this native request.
              detail.resolvePlayerSources();
            } catch (_) {
              detail.currentVideoQa.value = previousQuality;
              rethrow;
            }
            player.cacheVideoQa = id;
            qualityChanged = true;
            return snapshot(selectedQuality: true);
          case 'danmaku':
            final cid = player.cid;
            if (cid == null) return {'items': []};
            danmaku ??= PlDanmakuController(cid, player, player.isFileSource);
            final pos = (call.arguments as Map)['positionMs'] as int;
            await danmaku!.queryDanmaku(pos ~/ 360000);
            if (!lease.open || !ownsSource()) {
              throw PlatformException(code: 'closed');
            }
            return {
              'items': [
                for (var t = pos ~/ 100 * 100; t < pos + 1000; t += 100)
                  for (final e in danmaku!.getCurrentDanmaku(t) ?? [])
                    {'time': e.progress, 'text': e.content, 'color': e.color},
              ],
            };
          case 'position':
            if ((call.arguments as Map)['cid'] == player.cid) {
              detail.playedTime = Duration(
                milliseconds: (call.arguments as Map)['positionMs'] as int,
              );
              await detail.cacheLocalProgress(expectedCid: sessionCid);
            }
            return null;
          default:
            throw MissingPluginException();
        }
      });
      final result = await channel.invokeMapMethod<String, dynamic>(
        'open',
        snapshot(),
      );
      lease.close();
      channel.setMethodCallHandler(null);
      if (!ownsSource()) throw PlatformException(code: 'closed');
      final returnPosition = result?['cid'] == sessionCid
          ? (result?['positionMs'] as int?)
          : null;
      try {
        if (qualityChanged) {
          // Native playback has stopped. Drain this load before releasing active.
          await loadCinemaQuality(
            load: (autoplay) => detail.updatePlayer(autoplay: autoplay),
            pause: () async {
              if (player.cid == sessionCid && !detail.isClosed) {
                await player.pause();
              }
            },
          );
          if (player.dataStatus.value != DataStatus.loaded) {
            throw PlatformException(code: 'media');
          }
        }
        if (!ownsSource()) throw PlatformException(code: 'closed');
        if (returnPosition != null) {
          await player.seek(
            Duration(milliseconds: returnPosition),
            isSeek: false,
          );
        }
      } finally {
        // Reload resets playedTime to the old Flutter position. Preserve the
        // native return position even if media loading or seeking fails.
        if (returnPosition != null && ownsSource()) {
          detail.playedTime = Duration(milliseconds: returnPosition);
          await detail.cacheLocalProgress(expectedCid: sessionCid);
        }
      }
    } catch (error, stackTrace) {
      primaryFailure = (error: error, stackTrace: stackTrace);
    } finally {
      lease.close();
      // Never leave two audible players after a failed handoff or a return.
      await finishCinemaHandoff(
        pause: () async {
          if (player.cid == sessionCid && !detail.isClosed) {
            await player.pause();
          }
        },
        clearHandler: () => channel.setMethodCallHandler(null),
        disposeDanmaku: () => danmaku?.dispose(),
        resetActive: () {
          try {
            if (ownsSource()) {
              detail.autoPlay = previousAutoPlay;
            }
          } finally {
            active = false;
          }
        },
        primaryFailure: primaryFailure,
      );
    }
  }
}
