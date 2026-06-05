Write-Host "PulseBrief environment check"
Write-Host ""

Write-Host "Java:"
try {
    java -version
} catch {
    Write-Host "java not found"
}

Write-Host ""
Write-Host "Project JDK 17:"
if (Test-Path -LiteralPath "D:\Dev\jdk\jdk-17\bin\java.exe") {
    & "D:\Dev\jdk\jdk-17\bin\java.exe" -version
} else {
    Write-Host "D:\Dev\jdk\jdk-17 not found"
}

Write-Host ""
Write-Host "Flutter:"
if (Get-Command flutter -ErrorAction SilentlyContinue) {
    flutter --version
} elseif (Test-Path -LiteralPath "D:\Dev\flutter\bin\flutter.bat") {
    & "D:\Dev\flutter\bin\flutter.bat" --version
} else {
    Write-Host "flutter not found"
}

Write-Host ""
Write-Host "Docker:"
if (Get-Command docker -ErrorAction SilentlyContinue) {
    docker --version
} else {
    Write-Host "docker not found"
}

Write-Host ""
Write-Host "Node:"
if (Get-Command node -ErrorAction SilentlyContinue) {
    node --version
} else {
    Write-Host "node not found"
}

Write-Host ""
Write-Host "NPM:"
if (Get-Command npm -ErrorAction SilentlyContinue) {
    npm --version
} else {
    Write-Host "npm not found"
}

Write-Host ""
Write-Host "Maven Wrapper:"
if (Test-Path -LiteralPath ".\backend\mvnw.cmd") {
    Write-Host "backend/mvnw.cmd found"
} else {
    Write-Host "backend/mvnw.cmd not found"
}
