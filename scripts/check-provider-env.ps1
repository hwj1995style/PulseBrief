param(
    [string]$EnvFile = ""
)

$ErrorActionPreference = "Stop"

function Read-EnvFile {
    param([string]$Path)

    $values = @{}
    if ([string]::IsNullOrWhiteSpace($Path)) {
        return $values
    }

    if (-not (Test-Path -LiteralPath $Path)) {
        throw "Env file not found: $Path"
    }

    foreach ($line in Get-Content -LiteralPath $Path) {
        $trimmed = $line.Trim()
        if ($trimmed.Length -eq 0 -or $trimmed.StartsWith("#")) {
            continue
        }

        $separator = $trimmed.IndexOf("=")
        if ($separator -le 0) {
            throw "Invalid env line: $line"
        }

        $name = $trimmed.Substring(0, $separator).Trim()
        $value = $trimmed.Substring($separator + 1).Trim().Trim('"').Trim("'")
        $values[$name] = $value
    }

    return $values
}

function Get-ConfigValue {
    param(
        [hashtable]$FileValues,
        [string]$Name,
        [string]$Default = ""
    )

    $envValue = [Environment]::GetEnvironmentVariable($Name)
    if (-not [string]::IsNullOrWhiteSpace($envValue)) {
        return $envValue.Trim()
    }

    if ($FileValues.ContainsKey($Name)) {
        return [string]$FileValues[$Name]
    }

    return $Default
}

function Read-Bool {
    param(
        [string]$Name,
        [string]$Value
    )

    $normalized = $Value.Trim().ToLowerInvariant()
    if ($normalized -in @("", "false", "0", "no")) {
        return $false
    }
    if ($normalized -in @("true", "1", "yes")) {
        return $true
    }

    throw "$Name must be true or false, got '$Value'"
}

function Read-IntInRange {
    param(
        [string]$Name,
        [string]$Value,
        [int]$Default,
        [int]$Min,
        [int]$Max
    )

    $raw = if ([string]::IsNullOrWhiteSpace($Value)) { [string]$Default } else { $Value.Trim() }
    $parsed = 0
    if (-not [int]::TryParse($raw, [ref]$parsed)) {
        throw "$Name must be an integer, got '$Value'"
    }
    if ($parsed -lt $Min -or $parsed -gt $Max) {
        throw "$Name must be between $Min and $Max, got $parsed"
    }

    return $parsed
}

function Read-DoubleInRange {
    param(
        [string]$Name,
        [string]$Value,
        [double]$Default,
        [double]$Min,
        [double]$Max
    )

    $raw = if ([string]::IsNullOrWhiteSpace($Value)) { [string]$Default } else { $Value.Trim() }
    $parsed = 0.0
    if (-not [double]::TryParse(
        $raw,
        [System.Globalization.NumberStyles]::Float,
        [System.Globalization.CultureInfo]::InvariantCulture,
        [ref]$parsed
    )) {
        throw "$Name must be a decimal number, got '$Value'"
    }
    if ($parsed -lt $Min -or $parsed -gt $Max) {
        throw "$Name must be between $Min and $Max, got $parsed"
    }

    return $parsed
}

function Assert-NotPlaceholder {
    param(
        [string]$Name,
        [string]$Value
    )

    if ([string]::IsNullOrWhiteSpace($Value)) {
        throw "$Name is required"
    }

    $normalized = $Value.Trim().ToLowerInvariant()
    $placeholderTokens = @("changeme", "placeholder", "your-api-key", "your_api_key", "example.com", "todo")
    foreach ($token in $placeholderTokens) {
        if ($normalized.Contains($token)) {
            throw "$Name contains placeholder value '$token'"
        }
    }
}

