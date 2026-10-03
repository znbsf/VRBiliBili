import 'dart:io';
import 'package:flutter/foundation.dart' show kDebugMode;
import 'package:flutter/services.dart';
import 'package:PiliPlus/http/browser_ua.dart';
import 'package:PiliPlus/plugin/pl_player/controller.dart';
import 'package:PiliPlus/pages/video/controller.dart';
import 'package:PiliPlus/pages/video/introduction/ugc/controller.dart';
import 'package:PiliPlus/http/reply.dart';
import 'package:PiliPlus/models/common/video/video_quality.dart';
import 'package:PiliPlus/plugin/pl_player/models/data_status.dart';
import 'package:PiliPlus/pages/danmaku/controller.dart';
import 'package:PiliPlus/http/init.dart';

/// Only one decoder may play: pause Flutter before handing off to native VR.
abstract final class SpatialPlayer {
  static const channel = MethodChannel('vrbilibili/spatial');
  static bool active = false;

  static Future<void> open(PlPlayerController player, {
    String title = 'VRBiliBili', VideoDetailController? detail, UgcIntroController? intro,
  }) async {
    if (!Platform.isAndroid || active || player.processing) return;
    final source = player.dataSource;
    if (source.videoSource.isEmpty) return;
    active = true;
    PlDanmakuController? danmaku;
    int? danmakuCid;
    Map<String, dynamic> snapshot() => {
      'video': player.dataSource.videoSource,
      'audio': player.dataSource.audioSource,
      'positionMs': player.positionInMilliseconds,
      'title': intro?.videoDetail.value.title ?? title,
      'cid': player.cid,
      'headers': {'User-Agent': BrowserUa.pc, 'Referer': 'https://www.bilibili.com/'},
    };
    try {
      await player.pause();
      channel.setMethodCallHandler((call) async {
        switch (call.method) {
          case 'catalog':
            return {
              'episodes': [for (final p in intro?.videoDetail.value.pages ?? [])
                {'cid': p.cid, 'title': p.part ?? '分集', 'selected': p.cid == player.cid}],
              'qualities': [for (final q in detail?.data.dash?.video?.map((v) => v.id).toSet() ?? <int>{})
                {'id': q, 'title': VideoQuality.fromCode(q).desc}],
              'subtitles': [for (var i = 0; i < (detail?.subtitles.length ?? 0); i++)
                {'id': i, 'title': detail!.subtitles[i].lanDoc ?? detail.subtitles[i].lan}],
            };
          case 'danmaku':
            final cid = player.cid;
            if (cid == null) return {'items': []};
            if (danmakuCid != cid) {
              danmaku?.dispose(); danmakuCid = cid;
              danmaku = PlDanmakuController(cid, player, player.isFileSource);
            }
            final pos = (call.arguments as Map)['positionMs'] as int;
            await danmaku!.queryDanmaku(pos ~/ 360000);
            return {'items': [for (var t = pos ~/ 100 * 100; t < pos + 1000; t += 100)
              for (final e in danmaku!.getCurrentDanmaku(t) ?? [])
                {'time': e.progress, 'text': e.content, 'color': e.color}]};
          case 'subtitle':
            if (detail == null) return {'items': []};
            final index = (call.arguments as Map)['id'] as int;
            if (index < 0 || index >= detail.subtitles.length) return {'items': []};
            final path = detail.subtitles[index].subtitleUrl;
            if (path == null) return {'items': []};
            final uri = path.startsWith('//') ? 'https:$path' : path;
            final response = await Request().get(uri);
            return {'items': [for (final e in response.data?['body'] ?? [])
              {'from': e['from'], 'to': e['to'], 'text': e['content']}]};
          case 'comments':
            if (detail == null) return {'text': '该内容没有可用评论'};
            final response = await ReplyHttp.replyList(isLogin: false, oid: detail.aid,
                nextOffset: '', type: 1, page: 1);
            final replies = response.dataOrNull?.replies;
            return {'text': replies == null ? '评论暂不可用，可返回内容页查看' :
              replies.take(20).map((r) => '${r.member?.uname ?? ''}\n${r.content?.message ?? ''}').join('\n\n')};
          case 'select':
            if (intro == null || detail == null) throw PlatformException(code: 'unavailable');
            final cid = (call.arguments as Map)['cid'];
            final part = intro.videoDetail.value.pages!.firstWhere((p) => p.cid == cid);
            if (!await intro.onChangeEpisode(part)) throw PlatformException(code: 'select');
            final deadline = DateTime.now().add(const Duration(seconds: 25));
            while (detail.isQuerying || player.processing || player.cid != cid) {
              if (DateTime.now().isAfter(deadline)) throw PlatformException(code: 'timeout');
              await Future<void>.delayed(const Duration(milliseconds: 100));
            }
            if (player.dataStatus.value != DataStatus.loaded) throw PlatformException(code: 'media');
            await player.pause();
            return snapshot();
          case 'quality':
            if (detail == null) throw PlatformException(code: 'unavailable');
            final id = (call.arguments as Map)['id'] as int;
            detail.currentVideoQa.value = VideoQuality.fromCode(id);
            detail.updatePlayer();
            await Future<void>.delayed(const Duration(milliseconds: 100));
            final deadline = DateTime.now().add(const Duration(seconds: 25));
            while (player.processing) {
              if (DateTime.now().isAfter(deadline)) throw PlatformException(code: 'timeout');
              await Future<void>.delayed(const Duration(milliseconds: 100));
            }
            if (player.dataStatus.value != DataStatus.loaded) throw PlatformException(code: 'media');
            await player.pause();
            return snapshot();
          case 'refresh':
            if (detail == null) return snapshot();
            await detail.queryVideoUrl(fromReset: true);
            if (player.dataStatus.value != DataStatus.loaded) throw PlatformException(code: 'media');
            await player.pause();
            return snapshot();
          case 'position':
            // Persist only media identity and playback time through the existing store.
            final position = (call.arguments as Map)['positionMs'] as int;
            if (detail != null && (call.arguments as Map)['cid'] == player.cid) {
              detail
                ..playedTime = Duration(milliseconds: position)
                ..cacheLocalProgress();
            }
            return null;
          default: throw MissingPluginException();
        }
      });
      final result = await channel.invokeMapMethod<String, dynamic>('open', snapshot());
      if (result?['positionMs'] case final int position when result?['cid'] == player.cid) {
        // seekTo's default waits for a buffer event, which may never arrive while paused.
        await player.seek(Duration(milliseconds: position), isSeek: false);
        final deadline = DateTime.now().add(const Duration(seconds: 5));
        while ((player.positionInMilliseconds - position).abs() > 500 && DateTime.now().isBefore(deadline)) {
          await Future<void>.delayed(const Duration(milliseconds: 50));
        }
        if (detail != null) {
          detail
            ..playedTime = Duration(milliseconds: position)
            ..cacheLocalProgress();
        }
      }
      // Returning is an explicit pause, including interruptions and decoder errors.
      await player.pause();
      if (kDebugMode) await channel.invokeMethod<void>('debugHandoff', {
        'positionMs': player.positionInMilliseconds, 'cid': player.cid,
        'playing': player.playerStatus.isPlaying,
      });
    } finally {
      channel.setMethodCallHandler(null);
      danmaku?.dispose();
      active = false;
    }
  }
}
