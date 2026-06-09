import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:pulsebrief/app/routes.dart';
import 'package:pulsebrief/core/constants/app_assets.dart';
import 'package:pulsebrief/core/constants/app_strings.dart';
import 'package:pulsebrief/shared/models/article.dart';
import 'package:pulsebrief/shared/repositories/pulse_repository.dart';
import 'package:pulsebrief/shared/repositories/repository_scope.dart';
import 'package:pulsebrief/shared/theme/app_colors.dart';
import 'package:pulsebrief/shared/theme/app_radius.dart';
import 'package:pulsebrief/shared/theme/app_spacing.dart';
import 'package:pulsebrief/shared/theme/app_text_styles.dart';
import 'package:pulsebrief/shared/widgets/app_header.dart';
import 'package:pulsebrief/shared/widgets/brief_hero_card.dart';
import 'package:pulsebrief/shared/widgets/category_chip.dart';
import 'package:pulsebrief/shared/widgets/empty_state.dart';
import 'package:pulsebrief/shared/widgets/loading_state.dart';
import 'package:pulsebrief/shared/widgets/mini_player.dart';
import 'package:pulsebrief/shared/widgets/news_card.dart';
import 'package:pulsebrief/shared/widgets/pulse_card.dart';
import 'package:pulsebrief/shared/widgets/section_header.dart';
import 'package:pulsebrief/shared/widgets/source_badge.dart';

class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  String _selectedCategory = '全部';
  bool _isPlaying = false;
  bool _loaded = false;
  bool _isLoading = true;
  String? _errorMessage;
  DigestHero? _todayDigest;
  Article? _investmentPick;
  List<Article> _articles = const [];

  final _quickCategories = const ['全部', '财经', '科技', 'AI', '投行观点', '宏观'];

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (!_loaded) {
      _loaded = true;
      _loadHomeFeed();
    }
  }

  Future<void> _loadHomeFeed() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });
    try {
      final feed = await RepositoryScope.of(context).getHomeFeed(pageSize: 20);
      if (!mounted) return;
      setState(() {
        _todayDigest = feed.todayDigest;
        _investmentPick = feed.investmentPick;
        _articles = feed.articles;
        _isLoading = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _errorMessage = '首页内容加载失败，请稍后重试';
        _isLoading = false;
      });
    }
  }

  void _openArticle(Article article) {
    Navigator.pushNamed(context, PulseRoutes.articleDetail, arguments: article);
  }

  void _openPlayer() {
    Navigator.pushNamed(context, PulseRoutes.player);
  }

  void _toggleFavorite(Article article) {
    setState(() {
      _articles = _articles
          .map(
            (item) => item.id == article.id
                ? item.copyWith(isFavorited: !item.isFavorited)
                : item,
          )
          .toList();
    });
    final repository = RepositoryScope.of(context);
    if (article.isFavorited) {
      repository.unfavoriteArticle(article.id);
    } else {
      repository.favoriteArticle(article.id);
    }
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
    if (_articles.isEmpty) {
      return const SafeArea(bottom: false, child: EmptyState());
    }

    final visibleArticles = _selectedCategory == '全部'
        ? _articles
        : _articles
              .where((item) => item.categoryName.contains(_selectedCategory))
              .toList();
    final listedArticles = visibleArticles.isEmpty ? _articles : visibleArticles;
    final investmentPick =
        _investmentPick ??
        _articles.firstWhere(
          (item) => item.categoryName == '投行观点',
          orElse: () => _articles.first,
        );
    final todayDigest = _todayDigest;

    return SafeArea(
      bottom: false,
      child: CustomScrollView(
        slivers: [
          SliverToBoxAdapter(
            child: _HomeHeader(onSearch: () {}, onBell: () {}),
          ),
          SliverPadding(
            padding: const EdgeInsets.symmetric(
              horizontal: AppSpacing.pagePadding,
            ),
            sliver: SliverList.list(
              children: [
                BriefHeroCard(
                  title: todayDigest?.title ?? '今日全球简报',
                  subtitle: todayDigest?.subtitle ?? '精选 10 条全球重点资讯',
                  description: '为你精选全球热点、市场、科技、AI 与投行观点，10 分钟快速掌握世界动态。',
                  imageAsset: AppAssets.artCleanGlobal,
                  primaryAction: '一键播放今日简报',
                  onPrimary: () {
                    setState(() => _isPlaying = true);
                    _openPlayer();
                  },
                ),
                const SizedBox(height: AppSpacing.sectionGap),
                SizedBox(
                  height: 44,
                  child: ListView.separated(
                    scrollDirection: Axis.horizontal,
                    itemCount: _quickCategories.length,
                    separatorBuilder: (_, _) => const SizedBox(width: 10),
                    itemBuilder: (context, index) {
                      final label = _quickCategories[index];
                      return CategoryChip(
                        label: label,
                        state: _selectedCategory == label
                            ? CategoryChipState.selected
                            : CategoryChipState.normal,
                        onTap: () => setState(() => _selectedCategory = label),
                      );
                    },
                  ),
                ),
                const SizedBox(height: AppSpacing.sectionGap),
                NewsCard(
                  article: listedArticles.first,
                  onTap: () => _openArticle(listedArticles.first),
                  onPlay: _openPlayer,
                  onFavorite: () => _toggleFavorite(listedArticles.first),
                ),
                const SizedBox(height: AppSpacing.sectionGap),
                _InvestmentInsightCard(
                  article: investmentPick,
                  onTap: _openArticle,
                ),
                const SizedBox(height: AppSpacing.sectionGap),
                const SectionHeader(title: '热点资讯'),
                const SizedBox(height: AppSpacing.md),
              ],
            ),
          ),
          SliverPadding(
            padding: const EdgeInsets.fromLTRB(
              AppSpacing.pagePadding,
              0,
              AppSpacing.pagePadding,
              20,
            ),
            sliver: SliverList.separated(
              itemBuilder: (context, index) {
                final article = listedArticles[index % listedArticles.length];
                return NewsCard(
                  article: article,
                  onTap: () => _openArticle(article),
                  onPlay: _openPlayer,
                  onFavorite: () => _toggleFavorite(article),
                );
              },
              separatorBuilder: (_, _) => const SizedBox(height: AppSpacing.md),
              itemCount: listedArticles.length,
            ),
          ),
          if (_isPlaying)
            SliverPadding(
              padding: EdgeInsets.fromLTRB(
                AppSpacing.pagePadding,
                0,
                AppSpacing.pagePadding,
                AppSpacing.bottomNavHeight +
                    MediaQuery.paddingOf(context).bottom +
                    AppSpacing.md,
              ),
              sliver: SliverToBoxAdapter(
                child: MiniPlayer(
                  title: '今日全球早报：美股反弹，AI 与市场热点快速梳理',
                  isPlaying: _isPlaying,
                  onPlayPause: () => setState(() => _isPlaying = !_isPlaying),
                  onNext: () {},
                  onOpenPlayer: _openPlayer,
                ),
              ),
            )
          else
            SliverToBoxAdapter(
              child: SizedBox(
                height:
                    AppSpacing.bottomNavHeight +
                    MediaQuery.paddingOf(context).bottom +
                    AppSpacing.md,
              ),
            ),
        ],
      ),
    );
  }
}

