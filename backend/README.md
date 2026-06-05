# PulseBrief Backend

Spring Boot 3 backend for PulseBrief.

## Requirements

Use JDK 17 for this project. The repository keeps Java switching local to the current PowerShell session:

```powershell
..\scripts\use-jdk17.ps1
```

## Local Database

The local MySQL container publishes host port `3307` to avoid collisions with existing MySQL services on `3306`.

```powershell
docker compose -f ..\deploy\docker-compose.yml up -d
```

Default connection:

```text
jdbc:mysql://localhost:3307/pulsebrief
user: pulsebrief
password: pulsebrief_dev
```

## Test

```powershell
..\scripts\use-jdk17.ps1
.\mvnw.cmd test
```

## Run

```powershell
..\scripts\use-jdk17.ps1
.\mvnw.cmd spring-boot:run
```
