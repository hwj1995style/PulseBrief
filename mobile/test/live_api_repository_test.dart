import 'package:flutter_test/flutter_test.dart';
import 'package:pulsebrief/shared/data/pulse_api_transport.dart';
import 'package:pulsebrief/shared/repositories/api_pulse_repository.dart';
import 'package:pulsebrief/shared/repositories/pulse_repository.dart';

const _liveApiEnabled = bool.fromEnvironment('PULSEBRIEF_LIVE_API');
const _apiBaseUrl = String.fromEnvironment(
  'PULSEBRIEF_API_BASE_URL',
  defaultValue: 'http://localhost:8080/api',
);

void main() {
  test(
    'api repository works against a running v1 backend',
    () async {
      final repository = ApiPulseRepository(
        transport: HttpPulseApiTransport(baseUrl: _apiBaseUrl),
      );

      final session = await repository.login(
        account: 'wenjin@example.com',
        verificationCode: '123456',
        agreementAccepted: true,
      );
      expect(session.accessToken, isNotEmpty);

      final profile = await repository.getUserProfile();
      expect(profile.nickname, 'Wenjin');
      expect(profile.subscriptionCount, greaterThanOrEqualTo(0));

      final categories = await repository.getCategories();
      expect(categories, isNotEmpty);

      final homeFeed = await repository.getHomeFeed(pageSize: 5);
      expect(homeFeed.articles, isNotEmpty);
      expect(homeFeed.todayDigest.title, isNotEmpty);

      final article = await repository.getArticleDetail(
        homeFeed.articles.first.id,
      );
      expect(article.keyPoints, isNotEmpty);

      final digestFeed = await repository.getTodayDigest();
      expect(digestFeed.digests, isNotEmpty);
      expect(digestFeed.highlights, isNotEmpty);

      final subscriptions = await repository.saveSubscriptions(
        categoryCodes: const ['global', 'finance', 'ai', 'investment_view'],
        preferences: const PushPreferences(
          morningDigest: true,
          eveningReview: true,
          breakingNews: false,
          investmentView: true,
        ),
      );
      expect(subscriptions.selectedCategoryCodes, hasLength(4));

      expect(await repository.favoriteArticle(article.id), isTrue);
      final playbackId = await repository.recordPlayback(
        playType: 'ARTICLE',
        articleId: article.id,
        playTitle: '中文播放记录联调',
        durationSeconds: 168,
      );
      expect(playbackId, greaterThan(0));
    },
    skip: _liveApiEnabled ? false : 'Set PULSEBRIEF_LIVE_API=true to run.',
  );
}
