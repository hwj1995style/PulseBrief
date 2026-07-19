[CmdletBinding()]
param(
    [string] $BuildName = "",
    [int] $BuildNumber = 0,
    [ValidateSet("mock", "api")]
    [string] $DataSource = "mock",
    [string] $ApiBaseUrl = "http://10.0.2.2:8080/api"
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$repoRoot = Split-Path -Parent $PSScriptRoot
$mobileRoot = Join-Path $repoRoot "mobile"
$propertiesPath = Join-Path $mobileRoot "android\key.properties"
if (-not (Test-Path -LiteralPath $propertiesPath)) {
    & "$PSScriptRoot\setup-android-self-signing.ps1"
}

$env:JAVA_HOME = "D:\Dev\jdk\jdk-17"
$env:ANDROID_HOME = "D:\Dev\Android\Sdk"
$env:ANDROID_SDK_ROOT = "D:\Dev\Android\Sdk"
$env:ANDROID_USER_HOME = "D:\Dev\Android\.android"
$env:GRADLE_USER_HOME = "D:\Dev\Gradle"
$env:Path = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:ANDROID_HOME\cmdline-tools\latest\bin;$env:Path"

if ([string]::IsNullOrWhiteSpace($BuildName)) {
    $versionLine = Get-Content -LiteralPath (Join-Path $mobileRoot "pubspec.yaml") |
        Where-Object { $_ -match '^version:\s*([^+\s]+)' } |
        Select-Object -First 1
    if ($null -eq $versionLine -or $versionLine -notmatch '^version:\s*([^+\s]+)') {
        throw "Unable to read version from mobile/pubspec.yaml"
    }
    $BuildName = $Matches[1]
}
if ($BuildName -notmatch '^\d+\.\d+\.\d+$') { throw "BuildName must use semantic version format, for example 1.0.0" }
if ($BuildNumber -le 0) {
    $BuildNumber = [int][Math]::Floor(((Get-Date).ToUniversalTime() - [DateTime]::new(2020, 1, 1)).TotalMinutes)
}

. "$PSScriptRoot\use-flutter.ps1"
Push-Location $mobileRoot
try {
    $arguments = @(
        "build", "apk", "--release",
        "--build-name=$BuildName",
        "--build-number=$BuildNumber",
        "--dart-define=PULSEBRIEF_DATA_SOURCE=$DataSource"
    )
    if ($DataSource -eq "api") { $arguments += "--dart-define=PULSEBRIEF_API_BASE_URL=$ApiBaseUrl" }
    & flutter.bat @arguments
    if ($LASTEXITCODE -ne 0) { throw "Flutter release build failed with exit code $LASTEXITCODE" }
}
finally { Pop-Location }

$sourceApk = Join-Path $mobileRoot "build\app\outputs\flutter-apk\app-release.apk"
if (-not (Test-Path -LiteralPath $sourceApk)) { throw "Release APK was not produced: $sourceApk" }
$artifactRoot = Join-Path $repoRoot "artifacts\mobile"
New-Item -ItemType Directory -Force -Path $artifactRoot | Out-Null
$artifactName = "PulseBrief-$BuildName-$BuildNumber-$DataSource-release.apk"
$artifactPath = Join-Path $artifactRoot $artifactName
Copy-Item -LiteralPath $sourceApk -Destination $artifactPath -Force
$hash = (Get-FileHash -LiteralPath $artifactPath -Algorithm SHA256).Hash.ToLowerInvariant()
[IO.File]::WriteAllText("$artifactPath.sha256", "$hash  $artifactName`n", [Text.UTF8Encoding]::new($false))
$metadata = [ordered]@{
    app = "PulseBrief"
    buildName = $BuildName
    buildNumber = $BuildNumber
    dataSource = $DataSource
    apiBaseUrl = if ($DataSource -eq "api") { $ApiBaseUrl } else { $null }
    sha256 = $hash
    builtAtUtc = (Get-Date).ToUniversalTime().ToString("o")
    gitCommit = (& git -C $repoRoot rev-parse HEAD).Trim()
}
[IO.File]::WriteAllText("$artifactPath.json", ($metadata | ConvertTo-Json), [Text.UTF8Encoding]::new($false))

Write-Host "Self-use Android release created:"
Write-Host $artifactPath
Write-Host "SHA-256: $hash"
