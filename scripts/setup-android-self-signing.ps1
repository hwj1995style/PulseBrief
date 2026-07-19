[CmdletBinding()]
param(
    [string] $Alias = "pulsebrief-self-use",
    [string] $KeystorePath = ""
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$repoRoot = Split-Path -Parent $PSScriptRoot
$secretRoot = Join-Path $repoRoot ".local-secrets\android"
$propertiesPath = Join-Path $repoRoot "mobile\android\key.properties"
if ([string]::IsNullOrWhiteSpace($KeystorePath)) {
    $KeystorePath = Join-Path $secretRoot "pulsebrief-self-use.jks"
}
$KeystorePath = [IO.Path]::GetFullPath($KeystorePath)

if ((Test-Path -LiteralPath $KeystorePath) -and (Test-Path -LiteralPath $propertiesPath)) {
    Write-Host "Android self-use signing is already configured."
    Write-Host "Keystore: $KeystorePath"
    exit 0
}
if ((Test-Path -LiteralPath $KeystorePath) -xor (Test-Path -LiteralPath $propertiesPath)) {
    throw "Incomplete signing state. Restore both the keystore and mobile/android/key.properties before continuing."
}

$keytool = "D:\Dev\jdk\jdk-17\bin\keytool.exe"
if (-not (Test-Path -LiteralPath $keytool)) {
    $command = Get-Command keytool.exe -ErrorAction SilentlyContinue
    if ($null -eq $command) { throw "JDK keytool was not found." }
    $keytool = $command.Source
}

New-Item -ItemType Directory -Force -Path (Split-Path -Parent $KeystorePath) | Out-Null
$bytes = New-Object byte[] 24
$random = [Security.Cryptography.RandomNumberGenerator]::Create()
try { $random.GetBytes($bytes) } finally { $random.Dispose() }
$password = [Convert]::ToBase64String($bytes).Replace("+", "-").Replace("/", "_").TrimEnd("=")

& $keytool -genkeypair -v `
    -keystore $KeystorePath `
    -storepass $password `
    -keypass $password `
    -alias $Alias `
    -keyalg RSA `
    -keysize 3072 `
    -validity 9125 `
    -dname "CN=PulseBrief Self Use, OU=Personal, O=PulseBrief, L=Local, ST=Local, C=CN"
if ($LASTEXITCODE -ne 0) { throw "keytool failed with exit code $LASTEXITCODE" }

$escapedPath = $KeystorePath.Replace("\", "\\")
[IO.File]::WriteAllLines($propertiesPath, @(
    "storePassword=$password",
    "keyPassword=$password",
    "keyAlias=$Alias",
    "storeFile=$escapedPath"
), [Text.UTF8Encoding]::new($false))
[IO.File]::WriteAllLines((Join-Path $secretRoot "BACKUP-INSTRUCTIONS.txt"), @(
    "Back up both files together to an encrypted drive:",
    $KeystorePath,
    $propertiesPath,
    "Losing either file prevents future APK updates from retaining the same signing identity."
), [Text.UTF8Encoding]::new($false))

Write-Host "Android self-use signing created. Passwords were written only to the ignored local key.properties file."
Write-Host "Back up .local-secrets/android and mobile/android/key.properties to an encrypted drive."
