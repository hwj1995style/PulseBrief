import 'package:flutter/material.dart';
import 'package:pulsebrief/shared/theme/app_colors.dart';
import 'package:pulsebrief/shared/theme/app_radius.dart';
import 'package:pulsebrief/shared/theme/app_shadows.dart';
import 'package:pulsebrief/shared/theme/app_spacing.dart';

class PulseCard extends StatelessWidget {
  const PulseCard({
    super.key,
    required this.child,
    this.padding = const EdgeInsets.all(AppSpacing.cardPadding),
    this.margin,
    this.borderColor = AppColors.line,
    this.backgroundColor = AppColors.surface,
    this.radius = AppRadius.lg,
    this.shadow = true,
    this.clip = true,
    this.onTap,
  });

  final Widget child;
  final EdgeInsetsGeometry padding;
  final EdgeInsetsGeometry? margin;
  final Color borderColor;
  final Color backgroundColor;
  final double radius;
  final bool shadow;
  final bool clip;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final card = Container(
      margin: margin,
      padding: padding,
      decoration: BoxDecoration(
        color: backgroundColor,
        borderRadius: BorderRadius.circular(radius),
        border: Border.all(color: borderColor),
        boxShadow: shadow ? AppShadows.card : null,
      ),
      clipBehavior: clip ? Clip.antiAlias : Clip.none,
      child: child,
    );

    if (onTap == null) return card;

    return Material(
      color: Colors.transparent,
      child: InkWell(
        borderRadius: BorderRadius.circular(radius),
        onTap: onTap,
        child: card,
      ),
    );
  }
}
