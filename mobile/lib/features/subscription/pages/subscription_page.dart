import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:pulsebrief/app/routes.dart';
import 'package:pulsebrief/core/constants/app_assets.dart';
import 'package:pulsebrief/mock/mock_subscriptions.dart';
import 'package:pulsebrief/shared/models/subscription_topic.dart';
import 'package:pulsebrief/shared/repositories/pulse_repository.dart';
import 'package:pulsebrief/shared/repositories/repository_scope.dart';
import 'package:pulsebrief/shared/theme/app_colors.dart';
import 'package:pulsebrief/shared/theme/app_radius.dart';
import 'package:pulsebrief/shared/theme/app_spacing.dart';
import 'package:pulsebrief/shared/theme/app_text_styles.dart';
import 'package:pulsebrief/shared/widgets/app_header.dart';
import 'package:pulsebrief/shared/widgets/category_chip.dart';
import 'package:pulsebrief/shared/widgets/empty_state.dart';
import 'package:pulsebrief/shared/widgets/loading_state.dart';
import 'package:pulsebrief/shared/widgets/pulse_bottom_nav.dart';
import 'package:pulsebrief/shared/widgets/pulse_card.dart';

class SubscriptionPage extends StatefulWidget {
  const SubscriptionPage({super.key});

  @override
  State<SubscriptionPage> createState() => _SubscriptionPageState();
}

class _SubscriptionPageState extends State<SubscriptionPage> {
  bool _loaded = false;
  bool _isLoading = true;
  String? _errorMessage;
  int _todayMatchedCount = 0;
  List<SubscriptionTopic> _topics = const [];
  late List<SubscriptionTopic> _channels;
  final Map<String, bool> _pushPrefs = {
    '每日早报推送': true,
    '晚间复盘推送': true,
    '突发热点推送': false,
    '投行观点推送': false,
  };

  @override
  void initState() {
    super.initState();
    _channels = List<SubscriptionTopic>.from(focusChannels);
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (!_loaded) {
      _loaded = true;
      _loadSubscriptions();
    }
  }

  int get _selectedCount => _topics.where((topic) => topic.selected).length;

  List<SubscriptionTopic> get _selectedTopics {
    return _topics.where((topic) => topic.selected).toList();
  }

