# Database HA (PostgreSQL)

Today each service runs a **single-instance Postgres `Deployment`** — no replica, no failover.
A naive fix ("StatefulSet with replicas: 2" of the stock image) does **not** work: the second pod
just starts an independent empty database, it does not replicate the primary. Real Postgres HA
needs streaming replication + automatic failover, which means a **StatefulSet-backed operator**.

Recommended: **CloudNativePG (CNPG)** — each `Cluster` is 1 primary + N hot standbys with
synchronous/async streaming replication, automatic failover, and built-in backup/PITR.

## Install the operator (one-time)
```sh
kubectl apply --server-side -f \
  https://raw.githubusercontent.com/cloudnative-pg/cloudnative-pg/release-1.24/releases/cnpg-1.24.0.yaml
kubectl -n cnpg-system rollout status deploy/cnpg-controller-manager
```

## Per-service Cluster
Template: `k3s/infra/db-ha/user-service-db-cluster.yaml` (PostGIS variant for user-service).
Replicate per DB, changing name / database / schema / credential secret. Vanilla services use
`ghcr.io/cloudnative-pg/postgresql:15` instead of the `-postgis` image.

CNPG expects an app credential secret in its format:
```sh
kubectl -n tuttime create secret generic user-service-db-app \
  --from-literal=username=postgres \
  --from-literal=password='<real-password>'
```

## Cutover (per service, one at a time)
1. Apply the `Cluster`; wait for 3/3 instances ready:
   `kubectl -n tuttime get cluster user-service-db` (status `Cluster in healthy state`).
2. **Migrate data** from the old Deployment DB into the new cluster primary:
   ```sh
   kubectl -n tuttime exec deploy/user-service-db -- pg_dump -U postgres user_service_db \
     | kubectl -n tuttime exec -i user-service-db-1 -- psql -U postgres -d user_service_db
   ```
   (Or use CNPG `bootstrap.initdb.import` / a monolith import for a one-shot migration.)
3. Point the service at the primary RW service — edit `user-service/configmap.yaml`:
   `DATABASE_URL: jdbc:postgresql://user-service-db-rw:5432/user_service_db?currentSchema=user_service&...`
4. Restart the service; verify health + a read/write path.
5. Decommission the old `db-deployment.yaml` / `db-pvc.yaml` / `db-service.yaml` (remove from kustomization)
   only after the cutover is verified.

## Failover validation
```sh
kubectl -n tuttime delete pod user-service-db-1     # if it's the primary
# CNPG promotes a standby within seconds; -rw service repoints automatically.
kubectl -n tuttime get cluster user-service-db -o wide
```

## Notes
- This is **not wired into kustomization** (the CRDs must exist first, or `kubectl apply -k` fails on
  unknown kind `Cluster`). Apply the operator, then the Cluster manifests, as a separate step.
- Backups: uncomment the `backup.barmanObjectStore` block + create the bucket creds for real
  off-site DR / point-in-time recovery (ties into docs/DISASTER_RECOVERY.md — CNPG supersedes the
  pg_dump CronJob once adopted).
- Alternatives: Zalando Patroni operator, or a managed service (RDS/Cloud SQL) — the per-service
  `DATABASE_URL` change is the same shape.
- ⚠️ Not runtime-validated here — exercise install, replication, and failover on a test cluster.
