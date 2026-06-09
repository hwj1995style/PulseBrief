# User History Lists Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build V1 user favorites, read history, and playback history list flows across Spring Boot and Flutter.

**Architecture:** Spring Boot exposes authenticated list APIs backed by existing MySQL tables and reuses article card DTOs for article-based lists. Flutter extends `PulseRepository`, adds API/mock implementations, and routes MinePage entries to focused list pages.

**Tech Stack:** Spring Boot 3, Java 17, Spring Data JPA, MySQL/Flyway, Flutter, Dart tests.

---

## File Structure

- Create: `backend/src/main/java/com/pulsebrief/article/service/ArticleCardMapper.java`
- Modify: `backend/src/main/java/com/pulsebrief/article/service/ArticleQueryService.java`
- Modify: `backend/src/main/java/com/pulsebrief/article/api/ArticleController.java`
- Modify: `backend/src/main/java/com/pulsebrief/favorite/api/FavoriteController.java`
- Modify: `backend/src/main/java/com/pulsebrief/favorite/service/FavoriteService.java`
- Modify: `backend/src/main/java/com/pulsebrief/favorite/service/FavoriteApplicationService.java`
- Modify: `backend/src/main/java/com/pulsebrief/favorite/repository/UserFavoriteRepository.java`
- Create: `backend/src/main/java/com/pulsebrief/readhistory/...`
- Modify: `backend/src/main/java/com/pulsebrief/playback/...`
- Modify: `backend/src/main/java/com/pulsebrief/user/service/UserProfileApplicationService.java`
- Modify: `mobile/lib/shared/repositories/pulse_repository.dart`
- Modify: `mobile/lib/shared/repositories/mock_pulse_repository.dart`
- Modify: `mobile/lib/shared/repositories/api_pulse_repository.dart`
- Create: `mobile/lib/shared/models/playback_history_item.dart`
- Create: `mobile/lib/features/mine/pages/user_article_list_page.dart`
- Create: `mobile/lib/features/mine/pages/playback_history_page.dart`
- Modify: `mobile/lib/features/mine/pages/mine_page.dart`
- Modify: `mobile/lib/app/routes.dart`
- Modify: `mobile/lib/app/app.dart`

## Task 1: Backend Tests

- [ ] Add failing controller tests for `GET /api/user/favorites`, `GET /api/user/read-history`, `POST /api/user/read-history`, and `GET /api/playback/history`.
- [ ] Run targeted backend tests and verify they fail because routes or methods are missing.

## Task 2: Backend Implementation

- [ ] Extract `ArticleCardMapper`.
- [ ] Implement favorite list query sorted by favorite time.
- [ ] Implement read history entity/repository/service/controller.
- [ ] Implement playback history list query.
- [ ] Wire `readCount` into user profile aggregation.
- [ ] Run targeted backend tests and then `.\mvnw.cmd test`.

## Task 3: Flutter Repository Tests

- [ ] Add failing Dart tests for mock/API favorites, read history, playback history, and read-history recording.
- [ ] Run `flutter test test/repository_test.dart` and verify missing methods fail.

## Task 4: Flutter UI Implementation

- [ ] Add repository methods and `PlaybackHistoryItem`.
- [ ] Implement API/mock mapping.
- [ ] Add `UserArticleListPage` and `PlaybackHistoryPage`.
- [ ] Route MinePage entries to the new pages.
- [ ] Add widget coverage for opening the three pages.

## Task 5: Live Verification And Docs

- [ ] Restart Spring Boot after backend changes.
- [ ] Run live API test against `http://localhost:8080/api`.
- [ ] Build API mode Android debug APK.
- [ ] Update `docs/下一阶段任务清单.md`, `docs/Flutter API模式联调记录.md`, `docs/api/V1核心接口设计.md`, and `backend/README.md`.
- [ ] Commit and push.
