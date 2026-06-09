import 'package:flutter/material.dart';
import 'package:pulsebrief/shared/models/digest.dart';
import 'package:pulsebrief/shared/theme/app_colors.dart';
import 'package:pulsebrief/shared/theme/app_radius.dart';
import 'package:pulsebrief/shared/theme/app_spacing.dart';
import 'package:pulsebrief/shared/theme/app_text_styles.dart';
import 'package:pulsebrief/shared/widgets/pulse_card.dart';

class DigestCard extends StatelessWidget {
  const DigestCard({
    super.key,
    required this.digest,
    this.onPlay,
    this.onViewDetail,
  });

  final Digest digest;
  final VoidCallback? onPlay;
  final VoidCallback? onViewDetail;

  @override
  Widget build(BuildContext context) {
    return PulseCard(
      padding: const EdgeInsets.all(AppSpacing.lg),
      child: LayoutBuilder(
        builder: (context, constraints) {
          final compact = constraints.maxWidth < 390;

          return Row(
            children: [
              Container(
                width: compact ? 56 : 64,
                height: compact ? 56 : 64,
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    colors: _colorsForDigest(digest),
                    begin: Alignment.topLeft,
                    end: Alignment.bottomRight,
                  ),
                  borderRadius: BorderRadius.circular(AppRadius.lg),
                ),
                alignment: Alignment.center,
                child: Icon(_iconForDigest(digest), color: Colors.white),
              ),
              SizedBox(width: compact ? 10 : 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Wrap(
                      crossAxisAlignment: WrapCrossAlignment.center,
                      spacing: 8,
                      runSpacing: 4,
                      children: [
                        Text(
                          digest.title,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: AppTextStyles.sectionTitle.copyWith(
                            fontSize: compact ? 17 : 20,
                          ),
                        ),
                        _UpdateBadge(updateTime: digest.updateTime),
                      ],
                    ),
                    const SizedBox(height: 7),
                    Text(
                      digest.subtitle,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: AppTextStyles.body,
                    ),
                  ],
                ),
              ),
              SizedBox(width: compact ? 6 : 8),
              IconButton.outlined(
                onPressed: onPlay,
                style: IconButton.styleFrom(
                  foregroundColor: AppColors.primary,
                  side: const BorderSide(color: AppColors.borderBlue),
                  fixedSize: Size(compact ? 42 : 46, compact ? 42 : 46),
                ),
                icon: const Icon(Icons.play_arrow_rounded),
              ),
              SizedBox(width: compact ? 6 : 8),
              OutlinedButton(
                onPressed: onViewDetail,
                style: OutlinedButton.styleFrom(
                  padding: EdgeInsets.symmetric(horizontal: compact ? 8 : 12),
                  minimumSize: Size(compact ? 54 : 78, compact ? 40 : 44),
                ),
                child: Text(compact ? '详情' : '查看详情'),
              ),
            ],
          );
        },
      ),
    );
  }

  List<Color> _colorsForDigest(Digest digest) {
    if (_isNoon(digest)) {
      return const [Color(0xFF20B7D7), Color(0xFF54D0E5)];
    }
    if (_isEvening(digest)) {
      return const [Color(0xFF4F46E5), Color(0xFF7C6CF2)];
    }
    if (_isAi(digest)) {
      return const [Color(0xFF0B55D9), Color(0xFF2B77F0)];
    }
    if (_isInvestment(digest)) {
      return const [Color(0xFF2F76D2), Color(0xFF67A3F4)];
    }
    return const [AppColors.primary, Color(0xFF6EA8FF)];
  }

  IconData _iconForDigest(Digest digest) {
    if (_isNoon(digest)) {
      return Icons.access_time_rounded;
    }
    if (_isEvening(digest)) {
      return Icons.dark_mode_rounded;
    }
    if (_isAi(digest)) {
      return Icons.smart_toy_rounded;
    }
    if (_isInvestment(digest)) {
      return Icons.account_balance_rounded;
    }
    return Icons.wb_twilight_rounded;
  }

  bool _isNoon(Digest digest) {
    return digest.id == 'noon' || digest.title.contains('午间');
  }

  bool _isEvening(Digest digest) {
    return digest.id == 'evening' || digest.title.contains('晚间');
  }

  bool _isAi(Digest digest) {
    return digest.id == 'ai' || digest.title.contains('AI');
  }

  bool _isInvestment(Digest digest) {
    return digest.id == 'ib' || digest.title.contains('投行');
  }
}

class _UpdateBadge extends StatelessWidget {
  const _UpdateBadge({required this.updateTime});

  final String updateTime;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: AppColors.primarySoft,
        borderRadius: BorderRadius.circular(AppRadius.sm),
      ),
      child: Text(updateTime, style: AppTextStyles.label),
    );
  }
}