function Assert-HttpUrls {
    param(
        [string]$Name,
        [string]$Value
    )

    if ([string]::IsNullOrWhiteSpace($Value)) {
        throw "$Name is required"
    }

    $urls = $Value.Split(",", [System.StringSplitOptions]::RemoveEmptyEntries)
    if ($urls.Count -eq 0) {
        throw "$Name must contain at least one URL"
    }

    foreach ($url in $urls) {
        $candidate = $url.Trim()
        Assert-NotPlaceholder -Name $Name -Value $candidate
        $parsed = $null
        if (-not [Uri]::TryCreate($candidate, [UriKind]::Absolute, [ref]$parsed)) {
            throw "$Name contains invalid URL '$candidate'"
        }
        if ($parsed.Scheme -notin @("http", "https")) {
            throw "$Name URL must use http or https: '$candidate'"
        }
    }
}

$fileValues = Read-EnvFile -Path $EnvFile

$legacyAdminTokenEnabled = Read-Bool `
    -Name "PULSEBRIEF_ADMIN_LEGACY_TOKEN_ENABLED" `
    -Value (Get-ConfigValue -FileValues $fileValues -Name "PULSEBRIEF_ADMIN_LEGACY_TOKEN_ENABLED" -Default "false")
if ($legacyAdminTokenEnabled) {
    Assert-NotPlaceholder `
        -Name "PULSEBRIEF_ADMIN_LEGACY_TOKEN" `
        -Value (Get-ConfigValue -FileValues $fileValues -Name "PULSEBRIEF_ADMIN_LEGACY_TOKEN")
}

Read-IntInRange -Name "PULSEBRIEF_ADMIN_PASSWORD_MAX_AGE_DAYS" `
    -Value (Get-ConfigValue -FileValues $fileValues -Name "PULSEBRIEF_ADMIN_PASSWORD_MAX_AGE_DAYS" -Default "90") `
    -Default 90 -Min 1 -Max 365 | Out-Null
Read-IntInRange -Name "PULSEBRIEF_ADMIN_SESSION_CLEANUP_RETENTION_DAYS" `
    -Value (Get-ConfigValue -FileValues $fileValues -Name "PULSEBRIEF_ADMIN_SESSION_CLEANUP_RETENTION_DAYS" -Default "7") `
    -Default 7 -Min 1 -Max 90 | Out-Null
$adminSessionCleanupEnabled = Read-Bool `
    -Name "PULSEBRIEF_ADMIN_SESSION_CLEANUP_ENABLED" `
    -Value (Get-ConfigValue -FileValues $fileValues -Name "PULSEBRIEF_ADMIN_SESSION_CLEANUP_ENABLED" -Default "true")
if ($adminSessionCleanupEnabled) {
    Assert-NotPlaceholder -Name "PULSEBRIEF_ADMIN_SESSION_CLEANUP_CRON" `
        -Value (Get-ConfigValue -FileValues $fileValues -Name "PULSEBRIEF_ADMIN_SESSION_CLEANUP_CRON" -Default "0 15 3 * * *")
}
Write-Host "OK: Admin password rotation and session cleanup configuration are valid."

$adminBootstrapUsername = Get-ConfigValue -FileValues $fileValues -Name "PULSEBRIEF_ADMIN_BOOTSTRAP_USERNAME"
$adminBootstrapPassword = Get-ConfigValue -FileValues $fileValues -Name "PULSEBRIEF_ADMIN_BOOTSTRAP_PASSWORD"
if (-not [string]::IsNullOrWhiteSpace($adminBootstrapUsername) -or
    -not [string]::IsNullOrWhiteSpace($adminBootstrapPassword)) {
    Assert-NotPlaceholder -Name "PULSEBRIEF_ADMIN_BOOTSTRAP_USERNAME" -Value $adminBootstrapUsername
    Assert-NotPlaceholder -Name "PULSEBRIEF_ADMIN_BOOTSTRAP_PASSWORD" -Value $adminBootstrapPassword
    if ($adminBootstrapPassword.Length -lt 12) {
        throw "PULSEBRIEF_ADMIN_BOOTSTRAP_PASSWORD must be at least 12 characters"
    }
    $adminBootstrapRole = (Get-ConfigValue -FileValues $fileValues `
        -Name "PULSEBRIEF_ADMIN_BOOTSTRAP_ROLE" -Default "ADMIN").Trim().ToUpperInvariant()
    if ($adminBootstrapRole -notin @("VIEWER", "EDITOR", "ADMIN")) {
        throw "PULSEBRIEF_ADMIN_BOOTSTRAP_ROLE must be VIEWER, EDITOR, or ADMIN"
    }
    Write-Host "OK: Admin bootstrap credentials are configured."
}

