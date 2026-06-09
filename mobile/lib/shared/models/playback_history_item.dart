class PlaybackHistoryItem {
  const PlaybackHistoryItem({
    required this.id,
    required this.playType,
    required this.articleId,
    required this.digestId,
    required this.playTitle,
    required this.playTime,
    required this.durationSeconds,
  });

  final String id;
  final String playType;
  final String? articleId;
  final String? digestId;
  final String playTitle;
  final String playTime;
  final int durationSeconds;

  String get durationLabel {
    final minutes = durationSeconds ~/ 60;
    final seconds = durationSeconds % 60;
    return '${minutes.toString().padLeft(2, '0')}:${seconds.toString().padLeft(2, '0')}';
  }
}
