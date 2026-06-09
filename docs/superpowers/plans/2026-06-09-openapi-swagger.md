# OpenAPI Swagger Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add online OpenAPI JSON and Swagger UI documentation for PulseBrief V1 backend APIs.

**Architecture:** Spring Boot keeps the existing REST controllers unchanged and adds springdoc as a runtime documentation layer. A small configuration class declares API metadata and the development Bearer Token scheme, while tests verify generated OpenAPI JSON contains the key paths used by Flutter.

**Tech Stack:** Spring Boot 3.5, Java 17, Maven Wrapper, springdoc-openapi 2.8.17, JUnit/MockMvc.

---

## File Structure

- Create: `backend/src/main/java/com/pulsebrief/common/openapi/OpenApiConfig.java`
- Create: `backend/src/test/java/com/pulsebrief/openapi/OpenApiDocumentationTest.java`
- Modify: `backend/pom.xml`
- Modify: `backend/src/main/resources/application.yml`
- Modify: `docs/api/README.md`
- Modify: `backend/README.md`
- Modify: `docs/下一阶段任务清单.md`

## Task 1: Failing OpenAPI Contract Test

- [ ] **Step 1: Create `OpenApiDocumentationTest`**

Create `backend/src/test/java/com/pulsebrief/openapi/OpenApiDocumentationTest.java`:

```java
package com.pulsebrief.openapi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocumentationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void exposesOpenApiDocsForV1Api() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.info.title").value("PulseBrief V1 API"))
                .andExpect(jsonPath("$.paths['/api/health']").exists())
                .andExpect(jsonPath("$.paths['/api/auth/login']").exists())
                .andExpect(jsonPath("$.paths['/api/articles/home']").exists())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"));
    }
}
```

- [ ] **Step 2: Run the targeted backend test and verify failure**

Run:

```powershell
$env:JAVA_HOME='D:\Dev\jdk\jdk-17'
$env:Path='D:\Dev\jdk\jdk-17\bin;' + $env:Path
.\mvnw.cmd -Dtest=OpenApiDocumentationTest test
```

Expected: the test fails with 404 for `/v3/api-docs` because springdoc has not been added yet.

## Task 2: Add springdoc Dependency And Runtime Config

- [ ] **Step 1: Add Maven property and dependency**

In `backend/pom.xml`, add under `<properties>`:

```xml
<springdoc.version>2.8.17</springdoc.version>
```

Add under `<dependencies>` after `spring-boot-starter-web`:

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>${springdoc.version}</version>
</dependency>
```

- [ ] **Step 2: Add OpenAPI application settings**

In `backend/src/main/resources/application.yml`, add:

```yaml
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
    operations-sorter: method
    tags-sorter: alpha
  packages-to-scan: com.pulsebrief
  paths-to-match: /api/**
```

- [ ] **Step 3: Add OpenAPI metadata config**

Create `backend/src/main/java/com/pulsebrief/common/openapi/OpenApiConfig.java`:

```java
package com.pulsebrief.common.openapi;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "PulseBrief V1 API",
                version = "v1.0.0",
                description = "PulseBrief mobile app backend API for global news brief, digest, subscription, and user history flows."
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "Local development")
        },
        tags = {
                @Tag(name = "Health", description = "Service health check"),
                @Tag(name = "Auth", description = "Mock login and guest session"),
                @Tag(name = "Articles", description = "Home feed and article detail"),
                @Tag(name = "Categories", description = "News categories"),
                @Tag(name = "Digests", description = "Daily digest and audio briefing"),
                @Tag(name = "Subscriptions", description = "User subscription settings"),
                @Tag(name = "User", description = "User profile, favorites, and reading history"),
                @Tag(name = "Playback", description = "Audio playback history")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "dev-token"
)
public class OpenApiConfig {
}
```

- [ ] **Step 4: Run targeted backend test and verify pass**

Run:

```powershell
$env:JAVA_HOME='D:\Dev\jdk\jdk-17'
$env:Path='D:\Dev\jdk\jdk-17\bin;' + $env:Path
.\mvnw.cmd -Dtest=OpenApiDocumentationTest test
```

Expected: 1 test passes.

## Task 3: Docs Update And Full Verification

- [ ] **Step 1: Update API docs**

Update `docs/api/README.md` so it says springdoc is now connected and lists both:

```text
http://localhost:8080/swagger-ui.html
http://localhost:8080/v3/api-docs
```

- [ ] **Step 2: Update backend README**

Add a section to `backend/README.md` with the same URLs and the development token:

```text
Authorization: Bearer dev-token-1
```

- [ ] **Step 3: Update next task list**

Update `docs/下一阶段任务清单.md` with this stage marked complete and recommend endpoint annotation polish as the next incremental documentation task.

- [ ] **Step 4: Run full verification**

Run:

```powershell
$env:JAVA_HOME='D:\Dev\jdk\jdk-17'
$env:Path='D:\Dev\jdk\jdk-17\bin;' + $env:Path
.\mvnw.cmd test
```

Run in `mobile/`:

```powershell
$env:JAVA_HOME='D:\Dev\jdk\jdk-17'
$env:ANDROID_HOME='D:\Dev\Android\Sdk'
$env:ANDROID_SDK_ROOT='D:\Dev\Android\Sdk'
$env:ANDROID_USER_HOME='D:\Dev\Android\.android'
$env:ANDROID_AVD_HOME='D:\Dev\Android\Avd'
$env:GRADLE_USER_HOME='D:\Dev\Gradle'
$env:Path='D:\Dev\flutter\bin;D:\Dev\jdk\jdk-17\bin;D:\Dev\Android\Sdk\platform-tools;D:\Dev\Android\Sdk\emulator;D:\Dev\Android\Sdk\cmdline-tools\latest\bin;' + $env:Path
flutter analyze
flutter test
```

Expected: backend tests pass, Flutter analyze reports no issues, Flutter tests pass.

- [ ] **Step 5: Commit and push**

Run:

```powershell
git add backend docs
git commit -m "feat: add openapi documentation"
git push origin main
```