$enabled = Read-Bool `
    -Name "PULSEBRIEF_INGESTION_ENABLED" `
    -Value (Get-ConfigValue -FileValues $fileValues -Name "PULSEBRIEF_INGESTION_ENABLED" -Default "false")

$deepSeekEnabled = Read-Bool `
    -Name "PULSEBRIEF_DEEPSEEK_ENABLED" `
    -Value (Get-ConfigValue -FileValues $fileValues -Name "PULSEBRIEF_DEEPSEEK_ENABLED" -Default "false")
$deepSeekClassificationEnabled = Read-Bool `
    -Name "PULSEBRIEF_DEEPSEEK_CLASSIFICATION_ENABLED" `
    -Value (Get-ConfigValue -FileValues $fileValues -Name "PULSEBRIEF_DEEPSEEK_CLASSIFICATION_ENABLED" -Default "false")
$aiUsageEnabled = Read-Bool `
    -Name "PULSEBRIEF_AI_USAGE_ENABLED" `
    -Value (Get-ConfigValue -FileValues $fileValues -Name "PULSEBRIEF_AI_USAGE_ENABLED" -Default "true")
if ($aiUsageEnabled) {
    Read-IntInRange -Name "PULSEBRIEF_AI_DAILY_REQUEST_LIMIT" `
        -Value (Get-ConfigValue -FileValues $fileValues -Name "PULSEBRIEF_AI_DAILY_REQUEST_LIMIT" -Default "200") `
        -Default 200 -Min 1 -Max 100000 | Out-Null
    Read-IntInRange -Name "PULSEBRIEF_AI_DAILY_TOKEN_LIMIT" `
        -Value (Get-ConfigValue -FileValues $fileValues -Name "PULSEBRIEF_AI_DAILY_TOKEN_LIMIT" -Default "200000") `
        -Default 200000 -Min 1000 -Max 100000000 | Out-Null
    Read-IntInRange -Name "PULSEBRIEF_AI_WARNING_PERCENT" `
        -Value (Get-ConfigValue -FileValues $fileValues -Name "PULSEBRIEF_AI_WARNING_PERCENT" -Default "80") `
        -Default 80 -Min 1 -Max 100 | Out-Null
    Read-DoubleInRange -Name "PULSEBRIEF_AI_DEEPSEEK_INPUT_COST_PER_MILLION_USD" `
        -Value (Get-ConfigValue -FileValues $fileValues -Name "PULSEBRIEF_AI_DEEPSEEK_INPUT_COST_PER_MILLION_USD" -Default "0") `
        -Default 0 -Min 0 -Max 100000 | Out-Null
    Read-DoubleInRange -Name "PULSEBRIEF_AI_DEEPSEEK_OUTPUT_COST_PER_MILLION_USD" `
        -Value (Get-ConfigValue -FileValues $fileValues -Name "PULSEBRIEF_AI_DEEPSEEK_OUTPUT_COST_PER_MILLION_USD" -Default "0") `
        -Default 0 -Min 0 -Max 100000 | Out-Null
    Write-Host "OK: AI usage limits and cost configuration are valid."
}
if ($deepSeekEnabled -or $deepSeekClassificationEnabled) {
    Assert-NotPlaceholder `
        -Name "PULSEBRIEF_DEEPSEEK_API_KEY" `
        -Value (Get-ConfigValue -FileValues $fileValues -Name "PULSEBRIEF_DEEPSEEK_API_KEY")
    Assert-HttpUrls `
        -Name "PULSEBRIEF_DEEPSEEK_BASE_URL" `
        -Value (Get-ConfigValue -FileValues $fileValues -Name "PULSEBRIEF_DEEPSEEK_BASE_URL" -Default "https://api.deepseek.com/chat/completions")
    Assert-NotPlaceholder `
        -Name "PULSEBRIEF_DEEPSEEK_MODEL" `
        -Value (Get-ConfigValue -FileValues $fileValues -Name "PULSEBRIEF_DEEPSEEK_MODEL" -Default "deepseek-v4-flash")
    Read-IntInRange -Name "PULSEBRIEF_DEEPSEEK_TIMEOUT_SECONDS" `
        -Value (Get-ConfigValue -FileValues $fileValues -Name "PULSEBRIEF_DEEPSEEK_TIMEOUT_SECONDS" -Default "30") `
        -Default 30 -Min 5 -Max 120 | Out-Null
}
if ($deepSeekEnabled) {
    Read-IntInRange -Name "PULSEBRIEF_DEEPSEEK_MAX_INPUT_CHARACTERS" `
        -Value (Get-ConfigValue -FileValues $fileValues -Name "PULSEBRIEF_DEEPSEEK_MAX_INPUT_CHARACTERS" -Default "12000") `
        -Default 12000 -Min 500 -Max 50000 | Out-Null
    Read-IntInRange -Name "PULSEBRIEF_DEEPSEEK_MAX_OUTPUT_TOKENS" `
        -Value (Get-ConfigValue -FileValues $fileValues -Name "PULSEBRIEF_DEEPSEEK_MAX_OUTPUT_TOKENS" -Default "1200") `
        -Default 1200 -Min 300 -Max 4000 | Out-Null
    Write-Host "OK: DeepSeek summary provider configuration is present."
}
if ($deepSeekClassificationEnabled) {
    Read-DoubleInRange -Name "PULSEBRIEF_DEEPSEEK_CLASSIFICATION_MIN_CONFIDENCE" `
        -Value (Get-ConfigValue -FileValues $fileValues -Name "PULSEBRIEF_DEEPSEEK_CLASSIFICATION_MIN_CONFIDENCE" -Default "0.65") `
        -Default 0.65 -Min 0.0 -Max 1.0 | Out-Null
    Read-IntInRange -Name "PULSEBRIEF_DEEPSEEK_CLASSIFICATION_MAX_INPUT_CHARACTERS" `
        -Value (Get-ConfigValue -FileValues $fileValues -Name "PULSEBRIEF_DEEPSEEK_CLASSIFICATION_MAX_INPUT_CHARACTERS" -Default "4000") `
        -Default 4000 -Min 200 -Max 12000 | Out-Null
    Read-IntInRange -Name "PULSEBRIEF_DEEPSEEK_CLASSIFICATION_MAX_OUTPUT_TOKENS" `
        -Value (Get-ConfigValue -FileValues $fileValues -Name "PULSEBRIEF_DEEPSEEK_CLASSIFICATION_MAX_OUTPUT_TOKENS" -Default "300") `
        -Default 300 -Min 100 -Max 1000 | Out-Null
    Write-Host "OK: DeepSeek classification provider configuration is present."
}

