[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [ValidateNotNullOrEmpty()]
    [string] $Message,

    [switch] $KeepRemoteBranch
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repoRoot = Split-Path -Parent $PSScriptRoot
Push-Location $repoRoot
try {
    $branch = (& git branch --show-current).Trim()
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($branch)) {
        throw 'Unable to determine the current Git branch.'
    }
    if ($branch -eq 'main') {
        throw 'Work on a feature branch; finish-feature.ps1 never commits directly on main.'
    }

    & "$PSScriptRoot\verify-all.ps1"
    if ($LASTEXITCODE -ne 0) {
        throw 'Verification failed. No commit or merge was performed.'
    }

    & git add --all
    if ($LASTEXITCODE -ne 0) { throw 'git add failed.' }

    & git diff --cached --quiet
    if ($LASTEXITCODE -eq 0) {
        throw 'There are no staged changes to commit.'
    }
    if ($LASTEXITCODE -ne 1) { throw 'Unable to inspect staged changes.' }

    & git commit -m $Message
    if ($LASTEXITCODE -ne 0) { throw 'git commit failed.' }

    & git fetch origin main
    if ($LASTEXITCODE -ne 0) { throw 'Fetching origin/main failed.' }

    & git merge-base --is-ancestor origin/main $branch
    if ($LASTEXITCODE -ne 0) {
        throw 'The feature branch is not based on the latest origin/main. Rebase it, rerun verification, then finish again.'
    }

    & git switch main
    if ($LASTEXITCODE -ne 0) { throw 'Switching to main failed.' }
    & git merge --ff-only origin/main
    if ($LASTEXITCODE -ne 0) { throw 'Updating local main failed.' }
    & git merge --ff-only $branch
    if ($LASTEXITCODE -ne 0) { throw "Fast-forward merge of $branch failed." }
    & git push origin main
    if ($LASTEXITCODE -ne 0) { throw 'Pushing main failed; the local feature branch was kept.' }

    & git branch -d $branch
    if ($LASTEXITCODE -ne 0) { throw "main was pushed, but deleting local branch $branch failed." }

    if (-not $KeepRemoteBranch) {
        & git ls-remote --exit-code --heads origin $branch *> $null
        if ($LASTEXITCODE -eq 0) {
            & git push origin --delete $branch
            if ($LASTEXITCODE -ne 0) { throw "Local cleanup completed, but deleting origin/$branch failed." }
        }
        elseif ($LASTEXITCODE -ne 2) {
            throw "Unable to check whether origin/$branch exists."
        }
    }

    & git fetch origin --prune
    if ($LASTEXITCODE -ne 0) { throw 'Integration succeeded, but pruning remote-tracking branches failed.' }

    Write-Host "`nCommitted, merged into main, pushed, and cleaned branch '$branch'." -ForegroundColor Green
}
finally {
    Pop-Location
}
