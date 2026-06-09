import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:pulsebrief/app/routes.dart';
import 'package:pulsebrief/core/constants/app_assets.dart';
import 'package:pulsebrief/core/constants/app_strings.dart';
import 'package:pulsebrief/shared/repositories/pulse_repository.dart';
import 'package:pulsebrief/shared/repositories/repository_scope.dart';
import 'package:pulsebrief/shared/theme/app_colors.dart';
import 'package:pulsebrief/shared/theme/app_radius.dart';
import 'package:pulsebrief/shared/theme/app_spacing.dart';
import 'package:pulsebrief/shared/theme/app_text_styles.dart';
import 'package:pulsebrief/shared/widgets/app_header.dart';
import 'package:pulsebrief/shared/widgets/brief_hero_card.dart';
import 'package:pulsebrief/shared/widgets/digest_card.dart';
import 'package:pulsebrief/shared/widgets/empty_state.dart';
import 'package:pulsebrief/shared/widgets/loading_state.dart';
import 'package:pulsebrief/shared/widgets/pulse_card.dart';

class DigestPage extends StatefulWidget {
  const DigestPage({super.key});

  @override
  State<DigestPage> createState() => _DigestPageState();
}

class _DigestPageState extends State<DigestPage> {
  bool _loaded = false;
  bool _isLoading = true;
  String? _errorMessage;
  TodayDigestFeed? _feed;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (!_loaded) {
      _loaded = true;
      _loadDigest();
    }
  }

  Future<void> _loadDigest() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });
    try {
      final feed = await RepositoryScope.of(context).getTodayDigest();
      if (!mounted) return;
      setState(() {
        _feed = feed;
        _isLoading = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _errorMessage = '简报加载失败，请稍后重试';
        _isLoading = false;
      });
    }
  }

  void _openPlayer(BuildContext context) {
    Navigator.pushNamed(context, PulseRoutes.player);
  }

  @override
  Widget build(BuildContext context) {
    if (_isLoading) {
      return const SafeArea(bottom: false, child: LoadingState());
    }
    if (_errorMessage != null) {
      return SafeArea(
        bottom: false,
        child: EmptyState(title: '加载失败', message: _errorMessage!),
      );
    }
    final feed = _feed;
    if (feed == null) {
      return const SafeArea(bottom: false, child: EmptyState());
    }

    return SafeArea(
      bottom: false,
      child: CustomScrollView(
        slivers: [
          SliverToBoxAdapter(
            child: AppHeader(
              title: '简报',
              subtitle: '${AppStrings.today}\n每天几分钟，听懂全球重点',
              actions: [
                PulseIconButton(
                  icon: CupertinoIcons.calendar,
                  onPressed: () {},
                ),
              ],
            ),
          ),
          SliverPadding(
            padding: EdgeInsets.fromLTRB(
              AppSpacing.pagePadding,
              0,
              AppSpacing.pagePadding,
              AppSpacing.bottomNavHeight + MediaQuery.paddingOf(context).bottom,
            ),
            sliver: SliverList.list(
              children: [
                BriefHeroCard(
                  title: feed.headline.title,
                  subtitle: feed.headline.subtitle,
                  description: '覆盖全球热点、市场动态、科技趋势与机构观点，帮助你快速完成每日信息输入。',
                  imageAsset: AppAssets.artCleanDigest,
                  primaryAction: '播放整篇简报',
                  secondaryAction: '查看全文',
                  onPrimary: () => _openPlayer(context),
                  onSecondary: () {},
                ),
                const SizedBox(height: AppSpacing.sectionGap),
                ...feed.digests.map(
                  (digest) => Padding(
                    padding: const EdgeInsets.only(bottom: AppSpacing.md),
                    child: DigestCard(
                      digest: digest,
                      onPlay: () => _openPlayer(context),
                      onViewDetail: () {},
                    ),
                  ),
                ),
                const SizedBox(height: AppSpacing.sectionGap),
                PulseCard(
                  borderColor: AppColors.borderBlue,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          const Icon(
                            Icons.local_fire_department_rounded,
                            color: AppColors.danger,
                          ),
                          const SizedBox(width: 8),
                          Expanded(
                            child: Text(
                              '今日热点清单',
                              style: AppTextStyles.sectionTitle,
                            ),
                          ),
                          Text('查看全部', style: AppTextStyles.label),
                          const Icon(
                            CupertinoIcons.chevron_right,
                            size: 14,
                            color: AppColors.primary,
                          ),
                        ],
                      ),
                      const SizedBox(height: 14),
                      LayoutBuilder(
                        builder: (context, constraints) {
                          final twoColumns = constraints.maxWidth > 340;
                          if (!twoColumns) {
                            return Column(children: _rankRows(feed.highlights));
                          }
                          final left = feed.highlights.take(3).toList();
                          final right = feed.highlights
                              .skip(3)
                              .take(3)
                              .toList();
                          return Row(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Expanded(
                                child: Column(children: _rankRows(left)),
                              ),
                              const SizedBox(width: 12),
                              Expanded(
                                child: Column(
                                  children: _rankRows(right, startIndex: 4),
                                ),
                              ),
                            ],
                          );
                        },
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  List<Widget> _rankRows(List<String> items, {int startIndex = 1}) {
    return List.generate(items.length, (index) {
      final number = startIndex + index;
      return Padding(
        padding: const EdgeInsets.symmetric(vertical: 7),
        child: Row(
          children: [
            Container(
              width: 24,
              height: 24,
              alignment: Alignment.center,
              decoration: BoxDecoration(
                color: AppColors.primary,
                borderRadius: BorderRadius.circular(AppRadius.pill),
              ),
              child: Text(
                '$number',
                style: const TextStyle(
                  color: Colors.white,
                  fontWeight: FontWeight.w800,
                  fontSize: 12,
                ),
              ),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: Text(
                items[index],
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: AppTextStyles.body.copyWith(
                  color: AppColors.textPrimary,
                ),
              ),
            ),
          ],
        ),
      );
    });
  }
}