$openAiEnabled = Read-Bool `
    -Name "PULSEBRIEF_OPENAI_ENABLED" `
    -Value (Get-ConfigValue -FileValues $fileValues -Name "PULSEBRIEF_OPENAI_ENABLED" -Default "false")
if ($openAiEnabled) {
    Assert-NotPlaceholder `
        -Name "PULSEBRIEF_OPENAI_API_KEY" `
        -Value (Get-ConfigValue -FileValues $fileValues -Name "PULSEBRIEF_OPENAI_API_KEY")
    Assert-HttpUrls `
        -Name "PULSEBRIEF_OPENAI_BASE_URL" `
        -Value (Get-ConfigValue -FileValues $fileValues -Name "PULSEBRIEF_OPENAI_BASE_URL" -Default "https://api.openai.com/v1/responses")
    Assert-NotPlaceholder `
        -Name "PULSEBRIEF_OPENAI_MODEL" `
        -Value (Get-ConfigValue -FileValues $fileValues -Name "PULSEBRIEF_OPENAI_MODEL" -Default "gpt-5.6-luna")
    Read-IntInRange -Name "PULSEBRIEF_OPENAI_TIMEOUT_SECONDS" `
        -Value (Get-ConfigValue -FileValues $fileValues -Name "PULSEBRIEF_OPENAI_TIMEOUT_SECONDS" -Default "30") `
        -Default 30 -Min 5 -Max 120 | Out-Null
    Read-IntInRange -Name "PULSEBRIEF_OPENAI_MAX_INPUT_CHARACTERS" `
        -Value (Get-ConfigValue -FileValues $fileValues -Name "PULSEBRIEF_OPENAI_MAX_INPUT_CHARACTERS" -Default "12000") `
        -Default 12000 -Min 500 -Max 50000 | Out-Null
    Read-IntInRange -Name "PULSEBRIEF_OPENAI_MAX_OUTPUT_TOKENS" `
        -Value (Get-ConfigValue -FileValues $fileValues -Name "PULSEBRIEF_OPENAI_MAX_OUTPUT_TOKENS" -Default "1200") `
        -Default 1200 -Min 300 -Max 4000 | Out-Null
    Write-Host "OK: OpenAI summary provider configuration is present."
}

