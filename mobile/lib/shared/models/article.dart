class Article {
  const Article({
    required this.id,
    required this.title,
    required this.sourceName,
    required this.publishTime,
    required this.categoryName,
    required this.summary,
    required this.imageAsset,
    required this.duration,
    this.isHot = false,
    this.isBreaking = false,
    this.isFavorited = false,
    this.keyPoints = const [],
    this.impact = '',
  });

  final String id;
  final String title;
  final String sourceName;
  final String publishTime;
  final String categoryName;
  final String summary;
  final String imageAsset;
  final String duration;
  final bool isHot;
  final bool isBreaking;
  final bool isFavorited;
  final List<String> keyPoints;
  final String impact;

  Article copyWith({bool? isFavorited}) {
    return Article(
      id: id,
      title: title,
      sourceName: sourceName,
      publishTime: publishTime,
      categoryName: categoryName,
      summary: summary,
      imageAsset: imageAsset,
      duration: duration,
      isHot: isHot,
      isBreaking: isBreaking,
      isFavorited: isFavorited ?? this.isFavorited,
      keyPoints: keyPoints,
      impact: impact,
    );
  }
}
