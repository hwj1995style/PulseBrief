import 'dart:convert';
import 'dart:io';

abstract class PulseApiTransport {
  Future<Map<String, Object?>> getJson(String path, {String? token});

  Future<Map<String, Object?>> postJson(
    String path, {
    Map<String, Object?>? body,
    String? token,
  });

  Future<Map<String, Object?>> putJson(
    String path, {
    Map<String, Object?>? body,
    String? token,
  });

  Future<Map<String, Object?>> deleteJson(String path, {String? token});
}

class HttpPulseApiTransport implements PulseApiTransport {
  HttpPulseApiTransport({required this.baseUrl, HttpClient? client})
    : _client = client ?? HttpClient();

  final String baseUrl;
  final HttpClient _client;

  @override
  Future<Map<String, Object?>> getJson(String path, {String? token}) {
    return _request('GET', path, token: token);
  }

  @override
  Future<Map<String, Object?>> postJson(
    String path, {
    Map<String, Object?>? body,
    String? token,
  }) {
    return _request('POST', path, body: body, token: token);
  }

  @override
  Future<Map<String, Object?>> putJson(
    String path, {
    Map<String, Object?>? body,
    String? token,
  }) {
    return _request('PUT', path, body: body, token: token);
  }

  @override
  Future<Map<String, Object?>> deleteJson(String path, {String? token}) {
    return _request('DELETE', path, token: token);
  }

  Future<Map<String, Object?>> _request(
    String method,
    String path, {
    Map<String, Object?>? body,
    String? token,
  }) async {
    final request = await _client.openUrl(method, Uri.parse('$baseUrl$path'));
    request.headers.contentType = ContentType.json;
    if (token != null && token.isNotEmpty) {
      request.headers.set(HttpHeaders.authorizationHeader, 'Bearer $token');
    }
    if (body != null) {
      request.write(jsonEncode(body));
    }

    final response = await request.close();
    final text = await response.transform(utf8.decoder).join();
    final decoded = jsonDecode(text);
    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw PulseApiException(response.statusCode, text);
    }
    return (decoded as Map).cast<String, Object?>();
  }
}

class PulseApiException implements Exception {
  const PulseApiException(this.statusCode, this.body);

  final int statusCode;
  final String body;

  @override
  String toString() => 'PulseApiException($statusCode): $body';
}
