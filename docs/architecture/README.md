# Architecture

PulseBrief is moving to a full-stack architecture:

```text
Flutter mobile app -> Spring Boot API -> MySQL
React admin app ----^
```

The legacy React/Vite user PWA and Node mock API have been removed from the main workspace. Product implementation now proceeds through `mobile/`, `backend/`, `admin/`, and `deploy/`.