Write-Host "PulseBrief provider environment check"
if (-not [string]::IsNullOrWhiteSpace($EnvFile)) {
    Write-Host "Env file: $EnvFile"
}

if (-not $enabled) {
    Write-Host "OK: real ingestion is disabled."
    Write-Host "Set PULSEBRIEF_INGESTION_ENABLED=true only after provider config passes this check."
    exit 0
}

$providerKind = (Get-ConfigValue -FileValues $fileValues -Name "PULSEBRIEF_PROVIDER_KIND").Trim().ToUpperInvariant()
if ($providerKind -notin @("FIXTURE", "RSS", "API")) {
    throw "PULSEBRIEF_PROVIDER_KIND must be FIXTURE, RSS, or API when ingestion is enabled"
}

$licensePolicy = (Get-ConfigValue -FileValues $fileValues -Name "PULSEBRIEF_PROVIDER_LICENSE_POLICY" -Default "SUMMARY_ONLY").Trim().ToUpperInvariant()
if ($licensePolicy -notin @("SUMMARY_ONLY", "SNIPPET_ALLOWED", "FULLTEXT_ALLOWED", "PDF_ALLOWED", "LINK_ONLY", "UNKNOWN")) {
    throw "PULSEBRIEF_PROVIDER_LICENSE_POLICY is invalid: $licensePolicy"
}

$allowBackfill = Read-Bool `
    -Name "PULSEBRIEF_INGESTION_ALLOW_BACKFILL" `
    -Value (Get-ConfigValue -FileValues $fileValues -Name "PULSEBRIEF_INGESTION_ALLOW_BACKFILL" -Default "false")
if ($allowBackfill) {
    throw "PULSEBRIEF_INGESTION_ALLOW_BACKFILL must remain false for V1"
}

$maxAgeHours = Read-IntInRange `
    -Name "PULSEBRIEF_INGESTION_MAX_AGE_HOURS" `
    -Value (Get-ConfigValue -FileValues $fileValues -Name "PULSEBRIEF_INGESTION_MAX_AGE_HOURS" -Default "24") `
    -Default 24 `
    -Min 1 `
    -Max 72

$pdfMaxAgeHours = Read-IntInRange `
    -Name "PULSEBRIEF_INGESTION_PDF_MAX_AGE_HOURS" `
    -Value (Get-ConfigValue -FileValues $fileValues -Name "PULSEBRIEF_INGESTION_PDF_MAX_AGE_HOURS" -Default "72") `
    -Default 72 `
    -Min 1 `
    -Max 168

