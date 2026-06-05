# Full Stack Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Establish the PulseBrief full-stack foundation for Flutter + Spring Boot + MySQL while preserving the existing PWA prototype.

**Architecture:** The repository becomes a single workspace with `backend/` for Spring Boot 3 APIs, `mobile/` for Flutter, `admin/` reserved for the React admin app, `deploy/` for MySQL Docker Compose, and `scripts/` for local toolchain switching. Backend uses Java 17 via local scripts and Maven Wrapper instead of global Maven.

**Tech Stack:** Java 17, Spring Boot 3, Maven Wrapper, MySQL 8, Flyway, Flutter, Docker Compose, PowerShell.

---

## File Structure

Create:

- `scripts/use-jdk17.ps1`: Sets `JAVA_HOME` and `PATH` for the current shell only.
- `scripts/use-flutter.ps1`: Adds Flutter SDK to the current shell `PATH`.
- `scripts/check-env.ps1`: Reports Java, Maven Wrapper, Flutter, Docker, and Node status.
- `deploy/docker-compose.yml`: Runs MySQL 8 for local development.
- `deploy/mysql/init/README.md`: Documents how database initialization is handled by Flyway.
- `backend/pom.xml`: Spring Boot 3 Maven project definition.
- `backend/mvnw.cmd`, `backend/mvnw`, `backend/.mvn/wrapper/maven-wrapper.properties`: Maven Wrapper.
- `backend/src/main/java/com/pulsebrief/PulseBriefApplication.java`: Backend entrypoint.
- `backend/src/main/java/com/pulsebrief/health/HealthController.java`: Minimal API health endpoint.
- `backend/src/main/resources/application.yml`: Local backend config.
- `backend/src/main/resources/db/migration/V1__init_schema.sql`: Initial schema subset.
- `backend/src/test/java/com/pulsebrief/health/HealthControllerTest.java`: Controller smoke test.
- `mobile/README.md`: Flutter app setup placeholder if Flutter SDK is unavailable.
- `admin/README.md`: React admin setup placeholder.
- `docs/api/README.md`: API documentation entrypoint.
- `docs/sql/README.md`: SQL and migration documentation entrypoint.
- `docs/architecture/README.md`: Architecture documentation entrypoint.

Modify:

- `README.md`: Add concrete phase-one commands once files exist.
- `.gitignore`: Add Java, Flutter, Docker, IDE, and build outputs.

## Task 1: Environment Discovery

- [ ] **Step 1: Check current tools**

Run:

```powershell
java -version
Get-Command flutter -ErrorAction SilentlyContinue
Get-Command mvn -ErrorAction SilentlyContinue
Get-Command docker -ErrorAction SilentlyContinue
Get-Command winget -ErrorAction SilentlyContinue
Get-Command choco -ErrorAction SilentlyContinue
```

Expected: Java 8 is present, Flutter may be missing, global Maven may be missing, Docker may be missing or present.

- [ ] **Step 2: Locate installed Maven and candidate JDKs**

Run:

```powershell
$paths=@('D:\App','C:\Program Files','C:\ProgramData','C:\Users\11638')
Get-ChildItem -Path $paths -Recurse -Filter mvn.cmd -ErrorAction SilentlyContinue | Select-Object -First 20 FullName
Get-ChildItem -Path $paths -Directory -Recurse -ErrorAction SilentlyContinue | Where-Object { $_.Name -match 'jdk-17|jdk17|temurin|java-17' } | Select-Object -First 20 FullName
```

Expected: IntelliJ Maven may be found; JDK 17 may need installation.

## Task 2: Environment Scripts

- [ ] **Step 1: Create `scripts/use-jdk17.ps1`**

```powershell
param(
    [string]$JdkHome = "D:\Dev\jdk\jdk-17"
)

if (-not (Test-Path -LiteralPath $JdkHome)) {
    Write-Error "JDK 17 not found at $JdkHome. Install JDK 17 or pass -JdkHome <path>."
    exit 1
}

$env:JAVA_HOME = $JdkHome
$env:Path = "$JdkHome\bin;$env:Path"
java -version
```

- [ ] **Step 2: Create `scripts/use-flutter.ps1`**

```powershell
param(
    [string]$FlutterHome = "D:\Dev\flutter"
)

$flutterBin = Join-Path $FlutterHome "bin"
if (-not (Test-Path -LiteralPath $flutterBin)) {
    Write-Error "Flutter SDK not found at $FlutterHome. Install Flutter or pass -FlutterHome <path>."
    exit 1
}

$env:Path = "$flutterBin;$env:Path"
flutter --version
```

- [ ] **Step 3: Create `scripts/check-env.ps1`**

```powershell
Write-Host "PulseBrief environment check"
Write-Host "Java:"
java -version
Write-Host "Flutter:"
if (Get-Command flutter -ErrorAction SilentlyContinue) { flutter --version } else { Write-Host "flutter not found" }
Write-Host "Docker:"
if (Get-Command docker -ErrorAction SilentlyContinue) { docker --version } else { Write-Host "docker not found" }
Write-Host "Node:"
node --version
Write-Host "NPM:"
npm --version
```

- [ ] **Step 4: Run script smoke check**

Run:

```powershell
.\scripts\check-env.ps1
```

Expected: It reports tool availability without changing global environment.

## Task 3: Docker Compose MySQL

- [ ] **Step 1: Create `deploy/docker-compose.yml`**

