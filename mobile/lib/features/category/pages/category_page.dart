import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:pulsebrief/app/routes.dart';
import 'package:pulsebrief/core/constants/app_assets.dart';
import 'package:pulsebrief/shared/models/article.dart';
import 'package:pulsebrief/shared/models/news_category.dart';
import 'package:pulsebrief/shared/repositories/repository_scope.dart';
import 'package:pulsebrief/shared/theme/app_colors.dart';
import 'package:pulsebrief/shared/theme/app_spacing.dart';
import 'package:pulsebrief/shared/theme/app_text_styles.dart';
import 'package:pulsebrief/shared/widgets/app_header.dart';
import 'package:pulsebrief/shared/widgets/brief_hero_card.dart';
import 'package:pulsebrief/shared/widgets/category_chip.dart';
import 'package:pulsebrief/shared/widgets/empty_state.dart';
import 'package:pulsebrief/shared/widgets/loading_state.dart';
import 'package:pulsebrief/shared/widgets/news_card.dart';
import 'package:pulsebrief/shared/widgets/pulse_card.dart';
import 'package:pulsebrief/shared/widgets/section_header.dart';

class CategoryPage extends StatefulWidget {
  const CategoryPage({super.key});

  @override
  State<CategoryPage> createState() => _CategoryPageState();
}

class _CategoryPageState extends State<CategoryPage> {
  bool _loaded = false;
  bool _isLoading = true;
  String? _errorMessage;
  NewsCategory? _selected;
  List<NewsCategory> _categories = const [];
  List<Article> _articles = const [];

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (!_loaded) {
      _loaded = true;
      _loadCategories();
    }
  }

  Future<void> _loadCategories() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });
    try {
      final repository = RepositoryScope.of(context);
      final categories = await repository.getCategories();
      final selected = categories.firstWhere(
        (category) => category.code == 'finance',
        orElse: () => categories.first,
      );
      final articles = await repository.getArticles(
        categoryCode: selected.code,
        pageSize: 20,
      );
      if (!mounted) return;
      setState(() {
        _categories = categories;
        _selected = selected;
        _articles = articles;
        _isLoading = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _errorMessage = '分类内容加载失败，请稍后重试';
        _isLoading = false;
      });
    }
  }

  Future<void> _selectCategory(NewsCategory category) async {
    setState(() {
      _selected = category;
      _isLoading = true;
    });
    try {
      final articles = await RepositoryScope.of(context).getArticles(
        categoryCode: category.code,
        pageSize: 20,
      );
      if (!mounted) return;
      setState(() {
        _articles = articles;
        _isLoading = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _errorMessage = '分类内容加载失败，请稍后重试';
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

  @override
  Widget build(BuildContext context) {
    if (_errorMessage != null) {
      return SafeArea(
        bottom: false,
        child: EmptyState(title: '加载失败', message: _errorMessage!),
      );
    }
    if (_isLoading && _categories.isEmpty) {
      return const SafeArea(bottom: false, child: LoadingState());
    }
    final selected = _selected;
    if (selected == null || _categories.isEmpty) {
      return const SafeArea(bottom: false, child: EmptyState());
    }
    final articles = _articles;

    return SafeArea(
      bottom: false,
      child: CustomScrollView(
        slivers: [
          SliverToBoxAdapter(
            child: AppHeader(
              title: '分类',
              subtitle: '按主题查看你关注的全球资讯',
              actions: [
                PulseIconButton(icon: CupertinoIcons.search, onPressed: () {}),
              ],
            ),
          ),
          SliverToBoxAdapter(
            child: SizedBox(
              height: 44,
              child: ListView.separated(
                padding: const EdgeInsets.symmetric(
                  horizontal: AppSpacing.pagePadding,
                ),
                scrollDirection: Axis.horizontal,
                itemCount: _categories.length,
                separatorBuilder: (_, _) => const SizedBox(width: 10),
                itemBuilder: (context, index) {
                  final category = _categories[index];
                  return CategoryChip(
                    label: category.name,
                    state: category.code == selected.code
                        ? CategoryChipState.selected
                        : CategoryChipState.normal,
                    onTap: () => _selectCategory(category),
                  );
                },
              ),
            ),
          ),
          SliverPadding(
            padding: const EdgeInsets.all(AppSpacing.pagePadding),
            sliver: SliverList.list(
              children: [
                BriefHeroCard(
                  title: selected.name,
                  subtitle: selected.description,
                  description: '今日 ${selected.todayCount} 条更新，精选重点事件和可播报摘要。',
                  imageAsset: selected.code == 'finance'
                      ? AppAssets.artCleanFinance
                      : AppAssets.artCleanGlobal,
                  compact: true,
                ),
                const SizedBox(height: AppSpacing.sectionGap),
                PulseCard(
                  padding: const EdgeInsets.fromLTRB(14, 14, 14, 12),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          const Icon(
                            Icons.star_rounded,
                            color: AppColors.primary,
                          ),
                          const SizedBox(width: 8),
                          Text('今日重点', style: AppTextStyles.sectionTitle),
                          const Spacer(),
                          Text('重点关注', style: AppTextStyles.label),
                        ],
                      ),
                      const SizedBox(height: 14),
                      LayoutBuilder(
                        builder: (context, constraints) {
                          final compact = constraints.maxWidth < 360;
                          final focusArticles = articles.take(2).toList();
                          return compact
                              ? Column(
                                  children: [
                                    for (final article in focusArticles) ...[
                                      NewsCard(
                                        article: article,
                                        compact: true,
                                        onTap: () => _openArticle(article),
                                        onPlay: _openPlayer,
                                      ),
                                      if (article != focusArticles.last)
                                        const SizedBox(height: AppSpacing.md),
                                    ],
                                  ],
                                )
                              : Row(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    for (
                                      var index = 0;
                                      index < focusArticles.length;
                                      index++
                                    ) ...[
                                      Expanded(
                                        child: NewsCard(
                                          article: focusArticles[index],
                                          compact: true,
                                          onTap: () => _openArticle(
                                            focusArticles[index],
                                          ),
                                          onPlay: _openPlayer,
                                        ),
                                      ),
                                      if (index != focusArticles.length - 1)
                                        const SizedBox(width: AppSpacing.md),
                                    ],
                                  ],
                                );
                        },
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: AppSpacing.sectionGap),
                SectionHeader(title: '${selected.name}资讯'),
                const SizedBox(height: AppSpacing.md),
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
            sliver: SliverList.separated(
              itemCount: articles.length,
              itemBuilder: (context, index) {
                final article = articles[index];
                return NewsCard(
                  article: article,
                  onTap: () => _openArticle(article),
                  onPlay: _openPlayer,
                );
              },
              separatorBuilder: (_, _) => const SizedBox(height: AppSpacing.md),
            ),
          ),
        ],
      ),
    );
  }
}
