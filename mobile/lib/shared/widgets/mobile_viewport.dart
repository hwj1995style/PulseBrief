import 'package:flutter/material.dart';
import 'package:pulsebrief/shared/theme/app_colors.dart';

class MobileViewport extends StatelessWidget {
  const MobileViewport({super.key, required this.child});

  final Widget child;

  static const double maxWidth = 430;

  @override
  Widget build(BuildContext context) {
    return ColoredBox(
      color: AppColors.background,
      child: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: maxWidth),
          child: child,
        ),
      ),
    );
  }
}
