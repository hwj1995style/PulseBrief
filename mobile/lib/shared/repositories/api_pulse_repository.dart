import 'package:pulsebrief/core/constants/app_assets.dart';
import 'package:pulsebrief/mock/mock_subscriptions.dart';
import 'package:pulsebrief/shared/data/pulse_api_transport.dart';
import 'package:pulsebrief/shared/models/article.dart';
import 'package:pulsebrief/shared/models/digest.dart';
import 'package:pulsebrief/shared/models/news_category.dart';
import 'package:pulsebrief/shared/models/user_profile.dart';
import 'package:pulsebrief/shared/repositories/pulse_repository.dart';

class ApiPulseRepository implements PulseRepository {
  ApiPulseRepository({required this.transport});

  final PulseApiTransport transport;
  String _accessToken = '';

  @override
  Future<AuthSession> login({
    required String account,
    required String verificationCode,
    required bool agreementAccepted,
  }) async {
    final json = await transport.postJson(
      '/auth/login',
      body: {
        'account': account,
        'verificationCode': verificationCode,
        'agreementAccepted': agreementAccepted,
      },
    );
    final data = _dataMap(json);
    final user = _map(data['user']);
    _accessToken = _string(data['accessToken']);
    return AuthSession(
      accessToken: _accessToken,
      tokenType: _string(data['tokenType'], fallback: 'Bearer'),
      expiresIn: _int(data['expiresIn']),
      user: UserProfile(
        name: _string(user['nickname'], fallback: 'Wenjin'),
        bio: _string(user['bio'], fallback: '每天几分钟，掌握全球脉搏'),
        subscriptionCount: 12,
        favoriteCount: 28,
        readCount: 146,
        playCount: 34,
      ),
    );
  }

  @override
  Future<AuthSession> guest() async {
    final json = await transport.postJson('/auth/guest');
    final data = _dataMap(json);
    final user = _map(data['user']);
    return AuthSession(
      accessToken: _string(data['accessToken']),
      tokenType: _string(data['tokenType'], fallback: 'Guest'),
      expiresIn: _int(data['expiresIn']),
      user: UserProfile(
        name: _string(user['nickname'], fallback: '游客'),
        bio: _string(user['bio'], fallback: '游客模式'),
        subscriptionCount: 0,
        favoriteCount: 0,
        readCount: 0,
        playCount: 0,
      ),
    );
  }

  @override
  Future<List<NewsCategory>> getCategories() async {
    final json = await transport.getJson('/categories');
    return _dataList(json)
        .map(
          (item) => NewsCategory(
            code: _string(item['code']),
            name: _string(item['name']),
            description: _string(item['description']),
            todayCount: 0,
          ),
        )
        .toList();
  }

  @override
  Future<HomeFeed> getHomeFeed({
    String categoryCode = 'all',
    int pageSize = 20,
  }) async {
    final json = await transport.getJson(
      '/articles/home?categoryCode=$categoryCode&pageSize=$pageSize',
    );
    final data = _dataMap(json);
    final articles = _list(
      data['articles'],
    ).map((item) => _articleFromJson(_map(item))).toList();
    final investmentJson = _map(data['investmentPick']);
    return HomeFeed(
      todayDigest: _digestHeroFromJson(_map(data['todayDigest'])),
      investmentPick: investmentJson.isEmpty
          ? articles.first
          : _articleFromJson(investmentJson),
      articles: articles,
    );
  }

  @override
  Future<List<Article>> getArticles({
    String categoryCode = 'all',
    int page = 1,
    int pageSize = 20,
  }) async {
    final json = await transport.getJson(
      '/articles?categoryCode=$categoryCode&page=$page&pageSize=$pageSize',
    );
    final data = json['data'];
    if (data is List) {
      return data.map((item) => _articleFromJson(_map(item))).toList();
    }
    return _list(
      _map(data)['items'],
    ).map((item) => _articleFromJson(_map(item))).toList();
  }

  @override
  Future<Article> getArticleDetail(String id) async {
    final json = await transport.getJson('/articles/$id');
    final data = _dataMap(json);
    return _articleFromJson(data).copyWith(
      isFavorited: data['favorited'] == true,
    );
  }

  @override
  Future<TodayDigestFeed> getTodayDigest() async {
    final json = await transport.getJson('/digests/today');
    final data = _dataMap(json);
    return TodayDigestFeed(
      date: _string(data['date']),
      headline: _digestFromJson(_map(data['headline'])),
      digests: _list(
        data['digests'],
      ).map((item) => _digestFromJson(_map(item))).toList(),
      highlights: _list(data['highlights']).map(_string).toList(),
    );
  }

  @override
  Future<Digest> getDigestDetail(String id) async {
    final json = await transport.getJson('/digests/$id');
    final data = _dataMap(json);
    return Digest(
      id: _string(data['id']),
      title: _string(data['title']),
      subtitle: _string(data['sourceName']),
      updateTime: _string(data['updatedAt']),
      summary: _string(data['summary']),
      iconLabel: 'AI',
      duration: _string(data['duration']),
    );
  }

  @override
  Future<SubscriptionSnapshot> getSubscriptions() async {
    final json = await transport.getJson(
      '/user/subscriptions',
      token: _accessToken,
    );
    return _subscriptionFromJson(_dataMap(json));
  }

  @override
  Future<SubscriptionSnapshot> saveSubscriptions({
    required List<String> categoryCodes,
    required PushPreferences preferences,
  }) async {
    final json = await transport.putJson(
      '/user/subscriptions',
      token: _accessToken,
      body: {
        'categoryCodes': categoryCodes,
        'pushPreferences': preferences.toJson(),
      },
    );
    return _subscriptionFromJson(_dataMap(json));
  }

