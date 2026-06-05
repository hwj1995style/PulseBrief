import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:pulsebrief/shared/models/article.dart';
import 'package:pulsebrief/shared/theme/app_colors.dart';
import 'package:pulsebrief/shared/theme/app_radius.dart';
import 'package:pulsebrief/shared/theme/app_spacing.dart';
import 'package:pulsebrief/shared/theme/app_text_styles.dart';
import 'package:pulsebrief/shared/widgets/pulse_card.dart';
import 'package:pulsebrief/shared/widgets/source_badge.dart';

class NewsCard extends StatelessWidget {
  const NewsCard({
    super.key,
    required this.article,
    this.compact = false,
    this.onTap,
    this.onPlay,
    this.onFavorite,
  });

  final Article article;
  final bool compact;
  final VoidCallback? onTap;
  final VoidCallback? onPlay;
  final VoidCallback? onFavorite;

  @override
  Widget build(BuildContext context) {
    return PulseCard(
      onTap: onTap,
      padding: const EdgeInsets.all(AppSpacing.md),
      child: compact
          ? _CompactArticle(
              article: article,
              onPlay: onPlay,
              onFavorite: onFavorite,
            )
          : _LargeArticle(
              article: article,
              onPlay: onPlay,
              onFavorite: onFavorite,
            ),
    );
  }
}

class _LargeArticle extends StatelessWidget {
  const _LargeArticle({required this.article, this.onPlay, this.onFavorite});

  final Article article;
  final VoidCallback? onPlay;
  final VoidCallback? onFavorite;

  @override
  Widget build(BuildContext context) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _ArticleImage(article: article, width: 102, height: 96),
        const SizedBox(width: 14),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Expanded(
                    child: Text(
                      article.title,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: AppTextStyles.cardTitle,
                    ),
                  ),
                  const SizedBox(width: 8),
                  SourceBadge(
                    label: article.categoryName,
                    hot: article.isHot,
                    breaking: article.isBreaking,
                  ),
                ],
              ),
              const SizedBox(height: 7),
              Text(
                '${article.sourceName} · ${article.publishTime} · ${article.categoryName}',
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: AppTextStyles.meta,
              ),
              const SizedBox(height: 8),
              Text(
                article.summary,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: AppTextStyles.body,
              ),
              const SizedBox(height: 10),
              Row(
                children: [
                  _PlayPill(duration: article.duration, onTap: onPlay),
                  const Spacer(),
                  IconButton(
                    onPressed: onFavorite,
                    icon: Icon(
                      article.isFavorited
                          ? CupertinoIcons.bookmark_fill
                          : CupertinoIcons.bookmark,
                      color: article.isFavorited
                          ? AppColors.primary
                          : AppColors.textPrimary,
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _CompactArticle extends StatelessWidget {
  const _CompactArticle({required this.article, this.onPlay, this.onFavorite});

  final Article article;
  final VoidCallback? onPlay;
  final VoidCallback? onFavorite;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _ArticleImage(article: article, width: double.infinity, height: 140),
        const SizedBox(height: 10),
        Text(
          article.title,
          maxLines: 2,
          overflow: TextOverflow.ellipsis,
          style: AppTextStyles.cardTitle,
        ),
        const SizedBox(height: 6),
        Text(
          '${article.sourceName} · ${article.publishTime}',
          style: AppTextStyles.meta,
        ),
        const SizedBox(height: 7),
        Text(
          article.summary,
          maxLines: 2,
          overflow: TextOverflow.ellipsis,
          style: AppTextStyles.body,
        ),
        const SizedBox(height: 10),
        Row(
          children: [
            _PlayPill(duration: article.duration, onTap: onPlay),
            const Spacer(),
            IconButton(
              onPressed: onFavorite,
              icon: Icon(
                article.isFavorited
                    ? CupertinoIcons.bookmark_fill
                    : CupertinoIcons.bookmark,
                color: article.isFavorited
                    ? AppColors.primary
                    : AppColors.textPrimary,
              ),
            ),
          ],
        ),
      ],
    );
  }
}

class _ArticleImage extends StatelessWidget {
  const _ArticleImage({
    required this.article,
    required this.width,
    required this.height,
  });

  final Article article;
  final double width;
  final double height;

  @override
  Widget build(BuildContext context) {
    return ClipRRect(
      borderRadius: BorderRadius.circular(AppRadius.md),
      child: Image.asset(
        article.imageAsset,
        width: width,
        height: height,
        fit: BoxFit.cover,
      ),
    );
  }
}

class _PlayPill extends StatelessWidget {
  const _PlayPill({required this.duration, this.onTap});

  final String duration;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(AppRadius.pill),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 7),
        decoration: BoxDecoration(
          color: AppColors.primarySoft,
          borderRadius: BorderRadius.circular(AppRadius.pill),
          border: Border.all(color: AppColors.borderBlue),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(
              Icons.play_circle_fill,
              size: 20,
              color: AppColors.primary,
            ),
            const SizedBox(width: 5),
            Text('播放 $duration', style: AppTextStyles.label),
          ],
        ),
      ),
    );
  }
}
