import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:pulsebrief/core/constants/app_assets.dart';
import 'package:pulsebrief/mock/mock_articles.dart';
import 'package:pulsebrief/shared/models/article.dart';
import 'package:pulsebrief/shared/theme/app_colors.dart';
import 'package:pulsebrief/shared/theme/app_radius.dart';
import 'package:pulsebrief/shared/theme/app_spacing.dart';
import 'package:pulsebrief/shared/theme/app_text_styles.dart';
import 'package:pulsebrief/shared/widgets/app_header.dart';
import 'package:pulsebrief/shared/widgets/pulse_card.dart';
import 'package:pulsebrief/shared/widgets/section_header.dart';

class PlayerPage extends StatefulWidget {
  const PlayerPage({super.key, this.article});

  final Article? article;

  @override
  State<PlayerPage> createState() => _PlayerPageState();
}

class _PlayerPageState extends State<PlayerPage> {
  bool _isPlaying = true;
  bool _favorited = false;
  String _speed = '1.25x';

  Article get _article => widget.article ?? mockArticles.first;

  @override
  Widget build(BuildContext context) {
    final article = _article;

    return Scaffold(
      body: SafeArea(
        child: CustomScrollView(
          slivers: [
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.fromLTRB(20, 8, 20, 18),
                child: Row(
                  children: [
                    const BackSquareButton(),
                    const Expanded(
                      child: Center(
                        child: Text('正在播放', style: AppTextStyles.sectionTitle),
                      ),
                    ),
                    PulseIconButton(
                      icon: CupertinoIcons.list_bullet,
                      onPressed: () {},
                    ),
                  ],
                ),
              ),
            ),
            SliverPadding(
              padding: EdgeInsets.fromLTRB(
                AppSpacing.pagePadding,
                0,
                AppSpacing.pagePadding,
                MediaQuery.paddingOf(context).bottom + AppSpacing.xl,
              ),
              sliver: SliverList.list(
                children: [
                  _CoverCard(),
                  const SizedBox(height: 26),
                  Text(
                    '今日全球早报：美股反弹，AI 与市场热点快速梳理',
                    style: AppTextStyles.hero.copyWith(fontSize: 29),
                  ),
                  const SizedBox(height: 12),
                  Text('脉闻语音简报 · 每日早报', style: AppTextStyles.body),
                  const SizedBox(height: 8),
                  Text('更新于 08:30 · 总时长 08:12', style: AppTextStyles.meta),
                  const SizedBox(height: 22),
                  Slider(
                    value: 3.43,
                    min: 0,
                    max: 8.2,
                    activeColor: AppColors.primary,
                    inactiveColor: AppColors.line,
                    onChanged: (_) {},
                  ),
                  const Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text('03:26', style: AppTextStyles.body),
                      Text('08:12', style: AppTextStyles.body),
                    ],
                  ),
                  const SizedBox(height: 18),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      _TransportButton(
                        icon: Icons.skip_previous_rounded,
                        onTap: () {},
                      ),
                      const SizedBox(width: 32),
                      GestureDetector(
                        onTap: () => setState(() => _isPlaying = !_isPlaying),
                        child: Container(
                          width: 78,
                          height: 78,
                          decoration: BoxDecoration(
                            color: AppColors.primary,
                            shape: BoxShape.circle,
                            boxShadow: [
                              BoxShadow(
                                color: AppColors.primary.withValues(
                                  alpha: 0.32,
                                ),
                                blurRadius: 24,
                                offset: const Offset(0, 12),
                              ),
                            ],
                          ),
                          child: Icon(
                            _isPlaying
                                ? Icons.pause_rounded
                                : Icons.play_arrow_rounded,
                            color: Colors.white,
                            size: 40,
                          ),
                        ),
                      ),
                      const SizedBox(width: 32),
                      _TransportButton(
                        icon: Icons.skip_next_rounded,
                        onTap: () {},
                      ),
                    ],
                  ),
                  const SizedBox(height: 24),
                  _ControlsCard(
                    speed: _speed,
                    favorited: _favorited,
                    onSpeedChanged: (speed) => setState(() => _speed = speed),
                    onFavorite: () => setState(() => _favorited = !_favorited),
                  ),
                  const SizedBox(height: 26),
                  const SectionHeader(title: '当前播报要点'),
                  const SizedBox(height: AppSpacing.md),
                  ..._points(article).asMap().entries.map(
                    (entry) => Padding(
                      padding: const EdgeInsets.only(bottom: AppSpacing.md),
                      child: PulseCard(
                        child: Row(
                          children: [
                            Container(
                              width: 42,
                              height: 42,
                              alignment: Alignment.center,
                              decoration: BoxDecoration(
                                color: AppColors.primarySoft,
                                borderRadius: BorderRadius.circular(
                                  AppRadius.sm,
                                ),
                              ),
                              child: Text(
                                '${entry.key + 1}',
                                style: AppTextStyles.sectionTitle.copyWith(
                                  color: AppColors.primary,
                                ),
                              ),
                            ),
                            const SizedBox(width: 14),
                            Expanded(
                              child: Text(
                                entry.value,
                                style: AppTextStyles.body.copyWith(
                                  color: AppColors.textPrimary,
                                ),
                              ),
                            ),
                            const Icon(
                              CupertinoIcons.chevron_right,
                              color: AppColors.textSecondary,
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  List<String> _points(Article article) {
    if (article.keyPoints.isNotEmpty) return article.keyPoints.take(3).toList();
    return const [
      '英伟达 Blackwell Ultra 将于下半年量产，带动 AI 训练与推理需求。',
      '美联储维持谨慎表态，市场继续预期年内一次降息。',
      '标普 500 与纳指走强，科技股表现领先。',
    ];
  }
}

class _CoverCard extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Container(
      height: 298,
      padding: const EdgeInsets.all(AppSpacing.xl),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(AppRadius.xl),
        border: Border.all(color: AppColors.borderBlue),
        gradient: const LinearGradient(
          colors: [Color(0xFFF8FBFF), Color(0xFFE5F0FF)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
      ),
      child: Stack(
        children: [
          Positioned.fill(
            left: 110,
            right: -26,
            top: -18,
            bottom: -18,
            child: Image.asset(AppAssets.artCleanPlayer, fit: BoxFit.contain),
          ),
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('今日全球早报', style: AppTextStyles.hero.copyWith(fontSize: 34)),
              const SizedBox(height: 10),
              Text('精选 10 条重点资讯', style: AppTextStyles.sectionTitle),
              const Spacer(),
              Container(
                width: 58,
                height: 58,
                decoration: const BoxDecoration(
                  shape: BoxShape.circle,
                  gradient: LinearGradient(
                    colors: [AppColors.primary, Color(0xFF7FB1FF)],
                  ),
                ),
                child: const Icon(Icons.graphic_eq, color: Colors.white),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _TransportButton extends StatelessWidget {
  const _TransportButton({required this.icon, required this.onTap});

  final IconData icon;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      borderRadius: BorderRadius.circular(AppRadius.md),
      onTap: onTap,
      child: Container(
        width: 54,
        height: 54,
        decoration: BoxDecoration(
          color: AppColors.surface,
          borderRadius: BorderRadius.circular(AppRadius.md),
          border: Border.all(color: AppColors.line),
        ),
        child: Icon(icon, color: AppColors.textPrimary),
      ),
    );
  }
}

class _ControlsCard extends StatelessWidget {
  const _ControlsCard({
    required this.speed,
    required this.favorited,
    required this.onSpeedChanged,
    required this.onFavorite,
  });

  final String speed;
  final bool favorited;
  final ValueChanged<String> onSpeedChanged;
  final VoidCallback onFavorite;

  @override
  Widget build(BuildContext context) {
    return PulseCard(
      child: Column(
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Padding(
                padding: const EdgeInsets.only(top: 8),
                child: Text(
                  '倍速',
                  style: AppTextStyles.body.copyWith(
                    color: AppColors.textPrimary,
                  ),
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: ['1.0x', '1.25x', '1.5x']
                      .map(
                        (item) => ChoiceChip(
                          label: Text(item),
                          selected: speed == item,
                          onSelected: (_) => onSpeedChanged(item),
                        ),
                      )
                      .toList(),
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          const Row(
            children: [
              Expanded(
                child: _ControlIcon(icon: CupertinoIcons.clock, label: '定时关闭'),
              ),
              SizedBox(width: 16),
              Expanded(
                child: _ControlIcon(
                  icon: CupertinoIcons.list_bullet,
                  label: '播放列表',
                ),
              ),
            ],
          ),
          const Divider(height: 28),
          Row(
            children: [
              const Expanded(
                child: _ControlIcon(
                  icon: CupertinoIcons.arrow_up_right_square,
                  label: '跳转原文',
                ),
              ),
              Container(width: 1, height: 24, color: AppColors.line),
              Expanded(
                child: GestureDetector(
                  onTap: onFavorite,
                  child: _ControlIcon(
                    icon: favorited
                        ? CupertinoIcons.star_fill
                        : CupertinoIcons.star,
                    label: '收藏',
                    active: favorited,
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _ControlIcon extends StatelessWidget {
  const _ControlIcon({
    required this.icon,
    required this.label,
    this.active = false,
  });

  final IconData icon;
  final String label;
  final bool active;

  @override
  Widget build(BuildContext context) {
    final color = active ? AppColors.primary : AppColors.textPrimary;
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        Icon(icon, color: color),
        const SizedBox(height: 5),
        Text(label, style: AppTextStyles.meta.copyWith(color: color)),
      ],
    );
  }
}
