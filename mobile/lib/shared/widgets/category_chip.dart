import 'package:flutter/material.dart';
import 'package:pulsebrief/shared/theme/app_colors.dart';
import 'package:pulsebrief/shared/theme/app_radius.dart';
import 'package:pulsebrief/shared/theme/app_shadows.dart';
import 'package:pulsebrief/shared/theme/app_text_styles.dart';

enum CategoryChipState { normal, selected, disabled }

class CategoryChip extends StatelessWidget {
  const CategoryChip({
    super.key,
    required this.label,
    this.state = CategoryChipState.normal,
    this.onTap,
    this.trailing,
    this.expand = false,
  });

  final String label;
  final CategoryChipState state;
  final VoidCallback? onTap;
  final Widget? trailing;
  final bool expand;

  bool get _selected => state == CategoryChipState.selected;

  @override
  Widget build(BuildContext context) {
    final disabled = state == CategoryChipState.disabled;
    final child = AnimatedContainer(
      duration: const Duration(milliseconds: 180),
      constraints: const BoxConstraints(minHeight: 44),
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
      decoration: BoxDecoration(
        color: _selected ? AppColors.primary : AppColors.surface,
        borderRadius: BorderRadius.circular(AppRadius.lg),
        border: Border.all(
          color: _selected ? AppColors.primary : AppColors.borderSoftBlue,
        ),
        boxShadow: _selected ? AppShadows.softCard : null,
      ),
      child: Row(
        mainAxisSize: expand ? MainAxisSize.max : MainAxisSize.min,
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Flexible(
            child: FittedBox(
              fit: BoxFit.scaleDown,
              child: Text(
                label,
                maxLines: 1,
                style: AppTextStyles.label.copyWith(
                  color: disabled
                      ? AppColors.textTertiary
                      : _selected
                      ? Colors.white
                      : AppColors.textPrimary,
                  fontSize: 14,
                ),
              ),
            ),
          ),
          if (trailing != null) ...[const SizedBox(width: 8), trailing!],
        ],
      ),
    );

    if (expand) {
      return Expanded(
        child: GestureDetector(onTap: disabled ? null : onTap, child: child),
      );
    }

    return GestureDetector(onTap: disabled ? null : onTap, child: child);
  }
}
