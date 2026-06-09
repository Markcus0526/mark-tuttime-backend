# Disaster Recovery & Backup Runbook

Scope: the TutTime backend running on k3s (`tuttime` namespace). Covers PostgreSQL (per-service
databases), and recovery notes for Redis, Kafka, and stateless services.

## 1. What must be backed up

| Component | State | Backup strategy |
|-----------|-------|-----------------|
| PostgreSQL (10 per-service DBs) | **Critical, persistent** | Nightly `pg_dump` CronJob → PVC → off-site object storage |
| Redis | Cache + rate-limit + booking locks (rebuildable / short-lived) | No backup required; HA via replicas/Sentinel for availability |
| Kafka | Event log (in-flight) | No backup; rely on multi-broker replication (RF=3) for durability |
| Stateless services / api-gateway | None | Re-deployed from images; no state |
| Uploaded media (user/restaurant/menu `uploads` PVCs) | **Persistent** | Snapshot the PVCs or move to object storage/CDN (see review item) |

## 2. PostgreSQL backups

Logical backups are taken nightly via CronJob (template:
`k3s/infra/backup/postgres-backup-cronjob.yaml`, staging PVC: `backup-pvc.yaml`).

- One CronJob per database (replicate the template, changing name / `PGHOST` / `PGDATABASE` /
  secret). Each writes `${db}-<timestamp>.sql.gz` to the `postgres-backups` PVC and prunes files
  older than `RETENTION_DAYS` (default 7).
- These are **not** wired into `kustomization.yaml` by default — enable them once the off-site
  target below is configured, then add the files under the resources list.

### ⚠️ Off-site requirement (required for real DR)
A node-local PVC dies with the node. Backups MUST be copied off-cluster. Options:
- Add a sidecar/step that runs `aws s3 cp /backups/... s3://<bucket>/<db>/` (or `rclone`) after the dump.
- Or use a CSI volume-snapshot class with snapshots replicated to another zone.
Without this, the "backup" only protects against logical errors, not node/cluster loss.

## 3. Restore procedure (per database)

```sh
# 1. Pick the archive from the backups PVC (exec into any pod that mounts it, or copy out).
kubectl -n tuttime exec deploy/<any-pod-mounting-backups> -- ls -1 /backups

# 2. Scale the owning service down so nothing writes during restore.
kubectl -n tuttime scale deploy/user-service --replicas=0

# 3. Restore into the DB (drops/recreates objects from the dump).
gunzip -c /backups/user_service_db-<ts>.sql.gz | \
  kubectl -n tuttime exec -i deploy/user-service-db -- \
    psql -U postgres -d user_service_db

# 4. Scale the service back up and verify health.
kubectl -n tuttime scale deploy/user-service --replicas=2
kubectl -n tuttime rollout status deploy/user-service
```

Flyway note: restoring a dump also restores the `flyway_schema_history` table, so migrations
remain consistent. Do not re-run `clean`.

## 4. Full-cluster rebuild

1. Provision k3s; install prerequisites: cert-manager (for edge TLS) and the CSI/storage class.
2. Apply secrets/config (managed outside git — see secrets task).
3. `kubectl apply -k k3s/` — brings up infra, DBs (empty), services, ingress, NetworkPolicies.
4. Restore each database from the latest off-site dump (section 3).
5. Re-create Kafka topics if needed (`k3s/infra/kafka-topics-init.yaml`) — auto-create is disabled.
6. Verify `/health/readiness` on every service and an end-to-end booking→payment→order flow.

## 5. Targets (set per business requirement)

| Metric | Suggested target | Driver |
|--------|------------------|--------|
| RPO (max data loss) | ≤ 24h (nightly) → tighten to ≤ 1h with WAL archiving / more frequent dumps | backup frequency |
| RTO (time to restore) | ≤ 1h per DB from off-site dump | dump size + bandwidth |

To improve RPO below one day, move from nightly `pg_dump` to continuous WAL archiving
(e.g. pgBackRest / WAL-G) or a managed Postgres with point-in-time recovery.
