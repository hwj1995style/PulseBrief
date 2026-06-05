import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:pulsebrief/app/routes.dart';
import 'package:pulsebrief/core/constants/app_assets.dart';
import 'package:pulsebrief/mock/mock_articles.dart';
import 'package:pulsebrief/mock/mock_categories.dart';
import 'package:pulsebrief/shared/models/article.dart';
import 'package:pulsebrief/shared/models/news_category.dart';
import 'package:pulsebrief/shared/theme/app_colors.dart';
import 'package:pulsebrief/shared/theme/app_spacing.dart';
import 'package:pulsebrief/shared/theme/app_text_styles.dart';
import 'package:pulsebrief/shared/widgets/app_header.dart';
import 'package:pulsebrief/shared/widgets/brief_hero_card.dart';
import 'package:pulsebrief/shared/widgets/category_chip.dart';
import 'package:pulsebrief/shared/widgets/news_card.dart';
import 'package:pulsebrief/shared/widgets/pulse_card.dart';
import 'package:pulsebrief/shared/widgets/section_header.dart';

class CategoryPage extends StatefulWidget {
  const CategoryPage({super.key});

  @override
  State<CategoryPage> createState() => _CategoryPageState();
}

class _CategoryPageState extends State<CategoryPage> {
  NewsCategory _selected = mockCategories[1];
  late List<Article> _articles;

  @override
  void initState() {
    super.initState();
    _articles = List<Article>.from(mockArticles);
  }

  void _openArticle(Article article) {
    Navigator.pushNamed(context, PulseRoutes.articleDetail, arguments: article);
  }

  void _openPlayer() {
    Navigator.pushNamed(context, PulseRoutes.player);
  }

  List<Article> get _categoryArticles {
    final filtered = _articles
        .where(
          (article) =>
              article.categoryName.contains(_selected.name[0]) ||
              article.categoryName == _selected.name,
        )
        .toList();
    return filtered.isEmpty ? _articles : filtered;
  }

  @override
  Widget build(BuildContext context) {
    final articles = _categoryArticles;

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
                itemCount: mockCategories.length,
                separatorBuilder: (_, _) => const SizedBox(width: 10),
                itemBuilder: (context, index) {
                  final category = mockCategories[index];
                  return CategoryChip(
                    label: category.name,
                    state: category.code == _selected.code
                        ? CategoryChipState.selected
                        : CategoryChipState.normal,
                    onTap: () => setState(() => _selected = category),
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
                  title: _selected.name,
                  subtitle: _selected.description,
                  description: '今日 ${_selected.todayCount} 条更新，精选重点事件和可播报摘要。',
                  imageAsset: _selected.code == 'finance'
                      ? AppAssets.artFinanceGlobe
                      : AppAssets.artGlobalGlobe,
                  compact: true,
                ),
                const SizedBox(height: AppSpacing.lg),
                PulseCard(
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
                          return compact
                              ? Column(
                                  children: articles
                                      .take(2)
                                      .map(
                                        (article) => Padding(
                                          padding: const EdgeInsets.only(
                                            bottom: AppSpacing.md,
                                          ),
                                          child: NewsCard(
                                            article: article,
                                            compact: true,
                                            onTap: () => _openArticle(article),
                                            onPlay: _openPlayer,
                                          ),
                                        ),
                                      )
                                      .toList(),
                                )
                              : Row(
                                  children: articles
                                      .take(2)
                                      .map(
                                        (article) => Expanded(
                                          child: Padding(
                                            padding: const EdgeInsets.only(
                                              right: AppSpacing.md,
                                            ),
                                            child: NewsCard(
                                              article: article,
                                              compact: true,
                                              onTap: () =>
                                                  _openArticle(article),
                                              onPlay: _openPlayer,
                                            ),
                                          ),
                                        ),
                                      )
                                      .toList(),
                                );
                        },
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: AppSpacing.lg),
                SectionHeader(title: '${_selected.name}资讯'),
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
