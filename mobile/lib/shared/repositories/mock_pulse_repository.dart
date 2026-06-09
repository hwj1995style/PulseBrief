import 'package:pulsebrief/mock/mock_articles.dart';
import 'package:pulsebrief/mock/mock_categories.dart';
import 'package:pulsebrief/mock/mock_digests.dart';
import 'package:pulsebrief/mock/mock_subscriptions.dart';
import 'package:pulsebrief/mock/mock_user.dart';
import 'package:pulsebrief/shared/models/article.dart';
import 'package:pulsebrief/shared/models/digest.dart';
import 'package:pulsebrief/shared/models/news_category.dart';
import 'package:pulsebrief/shared/models/subscription_topic.dart';
import 'package:pulsebrief/shared/repositories/pulse_repository.dart';

class MockPulseRepository implements PulseRepository {
  MockPulseRepository()
    : _articles = List<Article>.from(mockArticles),
      _topics = List<SubscriptionTopic>.from(mockSubscriptionTopics);

  List<Article> _articles;
  List<SubscriptionTopic> _topics;
  String _token = '';

  @override
  Future<AuthSession> login({
    required String account,
    required String verificationCode,
    required bool agreementAccepted,
  }) async {
    _token = 'dev-token-1';
    return AuthSession(
      accessToken: _token,
      tokenType: 'Bearer',
      expiresIn: 604800,
      user: mockUser,
    );
  }

  @override
  Future<AuthSession> guest() async {
    _token = '';
    return const AuthSession(
      accessToken: '',
      tokenType: 'Guest',
      expiresIn: 0,
      user: mockUser,
    );
  }

  @override
  Future<List<NewsCategory>> getCategories() async {
    return List<NewsCategory>.from(mockCategories);
  }

  @override
  Future<HomeFeed> getHomeFeed({
    String categoryCode = 'all',
    int pageSize = 20,
  }) async {
    final articles = await getArticles(
      categoryCode: categoryCode,
      pageSize: pageSize,
    );
    final investmentPick = _articles.firstWhere(
      (article) => article.categoryName == '投行观点',
      orElse: () => articles.first,
    );
    return HomeFeed(
      todayDigest: const DigestHero(
        id: 'morning',
        title: '今日全球简报',
        subtitle: '精选 10 条全球重点资讯',
      ),
      investmentPick: investmentPick,
      articles: articles,
    );
  }

  @override
  Future<List<Article>> getArticles({
    String categoryCode = 'all',
    int page = 1,
    int pageSize = 20,
  }) async {
    if (categoryCode == 'all') {
      return _articles.take(pageSize).toList();
    }
    final category = mockCategories.firstWhere(
      (item) => item.code == categoryCode,
      orElse: () => mockCategories.first,
    );
    final filtered = _articles
        .where((article) => article.categoryName == category.name)
        .toList();
    return (filtered.isEmpty ? _articles : filtered).take(pageSize).toList();
  }

  @override
  Future<Article> getArticleDetail(String id) async {
    return [..._articles, ...relatedArticles].firstWhere(
      (article) => article.id == id,
      orElse: () => _articles.first,
    );
  }

  @override
  Future<TodayDigestFeed> getTodayDigest() async {
    return TodayDigestFeed(
      date: '6月5日',
      headline: mockDigests.first,
      digests: List<Digest>.from(mockDigests),
      highlights: List<String>.from(digestHighlights),
    );
  }

  @override
  Future<Digest> getDigestDetail(String id) async {
    return mockDigests.firstWhere(
      (digest) => digest.id == id,
      orElse: () => mockDigests.first,
    );
  }

  @override
  Future<SubscriptionSnapshot> getSubscriptions() async {
    return SubscriptionSnapshot(
      selectedCategoryCodes: const [
        'global',
        'finance',
        'ai',
        'investment_view',
        'tech',
      ],
      todayMatchedCount: 42,
      preferences: const PushPreferences(
        morningDigest: true,
        eveningReview: true,
        breakingNews: false,
        investmentView: false,
      ),
      topics: _topics,
    );
  }

  @override
  Future<SubscriptionSnapshot> saveSubscriptions({
    required List<String> categoryCodes,
    required PushPreferences preferences,
  }) async {
    _topics = _topics
        .map(
          (topic) => topic.copyWith(
            selected:
                categoryCodes.contains(topic.name) ||
                categoryCodes.contains(_codeForTopicName(topic.name)),
          ),
        )
        .toList();
    return SubscriptionSnapshot(
      selectedCategoryCodes: categoryCodes,
      todayMatchedCount: 42,
      preferences: preferences,
      topics: _topics,
    );
  }

  @override
  Future<bool> favoriteArticle(String articleId) async {
    _articles = _articles
        .map(
          (article) => article.id == articleId
              ? article.copyWith(isFavorited: true)
              : article,
        )
        .toList();
    return true;
  }

  @override
  Future<bool> unfavoriteArticle(String articleId) async {
    _articles = _articles
        .map(
          (article) => article.id == articleId
              ? article.copyWith(isFavorited: false)
              : article,
        )
        .toList();
    return false;
  }

  @override
  Future<int> recordPlayback({
    required String playType,
    String? articleId,
    String? digestId,
    required String playTitle,
    required int durationSeconds,
  }) async {
    return 1;
  }

  String _codeForTopicName(String name) {
    return switch (name) {
      '全球热点' => 'global',
      '财经市场' => 'finance',
      '科技趋势' => 'tech',
      'AI 前沿' => 'ai',
      '宏观政策' => 'macro',
      '投行观点' => 'investment_view',
      '中美动态' => 'china_us',
      '产业观察' => 'industry',
      '公司动态' => 'company',
      '半导体' => 'semiconductor',
      '新能源' => 'new_energy',
      '数字资产' => 'digital_asset',
      _ => name,
    };
  }
}
