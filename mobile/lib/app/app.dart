import 'package:flutter/material.dart';
import 'package:pulsebrief/app/routes.dart';
import 'package:pulsebrief/features/article/pages/article_detail_page.dart';
import 'package:pulsebrief/features/auth/pages/login_page.dart';
import 'package:pulsebrief/features/category/pages/category_page.dart';
import 'package:pulsebrief/features/digest/pages/digest_page.dart';
import 'package:pulsebrief/features/home/pages/home_page.dart';
import 'package:pulsebrief/features/mine/pages/mine_page.dart';
import 'package:pulsebrief/features/player/pages/player_page.dart';
import 'package:pulsebrief/features/subscription/pages/subscription_page.dart';
import 'package:pulsebrief/mock/mock_articles.dart';
import 'package:pulsebrief/shared/models/article.dart';
import 'package:pulsebrief/shared/repositories/pulse_repository.dart';
import 'package:pulsebrief/shared/repositories/pulse_repository_factory.dart';
import 'package:pulsebrief/shared/repositories/repository_scope.dart';
import 'package:pulsebrief/shared/theme/app_theme.dart';
import 'package:pulsebrief/shared/widgets/mobile_viewport.dart';
import 'package:pulsebrief/shared/widgets/pulse_bottom_nav.dart';

class PulseBriefApp extends StatelessWidget {
  const PulseBriefApp({super.key, this.repository});

  final PulseRepository? repository;

  @override
  Widget build(BuildContext context) {
    final appRepository = repository ?? PulseRepositoryFactory.create();

    return MaterialApp(
      title: '脉闻 PulseBrief',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.light(),
      initialRoute: PulseRoutes.login,
      onGenerateRoute: _onGenerateRoute,
      builder: (context, child) {
        return RepositoryScope(
          repository: appRepository,
          child: MobileViewport(child: child ?? const SizedBox.shrink()),
        );
      },
    );
  }

  Route<dynamic> _onGenerateRoute(RouteSettings settings) {
    switch (settings.name) {
      case PulseRoutes.login:
        return MaterialPageRoute(
          builder: (_) => const LoginPage(),
          settings: settings,
        );
      case PulseRoutes.main:
        final initialIndex = settings.arguments is int
            ? settings.arguments! as int
            : 0;
        return MaterialPageRoute(
          builder: (_) => MainShell(initialIndex: initialIndex),
          settings: settings,
        );
      case PulseRoutes.subscription:
        return MaterialPageRoute(
          builder: (_) => const SubscriptionPage(),
          settings: settings,
        );
      case PulseRoutes.articleDetail:
        final article = settings.arguments is Article
            ? settings.arguments! as Article
            : mockArticles.first;
        return MaterialPageRoute(
          builder: (_) => ArticleDetailPage(article: article),
          settings: settings,
        );
      case PulseRoutes.player:
        final article = settings.arguments is Article
            ? settings.arguments! as Article
            : null;
        return MaterialPageRoute(
          builder: (_) => PlayerPage(article: article),
          settings: settings,
        );
      default:
        return MaterialPageRoute(
          builder: (_) => const LoginPage(),
          settings: settings,
        );
    }
  }
}

class MainShell extends StatefulWidget {
  const MainShell({super.key, this.initialIndex = 0});

  final int initialIndex;

  @override
  State<MainShell> createState() => _MainShellState();
}

class _MainShellState extends State<MainShell> {
  late int _currentIndex;

  final _pages = const [HomePage(), CategoryPage(), DigestPage(), MinePage()];

  @override
  void initState() {
    super.initState();
    _currentIndex = widget.initialIndex.clamp(0, _pages.length - 1);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      extendBody: true,
      body: IndexedStack(index: _currentIndex, children: _pages),
      bottomNavigationBar: PulseBottomNav(
        currentIndex: _currentIndex,
        onTap: (index) => setState(() => _currentIndex = index),
      ),
    );
  }
}