  Future<void> _loadSubscriptions() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });
    try {
      final snapshot = await RepositoryScope.of(context).getSubscriptions();
      if (!mounted) return;
      setState(() {
        _applySnapshot(snapshot);
        _isLoading = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _errorMessage = '订阅内容加载失败，请稍后重试';
        _isLoading = false;
      });
    }
  }

  void _applySnapshot(SubscriptionSnapshot snapshot) {
    _topics = snapshot.topics;
    _todayMatchedCount = snapshot.todayMatchedCount;
    _pushPrefs['每日早报推送'] = snapshot.preferences.morningDigest;
    _pushPrefs['晚间复盘推送'] = snapshot.preferences.eveningReview;
    _pushPrefs['突发热点推送'] = snapshot.preferences.breakingNews;
    _pushPrefs['投行观点推送'] = snapshot.preferences.investmentView;
  }

  void _toggleTopic(int index) {
    setState(() {
      final topic = _topics[index];
      _topics[index] = topic.copyWith(selected: !topic.selected);
    });
  }

  void _toggleChannel(int index) {
    setState(() {
      final channel = _channels[index];
      _channels[index] = channel.copyWith(selected: !channel.selected);
    });
  }

  Future<void> _save() async {
    final preferences = PushPreferences(
      morningDigest: _pushPrefs['每日早报推送'] ?? true,
      eveningReview: _pushPrefs['晚间复盘推送'] ?? true,
      breakingNews: _pushPrefs['突发热点推送'] ?? false,
      investmentView: _pushPrefs['投行观点推送'] ?? false,
    );
    final selectedCodes = _selectedTopics.map((topic) {
      return _codeForTopicName(topic.name);
    }).toList();
    final snapshot = await RepositoryScope.of(
      context,
    ).saveSubscriptions(categoryCodes: selectedCodes, preferences: preferences);
    if (!mounted) return;
    setState(() => _applySnapshot(snapshot));
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(const SnackBar(content: Text('订阅已保存')));
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
      '中美动态' => 'china_us',
      '半导体' => 'semiconductor',
      '新能源' => 'new_energy',
      '数字资产' => 'digital_asset',
      _ => name,
    };
  }

  @override
  Widget build(BuildContext context) {
    final bottom = MediaQuery.paddingOf(context).bottom;

    if (_isLoading) {
      return const Scaffold(body: SafeArea(child: LoadingState()));
    }
    if (_errorMessage != null) {
      return Scaffold(
        body: SafeArea(
          child: EmptyState(title: '加载失败', message: _errorMessage!),
        ),
      );
    }

    return Scaffold(
      bottomNavigationBar: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(20, 10, 20, 8),
            child: SizedBox(
              width: double.infinity,
              child: FilledButton(
                onPressed: _save,
                style: FilledButton.styleFrom(
                  padding: const EdgeInsets.symmetric(vertical: 16),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(AppRadius.md),
                  ),
                ),
                child: const Text('保存订阅'),
              ),
            ),
          ),
          Text('你可以随时在我的页面中调整订阅', style: AppTextStyles.meta),
          PulseBottomNav(
            currentIndex: 3,
            onTap: (index) {
              Navigator.pushNamedAndRemoveUntil(
                context,
                PulseRoutes.main,
                (route) => false,
                arguments: index,
              );
            },
          ),
        ],
      ),
      body: SafeArea(
        bottom: false,
        child: CustomScrollView(
          slivers: [
            SliverToBoxAdapter(
              child: AppHeader(
                title: '我的订阅',
                subtitle: '选择你关注的全球热点与市场动态',
                centerTitle: true,
                leading: const BackSquareButton(),
                actions: [
                  TextButton(onPressed: _save, child: const Text('保存')),
                ],
              ),
            ),
            SliverPadding(
              padding: EdgeInsets.fromLTRB(20, 0, 20, 24 + bottom),
              sliver: SliverList.list(
                children: [
                  _OverviewCard(
                    selectedCount: _selectedCount,
                    selectedTopics: _selectedTopics,
                    todayMatchedCount: _todayMatchedCount,
                  ),
                  const SizedBox(height: AppSpacing.lg),
                  PulseCard(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text('推荐订阅', style: AppTextStyles.sectionTitle),
                        const SizedBox(height: AppSpacing.md),
                        LayoutBuilder(
                          builder: (context, constraints) {
                            final itemWidth =
                                (constraints.maxWidth - AppSpacing.md * 2) / 3;
                            return Wrap(
                              spacing: AppSpacing.md,
                              runSpacing: AppSpacing.md,
                              children: List.generate(_topics.length, (index) {
                                final topic = _topics[index];
                                return SizedBox(
                                  width: itemWidth,
                                  child: CategoryChip(
                                    label: topic.name,
                                    state: topic.selected
                                        ? CategoryChipState.selected
                                        : CategoryChipState.normal,
                                    trailing: topic.selected
                                        ? const Icon(
                                            Icons.check,
                                            color: Colors.white,
                                            size: 16,
                                          )
                                        : null,
                                    onTap: () => _toggleTopic(index),
                                  ),
                                );
                              }),
                            );
                          },
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: AppSpacing.lg),
                  PulseCard(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text('重点频道', style: AppTextStyles.sectionTitle),
                        const SizedBox(height: AppSpacing.md),
                        GridView.builder(
                          shrinkWrap: true,
                          physics: const NeverScrollableScrollPhysics(),
                          itemCount: _channels.length,
                          gridDelegate:
                              const SliverGridDelegateWithFixedCrossAxisCount(
                                crossAxisCount: 2,
                                childAspectRatio: 1.18,
                                crossAxisSpacing: 12,
                                mainAxisSpacing: 12,
                              ),
                          itemBuilder: (context, index) {
                            final channel = _channels[index];
                            return _ChannelCard(
                              topic: channel,
                              onChanged: (_) => _toggleChannel(index),
                            );
                          },
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: AppSpacing.lg),
                  PulseCard(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text('推送偏好', style: AppTextStyles.sectionTitle),
                        const SizedBox(height: AppSpacing.md),
                        ..._pushPrefs.entries.map(
                          (entry) => _PreferenceRow(
                            label: entry.key,
                            value: entry.value,
                            onChanged: (value) =>
                                setState(() => _pushPrefs[entry.key] = value),
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 18),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _OverviewCard extends StatelessWidget {
  const _OverviewCard({
    required this.selectedCount,
    required this.selectedTopics,
    required this.todayMatchedCount,
  });

  final int selectedCount;
  final List<SubscriptionTopic> selectedTopics;
  final int todayMatchedCount;

  @override
  Widget build(BuildContext context) {
    return PulseCard(
      borderColor: AppColors.borderBlue,
      backgroundColor: AppColors.surfaceTint,
      child: Stack(
        children: [
          Positioned(
            right: 0,
            top: -26,
            bottom: -20,
            child: Opacity(
              opacity: 0.78,
              child: Image.asset(AppAssets.artCleanSubscription, width: 196),
            ),
          ),
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('已订阅 $selectedCount 个主题', style: AppTextStyles.title),
              const SizedBox(height: 10),
              Text('系统将根据你的订阅生成首页内容和每日简报', style: AppTextStyles.body),
              const SizedBox(height: 18),
              Wrap(
                spacing: 10,
                runSpacing: 10,
                children: selectedTopics
                    .take(5)
                    .map(
                      (topic) => CategoryChip(
                        label: topic.name,
                        state: CategoryChipState.selected,
                      ),
                    )
                    .toList(),
              ),
              const SizedBox(height: 14),
              Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 12,
                  vertical: 10,
                ),
                decoration: BoxDecoration(
                  color: Colors.white.withValues(alpha: 0.72),
                  borderRadius: BorderRadius.circular(AppRadius.md),
                ),
                child: Text(
                  '今日已为你筛选 $todayMatchedCount 条重点资讯',
                  style: AppTextStyles.body.copyWith(
                    color: AppColors.textPrimary,
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

class _ChannelCard extends StatelessWidget {
  const _ChannelCard({required this.topic, required this.onChanged});

  final SubscriptionTopic topic;
  final ValueChanged<bool> onChanged;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(AppRadius.lg),
        border: Border.all(color: AppColors.line),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(
                topic.name.contains('AI')
                    ? Icons.smart_toy_outlined
                    : topic.name.contains('财经')
                    ? CupertinoIcons.clock
                    : Icons.account_balance_rounded,
                color: AppColors.primary,
              ),
              const Spacer(),
              Transform.scale(
                scale: 0.82,
                alignment: Alignment.centerRight,
                child: CupertinoSwitch(
                  value: topic.selected,
                  activeTrackColor: AppColors.primary,
                  onChanged: onChanged,
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          Text(topic.name, style: AppTextStyles.cardTitle),
          const SizedBox(height: 4),
          Text(
            topic.description,
            maxLines: 2,
            overflow: TextOverflow.ellipsis,
            style: AppTextStyles.meta,
          ),
        ],
      ),
    );
  }
}

class _PreferenceRow extends StatelessWidget {
  const _PreferenceRow({
    required this.label,
    required this.value,
    required this.onChanged,
  });

  final String label;
  final bool value;
  final ValueChanged<bool> onChanged;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 10),
      decoration: const BoxDecoration(
        border: Border(bottom: BorderSide(color: AppColors.line)),
      ),
      child: Row(
        children: [
          const Icon(CupertinoIcons.bell, color: AppColors.primary),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              label,
              style: AppTextStyles.body.copyWith(color: AppColors.textPrimary),
            ),
          ),
          CupertinoSwitch(
            value: value,
            activeTrackColor: AppColors.primary,
            onChanged: onChanged,
          ),
        ],
      ),
    );
  }
}
