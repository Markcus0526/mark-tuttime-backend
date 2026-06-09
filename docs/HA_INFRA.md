# HA Infrastructure: Kafka & Redis (rollout + validation)

⚠️ **These manifests render with `kubectl kustomize` but have NOT been runtime-validated on a
cluster.** KRaft-quorum and Sentinel-failover bring-up are timing/DNS-sensitive — apply to a
**test cluster first** and work through the validation checklist before production.

## What changed

### Kafka — single broker ➜ 3-broker KRaft cluster (SASL/SCRAM)
- `infra/kafka-cluster.yaml` — StatefulSet `tuttime-kafka-cluster` (3 brokers, per-pod node-id +
  advertised listener), headless service `kafka-headless`, client service `kafka` (`:9093`),
  per-broker PVC, `SASL_PLAINTEXT`/`SCRAM-SHA-256`, RF=3 / minISR=2, auto-create disabled.
- `infra/kafka-topics-init.yaml` — pre-creates all 14 topics **and** their `.DLT` dead-letter
  topics (the wired `DeadLetterPublishingRecoverer` uses the `.DLT` suffix) over SASL.
- **Removed:** `kafka-deployment.yaml`, `kafka-service.yaml`, `kafka-pvc.yaml` (single broker),
  and `kafka-users.yaml` (a Strimzi `KafkaUser` CRD that was incompatible with the manual broker).
- Services were already configured for this (each secret has `SPRING_KAFKA_PROPERTIES_SASL_JAAS_CONFIG`,
  configmaps set `SASL_PLAINTEXT` + bootstrap `kafka:9093`).

### Redis — 3 independent nodes ➜ 1 master + 2 replicas + 3 Sentinels
- `infra/redis-ha.yaml` — `tuttime-redis` StatefulSet (pod-0 master, others `replicaof` it),
  `tuttime-redis-sentinel` StatefulSet, and per-sentinel Services `tuttime-redis-sentinel-{1,2,3}`.
  Names match what 5 service configmaps already expected (`tuttime-redis-master` + those sentinel DNS names).
- **Removed:** `redis-deployment.yaml`, `redis-service.yaml`, `redis-pvc.yaml`.
- **api-gateway** configmap flipped from standalone `SPRING_DATA_REDIS_HOST` to the same Sentinel config.

### Shared infra now wired into kustomization
- `infra/global-infra-configmap.yaml` (`KAFKA_USERS_LIST`) and `infra/global-infra-secret.yaml`
  (`KAFKA_PASSWORDS_LIST`, `INFRA_REDIS_PASSWORD`).

## 🔑 Secret additions you must apply (you own secrets)
`global-infra-secret.yaml` / configmap now include placeholders that must be set to real values
**and kept consistent with the per-service secrets**:
- Added `notification-kafka-user` to `KAFKA_USERS_LIST` and `***REMOVED***` to
  `KAFKA_PASSWORDS_LIST` (notification-service is a Kafka consumer and was missing entirely).
  This must match `notification-service/secret.yaml`'s JAAS (`notification-kafka-user` /
  `***REMOVED***`).
- Added `KAFKA_INTER_BROKER_PASSWORD` (new inter-broker SCRAM account `inter_broker_user`).
- The lists are **positional**: `KAFKA_USERS_LIST[i]` ↔ `KAFKA_PASSWORDS_LIST[i]`. Keep order intact.

## Apply order (test cluster)
```sh
kubectl apply -k k3s/                 # global config/secret + redis-ha + kafka-cluster + everything
# Wait for infra to stabilise BEFORE services hammer it:
kubectl -n tuttime rollout status statefulset/tuttime-redis
kubectl -n tuttime rollout status statefulset/tuttime-redis-sentinel
kubectl -n tuttime rollout status statefulset/tuttime-kafka-cluster
kubectl -n tuttime wait --for=condition=complete job/kafka-topic-initializer --timeout=300s
```

## Validation checklist
**Kafka**
- [ ] All 3 broker pods `Running` and `Ready`.
- [ ] `kafka-topic-initializer` Job `Complete`; `kafka-topics.sh --list` (with SASL client config) shows 14 topics + 14 `.DLT`.
- [ ] A service produces/consumes (e.g. trigger a booking→payment→order flow); no SASL auth errors in logs.
- [ ] Kill one broker pod → cluster stays available (RF=3, minISR=2).

**Redis**
- [ ] `tuttime-redis-0` is master, `-1`/`-2` are replicas: `redis-cli -a <pw> info replication`.
- [ ] Each sentinel reports the master: `redis-cli -p 26379 -a <pw> sentinel master tuttime-redis-master`.
- [ ] Delete `tuttime-redis-0` → Sentinel promotes a replica within `down-after`+`failover-timeout`; gateway auth / rate-limit keep working.

## Known caveats / follow-ups
- **kafka-ui** (`infra/kafka-ui-*.yaml`) still points at the broker without SASL creds, so it will
  fail to connect until you add SASL client properties to it (non-critical dev tool).
- **Sentinel master seed**: sentinels seed the master at `tuttime-redis-0`. After a failover the
  Sentinels track the new master automatically; on full restart, ensure `tuttime-redis-0` rejoins
  (as replica if it was demoted) — Sentinel reconciles. Validate this explicitly.
- Consider the **Bitnami Helm charts** (Kafka + Redis sentinel) if Helm is acceptable — lower-risk
  than hand-rolled StatefulSets for these two.
- Default credentials in `global-infra-secret.yaml` are part of the **secrets-out-of-git** task you own.
