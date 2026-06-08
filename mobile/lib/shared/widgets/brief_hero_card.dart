import 'package:flutter/material.dart';
import 'package:pulsebrief/core/constants/app_assets.dart';
import 'package:pulsebrief/shared/theme/app_colors.dart';
import 'package:pulsebrief/shared/theme/app_radius.dart';
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
    this.imageOpacity = 0.9,
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
  final double imageOpacity;

  @override
  Widget build(BuildContext context) {
    final minHeight = compact ? 166.0 : 236.0;

    return Container(
      constraints: BoxConstraints(minHeight: minHeight),
      padding: EdgeInsets.fromLTRB(
        compact ? 20 : 22,
        compact ? 20 : 24,
        compact ? 16 : 18,
        compact ? 18 : 22,
      ),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(AppRadius.xl),
        border: Border.all(color: AppColors.borderBlue),
        gradient: const LinearGradient(
          colors: [Color(0xFFFAFDFF), Color(0xFFEAF4FF)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
      ),
      clipBehavior: Clip.antiAlias,
      child: Stack(
        clipBehavior: Clip.none,
        children: [
          Positioned.fill(
            left: compact ? 144 : 218,
            right: compact ? -26 : -38,
            top: compact ? -10 : -16,
            bottom: compact ? -12 : -18,
            child: Opacity(
              opacity: imageOpacity.clamp(0.0, 0.72),
              child: Image.asset(imageAsset, fit: BoxFit.contain),
            ),
          ),
          Positioned.fill(
            child: DecoratedBox(
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(AppRadius.xl),
                gradient: LinearGradient(
                  colors: [
                    Colors.white.withValues(alpha: 0),
                    Colors.white.withValues(alpha: 0.64),
                  ],
                  begin: Alignment.centerRight,
                  end: Alignment.centerLeft,
                ),
              ),
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
                style: AppTextStyles.hero.copyWith(fontSize: compact ? 28 : 34),
              ),
              const SizedBox(height: 10),
              Text(
                subtitle,
                style: AppTextStyles.sectionTitle.copyWith(
                  fontWeight: FontWeight.w500,
                  color: AppColors.textPrimary,
                ),
              ),
              const SizedBox(height: 10),
              SizedBox(
                width: compact ? 235 : 272,
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
