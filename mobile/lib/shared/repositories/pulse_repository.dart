import 'package:pulsebrief/shared/models/article.dart';
import 'package:pulsebrief/shared/models/digest.dart';
import 'package:pulsebrief/shared/models/news_category.dart';
import 'package:pulsebrief/shared/models/subscription_topic.dart';
import 'package:pulsebrief/shared/models/user_profile.dart';

abstract class PulseRepository {
  Future<AuthSession> login({
    required String account,
    required String verificationCode,
    required bool agreementAccepted,
  });

  Future<AuthSession> guest();

  Future<UserProfile> getUserProfile();

  Future<List<NewsCategory>> getCategories();

  Future<HomeFeed> getHomeFeed({
    String categoryCode = 'all',
    int pageSize = 20,
  });

  Future<List<Article>> getArticles({
    String categoryCode = 'all',
    int page = 1,
    int pageSize = 20,
  });

  Future<Article> getArticleDetail(String id);

  Future<TodayDigestFeed> getTodayDigest();

  Future<Digest> getDigestDetail(String id);

  Future<SubscriptionSnapshot> getSubscriptions();

  Future<SubscriptionSnapshot> saveSubscriptions({
    required List<String> categoryCodes,
    required PushPreferences preferences,
  });

  Future<bool> favoriteArticle(String articleId);

  Future<bool> unfavoriteArticle(String articleId);

  Future<int> recordPlayback({
    required String playType,
    String? articleId,
    String? digestId,
    required String playTitle,
    required int durationSeconds,
  });
}

class AuthSession {
  const AuthSession({
    required this.accessToken,
    required this.tokenType,
    required this.expiresIn,
    required this.user,
  });

  final String accessToken;
  final String tokenType;
  final int expiresIn;
  final UserProfile user;
}

class DigestHero {
  const DigestHero({
    required this.id,
    required this.title,
    required this.subtitle,
  });

  final String id;
  final String title;
  final String subtitle;
}

class HomeFeed {
  const HomeFeed({
    required this.todayDigest,
    required this.investmentPick,
    required this.articles,
  });

  final DigestHero todayDigest;
  final Article investmentPick;
  final List<Article> articles;
}

class TodayDigestFeed {
  const TodayDigestFeed({
    required this.date,
    required this.headline,
    required this.digests,
    required this.highlights,
  });

  final String date;
  final Digest headline;
  final List<Digest> digests;
  final List<String> highlights;
}

class PushPreferences {
  const PushPreferences({
    required this.morningDigest,
    required this.eveningReview,
    required this.breakingNews,
    required this.investmentView,
  });

  final bool morningDigest;
  final bool eveningReview;
  final bool breakingNews;
  final bool investmentView;

  Map<String, Object?> toJson() {
    return {
      'morningDigest': morningDigest,
      'eveningReview': eveningReview,
      'breakingNews': breakingNews,
      'investmentView': investmentView,
    };
  }
}

class SubscriptionSnapshot {
  const SubscriptionSnapshot({
    required this.selectedCategoryCodes,
    required this.todayMatchedCount,
    required this.preferences,
    required this.topics,
  });

  final List<String> selectedCategoryCodes;
  final int todayMatchedCount;
  final PushPreferences preferences;
  final List<SubscriptionTopic> topics;
}
