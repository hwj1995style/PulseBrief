import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:pulsebrief/app/routes.dart';
import 'package:pulsebrief/shared/models/article.dart';
import 'package:pulsebrief/shared/repositories/repository_scope.dart';
import 'package:pulsebrief/shared/theme/app_spacing.dart';
import 'package:pulsebrief/shared/widgets/app_header.dart';
import 'package:pulsebrief/shared/widgets/empty_state.dart';
import 'package:pulsebrief/shared/widgets/loading_state.dart';
import 'package:pulsebrief/shared/widgets/news_card.dart';

enum UserArticleListType { favorites, readHistory }

class UserArticleListPage extends StatefulWidget {
  const UserArticleListPage({super.key, required this.type});

  final UserArticleListType type;

  @override
  State<UserArticleListPage> createState() => _UserArticleListPageState();
}

class _UserArticleListPageState extends State<UserArticleListPage> {
  late Future<List<Article>> _articlesFuture;

  String get _title {
    return switch (widget.type) {
      UserArticleListType.favorites => '我的收藏',
      UserArticleListType.readHistory => '阅读历史',
    };
  }

  String get _subtitle {
    return switch (widget.type) {
      UserArticleListType.favorites => '你标记过的重点资讯',
      UserArticleListType.readHistory => '最近打开的结构化摘要',
    };
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    _articlesFuture = _loadArticles();
  }

  Future<List<Article>> _loadArticles() {
    final repository = RepositoryScope.of(context);
    return switch (widget.type) {
      UserArticleListType.favorites => repository.getFavoriteArticles(),
      UserArticleListType.readHistory => repository.getReadHistoryArticles(),
    };
  }

  Future<void> _refresh() async {
    setState(() {
      _articlesFuture = _loadArticles();
    });
    await _articlesFuture;
  }

  Future<void> _clearReadHistory() async {
    await RepositoryScope.of(context).clearReadHistory();
    setState(() {
      _articlesFuture = Future.value(const []);
    });
  }

  Future<void> _unfavorite(Article article) async {
    await RepositoryScope.of(context).unfavoriteArticle(article.id);
    setState(() {
      _articlesFuture = _articlesFuture.then(
        (items) => items.where((item) => item.id != article.id).toList(),
      );
    });
  }

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      bottom: false,
      child: RefreshIndicator(
        onRefresh: _refresh,
        child: CustomScrollView(
          physics: const AlwaysScrollableScrollPhysics(),
          slivers: [
            SliverToBoxAdapter(
              child: AppHeader(
                title: _title,
                subtitle: _subtitle,
                leading: const BackSquareButton(),
                actions: [
                  if (widget.type == UserArticleListType.readHistory)
                    PulseIconButton(
                      icon: CupertinoIcons.trash,
                      onPressed: _clearReadHistory,
                    ),
                ],
                topPadding: AppSpacing.md,
              ),
            ),
            FutureBuilder<List<Article>>(
              future: _articlesFuture,
              builder: (context, snapshot) {
                if (snapshot.connectionState != ConnectionState.done) {
                  return const SliverFillRemaining(child: LoadingState());
                }
                if (snapshot.hasError) {
                  return const SliverFillRemaining(
                    child: EmptyState(
                      title: '加载失败',
                      message: '暂时无法获取列表，请稍后重试。',
                    ),
                  );
                }
                final articles = snapshot.data ?? const [];
                if (articles.isEmpty) {
                  return SliverFillRemaining(
                    child: EmptyState(
                      title: '暂无内容',
                      message: widget.type == UserArticleListType.favorites
                          ? '收藏感兴趣的资讯后会出现在这里。'
                          : '打开资讯详情后会生成阅读历史。',
                    ),
                  );
                }
                return SliverPadding(
                  padding: const EdgeInsets.fromLTRB(
                    AppSpacing.pagePadding,
                    0,
                    AppSpacing.pagePadding,
                    120,
                  ),
                  sliver: SliverList.separated(
                    itemCount: articles.length,
                    separatorBuilder: (_, _) => const SizedBox(height: 12),
                    itemBuilder: (context, index) {
                      final article = articles[index];
                      return NewsCard(
                        article: article,
                        onTap: () => Navigator.pushNamed(
                          context,
                          PulseRoutes.articleDetail,
                          arguments: article,
                        ),
                        onPlay: () => Navigator.pushNamed(
                          context,
                          PulseRoutes.player,
                          arguments: article,
                        ),
                        onFavorite: widget.type == UserArticleListType.favorites
                            ? () => _unfavorite(article)
                            : null,
                      );
                    },
                  ),
                );
              },
            ),
          ],
        ),
      ),
    );
  }
}
