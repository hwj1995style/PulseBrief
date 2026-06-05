import 'package:flutter/material.dart';
import 'package:pulsebrief/core/constants/app_assets.dart';
import 'package:pulsebrief/shared/theme/app_colors.dart';
import 'package:pulsebrief/shared/theme/app_radius.dart';
import 'package:pulsebrief/shared/theme/app_spacing.dart';
import 'package:pulsebrief/shared/theme/app_text_styles.dart';

class BriefHeroCard extends StatelessWidget {
  const BriefHeroCard({
    super.key,
    required this.title,
    required this.subtitle,
    required this.description,
    this.imageAsset = AppAssets.artGlobalGlobe,
    this.primaryAction,
    this.secondaryAction,
    this.onPrimary,
    this.onSecondary,
    this.compact = false,
  });

  final String title;
  final String subtitle;
  final String description;
  final String imageAsset;
  final String? primaryAction;
  final String? secondaryAction;
  final VoidCallback? onPrimary;
  final VoidCallback? onSecondary;
  final bool compact;

  @override
  Widget build(BuildContext context) {
    return Container(
      constraints: BoxConstraints(minHeight: compact ? 160 : 220),
      padding: const EdgeInsets.all(AppSpacing.xl),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(AppRadius.xl),
        border: Border.all(color: AppColors.borderBlue),
        gradient: const LinearGradient(
          colors: [Color(0xFFF8FBFF), Color(0xFFEAF3FF)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
      ),
      child: Stack(
        children: [
          Positioned.fill(
            left: 130,
            child: Opacity(
              opacity: 0.86,
              child: Image.asset(imageAsset, fit: BoxFit.cover),
            ),
          ),
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                title,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: AppTextStyles.hero.copyWith(fontSize: compact ? 30 : 36),
              ),
              const SizedBox(height: 10),
              Text(
                subtitle,
                style: AppTextStyles.sectionTitle.copyWith(
                  fontWeight: FontWeight.w500,
                ),
              ),
              const SizedBox(height: 10),
              SizedBox(
                width: 260,
                child: Text(
                  description,
                  maxLines: compact ? 2 : 3,
                  overflow: TextOverflow.ellipsis,
                  style: AppTextStyles.body,
                ),
              ),
              if (primaryAction != null) ...[
                const SizedBox(height: 20),
                Wrap(
                  spacing: 10,
                  runSpacing: 10,
                  children: [
                    FilledButton.icon(
                      onPressed: onPrimary,
                      icon: const Icon(Icons.play_circle_fill_rounded),
                      label: Text(primaryAction!),
                      style: FilledButton.styleFrom(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 20,
                          vertical: 14,
                        ),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(AppRadius.pill),
                        ),
                      ),
                    ),
                    if (secondaryAction != null)
                      OutlinedButton(
                        onPressed: onSecondary,
                        style: OutlinedButton.styleFrom(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 20,
                            vertical: 14,
                          ),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(AppRadius.pill),
                          ),
                        ),
                        child: Text(secondaryAction!),
                      ),
                  ],
                ),
              ],
            ],
          ),
        ],
      ),
    );
  }
}
