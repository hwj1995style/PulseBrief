import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:pulsebrief/app/routes.dart';
import 'package:pulsebrief/shared/models/user_profile.dart';
import 'package:pulsebrief/shared/repositories/repository_scope.dart';
import 'package:pulsebrief/shared/theme/app_colors.dart';
import 'package:pulsebrief/shared/theme/app_radius.dart';
import 'package:pulsebrief/shared/theme/app_spacing.dart';
import 'package:pulsebrief/shared/theme/app_text_styles.dart';
import 'package:pulsebrief/shared/widgets/app_header.dart';
import 'package:pulsebrief/shared/widgets/category_chip.dart';
import 'package:pulsebrief/shared/widgets/empty_state.dart';
import 'package:pulsebrief/shared/widgets/loading_state.dart';
import 'package:pulsebrief/shared/widgets/pulse_card.dart';

class MinePage extends StatefulWidget {
  const MinePage({super.key});

  @override
  State<MinePage> createState() => _MinePageState();
}

class _MinePageState extends State<MinePage> {
  bool _loaded = false;
  bool _isLoading = true;
  String? _errorMessage;
  UserProfile? _profile;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (!_loaded) {
      _loaded = true;
      _loadProfile();
    }
  }

  Future<void> _loadProfile() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });
    try {
      final profile = await RepositoryScope.of(context).getUserProfile();
      if (!mounted) return;
      setState(() {
        _profile = profile;
        _isLoading = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _errorMessage = '用户资料加载失败，请稍后重试';
        _isLoading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_isLoading) {
      return const SafeArea(bottom: false, child: LoadingState());
    }
    if (_errorMessage != null) {
      return SafeArea(
        bottom: false,
        child: EmptyState(title: '加载失败', message: _errorMessage!),
      );
    }
    final profile = _profile;
    if (profile == null) {
      return const SafeArea(bottom: false, child: EmptyState());
    }

    return SafeArea(
      bottom: false,
      child: CustomScrollView(
        slivers: [
          SliverToBoxAdapter(
            child: AppHeader(
              title: '我的',
              topPadding: 16,
              bottomPadding: 20,
              actions: [
                PulseIconButton(
                  icon: CupertinoIcons.bell,
                  showDot: true,
                  onPressed: () {},
                ),
                const SizedBox(width: 10),
                PulseIconButton(
                  icon: CupertinoIcons.gear_alt,
                  onPressed: () {},
                ),
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
            sliver: SliverList.list(
              children: [
                _ProfileBlock(profile: profile),
                const SizedBox(height: AppSpacing.xl),
                PulseCard(
                  borderColor: AppColors.borderBlue,
                  backgroundColor: AppColors.surfaceTint,
                  child: Row(
                    children: [
                      Container(
                        width: 46,
                        height: 46,
                        decoration: BoxDecoration(
                          color: const Color(0xFFFFF3E4),
                          borderRadius: BorderRadius.circular(AppRadius.md),
                        ),
                        child: const Icon(
                          Icons.workspace_premium_rounded,
                          color: Color(0xFFC98A32),
                        ),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Text(
                          'PulseBrief 会员 · 解锁完整语音简报',
                          style: AppTextStyles.body.copyWith(
                            color: AppColors.textPrimary,
                          ),
                        ),
                      ),
                      Text('升级会员', style: AppTextStyles.label),
                      const Icon(
                        CupertinoIcons.chevron_right,
                        color: AppColors.primary,
                        size: 16,
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: AppSpacing.lg),
                PulseCard(
                  child: Column(
                    children: [
                      _MenuRow(
                        icon: CupertinoIcons.bookmark,
                        title: '我的订阅',
                        trailing: '订阅 ${profile.subscriptionCount} 个主题',
                        onTap: () => Navigator.pushNamed(
                          context,
                          PulseRoutes.subscription,
                        ),
                      ),
                      _MenuRow(
                        icon: CupertinoIcons.star,
                        title: '我的收藏',
                        trailing: '收藏 ${profile.favoriteCount} 条',
                        onTap: () => Navigator.pushNamed(
                          context,
                          PulseRoutes.favoriteArticles,
                        ),
                      ),
                      _MenuRow(
                        icon: CupertinoIcons.clock,
                        title: '阅读历史',
                        trailing: '最近阅读 ${profile.readCount} 条',
                        onTap: () => Navigator.pushNamed(
                          context,
                          PulseRoutes.readHistory,
                        ),
                      ),
                      _MenuRow(
                        icon: CupertinoIcons.play_circle,
                        title: '播放历史',
                        trailing: '最近播放 ${profile.playCount} 次',
                        onTap: () => Navigator.pushNamed(
                          context,
                          PulseRoutes.playbackHistory,
                        ),
                        showDivider: false,
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: AppSpacing.lg),
                PulseCard(
                  child: const Column(
                    children: [
                      _MenuRow(
                        icon: CupertinoIcons.slider_horizontal_3,
                        title: '播报设置',
                      ),
                      _MenuRow(icon: CupertinoIcons.bell, title: '推送设置'),
                      _MenuRow(
                        icon: CupertinoIcons.moon,
                        title: '主题模式',
                        trailing: '跟随系统',
                      ),
                      _MenuRow(
                        icon: CupertinoIcons.archivebox,
                        title: '清除缓存',
                        trailing: '36.8MB',
                      ),
                      _MenuRow(
                        icon: CupertinoIcons.info_circle,
                        title: '关于我们',
                        showDivider: false,
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: AppSpacing.lg),
                PulseCard(
                  borderColor: AppColors.borderBlue,
                  backgroundColor: AppColors.surfaceTint,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Expanded(
                            child: Text(
                              '我的简报偏好',
                              style: AppTextStyles.sectionTitle,
                            ),
                          ),
                          OutlinedButton(
                            onPressed: () => Navigator.pushNamed(
                              context,
                              PulseRoutes.subscription,
                            ),
                            child: const Text('管理订阅'),
                          ),
                        ],
                      ),
                      const SizedBox(height: AppSpacing.md),
                      const Wrap(
                        spacing: 10,
                        runSpacing: 10,
                        children: [
                          CategoryChip(
                            label: '财经',
                            state: CategoryChipState.selected,
                          ),
                          CategoryChip(label: 'AI'),
                          CategoryChip(label: '投行观点'),
                          CategoryChip(label: '科技'),
                        ],
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: AppSpacing.xl),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceAround,
                  children: const [
                    _FooterLink(
                      label: '意见反馈',
                      icon: CupertinoIcons.chat_bubble_text,
                    ),
                    _FooterLink(label: '用户协议'),
                    _FooterLink(label: '隐私政策'),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _ProfileBlock extends StatelessWidget {
  const _ProfileBlock({required this.profile});

  final UserProfile profile;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Container(
          width: 92,
          height: 92,
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            gradient: const LinearGradient(
              colors: [Color(0xFFEAF2FF), Color(0xFFD6E7FF)],
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
            ),
            border: Border.all(color: AppColors.borderBlue),
          ),
          child: const Icon(
            Icons.person_rounded,
            color: AppColors.primary,
            size: 62,
          ),
        ),
        const SizedBox(width: 20),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Flexible(
                    child: Text(profile.name, style: AppTextStyles.hero),
                  ),
                  const SizedBox(width: 8),
                  const Icon(
                    Icons.verified_rounded,
                    color: Color(0xFF3B82F6),
                    size: 24,
                  ),
                ],
              ),
              const SizedBox(height: 8),
              Text(profile.bio, style: AppTextStyles.body),
            ],
          ),
        ),
      ],
    );
  }
}

class _MenuRow extends StatelessWidget {
  const _MenuRow({
    required this.icon,
    required this.title,
    this.trailing,
    this.onTap,
    this.showDivider = true,
  });

  final IconData icon;
  final String title;
  final String? trailing;
  final VoidCallback? onTap;
  final bool showDivider;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 16),
        decoration: BoxDecoration(
          border: showDivider
              ? const Border(bottom: BorderSide(color: AppColors.line))
              : null,
        ),
        child: Row(
          children: [
            Icon(icon, color: AppColors.primary, size: 28),
            const SizedBox(width: 20),
            Expanded(
              child: Text(
                title,
                style: AppTextStyles.sectionTitle.copyWith(fontSize: 19),
              ),
            ),
            if (trailing != null) Text(trailing!, style: AppTextStyles.body),
            const SizedBox(width: 8),
            const Icon(
              CupertinoIcons.chevron_right,
              color: AppColors.textTertiary,
              size: 18,
            ),
          ],
        ),
      ),
    );
  }
}

class _FooterLink extends StatelessWidget {
  const _FooterLink({required this.label, this.icon});

  final String label;
  final IconData? icon;

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        if (icon != null) ...[
          Icon(icon, color: AppColors.textSecondary, size: 18),
          const SizedBox(width: 5),
        ],
        Text(label, style: AppTextStyles.body),
      ],
    );
  }
}
