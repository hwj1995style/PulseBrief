#!/usr/bin/env sh
set -eu

DEPLOY_ROOT="${PULSEBRIEF_DEPLOY_ROOT:-/opt/pulsebrief}"
BACKUP_ROOT="${PULSEBRIEF_BACKUP_ROOT:-/var/backups/pulsebrief}"
RETENTION_DAYS="${PULSEBRIEF_BACKUP_RETENTION_DAYS:-14}"
TIMESTAMP="$(date -u +%Y%m%dT%H%M%SZ)"
mkdir -p "$BACKUP_ROOT"

cd "$DEPLOY_ROOT/repo/deploy/production"
docker compose --env-file "$DEPLOY_ROOT/.env" exec -T mysql \
  sh -c 'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction --routines --triggers pulsebrief' \
  | gzip -9 > "$BACKUP_ROOT/pulsebrief-$TIMESTAMP.sql.gz"

find "$BACKUP_ROOT" -type f -name 'pulsebrief-*.sql.gz' -mtime "+$RETENTION_DAYS" -delete
sha256sum "$BACKUP_ROOT/pulsebrief-$TIMESTAMP.sql.gz" > "$BACKUP_ROOT/pulsebrief-$TIMESTAMP.sql.gz.sha256"