  @override
  Future<bool> favoriteArticle(String articleId) async {
    final json = await transport.postJson(
      '/articles/$articleId/favorite',
      token: _accessToken,
    );
    return _dataMap(json)['favorited'] == true;
  }

  @override
  Future<bool> unfavoriteArticle(String articleId) async {
    final json = await transport.deleteJson(
      '/articles/$articleId/favorite',
      token: _accessToken,
    );
    return _dataMap(json)['favorited'] == true;
  }

  @override
  Future<int> recordPlayback({
    required String playType,
    String? articleId,
    String? digestId,
    required String playTitle,
    required int durationSeconds,
  }) async {
    final json = await transport.postJson(
      '/playback/history',
      token: _accessToken,
      body: {
        'playType': playType,
        'articleId': int.tryParse(articleId ?? ''),
        'digestId': int.tryParse(digestId ?? ''),
        'playTitle': playTitle,
        'durationSeconds': durationSeconds,
      },
    );
    return _int(_dataMap(json)['id']);
  }

  Article _articleFromJson(Map<String, Object?> json) {
    final categoryCode = _string(json['categoryCode']);
    final categoryName = _string(json['categoryName']);
    return Article(
      id: _string(json['id']),
      title: _string(json['title']),
      sourceName: _string(json['sourceName']),
      publishTime: _string(json['publishTime']),
      categoryName: categoryName,
      summary: _string(json['summary'], fallback: _string(json['aiSummary'])),
      imageAsset: _imageFor(categoryCode, categoryName),
      duration: _string(json['audioDuration'], fallback: '02:48'),
      isHot: json['hot'] == true,
      isBreaking: json['breaking'] == true,
      isFavorited: json['favorited'] == true,
      keyPoints: _list(json['keyPoints']).map(_string).toList(),
      impact: _string(json['impactAnalysis']),
    );
  }

  DigestHero _digestHeroFromJson(Map<String, Object?> json) {
    return DigestHero(
      id: _string(json['digestId'], fallback: _string(json['id'])),
      title: _string(json['title']),
      subtitle: _string(json['subtitle']),
    );
  }

  Digest _digestFromJson(Map<String, Object?> json) {
    return Digest(
      id: _string(json['id']),
      title: _string(json['title']),
      subtitle: _string(json['subtitle']),
      updateTime: _string(json['updateTime']),
      summary: _string(json['summary'], fallback: _string(json['subtitle'])),
      iconLabel: _digestIcon(_string(json['title'])),
      duration: _string(json['duration']),
    );
  }

  SubscriptionSnapshot _subscriptionFromJson(Map<String, Object?> json) {
    final selectedCodes = _list(
      json['selectedCategoryCodes'],
    ).map(_string).toList();
    final preferences = _preferencesFromJson(_map(json['pushPreferences']));
    final topics = mockSubscriptionTopics
        .map(
          (topic) => topic.copyWith(
            selected:
                selectedCodes.contains(_codeForTopicName(topic.name)) ||
                selectedCodes.contains(topic.name),
          ),
        )
        .toList();
    return SubscriptionSnapshot(
      selectedCategoryCodes: selectedCodes,
      todayMatchedCount: _int(json['todayMatchedCount']),
      preferences: preferences,
      topics: topics,
    );
  }

  PushPreferences _preferencesFromJson(Map<String, Object?> json) {
    return PushPreferences(
      morningDigest: json['morningDigest'] != false,
      eveningReview: json['eveningReview'] != false,
      breakingNews: json['breakingNews'] == true,
      investmentView: json['investmentView'] == true,
    );
  }

  String _imageFor(String categoryCode, String categoryName) {
    if (categoryCode == 'ai' || categoryName.contains('AI')) {
      return AppAssets.thumbNvidia;
    }
    if (categoryCode == 'finance' || categoryName.contains('财经')) {
      return AppAssets.thumbMarket;
    }
    if (categoryCode == 'investment_view' || categoryName.contains('投行')) {
      return AppAssets.thumbGoldman;
    }
    if (categoryCode == 'macro' || categoryName.contains('宏观')) {
      return AppAssets.thumbFed;
    }
    if (categoryCode == 'industry') {
      return AppAssets.thumbOil;
    }
    return AppAssets.thumbCity;
  }

  String _digestIcon(String title) {
    if (title.contains('午间')) return '◴';
    if (title.contains('晚间')) return '☾';
    if (title.contains('AI')) return 'AI';
    return '☀';
  }

  String _codeForTopicName(String name) {
    return switch (name) {
      '全球热点' => 'global',
      '财经市场' => 'finance',
      '科技趋势' => 'tech',
      'AI 前沿' => 'ai',
      '宏观政策' => 'macro',
      '投行观点' => 'investment_view',
      '产业观察' => 'industry',
      '公司动态' => 'company',
      _ => name,
    };
  }

  Map<String, Object?> _dataMap(Map<String, Object?> json) {
    return _map(json['data']);
  }

  List<Map<String, Object?>> _dataList(Map<String, Object?> json) {
    return _list(json['data']).map(_map).toList();
  }

  Map<String, Object?> _map(Object? value) {
    if (value is Map) {
      return value.cast<String, Object?>();
    }
    return const {};
  }

  List<Object?> _list(Object? value) {
    if (value is List) {
      return value.cast<Object?>();
    }
    return const [];
  }

  String _string(Object? value, {String fallback = ''}) {
    if (value == null) {
      return fallback;
    }
    final text = value.toString();
    return text.isEmpty ? fallback : text;
  }

  int _int(Object? value) {
    if (value is int) {
      return value;
    }
    return int.tryParse(value?.toString() ?? '') ?? 0;
  }
}
