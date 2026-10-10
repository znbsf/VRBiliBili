import 'package:material_ui/material_ui.dart';

/// Ordinary panel policy: preserve proportions and crop excess edges.
/// No source-ratio override, stretching, or second player is introduced.
class PanelVideoFit extends StatelessWidget {
  const PanelVideoFit({super.key, required this.child, this.alignment = Alignment.center});
  final Widget child;
  final AlignmentGeometry alignment;
  @override
  Widget build(BuildContext context) => FittedBox(
    fit: BoxFit.cover, alignment: alignment, child: child);
}
