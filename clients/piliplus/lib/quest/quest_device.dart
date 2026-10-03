import 'package:flutter/services.dart';

abstract final class QuestDevice {
  static const channel = MethodChannel('vrbilibili/device');
  static bool isQuest = false;
}
