[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repoRoot = Split-Path -Parent $PSScriptRoot

function Invoke-Step {
    param(
        [Parameter(Mandatory)] [string] $Name,
        [Parameter(Mandatory)] [scriptblock] $Action
    )

    Write-Host "`n==> $Name" -ForegroundColor Cyan
    & $Action
    if ($LASTEXITCODE -ne 0) {
        throw "$Name failed with exit code $LASTEXITCODE."
    }
}

Push-Location $repoRoot
try {
    $nodeHome = Join-Path $env:ProgramFiles 'nodejs'
    if (-not (Get-Command npm.cmd -ErrorAction SilentlyContinue) -and (Test-Path "$nodeHome\npm.cmd")) {
        $env:Path = "$nodeHome;$env:Path"
    }

    Invoke-Step 'Start test database' {
        & docker compose -f "$repoRoot\deploy\docker-compose.yml" up -d --wait
    }

    . "$PSScriptRoot\use-jdk17.ps1"
    Invoke-Step 'Backend tests' {
        Push-Location "$repoRoot\backend"
        try { & .\mvnw.cmd test } finally { Pop-Location }
    }

    Invoke-Step 'Admin dependency install' {
        Push-Location "$repoRoot\admin"
        try { & npm.cmd ci } finally { Pop-Location }
    }
    Invoke-Step 'Admin tests' {
        Push-Location "$repoRoot\admin"
        try { & npm.cmd test -- --run } finally { Pop-Location }
    }
    Invoke-Step 'Admin lint' {
        Push-Location "$repoRoot\admin"
        try { & npm.cmd run lint } finally { Pop-Location }
    }
    Invoke-Step 'Admin build' {
        Push-Location "$repoRoot\admin"
        try { & npm.cmd run build } finally { Pop-Location }
    }

    . "$PSScriptRoot\use-flutter.ps1"
    Invoke-Step 'Flutter dependency install' {
        Push-Location "$repoRoot\mobile"
        try { & flutter.bat pub get } finally { Pop-Location }
    }
    Invoke-Step 'Flutter analyze' {
        Push-Location "$repoRoot\mobile"
        try { & flutter.bat analyze } finally { Pop-Location }
    }
    Invoke-Step 'Flutter tests' {
        Push-Location "$repoRoot\mobile"
        try { & flutter.bat test } finally { Pop-Location }
    }

    Invoke-Step 'Docker Compose configuration' {
        & docker compose -f "$repoRoot\deploy\docker-compose.yml" config --quiet
    }

    Write-Host "`nAll verification steps passed." -ForegroundColor Green
}
finally {
    Pop-Location
}
