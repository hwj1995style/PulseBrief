import 'package:flutter/cupertino.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:pulsebrief/app/app.dart';

void main() {
  testWidgets('PulseBrief app launches on home page', (tester) async {
    await tester.binding.setSurfaceSize(const Size(390, 844));
    await tester.pumpWidget(const PulseBriefApp());
    await tester.pumpAndSettle();

    expect(find.text('脉闻'), findsOneWidget);
    expect(find.text('今日全球简报'), findsOneWidget);
  });

  testWidgets('Main navigation and key routes are reachable', (tester) async {
    await tester.binding.setSurfaceSize(const Size(390, 844));
    await tester.pumpWidget(const PulseBriefApp());
    await tester.pumpAndSettle();

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
