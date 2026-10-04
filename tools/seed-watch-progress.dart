import 'dart:io';

import 'package:hive_ce/hive.dart';

/// Local instrumentation input: a real legacy untyped Hive file, not JSON mocks.
Future<void> main(List<String> args) async {
  final directory = Directory(args.single);
  await directory.create(recursive: true);
  Hive.init(directory.path);
  final box = await Hive.openBox<dynamic>('watchProgress');
  await box.putAll({'9000001': 'legacy-wrong-type', '9000002': -9000});
  await box.close();
  print('${directory.path}/watchprogress.hive');
}
