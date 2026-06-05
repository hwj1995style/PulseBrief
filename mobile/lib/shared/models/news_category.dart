class NewsCategory {
  const NewsCategory({
    required this.code,
    required this.name,
    required this.description,
    required this.todayCount,
  });

  final String code;
  final String name;
  final String description;
  final int todayCount;
}
