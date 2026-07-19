[CmdletBinding()]
param(
    [string] $SourceEnvFile = "",
    [string] $OutputDirectory = ""
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repoRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($SourceEnvFile)) {
    $SourceEnvFile = Join-Path $repoRoot '.env.local'
}
if ([string]::IsNullOrWhiteSpace($OutputDirectory)) {
    $OutputDirectory = Join-Path $repoRoot '.local-secrets\server'
}

$runtimeEnvPath = Join-Path $OutputDirectory 'production.env'
$credentialPath = Join-Path $OutputDirectory 'admin-initial-credentials.txt'

if ((Test-Path -LiteralPath $runtimeEnvPath) -or (Test-Path -LiteralPath $credentialPath)) {
    throw "Production secrets already exist under $OutputDirectory. Existing secrets were not overwritten."
}

function Read-EnvValues([string] $Path) {
    $values = @{}
    if (-not (Test-Path -LiteralPath $Path)) { return $values }
    foreach ($line in Get-Content -LiteralPath $Path) {
        $trimmed = $line.Trim()
        if ($trimmed.Length -eq 0 -or $trimmed.StartsWith('#')) { continue }
        $separator = $trimmed.IndexOf('=')
        if ($separator -le 0) { continue }
        $name = $trimmed.Substring(0, $separator).Trim()
        $value = $trimmed.Substring($separator + 1).Trim().Trim('"').Trim("'")
        $values[$name] = $value
    }
    return $values
}

function New-RandomSecret([int] $Bytes = 32) {
    $buffer = [byte[]]::new($Bytes)
    $generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $generator.GetBytes($buffer)
    }
    finally {
        $generator.Dispose()
    }
    return [Convert]::ToBase64String($buffer).TrimEnd('=').Replace('+', '-').Replace('/', '_')
}

function Get-Value([hashtable] $Values, [string] $Name, [string] $Default = '') {
    $processValue = [Environment]::GetEnvironmentVariable($Name)
    if (-not [string]::IsNullOrWhiteSpace($processValue)) { return $processValue.Trim() }
    if ($Values.ContainsKey($Name)) { return [string] $Values[$Name] }
    return $Default
}

$sourceValues = Read-EnvValues $SourceEnvFile
$deepSeekKey = Get-Value $sourceValues 'PULSEBRIEF_DEEPSEEK_API_KEY'
if ([string]::IsNullOrWhiteSpace($deepSeekKey)) {
    throw "PULSEBRIEF_DEEPSEEK_API_KEY is missing from $SourceEnvFile."
}
if ($deepSeekKey.Contains("`r") -or $deepSeekKey.Contains("`n")) {
    throw 'PULSEBRIEF_DEEPSEEK_API_KEY must be a single-line value.'
}

$adminPassword = New-RandomSecret 30
$utf8 = [System.Text.UTF8Encoding]::new($false)
[System.IO.Directory]::CreateDirectory($OutputDirectory) | Out-Null

