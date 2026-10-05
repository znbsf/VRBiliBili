import 'dart:async';

typedef CinemaFailure = ({Object error, StackTrace stackTrace});

/// A page can select its next CID before the old decoder has been replaced.
bool cinemaOwnsSource({
  required int? sessionCid,
  required int? playerCid,
  required int? detailCid,
  required bool closed,
  required bool querying,
}) =>
    sessionCid != null &&
    playerCid == sessionCid &&
    detailCid == sessionCid &&
    !closed &&
    !querying;

/// The MethodChannel handler stops accepting work before return synchronization.
final class CinemaHandoffLease {
  bool _open = true;
  bool get open => _open;
  void close() => _open = false;
}

/// Reload after native playback ends; drain loading before releasing ownership.
/// Future.timeout cannot cancel a decoder load, so this deliberately awaits it.
Future<void> loadCinemaQuality({
  required Future<void> Function(bool autoplay) load,
  required Future<void> Function() pause,
}) async {
  CinemaFailure? failure;
  try {
    await load(false);
  } catch (error, stackTrace) {
    failure = (error: error, stackTrace: stackTrace);
  }
  try {
    await pause();
  } catch (error, stackTrace) {
    failure ??= (error: error, stackTrace: stackTrace);
  }
  if (failure != null) {
    Error.throwWithStackTrace(failure.error, failure.stackTrace);
  }
}

/// Every cleanup step runs; the original handoff error takes precedence.
Future<void> finishCinemaHandoff({
  required FutureOr<void> Function() pause,
  required FutureOr<void> Function() clearHandler,
  required FutureOr<void> Function() disposeDanmaku,
  required FutureOr<void> Function() resetActive,
  CinemaFailure? primaryFailure,
}) async {
  var failure = primaryFailure;
  for (final action in [pause, clearHandler, disposeDanmaku, resetActive]) {
    try {
      await action();
    } catch (error, stackTrace) {
      failure ??= (error: error, stackTrace: stackTrace);
    }
  }
  if (failure != null) {
    Error.throwWithStackTrace(failure.error, failure.stackTrace);
  }
}
