[CmdletBinding()]
param(
    [string] $SdkRoot = "D:\Dev\Android\Sdk"
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$toolsVersion = "14742923"
$downloadUrl = "https://dl.google.com/android/repository/commandlinetools-win-${toolsVersion}_latest.zip"
$expectedSha1 = "16b3f45ddb3d85ea6bbe6a1c0b47146daf0db450"
$androidRoot = [IO.Path]::GetFullPath((Split-Path -Parent $SdkRoot))
$SdkRoot = [IO.Path]::GetFullPath($SdkRoot)
if (-not $SdkRoot.StartsWith($androidRoot, [StringComparison]::OrdinalIgnoreCase)) {
    throw "SDK root must remain inside $androidRoot"
}

$downloadRoot = Join-Path $androidRoot "downloads"
$archivePath = Join-Path $downloadRoot "commandlinetools-win-${toolsVersion}.zip"
$stagingPath = Join-Path $androidRoot "cmdline-tools-staging-$toolsVersion"
$latestPath = Join-Path $SdkRoot "cmdline-tools\latest"
New-Item -ItemType Directory -Force -Path $downloadRoot | Out-Null

if (-not (Test-Path -LiteralPath (Join-Path $latestPath "bin\sdkmanager.bat"))) {
    if (-not (Test-Path -LiteralPath $archivePath)) {
        Write-Host "Downloading official Android command-line tools..."
        try {
            Invoke-WebRequest -Uri $downloadUrl -OutFile $archivePath -UseBasicParsing
        }
        catch {
            if (Test-Path -LiteralPath $archivePath) { Remove-Item -LiteralPath $archivePath -Force }
            throw "Android command-line tools download failed: $($_.Exception.Message)"
        }
    }
    $actualSha1 = (Get-FileHash -LiteralPath $archivePath -Algorithm SHA1).Hash.ToLowerInvariant()
    if ($actualSha1 -ne $expectedSha1) {
        throw "Android command-line tools checksum mismatch. Expected $expectedSha1, got $actualSha1"
    }

    if (Test-Path -LiteralPath $stagingPath) {
        $resolvedStaging = [IO.Path]::GetFullPath($stagingPath)
        if (-not $resolvedStaging.StartsWith($androidRoot, [StringComparison]::OrdinalIgnoreCase)) {
            throw "Unsafe staging path: $resolvedStaging"
        }
        Remove-Item -LiteralPath $resolvedStaging -Recurse -Force
    }
    Expand-Archive -LiteralPath $archivePath -DestinationPath $stagingPath
    New-Item -ItemType Directory -Force -Path $latestPath | Out-Null
    Copy-Item -Path (Join-Path $stagingPath "cmdline-tools\*") -Destination $latestPath -Recurse -Force
    Remove-Item -LiteralPath $stagingPath -Recurse -Force
}

$env:JAVA_HOME = "D:\Dev\jdk\jdk-17"
$env:ANDROID_HOME = $SdkRoot
$env:ANDROID_SDK_ROOT = $SdkRoot
$env:Path = "$env:JAVA_HOME\bin;$latestPath\bin;$SdkRoot\platform-tools;$env:Path"
$sdkManager = Join-Path $latestPath "bin\sdkmanager.bat"

Write-Host "Accepting Android SDK licenses confirmed by the user..."
1..20 | ForEach-Object { "y" } | & $sdkManager "--sdk_root=$SdkRoot" --licenses | Out-Host
if ($LASTEXITCODE -ne 0) { throw "Android SDK license acceptance failed." }

Write-Host "Installing Android SDK packages..."
& $sdkManager "--sdk_root=$SdkRoot" "platform-tools" "platforms;android-36" "build-tools;36.0.0"
if ($LASTEXITCODE -ne 0) { throw "Android SDK package installation failed." }

& "D:\Dev\flutter\bin\flutter.bat" config --android-sdk $SdkRoot | Out-Host
Write-Host "Android SDK installed at $SdkRoot"
