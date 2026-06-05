import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:pulsebrief/app/routes.dart';
import 'package:pulsebrief/mock/mock_articles.dart';
import 'package:pulsebrief/shared/models/article.dart';
import 'package:pulsebrief/shared/theme/app_colors.dart';
import 'package:pulsebrief/shared/theme/app_radius.dart';
import 'package:pulsebrief/shared/theme/app_spacing.dart';
import 'package:pulsebrief/shared/theme/app_text_styles.dart';
import 'package:pulsebrief/shared/widgets/app_header.dart';
import 'package:pulsebrief/shared/widgets/pulse_card.dart';
import 'package:pulsebrief/shared/widgets/section_header.dart';
import 'package:pulsebrief/shared/widgets/source_badge.dart';

class ArticleDetailPage extends StatefulWidget {
  const ArticleDetailPage({super.key, required this.article});

  final Article article;

  @override
  State<ArticleDetailPage> createState() => _ArticleDetailPageState();
}

class _ArticleDetailPageState extends State<ArticleDetailPage> {
  late bool _favorited;

  @override
  void initState() {
    super.initState();
    _favorited = widget.article.isFavorited;
  }

  void _openPlayer() {
    Navigator.pushNamed(context, PulseRoutes.player, arguments: widget.article);
  }

  @override
  Widget build(BuildContext context) {
    final article = widget.article;
    final keyPoints = article.keyPoints.isEmpty
        ? const [
            '市场对 AI 基础设施投资的关注度继续提升。',
            '芯片、云计算与数据中心产业链可能持续受益。',
            '相关板块短期或受情绪和资金流影响。',
          ]
        : article.keyPoints;
    final bottom = MediaQuery.paddingOf(context).bottom;

    return Scaffold(
      bottomNavigationBar: _DetailActionBar(
        favorited: _favorited,
        onPlay: _openPlayer,
        onFavorite: () => setState(() => _favorited = !_favorited),
        bottom: bottom,
      ),
      body: SafeArea(
        bottom: false,
        child: CustomScrollView(
          slivers: [
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.fromLTRB(20, 8, 20, 12),
                child: Row(
                  children: [
                    const BackSquareButton(),
                    const Spacer(),
                    PulseIconButton(
                      icon: CupertinoIcons.square_arrow_up,
                      onPressed: () {},
                    ),
                  ],
                ),
              ),
            ),
            SliverPadding(
              padding: EdgeInsets.fromLTRB(
                AppSpacing.pagePadding,
                AppSpacing.md,
                AppSpacing.pagePadding,
                120 + bottom,
              ),
              sliver: SliverList.list(
                children: [
                  Text(article.title, style: AppTextStyles.hero),
                  const SizedBox(height: 20),
                  Row(
                    children: [
                      Expanded(
                        child: Text(
                          '${article.sourceName} · ${article.publishTime} · ${article.categoryName}',
                          style: AppTextStyles.body.copyWith(
                            color: AppColors.textPrimary,
                          ),
                        ),
                      ),
                      SourceBadge(
                        label: article.categoryName,
                        hot: article.isHot,
                        breaking: article.isBreaking,
                      ),
                    ],
                  ),
                  const SizedBox(height: 24),
                  _AiSummaryCard(article: article),
                  const SizedBox(height: 28),
                  const SectionHeader(title: '核心要点'),
                  const SizedBox(height: 12),
                  PulseCard(
                    child: Column(
                      children: keyPoints
                          .map(
                            (point) => Padding(
                              padding: const EdgeInsets.symmetric(vertical: 7),
                              child: Row(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Container(
                                    width: 7,
                                    height: 7,
                                    margin: const EdgeInsets.only(top: 8),
                                    decoration: const BoxDecoration(
                                      color: AppColors.primary,
                                      shape: BoxShape.circle,
                                    ),
                                  ),
                                  const SizedBox(width: 12),
                                  Expanded(
                                    child: Text(
                                      point,
                                      style: AppTextStyles.body.copyWith(
                                        color: AppColors.textPrimary,
                                      ),
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          )
                          .toList(),
                    ),
                  ),
                  const SizedBox(height: 28),
                  const SectionHeader(title: '可能影响'),
                  const SizedBox(height: 12),
                  PulseCard(
                    borderColor: AppColors.borderBlue,
                    backgroundColor: AppColors.surfaceTint,
                    child: Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Container(
                          width: 28,
                          height: 28,
                          decoration: const BoxDecoration(
                            color: AppColors.primary,
                            shape: BoxShape.circle,
                          ),
                          child: const Icon(
                            Icons.verified_rounded,
                            color: Colors.white,
                            size: 18,
                          ),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Text(
                            article.impact.isEmpty
                                ? '相关事件可能影响市场风险偏好和产业链估值，建议关注后续公开信息和多来源验证。'
                                : article.impact,
                            style: AppTextStyles.body.copyWith(
                              color: AppColors.textPrimary,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 28),
                  const SectionHeader(title: '相关资讯'),
                  const SizedBox(height: 12),
                  PulseCard(
                    padding: EdgeInsets.zero,
                    child: Column(
                      children: relatedArticles.map((item) {
                        return ListTile(
                          contentPadding: const EdgeInsets.symmetric(
                            horizontal: 14,
                          ),
                          leading: ClipRRect(
                            borderRadius: BorderRadius.circular(AppRadius.sm),
                            child: Image.asset(
                              item.imageAsset,
                              width: 76,
                              height: 52,
                              fit: BoxFit.cover,
                            ),
                          ),
                          title: Text(
                            item.title,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: AppTextStyles.cardTitle.copyWith(
                              fontSize: 15,
                            ),
                          ),
                          subtitle: Text(
                            '${item.sourceName} · ${item.publishTime}',
                            style: AppTextStyles.meta,
                          ),
                          trailing: const Icon(
                            CupertinoIcons.chevron_right,
                            size: 18,
                          ),
                          onTap: () {
                            Navigator.pushReplacementNamed(
                              context,
                              PulseRoutes.articleDetail,
                              arguments: item,
                            );
                          },
                        );
                      }).toList(),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _AiSummaryCard extends StatelessWidget {
  const _AiSummaryCard({required this.article});

  final Article article;

  @override
  Widget build(BuildContext context) {
    return PulseCard(
      borderColor: AppColors.borderBlue,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Icon(Icons.auto_awesome_rounded, color: AppColors.primary),
              const SizedBox(width: 10),
              Text('AI 摘要', style: AppTextStyles.sectionTitle),
            ],
          ),
          const SizedBox(height: 14),
          Text(
            article.summary,
            style: AppTextStyles.body.copyWith(
              color: AppColors.textPrimary,
              fontSize: 16,
              height: 1.7,
            ),
          ),
        ],
      ),
    );
  }
}

class _DetailActionBar extends StatelessWidget {
  const _DetailActionBar({
    required this.favorited,
    required this.onPlay,
    required this.onFavorite,
    required this.bottom,
  });

  final bool favorited;
  final VoidCallback onPlay;
  final VoidCallback onFavorite;
  final double bottom;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: EdgeInsets.fromLTRB(16, 12, 16, bottom + 10),
      decoration: const BoxDecoration(
        color: AppColors.surface,
        boxShadow: [
          BoxShadow(
            color: Color(0x1A061331),
            blurRadius: 24,
            offset: Offset(0, -8),
          ),
        ],
      ),
      child: Row(
        children: [
          _ActionItem(
            icon: CupertinoIcons.headphones,
            label: '语音播报',
            onTap: onPlay,
          ),
          _ActionItem(
            icon: favorited ? CupertinoIcons.star_fill : CupertinoIcons.star,
            label: '收藏',
            onTap: onFavorite,
            active: favorited,
          ),
          _ActionItem(
            icon: CupertinoIcons.square_arrow_up,
            label: '分享',
            onTap: () {},
          ),
          const SizedBox(width: 8),
          Expanded(
            child: FilledButton.icon(
              onPressed: () {},
              icon: const Icon(CupertinoIcons.arrow_up_right_square),
              label: const Text('阅读原文'),
              style: FilledButton.styleFrom(
                padding: const EdgeInsets.symmetric(vertical: 14),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(AppRadius.md),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _ActionItem extends StatelessWidget {
  const _ActionItem({
    required this.icon,
    required this.label,
    required this.onTap,
    this.active = false,
  });

  final IconData icon;
  final String label;
  final VoidCallback onTap;
  final bool active;

  @override
  Widget build(BuildContext context) {
    final color = active ? AppColors.primary : AppColors.textPrimary;

    return SizedBox(
      width: 68,
      child: InkWell(
        borderRadius: BorderRadius.circular(AppRadius.md),
        onTap: onTap,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, color: color),
            const SizedBox(height: 6),
            Text(label, style: AppTextStyles.meta.copyWith(color: color)),
          ],
        ),
      ),
    );
  }
}
