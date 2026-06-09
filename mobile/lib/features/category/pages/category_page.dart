import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:pulsebrief/app/routes.dart';
import 'package:pulsebrief/core/constants/app_assets.dart';
import 'package:pulsebrief/shared/models/article.dart';
import 'package:pulsebrief/shared/models/news_category.dart';
import 'package:pulsebrief/shared/repositories/pulse_repository.dart';
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
      final initial = await _loadInitialCategory(
        repository: repository,
        categories: categories,
      );
      if (!mounted) return;
      setState(() {
        _categories = categories;
        _selected = initial.category;
        _articles = initial.articles;
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

  Future<_CategoryLoadResult> _loadInitialCategory({
    required PulseRepository repository,
    required List<NewsCategory> categories,
  }) async {
    final preferred = [
      ...categories.where((category) => category.code == 'finance'),
      ...categories.where((category) => category.code != 'finance'),
    ];
    for (final category in preferred) {
      final articles = await repository.getArticles(
        categoryCode: category.code,
        pageSize: 20,
      );
      if (articles.isNotEmpty) {
        return _CategoryLoadResult(category: category, articles: articles);
      }
    }
    return _CategoryLoadResult(
      category: categories.first,
      articles: const <Article>[],
    );
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
    final updateCount = articles.isEmpty ? selected.todayCount : articles.length;

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
                  description: '今日 $updateCount 条更新，精选重点事件和可播报摘要。',
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
                          if (focusArticles.isEmpty) {
                            return const _CategoryEmptyHint();
                          }
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
            sliver: articles.isEmpty
                ? const SliverToBoxAdapter(
                    child: EmptyState(
                      title: '暂无分类资讯',
                      message: '该主题暂时没有新的可播报摘要，可以先查看其他分类。',
                    ),
                  )
                : SliverList.separated(
                    itemCount: articles.length,
                    itemBuilder: (context, index) {
                      final article = articles[index];
                      return NewsCard(
                        article: article,
                        onTap: () => _openArticle(article),
                        onPlay: _openPlayer,
                      );
                    },
                    separatorBuilder: (_, _) =>
                        const SizedBox(height: AppSpacing.md),
                  ),
          ),
        ],
      ),
    );
  }
}

class _CategoryLoadResult {
  const _CategoryLoadResult({required this.category, required this.articles});

  final NewsCategory category;
  final List<Article> articles;
}

class _CategoryEmptyHint extends StatelessWidget {
  const _CategoryEmptyHint();

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 18),
      child: Text(
        '该分类暂时没有新的重点资讯',
        textAlign: TextAlign.center,
        style: AppTextStyles.body,
      ),
    );
  }
}
