import 'package:flutter/widgets.dart';
import 'package:pulsebrief/shared/repositories/pulse_repository.dart';

class RepositoryScope extends InheritedWidget {
  const RepositoryScope({
    super.key,
    required this.repository,
    required super.child,
  });

  final PulseRepository repository;

  static PulseRepository of(BuildContext context) {
    final scope = context.dependOnInheritedWidgetOfExactType<RepositoryScope>();
    assert(scope != null, 'RepositoryScope was not found in the widget tree.');
    return scope!.repository;
  }

  @override
  bool updateShouldNotify(RepositoryScope oldWidget) {
    return repository != oldWidget.repository;
  }
}
