import 'package:flutter/cupertino.dart';
import 'package:pulsebrief/shared/models/playback_history_item.dart';
import 'package:pulsebrief/shared/repositories/repository_scope.dart';
import 'package:pulsebrief/shared/theme/app_colors.dart';
import 'package:pulsebrief/shared/theme/app_radius.dart';
import 'package:pulsebrief/shared/theme/app_spacing.dart';
import 'package:pulsebrief/shared/theme/app_text_styles.dart';
import 'package:pulsebrief/shared/widgets/app_header.dart';
import 'package:pulsebrief/shared/widgets/empty_state.dart';
import 'package:pulsebrief/shared/widgets/loading_state.dart';
import 'package:pulsebrief/shared/widgets/pulse_card.dart';

class PlaybackHistoryPage extends StatefulWidget {
  const PlaybackHistoryPage({super.key});

  @override
  State<PlaybackHistoryPage> createState() => _PlaybackHistoryPageState();
}

class _PlaybackHistoryPageState extends State<PlaybackHistoryPage> {
  late Future<List<PlaybackHistoryItem>> _historyFuture;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    _historyFuture = RepositoryScope.of(context).getPlaybackHistory();
  }

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      bottom: false,
      child: CustomScrollView(
        slivers: [
          const SliverToBoxAdapter(
            child: AppHeader(
              title: '播放历史',
              subtitle: '最近听过的资讯播报与每日简报',
              leading: BackSquareButton(),
              topPadding: AppSpacing.md,
            ),
          ),
          FutureBuilder<List<PlaybackHistoryItem>>(
            future: _historyFuture,
            builder: (context, snapshot) {
              if (snapshot.connectionState != ConnectionState.done) {
                return const SliverFillRemaining(child: LoadingState());
              }
              if (snapshot.hasError) {
                return const SliverFillRemaining(
                  child: EmptyState(
                    title: '加载失败',
                    message: '暂时无法获取播放历史，请稍后重试。',
                  ),
                );
              }
              final items = snapshot.data ?? const [];
              if (items.isEmpty) {
                return const SliverFillRemaining(
                  child: EmptyState(
                    title: '暂无播放历史',
                    message: '播放资讯或简报后会出现在这里。',
                  ),
                );
              }
              return SliverPadding(
                padding: const EdgeInsets.fromLTRB(
                  AppSpacing.pagePadding,
                  0,
                  AppSpacing.pagePadding,
                  120,
                ),
                sliver: SliverList.separated(
                  itemCount: items.length,
                  separatorBuilder: (_, _) => const SizedBox(height: 12),
                  itemBuilder: (context, index) {
                    return _PlaybackHistoryCard(item: items[index]);
                  },
                ),
              );
            },
          ),
        ],
      ),
    );
  }
}

class _PlaybackHistoryCard extends StatelessWidget {
  const _PlaybackHistoryCard({required this.item});

  final PlaybackHistoryItem item;

  @override
  Widget build(BuildContext context) {
    return PulseCard(
      child: Row(
        children: [
          Container(
            width: 46,
            height: 46,
            decoration: BoxDecoration(
              color: AppColors.primarySoft,
              borderRadius: BorderRadius.circular(AppRadius.md),
              border: Border.all(color: AppColors.borderSoftBlue),
            ),
            child: Icon(
              item.playType == 'DIGEST'
                  ? CupertinoIcons.waveform
                  : CupertinoIcons.play_circle_fill,
              color: AppColors.primary,
              size: 24,
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  item.playTitle,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: AppTextStyles.cardTitle,
                ),
                const SizedBox(height: 6),
                Text(
                  '${item.playTime} · ${item.durationLabel}',
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: AppTextStyles.meta,
                ),
              ],
            ),
          ),
          const SizedBox(width: 8),
          const Icon(
            CupertinoIcons.chevron_right,
            color: AppColors.textMuted,
            size: 20,
          ),
        ],
      ),
    );
  }
}
