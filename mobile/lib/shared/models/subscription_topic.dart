class SubscriptionTopic {
  const SubscriptionTopic({
    required this.name,
    required this.description,
    this.selected = false,
  });

  final String name;
  final String description;
  final bool selected;

  SubscriptionTopic copyWith({bool? selected}) {
    return SubscriptionTopic(
      name: name,
      description: description,
      selected: selected ?? this.selected,
    );
  }
}
