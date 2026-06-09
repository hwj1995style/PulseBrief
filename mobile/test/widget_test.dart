import 'package:flutter/cupertino.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:pulsebrief/app/app.dart';

void main() {
  testWidgets('PulseBrief app launches on login page', (tester) async {
    await tester.binding.setSurfaceSize(const Size(390, 844));
    await tester.pumpWidget(const PulseBriefApp());
    await tester.pumpAndSettle();

    expect(find.text('脉 闻'), findsOneWidget);
    expect(find.text('PulseBrief'), findsOneWidget);
    expect(find.text('登录 / 注册'), findsOneWidget);
    expect(find.text('游客模式进入'), findsOneWidget);
  });

  testWidgets('Login requires agreement when unchecked', (tester) async {
    await tester.binding.setSurfaceSize(const Size(390, 844));
    await tester.pumpWidget(const PulseBriefApp());
    await tester.pumpAndSettle();

    await tester.tap(find.byKey(const ValueKey('agreement-checkbox')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const ValueKey('login-submit-button')));
    await tester.pumpAndSettle();

    expect(find.text('请先同意用户协议和隐私政策'), findsOneWidget);
  });

  testWidgets('Main navigation and key routes are reachable', (tester) async {
    await tester.binding.setSurfaceSize(const Size(390, 844));
    await tester.pumpWidget(const PulseBriefApp());
    await tester.pumpAndSettle();

    await tester.tap(find.text('游客模式进入'));
    await tester.pumpAndSettle();
    expect(find.text('今日全球简报'), findsOneWidget);

    await tester.tap(find.text('分类').last);
    await tester.pumpAndSettle();
    expect(find.text('按主题查看你关注的全球资讯'), findsOneWidget);

    await tester.tap(find.text('简报').last);
    await tester.pumpAndSettle();
    expect(find.textContaining('每天几分钟，听懂全球重点'), findsOneWidget);

    await tester.tap(find.text('我的').last);
    await tester.pumpAndSettle();
    expect(find.text('Wenjin'), findsOneWidget);

    await tester.tap(find.text('我的订阅'));
    await tester.pumpAndSettle();
    expect(find.textContaining('选择你关注的全球热点与市场动态'), findsOneWidget);

    await tester.tap(find.byIcon(CupertinoIcons.chevron_left));
    await tester.pumpAndSettle();
    await tester.tap(find.text('我的收藏'));
    await tester.pumpAndSettle();
    expect(find.text('我的收藏'), findsWidgets);
    expect(find.byIcon(CupertinoIcons.bookmark_fill), findsWidgets);

    await tester.tap(find.byIcon(CupertinoIcons.chevron_left));
    await tester.pumpAndSettle();
    await tester.tap(find.text('阅读历史'));
    await tester.pumpAndSettle();
    expect(find.text('阅读历史'), findsWidgets);
    expect(find.byIcon(CupertinoIcons.trash), findsOneWidget);

    await tester.tap(find.byIcon(CupertinoIcons.chevron_left));
    await tester.pumpAndSettle();
    await tester.tap(find.text('播放历史'));
    await tester.pumpAndSettle();
    expect(find.text('播放历史'), findsWidgets);
    expect(find.byIcon(CupertinoIcons.trash), findsOneWidget);

    await tester.tap(find.byIcon(CupertinoIcons.chevron_left));
    await tester.pumpAndSettle();
    await tester.tap(find.text('首页').last);
    await tester.pumpAndSettle();

    await tester.tap(find.textContaining('英伟达推出').first);
    await tester.pumpAndSettle();
    expect(find.text('AI 摘要'), findsOneWidget);

    await tester.tap(find.text('语音播报'));
    await tester.pumpAndSettle();
    expect(find.text('正在播放'), findsOneWidget);
  });
}