$runtimeLines = @(
    'PULSEBRIEF_DOMAIN=huawj.com.cn'
    "PULSEBRIEF_MYSQL_ROOT_PASSWORD=$(New-RandomSecret 36)"
    "PULSEBRIEF_DB_PASSWORD=$(New-RandomSecret 36)"
    'PULSEBRIEF_ADMIN_BOOTSTRAP_USERNAME=admin'
    "PULSEBRIEF_ADMIN_BOOTSTRAP_PASSWORD=$adminPassword"
    'PULSEBRIEF_ADMIN_BOOTSTRAP_DISPLAY_NAME=PulseBrief Admin'
    'PULSEBRIEF_ADMIN_SESSION_HOURS=12'
    'PULSEBRIEF_ADMIN_PASSWORD_MAX_AGE_DAYS=90'
    'PULSEBRIEF_INGESTION_ENABLED=false'
    'PULSEBRIEF_INGESTION_SCHEDULING_ENABLED=false'
    "PULSEBRIEF_DEEPSEEK_ENABLED=$(Get-Value $sourceValues 'PULSEBRIEF_DEEPSEEK_ENABLED' 'true')"
    "PULSEBRIEF_DEEPSEEK_CLASSIFICATION_ENABLED=$(Get-Value $sourceValues 'PULSEBRIEF_DEEPSEEK_CLASSIFICATION_ENABLED' 'true')"
    "PULSEBRIEF_DEEPSEEK_API_KEY=$deepSeekKey"
    "PULSEBRIEF_DEEPSEEK_BASE_URL=$(Get-Value $sourceValues 'PULSEBRIEF_DEEPSEEK_BASE_URL' 'https://api.deepseek.com/chat/completions')"
    "PULSEBRIEF_DEEPSEEK_MODEL=$(Get-Value $sourceValues 'PULSEBRIEF_DEEPSEEK_MODEL' 'deepseek-v4-flash')"
    "PULSEBRIEF_DEEPSEEK_TIMEOUT_SECONDS=$(Get-Value $sourceValues 'PULSEBRIEF_DEEPSEEK_TIMEOUT_SECONDS' '30')"
    "PULSEBRIEF_DEEPSEEK_MAX_INPUT_CHARACTERS=$(Get-Value $sourceValues 'PULSEBRIEF_DEEPSEEK_MAX_INPUT_CHARACTERS' '12000')"
    "PULSEBRIEF_DEEPSEEK_MAX_OUTPUT_TOKENS=$(Get-Value $sourceValues 'PULSEBRIEF_DEEPSEEK_MAX_OUTPUT_TOKENS' '1200')"
    "PULSEBRIEF_DEEPSEEK_CLASSIFICATION_MIN_CONFIDENCE=$(Get-Value $sourceValues 'PULSEBRIEF_DEEPSEEK_CLASSIFICATION_MIN_CONFIDENCE' '0.65')"
    "PULSEBRIEF_DEEPSEEK_CLASSIFICATION_MAX_INPUT_CHARACTERS=$(Get-Value $sourceValues 'PULSEBRIEF_DEEPSEEK_CLASSIFICATION_MAX_INPUT_CHARACTERS' '4000')"
    "PULSEBRIEF_DEEPSEEK_CLASSIFICATION_MAX_OUTPUT_TOKENS=$(Get-Value $sourceValues 'PULSEBRIEF_DEEPSEEK_CLASSIFICATION_MAX_OUTPUT_TOKENS' '300')"
    "PULSEBRIEF_AI_DAILY_REQUEST_LIMIT=$(Get-Value $sourceValues 'PULSEBRIEF_AI_DAILY_REQUEST_LIMIT' '200')"
    "PULSEBRIEF_AI_DAILY_TOKEN_LIMIT=$(Get-Value $sourceValues 'PULSEBRIEF_AI_DAILY_TOKEN_LIMIT' '200000')"
    "PULSEBRIEF_AI_WARNING_PERCENT=$(Get-Value $sourceValues 'PULSEBRIEF_AI_WARNING_PERCENT' '80')"
    "PULSEBRIEF_AI_DEEPSEEK_INPUT_COST_PER_MILLION_USD=$(Get-Value $sourceValues 'PULSEBRIEF_AI_DEEPSEEK_INPUT_COST_PER_MILLION_USD' '0')"
    "PULSEBRIEF_AI_DEEPSEEK_OUTPUT_COST_PER_MILLION_USD=$(Get-Value $sourceValues 'PULSEBRIEF_AI_DEEPSEEK_OUTPUT_COST_PER_MILLION_USD' '0')"
)

[System.IO.File]::WriteAllLines($runtimeEnvPath, $runtimeLines, $utf8)
[System.IO.File]::WriteAllLines($credentialPath, @(
    'PulseBrief production Admin initial credentials'
    'URL=https://huawj.com.cn'
    'Username=admin'
    "Password=$adminPassword"
    'Change the password immediately after the first login.'
), $utf8)

Write-Host "Production runtime secrets created: $runtimeEnvPath"
Write-Host "Initial Admin credentials created: $credentialPath"
Write-Host 'Secret values were not printed. Both files are ignored by Git.'
