[CmdletBinding()]
param(
    [string] $Server = '47.112.14.22',
    [string] $User = 'root',
    [string] $PrivateKey = '',
    [string] $EnvFile = '',
    [string] $ExpectedKeyFingerprint = '2d3670f7a3aba6f09b36ee9cb9669ace'
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repoRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($PrivateKey)) {
    $PrivateKey = Join-Path $repoRoot '.local-secrets\server\aliyun1.pem'
}
if ([string]::IsNullOrWhiteSpace($EnvFile)) {
    $EnvFile = Join-Path $repoRoot '.local-secrets\server\production.env'
}
$credentialFile = Join-Path (Split-Path -Parent $EnvFile) 'admin-initial-credentials.txt'

foreach ($requiredFile in @($PrivateKey, $EnvFile, $credentialFile)) {
    if (-not (Test-Path -LiteralPath $requiredFile)) { throw "Required local file not found: $requiredFile" }
}

Push-Location $repoRoot
try {
    $branch = (& git branch --show-current).Trim()
    if ($LASTEXITCODE -ne 0 -or $branch -ne 'main') { throw 'Production deployment must run from main.' }
    & git diff --quiet
    if ($LASTEXITCODE -ne 0) { throw 'Tracked working-tree changes exist. Commit and verify them before deployment.' }
    & git diff --cached --quiet
    if ($LASTEXITCODE -ne 0) { throw 'Staged changes exist. Commit and verify them before deployment.' }

    $fingerprintLine = (& ssh-keygen -lf $PrivateKey -E md5).Trim()
    if ($LASTEXITCODE -ne 0) { throw 'Unable to read the SSH private-key fingerprint.' }
    $actualFingerprint = ([regex]::Match($fingerprintLine, 'MD5:([0-9a-f:]+)', 'IgnoreCase').Groups[1].Value -replace ':', '').ToLowerInvariant()
    if ($actualFingerprint -ne $ExpectedKeyFingerprint.ToLowerInvariant()) {
        throw "SSH key fingerprint mismatch. Expected $ExpectedKeyFingerprint."
    }

    $tempRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("pulsebrief-deploy-" + [Guid]::NewGuid().ToString('N'))
    [System.IO.Directory]::CreateDirectory($tempRoot) | Out-Null
    try {
        $archive = Join-Path $tempRoot 'pulsebrief.tar'
        & git archive --format=tar --output=$archive HEAD
        if ($LASTEXITCODE -ne 0) { throw 'Unable to create the deployment archive.' }

        $releaseId = (Get-Date).ToUniversalTime().ToString('yyyyMMddTHHmmssZ')
        $remoteTemp = "/tmp/pulsebrief-deploy-$releaseId"
        $sshTarget = "$User@$Server"
        $sshOptions = @('-i', $PrivateKey, '-o', 'BatchMode=yes', '-o', 'StrictHostKeyChecking=accept-new', '-o', 'ConnectTimeout=15')

        & ssh @sshOptions $sshTarget "install -d -m 700 '$remoteTemp'"
        if ($LASTEXITCODE -ne 0) { throw 'Unable to prepare the remote upload directory.' }
        & scp @sshOptions $archive "${sshTarget}:${remoteTemp}/pulsebrief.tar"
        if ($LASTEXITCODE -ne 0) { throw 'Unable to upload the deployment archive.' }
        & scp @sshOptions $EnvFile "${sshTarget}:${remoteTemp}/production.env"
        if ($LASTEXITCODE -ne 0) { throw 'Unable to upload the production environment.' }

        $remoteScript = @'
set -euo pipefail
remote_temp="$1"
release_id="$2"
deploy_root=/opt/pulsebrief
release_root="$deploy_root/releases/$release_id"

install -d -m 700 "$deploy_root" "$deploy_root/releases"
install -d -m 755 "$release_root"
tar -xf "$remote_temp/pulsebrief.tar" -C "$release_root"
install -m 600 "$remote_temp/production.env" "$deploy_root/.env"

if ! swapon --show=NAME --noheadings | grep -q .; then
    if [ ! -f /swapfile ]; then
        fallocate -l 2G /swapfile
        chmod 600 /swapfile
        mkswap /swapfile >/dev/null
    fi
    swapon /swapfile
    grep -q '^/swapfile ' /etc/fstab || printf '/swapfile none swap sw 0 0\n' >> /etc/fstab
fi

if ! command -v firewall-offline-cmd >/dev/null 2>&1; then
    dnf install -y firewalld >/dev/null
fi
firewall-offline-cmd --zone=public --add-service=ssh >/dev/null || true
firewall-offline-cmd --zone=public --add-service=http >/dev/null || true
firewall-offline-cmd --zone=public --add-service=https >/dev/null || true
systemctl enable --now firewalld >/dev/null
docker_zone="$(firewall-cmd --get-zone-of-interface=docker0 2>/dev/null || true)"
if [ -z "$docker_zone" ] || [ "$docker_zone" = "no zone" ]; then
    systemctl restart docker
fi

if [ -e "$deploy_root/repo" ] && [ ! -L "$deploy_root/repo" ]; then
    mv "$deploy_root/repo" "$deploy_root/repo-pre-releases-$release_id"
fi
ln -sfn "$release_root" "$deploy_root/repo"

install -d -m 700 /var/backups/pulsebrief
chmod 755 "$release_root/deploy/production/backup.sh"
install -m 644 "$release_root/deploy/production/pulsebrief-backup.service" /etc/systemd/system/pulsebrief-backup.service
install -m 644 "$release_root/deploy/production/pulsebrief-backup.timer" /etc/systemd/system/pulsebrief-backup.timer
systemctl daemon-reload
systemctl enable --now pulsebrief-backup.timer >/dev/null

cd "$release_root/deploy/production"
docker compose --env-file "$deploy_root/.env" build backend
docker compose --env-file "$deploy_root/.env" build admin
docker compose --env-file "$deploy_root/.env" up -d --remove-orphans

for attempt in $(seq 1 36); do
    if docker compose --env-file "$deploy_root/.env" exec -T backend wget -q -O /dev/null http://127.0.0.1:8080/api/health; then
        break
    fi
    if [ "$attempt" -eq 36 ]; then
        docker compose --env-file "$deploy_root/.env" ps
        docker compose --env-file "$deploy_root/.env" logs --tail=120 backend gateway
        exit 1
    fi
    sleep 5
done

docker compose --env-file "$deploy_root/.env" exec -T backend sh -eu -c '
payload=$(printf "{\"username\":\"%s\",\"password\":\"%s\"}" "$PULSEBRIEF_ADMIN_BOOTSTRAP_USERNAME" "$PULSEBRIEF_ADMIN_BOOTSTRAP_PASSWORD")
wget -q --post-data="$payload" --header="Content-Type: application/json" -O /dev/null http://127.0.0.1:8080/api/admin/auth/login
'
rm -rf "$remote_temp"
printf 'PulseBrief containers are healthy and Admin login was verified.\n'
'@
        $remoteScriptPath = Join-Path $tempRoot 'deploy-remote.sh'
        [System.IO.File]::WriteAllText(
            $remoteScriptPath,
            ($remoteScript -replace "`r`n", "`n"),
            [System.Text.UTF8Encoding]::new($false)
        )
        & scp @sshOptions $remoteScriptPath "${sshTarget}:${remoteTemp}/deploy-remote.sh"
        if ($LASTEXITCODE -ne 0) { throw 'Unable to upload the remote deployment runner.' }
        & ssh @sshOptions $sshTarget "bash '$remoteTemp/deploy-remote.sh' '$remoteTemp' '$releaseId'"
        if ($LASTEXITCODE -ne 0) { throw 'Remote deployment or internal smoke test failed.' }

        $runtimeLines = Get-Content -LiteralPath $EnvFile | Where-Object {
            $_ -notmatch '^PULSEBRIEF_ADMIN_BOOTSTRAP_(USERNAME|PASSWORD)='
        }
        $sanitizedEnv = Join-Path $tempRoot 'production-sanitized.env'
        [System.IO.File]::WriteAllLines($sanitizedEnv, $runtimeLines, [System.Text.UTF8Encoding]::new($false))
        & scp @sshOptions $sanitizedEnv "${sshTarget}:${remoteTemp}-sanitize.env"
        if ($LASTEXITCODE -ne 0) { throw 'Unable to upload the sanitized production environment.' }
        $sanitizeCommand = "install -m 600 '${remoteTemp}-sanitize.env' /opt/pulsebrief/.env && rm -f '${remoteTemp}-sanitize.env' && cd /opt/pulsebrief/repo/deploy/production && docker compose --env-file /opt/pulsebrief/.env up -d --force-recreate backend && for i in `$(seq 1 24); do docker compose --env-file /opt/pulsebrief/.env exec -T backend wget -q -O /dev/null http://127.0.0.1:8080/api/health && exit 0; sleep 5; done; exit 1"
        & ssh @sshOptions $sshTarget $sanitizeCommand
        if ($LASTEXITCODE -ne 0) { throw 'Admin bootstrap removal or backend restart failed.' }
        [System.IO.File]::WriteAllLines($EnvFile, $runtimeLines, [System.Text.UTF8Encoding]::new($false))

        & curl.exe --fail --silent --show-error --max-time 30 --output NUL 'https://huawj.com.cn/api/health'
        if ($LASTEXITCODE -ne 0) {
            throw 'Internal deployment succeeded, but the public HTTPS health check failed. Check Alibaba Cloud security-group ports 80/443 and DNS proxy settings.'
        }

        Write-Host 'Deployment complete: https://huawj.com.cn'
        Write-Host "Initial Admin credentials remain only in: $credentialFile"
        Write-Host 'The server bootstrap username/password were removed after login verification.'
    }
    finally {
        if (Test-Path -LiteralPath $tempRoot) { Remove-Item -LiteralPath $tempRoot -Recurse -Force }
    }
}
finally {
    Pop-Location
}