class _HomeHeader extends StatelessWidget {
  const _HomeHeader({required this.onSearch, required this.onBell});

  final VoidCallback onSearch;
  final VoidCallback onBell;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(
        AppSpacing.pagePadding,
        14,
        AppSpacing.pagePadding,
        AppSpacing.lg,
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  crossAxisAlignment: CrossAxisAlignment.end,
                  children: [
                    Text('脉闻', style: AppTextStyles.pageTitle),
                    const SizedBox(width: 10),
                    Flexible(
                      child: Padding(
                        padding: const EdgeInsets.only(bottom: 4),
                        child: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Flexible(
                              child: Text(
                                AppStrings.appNameEn,
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                                style: AppTextStyles.title.copyWith(
                                  fontSize: 18,
                                  color: AppColors.primaryDark,
                                ),
                              ),
                            ),
                            const SizedBox(width: 5),
                            const Icon(
                              Icons.graphic_eq_rounded,
                              size: 19,
                              color: AppColors.primary,
                            ),
                          ],
                        ),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 6),
                Text(AppStrings.today, style: AppTextStyles.body),
                const SizedBox(height: 4),
                Text('早上好，今天也要掌握全球脉搏', style: AppTextStyles.body),
              ],
            ),
          ),
          PulseIconButton(
            icon: CupertinoIcons.bell,
            showDot: true,
            onPressed: onBell,
          ),
          const SizedBox(width: 10),
          PulseIconButton(icon: CupertinoIcons.search, onPressed: onSearch),
        ],
      ),
    );
  }
}

class _InvestmentInsightCard extends StatelessWidget {
  const _InvestmentInsightCard({required this.article, required this.onTap});

  final Article article;
  final ValueChanged<Article> onTap;

  @override
  Widget build(BuildContext context) {
    return PulseCard(
      borderColor: AppColors.borderBlue,
      backgroundColor: AppColors.surfaceTint,
      onTap: () => onTap(article),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Icon(
                Icons.account_balance_rounded,
                color: AppColors.primary,
              ),
              const SizedBox(width: 8),
              Expanded(
                child: Text('今日投行观点', style: AppTextStyles.sectionTitle),
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
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              ClipRRect(
                borderRadius: BorderRadius.circular(AppRadius.md),
                child: Image.asset(
                  article.imageAsset,
                  width: 92,
                  height: 92,
                  fit: BoxFit.cover,
                ),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      article.title,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: AppTextStyles.cardTitle,
                    ),
                    const SizedBox(height: 6),
                    Text(
                      '${article.sourceName} · ${article.publishTime}',
                      style: AppTextStyles.meta,
                    ),
                    const SizedBox(height: 8),
                    Text(
                      article.summary,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: AppTextStyles.body,
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 8),
              const SourceBadge(label: '投行观点'),
            ],
          ),
        ],
      ),
    );
  }
}
