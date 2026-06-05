param(
    [string]$FlutterHome = "D:\Dev\flutter"
)

$flutterBin = Join-Path $FlutterHome "bin"

if (-not (Test-Path -LiteralPath $flutterBin)) {
    Write-Error "Flutter SDK not found at $FlutterHome. Install Flutter or pass -FlutterHome <path>."
    exit 1
}

$env:Path = "$flutterBin;$env:Path"

Write-Host "FLUTTER_HOME=$FlutterHome"
flutter --version
