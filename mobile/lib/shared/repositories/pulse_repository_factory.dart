import 'package:pulsebrief/shared/data/pulse_api_transport.dart';
import 'package:pulsebrief/shared/repositories/api_pulse_repository.dart';
import 'package:pulsebrief/shared/repositories/mock_pulse_repository.dart';
import 'package:pulsebrief/shared/repositories/pulse_repository.dart';

class PulseRepositoryFactory {
  const PulseRepositoryFactory._();

  static const String _envDataSource = String.fromEnvironment(
    'PULSEBRIEF_DATA_SOURCE',
    defaultValue: 'mock',
  );
  static const String _envApiBaseUrl = String.fromEnvironment(
    'PULSEBRIEF_API_BASE_URL',
    defaultValue: 'http://10.0.2.2:8080/api',
  );

  static PulseRepository create({
    String? dataSource,
    String? apiBaseUrl,
  }) {
    final source = (dataSource ?? _envDataSource).toLowerCase();
    if (source == 'api') {
      return ApiPulseRepository(
        transport: HttpPulseApiTransport(baseUrl: apiBaseUrl ?? _envApiBaseUrl),
      );
    }
    return MockPulseRepository();
  }
}
