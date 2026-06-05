import 'package:flutter/material.dart';
import 'package:pulsebrief/shared/theme/app_colors.dart';
import 'package:pulsebrief/shared/theme/app_radius.dart';
import 'package:pulsebrief/shared/theme/app_text_styles.dart';

class SourceBadge extends StatelessWidget {
  const SourceBadge({
    super.key,
    required this.label,
    this.hot = false,
    this.breaking = false,
  });

  final String label;
  final bool hot;
  final bool breaking;

  @override
  Widget build(BuildContext context) {
    final color = hot || breaking ? AppColors.danger : AppColors.primary;
    final text = hot
        ? '热点'
        : breaking
        ? '突发'
        : label;

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 5),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.06),
        borderRadius: BorderRadius.circular(AppRadius.sm),
        border: Border.all(color: color.withValues(alpha: 0.45)),
      ),
      child: Text(text, style: AppTextStyles.label.copyWith(color: color)),
    );
  }
}
