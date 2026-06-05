class UserProfile {
  const UserProfile({
    required this.name,
    required this.bio,
    required this.subscriptionCount,
    required this.favoriteCount,
    required this.readCount,
    required this.playCount,
  });

  final String name;
  final String bio;
  final int subscriptionCount;
  final int favoriteCount;
  final int readCount;
  final int playCount;
}
