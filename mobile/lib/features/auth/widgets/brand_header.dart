import 'package:flutter/material.dart';
import 'package:pulsebrief/core/constants/app_assets.dart';
import 'package:pulsebrief/shared/theme/app_colors.dart';
import 'package:pulsebrief/shared/theme/app_spacing.dart';
import 'package:pulsebrief/shared/theme/app_text_styles.dart';

class BrandHeader extends StatelessWidget {
  const BrandHeader({super.key});

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        final width = constraints.maxWidth;
        final height = (width * 0.78).clamp(282.0, 340.0).toDouble();

        return SizedBox(
          height: height,
          child: Stack(
            alignment: Alignment.center,
            children: [
              Positioned(
                right: -width * 0.16,
                top: 14,
                child: Opacity(
                  opacity: 0.36,
                  child: Image.asset(
                    AppAssets.artGlobalGlobe,
                    width: width * 1.04,
                    fit: BoxFit.contain,
                  ),
                ),
              ),
              Positioned(
                left: width * 0.12,
                top: height * 0.42,
                child: _FloatingSignal(
                  icon: Icons.query_stats_rounded,
                  color: AppColors.primary,
                ),
              ),
              Positioned(
                right: width * 0.1,
                top: height * 0.58,
                child: const _FloatingSignal(
                  label: 'AI',
                  color: AppColors.cyan,
                ),
              ),
              Padding(
                padding: const EdgeInsets.only(top: AppSpacing.xxl),
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    const _PulseLogoMark(),
                    const SizedBox(height: 18),
                    Text(
                      '脉 闻',
                      style: AppTextStyles.pageTitle.copyWith(
                        fontSize: 44,
                        height: 1,
                      ),
                    ),
                    const SizedBox(height: 10),
                    Text(
                      'PulseBrief',
                      style: AppTextStyles.title.copyWith(
                        fontSize: 29,
                        color: AppColors.primaryDark,
                      ),
                    ),
                    const SizedBox(height: 14),
                    Text(
                      '听见全球热点，掌握市场脉搏',
                      style: AppTextStyles.body.copyWith(
                        fontSize: 18,
                        color: AppColors.textSecondary,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        );
      },
    );
  }
}

class _PulseLogoMark extends StatelessWidget {
  const _PulseLogoMark();

  @override
  Widget build(BuildContext context) {
    final heights = [34.0, 66.0, 90.0, 66.0, 34.0];

    return SizedBox(
      width: 120,
      height: 104,
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        crossAxisAlignment: CrossAxisAlignment.center,
        children: List.generate(heights.length, (index) {
          final isAccent = index == 0 || index == heights.length - 1;
          return Padding(
            padding: const EdgeInsets.symmetric(horizontal: 5),
            child: Stack(
              alignment: Alignment.center,
              children: [
                Container(
                  width: 14,
                  height: heights[index],
                  decoration: BoxDecoration(
                    color: AppColors.primary,
                    borderRadius: BorderRadius.circular(999),
                  ),
                ),
                if (isAccent)
                  Positioned(
                    top: 6,
                    child: Container(
                      width: 14,
                      height: 14,
                      decoration: const BoxDecoration(
                        color: AppColors.cyan,
                        shape: BoxShape.circle,
                      ),
                    ),
                  ),
                if (index == 2)
                  Positioned(
                    bottom: 8,
                    child: Container(
                      width: 14,
                      height: 14,
                      decoration: const BoxDecoration(
                        color: AppColors.cyan,
                        shape: BoxShape.circle,
                      ),
                    ),
                  ),
              ],
            ),
          );
        }),
      ),
    );
  }
}

class _FloatingSignal extends StatelessWidget {
  const _FloatingSignal({this.icon, this.label, required this.color});

  final IconData? icon;
  final String? label;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 54,
      height: 54,
      decoration: BoxDecoration(
        color: Colors.white.withValues(alpha: 0.58),
        shape: BoxShape.circle,
        border: Border.all(color: Colors.white),
      ),
      alignment: Alignment.center,
      child: label != null
          ? Text(label!, style: AppTextStyles.title.copyWith(color: color))
          : Icon(icon, color: color, size: 30),
    );
  }
}
