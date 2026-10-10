import 'package:material_ui/material_ui.dart';

/// Decoder ticks cannot move the thumb during a drag. Seek once on release,
/// without changing play/pause state or flooding asynchronous decoder seeks.
class PanelSeekBar extends StatefulWidget {
  const PanelSeekBar({super.key, required this.durationSeconds,
    required this.positionSeconds, required this.onSeek});
  final int durationSeconds;
  final int positionSeconds;
  final ValueChanged<int> onSeek;
  @override
  State<PanelSeekBar> createState() => _PanelSeekBarState();
}

class _PanelSeekBarState extends State<PanelSeekBar> {
  double? _drag;
  @override
  void didUpdateWidget(covariant PanelSeekBar oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.durationSeconds != oldWidget.durationSeconds) _drag = null;
  }
  @override
  Widget build(BuildContext context) {
    final limit = widget.durationSeconds > 0 ? widget.durationSeconds.toDouble() : 1.0;
    return Slider(
      value: (_drag ?? widget.positionSeconds.toDouble()).clamp(0, limit),
      max: limit,
      onChanged: widget.durationSeconds > 0 ? (value) => setState(() => _drag = value) : null,
      onChangeEnd: widget.durationSeconds > 0 ? (value) {
        setState(() => _drag = null);
        widget.onSeek(value.round().clamp(0, widget.durationSeconds));
      } : null,
    );
  }
}
