import 'package:flutter_test/flutter_test.dart';
import 'package:material_ui/material_ui.dart';
import 'package:PiliPlus/quest/panel_seek_bar.dart';
import 'package:PiliPlus/quest/panel_video_fit.dart';

void main() {
  testWidgets('drag holds preview across decoder ticks and commits exactly once', (tester) async {
    final seeks = <int>[];
    Widget scene(int position) => MaterialApp(home: Scaffold(body: SizedBox(width: 600,
      child: PanelSeekBar(durationSeconds: 120, positionSeconds: position, onSeek: seeks.add))));
    await tester.pumpWidget(scene(12));
    final rect = tester.getRect(find.byType(Slider));
    final gesture = await tester.startGesture(Offset(rect.left + rect.width * .3, rect.center.dy));
    await gesture.moveTo(Offset(rect.left + rect.width * .7, rect.center.dy));
    await tester.pump();
    final preview = tester.widget<Slider>(find.byType(Slider)).value;
    await tester.pumpWidget(scene(13));
    expect(tester.widget<Slider>(find.byType(Slider)).value, preview);
    expect(seeks, isEmpty);
    await gesture.up(); await tester.pump();
    expect(seeks, hasLength(1)); expect(seeks.single, inInclusiveRange(75, 95));
  });
  testWidgets('unknown duration disables seeking; decoder overshoot stays in range', (tester) async {
    final seeks = <int>[];
    Future<void> pump(int duration,int position) => tester.pumpWidget(MaterialApp(home: Scaffold(
      body: PanelSeekBar(durationSeconds: duration,positionSeconds: position,onSeek: seeks.add))));
    await pump(0,10); expect(tester.widget<Slider>(find.byType(Slider)).onChanged,isNull);
    await pump(60,90); expect(tester.widget<Slider>(find.byType(Slider)).value,60);
    await pump(60,-5); expect(tester.widget<Slider>(find.byType(Slider)).value,0);
    expect(seeks,isEmpty);
  });
  testWidgets('switching source does not apply an unfinished drag to another CID', (tester) async {
    final seeks = <int>[];
    Widget scene(int cid) => MaterialApp(home: Scaffold(body: PanelSeekBar(
      key: ValueKey(cid),durationSeconds: 60,positionSeconds: 5,onSeek: seeks.add)));
    await tester.pumpWidget(scene(1));
    final gesture=await tester.startGesture(tester.getCenter(find.byType(Slider)));
    await tester.pumpWidget(scene(2)); await gesture.up(); await tester.pump();
    expect(seeks,isEmpty);expect(tester.widget<Slider>(find.byType(Slider)).value,5);
  });
  for(final source in [const Size(1920,1080),const Size(1440,1080),const Size(1080,1920),const Size(2560,1080)]) {
    for(final area in [const Size(640,360),const Size(500,500)]) {
      testWidgets('cover preserves ${source.width}:${source.height} in $area', (tester) async {
        const imageKey=ValueKey('source');
        await tester.pumpWidget(Directionality(textDirection: TextDirection.ltr,child: Center(
          child: SizedBox.fromSize(size: area,child: PanelVideoFit(
            child: SizedBox(key: imageKey,width: source.width,height: source.height))))));
        final box=tester.renderObject<RenderBox>(find.byKey(imageKey));
        final a=box.localToGlobal(Offset.zero);
        final b=box.localToGlobal(Offset(source.width,source.height));
        final width=b.dx-a.dx,height=b.dy-a.dy;
        expect(width/height,closeTo(source.width/source.height,0.00001));
        expect(width+0.01,greaterThanOrEqualTo(area.width));
        expect(height+0.01,greaterThanOrEqualTo(area.height));
      });
    }
  }
}
