import 'dart:async';
import 'dart:convert';

import '../../clients/piliplus/lib/quest/cinema_handoff.dart';

void expect(bool result, String message) {
  if (!result) throw StateError(message);
}

Future<Object?> errorFrom(Future<void> operation) async {
  try {
    await operation;
    return null;
  } catch (error) {
    return error;
  }
}

Future<void> main() async {
  final passed = <String>[];
  bool owns(
    int? detailCid, {
    int? playerCid = 7,
    bool querying = false,
    bool closed = false,
  }) => cinemaOwnsSource(
    sessionCid: 7,
    playerCid: playerCid,
    detailCid: detailCid,
    closed: closed,
    querying: querying,
  );
  expect(owns(7), 'Current loaded source must retain ownership');
  passed.add('current_source_has_ownership');
  expect(
    !owns(8) && !owns(7, playerCid: 8) && !owns(7, playerCid: null),
    'Pending page or decoder CID cannot own old progress',
  );
  passed.add('pending_page_cid_and_replaced_decoder_are_rejected');
  expect(
    !owns(7, querying: true) && !owns(7, closed: true),
    'Querying or disposed detail cannot accept a cinema request',
  );
  passed.add('querying_and_closed_detail_are_rejected');
  var pageCid = 7;
  final delayedReply = Completer<void>();
  final oldReply = () async {
    await delayedReply.future;
    return owns(pageCid);
  }();
  pageCid = 8;
  delayedReply.complete();
  expect(
    !await oldReply,
    'Late native reply after page CID switch must be rejected',
  );
  passed.add('late_reply_rechecks_page_cid_after_await');
  final ready = Completer<void>();
  final events = <String>[];
  var plays = 0;
  final success = loadCinemaQuality(
    load: (autoplay) async {
      events.add('load');
      if (autoplay) plays++;
      await ready.future;
      events.add('loaded');
    },
    pause: () async => events.add('pause'),
  );
  await Future<void>.delayed(Duration.zero);
  expect(
    events.join(',') == 'load',
    'Must await media load before final pause',
  );
  ready.complete();
  await success;
  expect(
    events.join(',') == 'load,loaded,pause' && plays == 0,
    'Reload must stay non-autoplay throughout',
  );
  passed.add('quality_success_waits_and_never_autoplays');

  for (final failingPause in [false, true]) {
    final loadError = StateError('injected load failure');
    var paused = false;
    final error = await errorFrom(
      loadCinemaQuality(
        load: (autoplay) async {
          expect(!autoplay, 'Failed reload cannot autoplay');
          throw loadError;
        },
        pause: () async {
          paused = true;
          if (failingPause) throw StateError('injected pause failure');
        },
      ),
    );
    expect(
      paused && identical(error, loadError),
      'Pause must run after failed load and preserve load error',
    );
    passed.add(
      'quality_failure_pause_${failingPause ? 'also_fails' : 'succeeds'}',
    );
  }
  for (final failedLoad in [false, true]) {
    final lease = CinemaHandoffLease();
    final pending = Completer<void>();
    var active = true;
    var newPlayPaused = false;
    var callsAfterClose = 0;
    var newPlaybackStarted = false;
    lease.close();
    Future<void> staleRequest() async {
      if (!lease.open) return;
      callsAfterClose++;
      if (newPlaybackStarted) newPlayPaused = true;
    }

    final completion = errorFrom(() async {
      CinemaFailure? original;
      try {
        await loadCinemaQuality(
          load: (autoplay) async {
            expect(!autoplay, 'Return synchronization cannot autoplay');
            await pending.future;
            if (failedLoad) throw StateError('return load failed');
          },
          pause: () async {
            if (newPlaybackStarted) newPlayPaused = true;
          },
        );
      } catch (error, stackTrace) {
        original = (error: error, stackTrace: stackTrace);
      } finally {
        await finishCinemaHandoff(
          pause: () async {},
          clearHandler: () {},
          disposeDanmaku: () {},
          resetActive: () {
            active = false;
          },
          primaryFailure: original,
        );
      }
    }());
    await Future<void>.delayed(Duration.zero);
    expect(active, 'Keep ownership while the real load is pending');
    await staleRequest();
    pending.complete();
    final returnError = await completion;
    expect(
      !active && (failedLoad ? returnError is StateError : returnError == null),
      'Drain both successful and failed load before releasing ownership',
    );
    newPlaybackStarted = true;
    await staleRequest();
    expect(
      callsAfterClose == 0 && !newPlayPaused,
      'A closed handler cannot pause or overwrite subsequent playback',
    );
    passed.add(
      'return_reload_drained_and_old_requests_rejected_${failedLoad ? 'failure' : 'success'}',
    );
  }
  final closed = CinemaHandoffLease()
    ..close()
    ..close();
  expect(!closed.open, 'Closing remains idempotent');
  passed.add('closing_lease_is_idempotent');

  final pauseError = StateError('injected final pause failure');
  final error = await errorFrom(
    loadCinemaQuality(load: (_) async {}, pause: () async => throw pauseError),
  );
  expect(
    identical(error, pauseError),
    'Successful loading must report failed final pause',
  );
  passed.add('quality_reports_pause_failure');

  for (final failedStep in [-1, 0, 1, 2, 3]) {
    var active = true;
    final called = <int>[];
    final cleanupError = StateError('injected cleanup failure');
    Future<void> action(int step) async {
      called.add(step);
      if (step == 3) active = false;
      if (step == failedStep) throw cleanupError;
    }

    final cleanupFailure = await errorFrom(
      finishCinemaHandoff(
        pause: () => action(0),
        clearHandler: () => action(1),
        disposeDanmaku: () => action(2),
        resetActive: () => action(3),
      ),
    );
    expect(
      called.join(',') == '0,1,2,3' && !active,
      'Every cleanup action and entry reset must run',
    );
    expect(
      failedStep == -1
          ? cleanupFailure == null
          : identical(cleanupFailure, cleanupError),
      'First cleanup failure must be retained',
    );
    passed.add('cleanup_failure_step_$failedStep');
  }
  final primary = StateError('original handoff failure');
  var reset = false;
  final primaryFailure = await errorFrom(
    finishCinemaHandoff(
      pause: () => throw StateError('secondary pause failure'),
      clearHandler: () => throw StateError('secondary channel failure'),
      disposeDanmaku: () => throw StateError('secondary disposal failure'),
      resetActive: () {
        reset = true;
      },
      primaryFailure: (error: primary, stackTrace: StackTrace.current),
    ),
  );
  expect(
    reset && identical(primaryFailure, primary),
    'Cleanup cannot mask original playback error',
  );
  passed.add('original_handoff_error_survives_all_cleanup_failures');
  print(
    jsonEncode({
      'passed': passed.length,
      'cases': passed,
      'coverage': 'Production pure-Dart handoff helpers; no Flutter/player/device runtime',
    }),
  );
}
