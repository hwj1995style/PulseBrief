param(
    [string]$JdkHome = "D:\Dev\jdk\jdk-17"
)

if (-not (Test-Path -LiteralPath $JdkHome)) {
    Write-Error "JDK 17 not found at $JdkHome. Install JDK 17 or pass -JdkHome <path>."
    exit 1
}

$env:JAVA_HOME = $JdkHome
$env:Path = "$JdkHome\bin;$env:Path"

Write-Host "JAVA_HOME=$env:JAVA_HOME"
java -version
