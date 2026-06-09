import 'package:flutter_test/flutter_test.dart';
import 'package:pulsebrief/shared/data/pulse_api_transport.dart';
import 'package:pulsebrief/shared/repositories/api_pulse_repository.dart';
import 'package:pulsebrief/shared/repositories/mock_pulse_repository.dart';
import 'package:pulsebrief/shared/repositories/pulse_repository_factory.dart';

void main() {
  test('mock repository returns app-ready seed data', () async {
    final repository = MockPulseRepository();

    final session = await repository.login(
      account: 'wenjin@example.com',
      verificationCode: '123456',
      agreementAccepted: true,
    );
    final categories = await repository.getCategories();
    final homeFeed = await repository.getHomeFeed();
    final digestFeed = await repository.getTodayDigest();

    expect(session.user.nickname, 'Wenjin');
    expect(categories, isNotEmpty);
    expect(homeFeed.articles, isNotEmpty);
    expect(homeFeed.investmentPick.categoryName, '投行观点');
    expect(digestFeed.digests, isNotEmpty);
  });

  test('repository factory defaults to mock and can create api repository', () {
    final mockRepository = PulseRepositoryFactory.create(dataSource: 'mock');
    final apiRepository = PulseRepositoryFactory.create(
      dataSource: 'api',
      apiBaseUrl: 'http://10.0.2.2:8080/api',
    );

    expect(mockRepository, isA<MockPulseRepository>());
    expect(apiRepository, isA<ApiPulseRepository>());
  });

  test('api repository maps v1 api responses into ui models', () async {
    final repository = ApiPulseRepository(
      transport: _FakePulseApiTransport({
        'POST /auth/login': {
          'code': 'OK',
          'data': {
            'accessToken': 'dev-token-1',
            'tokenType': 'Bearer',
            'expiresIn': 604800,
            'user': {
              'id': 1,
              'nickname': 'Wenjin',
              'avatarUrl': '',
              'bio': '每天几分钟，掌握全球脉搏',
            },
          },
        },
        'GET /categories': {
          'code': 'OK',
          'data': [
            {
              'id': 2,
              'code': 'finance',
              'name': '财经市场',
              'description': '关注全球市场',
              'sortNo': 20,
              'enabled': true,
            },
          ],
        },
        'GET /articles/home?categoryCode=all&pageSize=20': {
          'code': 'OK',
          'data': {
            'todayDigest': {
              'digestId': 1,
              'title': '今日全球简报',
              'subtitle': '精选 10 条全球重点资讯',
            },
            'investmentPick': _articleJson(id: 1, categoryName: '投行观点'),
            'articles': [_articleJson(id: 1, categoryName: '投行观点')],
          },
        },
        'GET /digests/today': {
          'code': 'OK',
          'data': {
            'date': '2026-06-08',
            'headline': {
              'id': 1,
              'title': '今日全球早报',
              'subtitle': '精选 10 条重点',
              'duration': '08:12',
              'updateTime': '08:30 更新',
            },
            'digests': [
              {
                'id': 1,
                'title': '今日全球早报',
                'subtitle': '精选 10 条重点',
                'duration': '08:12',
                'updateTime': '08:30 更新',
              },
            ],
            'highlights': ['英伟达 Blackwell Ultra 发布'],
          },
        },
      }),
    );

    final session = await repository.login(
      account: 'wenjin@example.com',
      verificationCode: '123456',
      agreementAccepted: true,
    );
    final categories = await repository.getCategories();
    final homeFeed = await repository.getHomeFeed();
    final digestFeed = await repository.getTodayDigest();

    expect(session.accessToken, 'dev-token-1');
    expect(categories.single.name, '财经市场');
    expect(homeFeed.todayDigest.title, '今日全球简报');
    expect(homeFeed.articles.single.id, '1');
    expect(homeFeed.articles.single.publishTime, isNot(contains('T')));
    expect(homeFeed.articles.single.publishTime, contains('09:30'));
    expect(digestFeed.highlights.single, contains('Blackwell'));
  });
}

Map<String, Object?> _articleJson({required int id, required String categoryName}) {
  return {
    'id': id,
    'title': '高盛：AI 基建投资仍将持续',
    'sourceName': 'Goldman Sachs Research',
    'publishTime': '2026-06-08T09:30:00+08:00',
    'categoryCode': 'investment_view',
    'categoryName': categoryName,
    'summary': 'AI 基础设施投资仍处于扩张阶段。',
    'imageUrl': '',
    'audioDuration': '02:48',
    'hot': true,
    'breaking': false,
    'favorited': false,
  };
}

class _FakePulseApiTransport implements PulseApiTransport {
  _FakePulseApiTransport(this.responses);

  final Map<String, Map<String, Object?>> responses;

  @override
  Future<Map<String, Object?>> deleteJson(String path, {String? token}) async {
    return responses['DELETE $path'] ?? {'code': 'OK', 'data': {}};
  }

  @override
  Future<Map<String, Object?>> getJson(String path, {String? token}) async {
    return responses['GET $path'] ?? {'code': 'OK', 'data': {}};
  }

  @override
  Future<Map<String, Object?>> postJson(
    String path, {
    Map<String, Object?>? body,
    String? token,
  }) async {
    return responses['POST $path'] ?? {'code': 'OK', 'data': {}};
  }

  @override
  Future<Map<String, Object?>> putJson(
    String path, {
    Map<String, Object?>? body,
    String? token,
  }) async {
    return responses['PUT $path'] ?? {'code': 'OK', 'data': {}};
  }
}
