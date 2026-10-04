import 'package:material_ui/material_ui.dart';

/// Shared by catalogue, search, account, channel and playback routes on Quest.
abstract final class QuestTheme {
  static const background = Color(0xFF101114);
  static const surface = Color(0xFF1C1E23);
  static const accent = Color(0xFFFB7299);

  static ThemeData apply(ThemeData base) {
    final scheme = ColorScheme.fromSeed(seedColor: accent, brightness: Brightness.dark).copyWith(
      primary: accent, surface: background, surfaceContainer: surface,
      surfaceContainerLow: const Color(0xFF16181C),
      surfaceContainerHigh: const Color(0xFF272A30),
      surfaceContainerHighest: const Color(0xFF30343C),
      onSurface: const Color(0xFFF1F2F5), onSurfaceVariant: const Color(0xFFB2B7C2),
    );
    return base.copyWith(
      brightness: Brightness.dark, colorScheme: scheme,
      textTheme: base.textTheme.apply(bodyColor: scheme.onSurface, displayColor: scheme.onSurface),
      iconTheme: base.iconTheme.copyWith(color: scheme.onSurfaceVariant),
      scaffoldBackgroundColor: background, canvasColor: background,
      hoverColor: Colors.white.withValues(alpha: .10),
      focusColor: Colors.white.withValues(alpha: .16),
      appBarTheme: base.appBarTheme.copyWith(backgroundColor: background,
        toolbarHeight: 64, titleTextStyle: const TextStyle(fontSize: 20, fontWeight: FontWeight.w600, color: Colors.white)),
      iconButtonTheme: IconButtonThemeData(style: IconButton.styleFrom(minimumSize: const Size(52, 52))),
      listTileTheme: base.listTileTheme.copyWith(minTileHeight: 60, iconColor: scheme.onSurfaceVariant),
      cardTheme: base.cardTheme.copyWith(color: surface, elevation: 0, surfaceTintColor: Colors.transparent,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14))),
      dialogTheme: base.dialogTheme.copyWith(backgroundColor: surface,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        titleTextStyle: const TextStyle(fontSize: 22, color: Colors.white, fontWeight: FontWeight.w600)),
      popupMenuTheme: base.popupMenuTheme.copyWith(color: surface,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16))),
      tabBarTheme: base.tabBarTheme.copyWith(labelColor: Colors.white, unselectedLabelColor: scheme.onSurfaceVariant,
        indicatorSize: TabBarIndicatorSize.tab,
        indicator: BoxDecoration(color: const Color(0xFF343740), borderRadius: BorderRadius.circular(24)),
        labelStyle: const TextStyle(fontSize: 16, fontWeight: FontWeight.w600)),
      sliderTheme: base.sliderTheme.copyWith(trackHeight: 3, activeTrackColor: accent, thumbColor: Colors.white),
    );
  }
}