$maxItems = Read-IntInRange `
    -Name "PULSEBRIEF_INGESTION_MAX_ITEMS_PER_SOURCE" `
    -Value (Get-ConfigValue -FileValues $fileValues -Name "PULSEBRIEF_INGESTION_MAX_ITEMS_PER_SOURCE" -Default "50") `
    -Default 50 `
    -Min 1 `
    -Max 100

$rateLimit = Read-IntInRange `
    -Name "PULSEBRIEF_PROVIDER_RATE_LIMIT_PER_HOUR" `
    -Value (Get-ConfigValue -FileValues $fileValues -Name "PULSEBRIEF_PROVIDER_RATE_LIMIT_PER_HOUR" -Default "60") `
    -Default 60 `
    -Min 1 `
    -Max 60

if ($providerKind -eq "RSS") {
    Assert-HttpUrls `
        -Name "PULSEBRIEF_RSS_FEED_URLS" `
        -Value (Get-ConfigValue -FileValues $fileValues -Name "PULSEBRIEF_RSS_FEED_URLS")
}

if ($providerKind -eq "API") {
    Assert-HttpUrls `
        -Name "PULSEBRIEF_PROVIDER_API_BASE_URL" `
        -Value (Get-ConfigValue -FileValues $fileValues -Name "PULSEBRIEF_PROVIDER_API_BASE_URL")
    Assert-NotPlaceholder `
        -Name "PULSEBRIEF_PROVIDER_API_KEY" `
        -Value (Get-ConfigValue -FileValues $fileValues -Name "PULSEBRIEF_PROVIDER_API_KEY")
}

$allowFullText = Read-Bool `
    -Name "PULSEBRIEF_INGESTION_ALLOW_FULL_TEXT" `
    -Value (Get-ConfigValue -FileValues $fileValues -Name "PULSEBRIEF_INGESTION_ALLOW_FULL_TEXT" -Default "false")
$allowPdfDownload = Read-Bool `
    -Name "PULSEBRIEF_INGESTION_ALLOW_PDF_DOWNLOAD" `
    -Value (Get-ConfigValue -FileValues $fileValues -Name "PULSEBRIEF_INGESTION_ALLOW_PDF_DOWNLOAD" -Default "false")

$licenseNote = Get-ConfigValue -FileValues $fileValues -Name "PULSEBRIEF_PROVIDER_LICENSE_NOTE"
if ($allowFullText -and $licensePolicy -ne "FULLTEXT_ALLOWED") {
    throw "PULSEBRIEF_INGESTION_ALLOW_FULL_TEXT=true requires PULSEBRIEF_PROVIDER_LICENSE_POLICY=FULLTEXT_ALLOWED"
}
if ($allowPdfDownload -and $licensePolicy -ne "PDF_ALLOWED") {
    throw "PULSEBRIEF_INGESTION_ALLOW_PDF_DOWNLOAD=true requires PULSEBRIEF_PROVIDER_LICENSE_POLICY=PDF_ALLOWED"
}
if (($allowFullText -or $allowPdfDownload) -and $licenseNote.Trim().Length -lt 10) {
    throw "Full text or PDF collection requires PULSEBRIEF_PROVIDER_LICENSE_NOTE with a concrete authorization note"
}

Write-Host "OK: provider config is safe to use for manual ingestion checks."
Write-Host "Provider kind: $providerKind"
Write-Host "License policy: $licensePolicy"
Write-Host "Max age hours: $maxAgeHours"
Write-Host "PDF max age hours: $pdfMaxAgeHours"
Write-Host "Max items per source: $maxItems"
Write-Host "Rate limit per hour: $rateLimit"