```yaml
services:
  mysql:
    image: mysql:8.4
    container_name: pulsebrief-mysql
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: pulsebrief_root
      MYSQL_DATABASE: pulsebrief
      MYSQL_USER: pulsebrief
      MYSQL_PASSWORD: pulsebrief_dev
      TZ: Asia/Shanghai
    ports:
      - "3307:3306"
    volumes:
      - pulsebrief_mysql_data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-upulsebrief", "-ppulsebrief_dev"]
      interval: 10s
      timeout: 5s
      retries: 10

volumes:
  pulsebrief_mysql_data:
```

- [ ] **Step 2: Create `deploy/mysql/init/README.md`**

```markdown
# MySQL Init

PulseBrief uses Flyway migrations from `backend/src/main/resources/db/migration`.
This folder is reserved for local-only database notes and should not contain authoritative schema files.
```

- [ ] **Step 3: Validate Compose syntax if Docker is available**

Run:

```powershell
docker compose -f deploy/docker-compose.yml config
```

Expected: Compose renders normalized config. If Docker is missing, document the blocker.

## Task 4: Spring Boot Backend Skeleton

- [ ] **Step 1: Create backend project files**

Create `backend/pom.xml` with Spring Boot web, validation, data-jpa, flyway, mysql connector, springdoc, and test dependencies.

- [ ] **Step 2: Write failing health controller test**

Create `backend/src/test/java/com/pulsebrief/health/HealthControllerTest.java`:

```java
package com.pulsebrief.health;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthController.class)
class HealthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsApiHealthStatus() throws Exception {
        mockMvc.perform(get("/api/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.service").value("pulsebrief-backend"));
    }
}
```

- [ ] **Step 3: Run test and verify RED**

Run:

```powershell
.\scripts\use-jdk17.ps1
cd backend
.\mvnw.cmd -Dtest=HealthControllerTest test
```

Expected: Fails because `HealthController` does not exist.

- [ ] **Step 4: Implement minimal backend classes**

Create:

```java
package com.pulsebrief;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PulseBriefApplication {
    public static void main(String[] args) {
        SpringApplication.run(PulseBriefApplication.class, args);
    }
}
```

Create:

```java
package com.pulsebrief.health;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "pulsebrief-backend");
    }
}
```

- [ ] **Step 5: Run test and verify GREEN**

Run:

```powershell
cd backend
.\mvnw.cmd -Dtest=HealthControllerTest test
```

Expected: `HealthControllerTest` passes.

## Task 5: Initial Schema

- [ ] **Step 1: Create Flyway migration**

Create `backend/src/main/resources/db/migration/V1__init_schema.sql` with the first ten product tables from the product design document.

- [ ] **Step 2: Add schema documentation**

Create `docs/sql/README.md` explaining that schema lives in Flyway migrations and product comments should stay aligned with table comments.

- [ ] **Step 3: Run backend tests**

Run:

```powershell
cd backend
.\mvnw.cmd test
```

Expected: Tests pass. Database integration is verified in the next phase once MySQL is running.

## Task 6: Flutter Mobile Foundation

- [ ] **Step 1: Install or detect Flutter**

Run:

```powershell
Get-Command flutter -ErrorAction SilentlyContinue
```

Expected: If missing, install Flutter SDK to `D:\Dev\flutter` before running `flutter create`.

- [ ] **Step 2: Create Flutter app when Flutter is available**

Run:

```powershell
flutter create --org com.pulsebrief --project-name pulsebrief mobile
cd mobile
flutter test
```

Expected: Flutter creates a runnable starter app and tests pass.

- [ ] **Step 3: Create placeholder when Flutter is unavailable**

Create `mobile/README.md`:

```markdown
# PulseBrief Mobile

Flutter SDK is required to create and run the mobile app.

Recommended install path: `D:\Dev\flutter`

After installing Flutter:

```powershell
.\scripts\use-flutter.ps1
flutter create --org com.pulsebrief --project-name pulsebrief mobile
cd mobile
flutter test
```
```

## Task 7: Documentation And Root README

- [ ] **Step 1: Create docs entrypoints**

Create:

- `docs/api/README.md`
- `docs/architecture/README.md`
- `admin/README.md`

- [ ] **Step 2: Update `README.md`**

Add phase-one concrete commands:

```powershell
.\scripts\check-env.ps1
.\scripts\use-jdk17.ps1
docker compose -f deploy/docker-compose.yml up -d
cd backend
.\mvnw.cmd test
```

- [ ] **Step 3: Run repository verification**

Run:

```powershell
npm test
npm run build
```

Expected: Existing PWA checks still pass.

## Task 8: Commit And Push

- [ ] **Step 1: Inspect changes**

Run:

```powershell
git status --short --branch
```

- [ ] **Step 2: Commit**

Run:

```powershell
git add scripts deploy backend mobile admin docs README.md .gitignore
git commit -m "feat: add full stack foundation"
```

- [ ] **Step 3: Push**

Run:

```powershell
git push
```

Expected: GitHub `main` contains the phase-one foundation.

## Self-Review

Spec coverage:

1. Multi-JDK strategy is covered by scripts.
2. Spring Boot foundation is covered by backend skeleton.
3. MySQL local development is covered by Docker Compose.
4. Flutter foundation is covered by detection and create-or-placeholder path.
5. Existing PWA preservation is covered by root verification.

Placeholders:

1. Flutter placeholder is intentional only if Flutter SDK is unavailable.
2. Admin placeholder is intentional because admin implementation starts in a later phase.

Type consistency:

1. Backend package is consistently `com.pulsebrief`.
2. Health endpoint is consistently `/api/health`.
